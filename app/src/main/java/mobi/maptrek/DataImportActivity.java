package mobi.maptrek;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import mobi.maptrek.io.GPXManager;
import mobi.maptrek.io.KMLManager;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.MonitoredInputStream;
import mobi.maptrek.util.ProgressHandler;
import mobi.maptrek.util.ProgressListener;

public class DataImportActivity extends Activity {
    private static final String TAG = "DataImportActivity";
    private static final String DATA_IMPORT_FRAGMENT = "dataImportFragment";
    private static final DateFormat SUFFIX_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());

    private DataImportFragment mDataImportFragment;
    private ImportProgressHandler mProgressHandler;

    private TextView mFileNameView;
    private ProgressBar mProgressBar;
    private Button mActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_data_import);

        mFileNameView = (TextView) findViewById(R.id.filename);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mActionButton = (Button) findViewById(R.id.action);

        FragmentManager fm = getFragmentManager();
        mDataImportFragment = (DataImportFragment) fm.findFragmentByTag(DATA_IMPORT_FRAGMENT);
        if (mDataImportFragment == null) {
            mDataImportFragment = new DataImportFragment();
            fm.beginTransaction().add(mDataImportFragment, DATA_IMPORT_FRAGMENT).commit();
            mDataImportFragment.setIntent(getIntent());
        }
        mProgressHandler = new ImportProgressHandler(mProgressBar);
        mDataImportFragment.setProgressHandler(mProgressHandler);

        mActionButton.setText(R.string.cancel);
        mActionButton.setTag(false);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean finished = (boolean) mActionButton.getTag();
                if (finished) {
                    Intent iLaunch = new Intent(Intent.ACTION_MAIN);
                    iLaunch.addCategory(Intent.CATEGORY_LAUNCHER);
                    iLaunch.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
                    iLaunch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(iLaunch);
                } else {
                    mDataImportFragment.stopImport();
                }
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressHandler = null;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("filename", mFileNameView.getText().toString());
        savedInstanceState.putInt("progressBarMax", mProgressBar.getMax());
        savedInstanceState.putInt("progressBarProgress", mProgressBar.getProgress());
        savedInstanceState.putString("action", mActionButton.getText().toString());
        savedInstanceState.putBoolean("finished", (Boolean) mActionButton.getTag());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mFileNameView.setText(savedInstanceState.getString("filename"));
        mProgressBar.setMax(savedInstanceState.getInt("progressBarMax"));
        mProgressBar.setProgress(savedInstanceState.getInt("progressBarProgress"));
        mActionButton.setText(savedInstanceState.getString("action"));
        mActionButton.setTag(savedInstanceState.getBoolean("finished"));
    }

    @SuppressLint("HandlerLeak")
    public class ImportProgressHandler extends ProgressHandler {
        public ImportProgressHandler(ProgressBar progressBar) {
            super(progressBar);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == STOP_PROGRESS) {
                mFileNameView.setText(R.string.msgDataImported);
                mProgressBar.setVisibility(View.GONE);
                mActionButton.setText(R.string.startApplication);
                mActionButton.setTag(true);
            }
        }

        @Override
        public void onProgressAnnotated(final String annotation) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFileNameView.setText(annotation);
                }
            });
        }
    }

    public static class DataImportFragment extends Fragment {
        private HandlerThread mBackgroundThread;
        private Handler mBackgroundHandler;
        private MonitoredInputStream mInputStream;
        private Intent mIntent;
        private ProgressListener mProgressListener;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);

            mBackgroundThread = new HandlerThread("BackgroundThread");
            mBackgroundThread.setPriority(Thread.MIN_PRIORITY);
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }

        @Override
        public void onStart() {
            super.onStart();
            if (mIntent != null) {
                processIntent(mIntent);
                mIntent = null;
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mProgressListener = null;
        }

        public void setIntent(Intent intent) {
            mIntent = intent;
        }

        public void setProgressHandler(ProgressListener listener) {
            mProgressListener = listener;
        }

        private void processIntent(final Intent intent) {
            // It appears to be fast enough but I will live it in separate background thread
            final Message m = Message.obtain(mBackgroundHandler, new Runnable() {
                @Override
                public void run() {
                    String action = intent.getAction();
                    String type = intent.getType();
                    Log.e(TAG, "Action: " + action);
                    Log.e(TAG, "Type: " + type);

                    if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_VIEW.equals(action)) {
                        handleSendImage(intent);
                    } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                        handleSendMultipleImages(intent);
                    }
                }
            });
            mBackgroundHandler.sendMessage(m);
        }

        private void handleSendImage(Intent intent) {
            Uri streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (streamUri != null) {
                readFile(streamUri);
            } else {
                readFile(intent.getData());
            }
        }

        private void handleSendMultipleImages(Intent intent) {
            ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (imageUris != null) {
                // Update UI to reflect multiple images being shared
            }
        }

        private void readFile(Uri uri) {
            Context context = getContext();
            Log.e(TAG, uri.toString());
            String scheme = uri.getScheme();
            if ("file".equals(scheme)) {
                String path = uri.getPath();
                File src = new File(path);
                String name = uri.getLastPathSegment();
                long size = src.length();
                Log.e(TAG, "file: " + name + " [" + size + "]");
                File dst = getDestinationFile(name);
                try {
                    FileUtils.copyFile(src, dst);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                ContentResolver resolver = context.getContentResolver();
                Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
                String name = null;
                long length = -1;
                if (cursor != null) {
                    Log.e(TAG, "   from cursor");
                    cursor.moveToFirst();
                    name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    length = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                    cursor.close();
                } else {
                    try {
                        AssetFileDescriptor afd = resolver.openAssetFileDescriptor(uri, "r");
                        if (afd != null)
                            length = afd.getLength();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "Length: " + length);
                }
                //TODO This is just to not crash, should be correctly implemented
                if (name == null)
                    name = uri.getLastPathSegment();

                Log.e(TAG, "Import: [" + name + "][" + length + "]");

                if (!name.endsWith(KMLManager.EXTENSION) && !name.endsWith(GPXManager.EXTENSION)) {
                    Log.e(TAG, "Unsupported file format");
                    return;
                }

                mProgressListener.onProgressStarted((int) length);
                mProgressListener.onProgressAnnotated(name);
                File dst = null;
                try {
                    dst = getDestinationFile(name);

                    mInputStream = new MonitoredInputStream(resolver.openInputStream(uri));
                    mInputStream.addChangeListener(new MonitoredInputStream.ChangeListener() {
                        @Override
                        public void stateChanged(long location) {
                            if (mProgressListener != null) {
                                //TODO Divide progress by 1024
                                mProgressListener.onProgressChanged((int) location);
                            }
                        }
                    });

                    FileUtils.copyStreamToFile(mInputStream, dst);
                    mProgressListener.onProgressFinished();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (dst != null && dst.exists())
                        //noinspection ResultOfMethodCallIgnored
                        dst.delete();
                }
            } else {
                Log.e(TAG, "Unsupported transfer method");
            }
        }

        @Nullable
        private File getDestinationFile(String filename) {
            File dir = getContext().getExternalFilesDir("data");
            if (dir == null) {
                Log.e(TAG, "Data path unavailable");
                return null;
            }
            File destination = new File(dir, filename);
            if (destination.exists()) {
                String ext = filename.substring(filename.lastIndexOf("."));
                filename = filename.replace(ext, "-" + SUFFIX_FORMAT.format(new Date()) + ext);
                destination = new File(dir, filename);
            }
            return destination;
        }

        public void stopImport() {
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
