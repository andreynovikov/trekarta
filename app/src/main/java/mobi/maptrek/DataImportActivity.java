/*
 * Copyright 2021 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import mobi.maptrek.io.GPXManager;
import mobi.maptrek.io.KMLManager;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.MonitoredInputStream;
import mobi.maptrek.util.ProgressHandler;
import mobi.maptrek.util.ProgressListener;

public class DataImportActivity extends Activity {
    private static final Logger logger = LoggerFactory.getLogger(DataImportActivity.class);
    private static final String DATA_IMPORT_FRAGMENT = "dataImportFragment";
    private static final DateFormat SUFFIX_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT);
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private DataImportFragment mDataImportFragment;
    private ImportProgressHandler mProgressHandler;

    private TextView mFileNameView;
    private ProgressBar mProgressBar;
    private Button mActionButton;

    private Runnable mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_data_import);

        mFileNameView = findViewById(R.id.filename);
        mProgressBar = findViewById(R.id.progressBar);
        mActionButton = findViewById(R.id.action);

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
        mActionButton.setOnClickListener(v -> {
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
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mFileNameView.setText(savedInstanceState.getString("filename"));
        mProgressBar.setMax(savedInstanceState.getInt("progressBarMax"));
        mProgressBar.setProgress(savedInstanceState.getInt("progressBarProgress"));
        mActionButton.setText(savedInstanceState.getString("action"));
        mActionButton.setTag(savedInstanceState.getBoolean("finished"));
    }

    private void askForPermission(Runnable task) {
        mTask = task;
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                String name;
                try {
                    PackageManager pm = getPackageManager();
                    PermissionInfo permissionInfo = pm.getPermissionInfo(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.GET_META_DATA);
                    name = (String) permissionInfo.loadLabel(pm);
                } catch (PackageManager.NameNotFoundException e) {
                    logger.error("Failed to obtain name for permission", e);
                    name = "read external storage";
                }
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.msgReadExternalStorageRationale, name))
                        .setPositiveButton(R.string.ok, (dialog, which) -> requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE))
                        .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
                        .create()
                        .show();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }

        } else {
            mDataImportFragment.startImport(mTask);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mTask != null)
                        mDataImportFragment.startImport(mTask);
                } else {
                    finish();
                }
                // return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @SuppressLint("HandlerLeak")
    private class ImportProgressHandler extends ProgressHandler {
        ImportProgressHandler(ProgressBar progressBar) {
            super(progressBar);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == STOP_PROGRESS) {
                mFileNameView.setText(R.string.msgDataImported);
                mProgressBar.setVisibility(View.GONE);
                mActionButton.setText(MapTrek.isMainActivityRunning ? R.string.ok : R.string.startApplication);
                mActionButton.setTag(true);
            }
        }

        @Override
        public void onProgressAnnotated(final String annotation) {
            runOnUiThread(() -> mFileNameView.setText(annotation));
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            mFileNameView.setText(message);
            mProgressBar.setVisibility(View.GONE);
            mActionButton.setText(R.string.close);
            mActionButton.setTag(false);
        });
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

            mBackgroundThread = new HandlerThread("DataImportThread");
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

        @Override
        public void onDestroy() {
            super.onDestroy();
            mBackgroundThread.interrupt();
            mBackgroundHandler.removeCallbacksAndMessages(null);
            mBackgroundThread.quit();
            mBackgroundThread = null;
        }

        public void setIntent(Intent intent) {
            mIntent = intent;
        }

        public void setProgressHandler(ProgressListener listener) {
            mProgressListener = listener;
        }

        private void processIntent(final Intent intent) {
            String action = intent.getAction();
            String type = intent.getType();
            logger.debug("Action: {}", action);
            logger.debug("Type: {}", type);
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_VIEW.equals(action)) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri == null)
                    uri = intent.getData();
                logger.debug("Uri: {}", uri.toString());
                logger.debug("Authority: {}", uri.getAuthority());
                final Uri finalUri = uri;
                Runnable task = () -> readFile(finalUri);
                String scheme = uri.getScheme();
                if ("file".equals(scheme)) {
                    ((DataImportActivity) getActivity()).askForPermission(task);
                } else {
                    startImport(task);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                startImport(() -> {
                    ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    //FIXME Not tested at all!
                    if (uris != null) {
                        for (Uri uri : uris)
                            readFile(uri);
                    }
                });
            }
        }

        private void readFile(Uri uri) {
            DataImportActivity activity = (DataImportActivity) getActivity();

            String name = null;
            long length = -1;

            String scheme = uri.getScheme();
            if ("file".equals(scheme)) {
                String path = uri.getPath();
                File src = new File(path);
                name = uri.getLastPathSegment();
                length = src.length();
                try {
                    mInputStream = new MonitoredInputStream(new FileInputStream(src));
                } catch (FileNotFoundException e) {
                    logger.error("Failed to get imported file stream", e);
                    activity.showError(getString(R.string.msgFailedToGetFile));
                    return;
                }
                logger.debug("file: {} [{}]", name, length);
            } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                ContentResolver resolver = activity.getContentResolver();
                Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
                if (cursor != null) {
                    logger.debug("   from cursor");
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        length = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                    }
                    cursor.close();
                }
                if (name == null) {
                    cursor = resolver.query(uri, new String[]{"_data"}, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                    }
                    if (cursor != null)
                        cursor.close();
                }
                if (length == -1) {
                    try {
                        AssetFileDescriptor afd = resolver.openAssetFileDescriptor(uri, "r");
                        if (afd != null)
                            length = afd.getLength();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                //TODO Find out how to get file name from chrome
                if (name == null) {
                    logger.error("Failed to get file name");
                    activity.showError(getString(R.string.msgFailedToGetFileName));
                    return;
                }

                try {
                    mInputStream = new MonitoredInputStream(resolver.openInputStream(uri));
                } catch (FileNotFoundException e) {
                    logger.error("Failed to get imported file stream", e);
                    activity.showError(getString(R.string.msgFailedToGetFile));
                    return;
                }
                logger.debug("Import: [{}][{}]", name, length);
            } else {
                logger.warn("Unsupported transfer method");
                activity.showError(getString(R.string.msgFailedToGetFile));
                return;
            }

            if (!name.endsWith(TrackManager.EXTENSION) &&
                    !name.endsWith(KMLManager.EXTENSION) &&
                    !name.endsWith(KMLManager.ZIP_EXTENSION) &&
                    !name.endsWith(GPXManager.EXTENSION) &&
                    !name.endsWith(".mbtiles") &&
                    !name.endsWith(".sqlitedb")) {
                logger.warn("Unsupported file format");
                activity.showError(getString(R.string.msgUnsupportedFileFormat));
                return;
            }

            mProgressListener.onProgressStarted((int) length);
            mProgressListener.onProgressAnnotated(name);
            File dst = null;
            try {
                dst = getDestinationFile(name);

                mInputStream.addChangeListener(location -> {
                    if (mProgressListener != null) {
                        //TODO Divide progress by 1024
                        mProgressListener.onProgressChanged((int) location);
                    }
                });

                FileUtils.copyStreamToFile(mInputStream, dst);
                mProgressListener.onProgressFinished();
            } catch (IOException e) {
                e.printStackTrace();
                if (dst != null && dst.exists())
                    //noinspection ResultOfMethodCallIgnored
                    dst.delete();
                activity.showError(getString(R.string.msgFailedToGetFile));
            }
        }

        @Nullable
        private File getDestinationFile(String filename) {
            boolean isMap = filename.endsWith(".mbtiles") || filename.endsWith(".sqlitedb");
            File dir = MapTrek.getApplication().getExternalDir(isMap ? "maps" : "data");
            if (dir == null) {
                logger.error("Path for {} unavailable", isMap ? "maps" : "data");
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

        public void startImport(Runnable task) {
            final Message m = Message.obtain(mBackgroundHandler, task);
            mBackgroundHandler.sendMessage(m);
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
