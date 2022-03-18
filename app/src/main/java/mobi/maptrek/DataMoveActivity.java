/*
 * Copyright 2022 Andrey Novikov
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.MonitoredInputStream;
import mobi.maptrek.util.ProgressHandler;
import mobi.maptrek.util.ProgressListener;

public class DataMoveActivity extends Activity {
    private static final Logger logger = LoggerFactory.getLogger(DataMoveActivity.class);
    private static final String DATA_MOVE_FRAGMENT = "dataMoveFragment";
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private static final String[] directories = new String[]{"data", "databases", "maps", "native"};

    private DataMoveFragment mDataMoveFragment;
    private MoveProgressHandler mProgressHandler;

    private TextView mFileNameView;
    private ProgressBar mProgressBar;
    private Button mActionButton;
    private static boolean mHasFolderPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_data_import);

        mFileNameView = findViewById(R.id.filename);
        mProgressBar = findViewById(R.id.progressBar);
        mActionButton = findViewById(R.id.action);

        FragmentManager fm = getFragmentManager();
        mDataMoveFragment = (DataMoveFragment) fm.findFragmentByTag(DATA_MOVE_FRAGMENT);
        if (mDataMoveFragment == null) {
            mDataMoveFragment = new DataMoveFragment();
            fm.beginTransaction().add(mDataMoveFragment, DATA_MOVE_FRAGMENT).commit();
        }
        mProgressHandler = new MoveProgressHandler(mProgressBar);
        mDataMoveFragment.setProgressHandler(mProgressHandler);

        mActionButton.setText(R.string.cancel);
        mActionButton.setTag(false);
        mActionButton.setOnClickListener(v -> {
            boolean finished = (boolean) mActionButton.getTag();
            if (!finished)
                mDataMoveFragment.stopDataMove();
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

    private void askForPermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                String name;
                try {
                    PackageManager pm = getPackageManager();
                    PermissionInfo permissionInfo = pm.getPermissionInfo(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.GET_META_DATA);
                    name = (String) permissionInfo.loadLabel(pm);
                } catch (PackageManager.NameNotFoundException e) {
                    logger.error("Failed to obtain name for permission", e);
                    name = "read external storage";
                }
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.msgWriteExternalStorageRationale, name))
                        .setPositiveButton(R.string.ok, (dialog, which) -> requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE))
                        .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
                        .create()
                        .show();
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }

        } else {
            mDataMoveFragment.startDataMove(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mDataMoveFragment.startDataMove(null);
            } else {
                Configuration.setNewExternalStorage(null);
                finish();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void askForFolderPermission() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(new File(Configuration.getExternalStorage())));
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            mHasFolderPermission = true;
            if (resultData != null)
                mDataMoveFragment.startDataMove(resultData.getData());
        }
    }

    @SuppressLint("HandlerLeak")
    private class MoveProgressHandler extends ProgressHandler {
        MoveProgressHandler(ProgressBar progressBar) {
            super(progressBar);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == STOP_PROGRESS) {
                Bundle data = msg.getData();
                for (String dir : directories) {
                    File sourcePath = new File(data.getString("source"), dir);
                    emptyDirectory(sourcePath);
                }
                if (MapTrek.getApplication().getExternalDirectory().getAbsolutePath().equals(Configuration.getExternalStorage()))
                    //noinspection ResultOfMethodCallIgnored,ConstantConditions
                    new File(data.getString("source")).delete();
                // save new data source
                if (MainActivity.NEW_APPLICATION_STORAGE.equals(Configuration.getNewExternalStorage()))
                    Configuration.setExternalStorage(null);
                else
                    Configuration.setExternalStorage(data.getString("destination"));
                Configuration.setNewExternalStorage(null);
                Configuration.commit();
                mProgressBar.setVisibility(View.GONE);
                mActionButton.setVisibility(View.GONE);
                mFileNameView.setText(R.string.finishing);
                if (Configuration.getExternalStorage() == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DataMoveActivity.this);
                    builder.setTitle(R.string.actionMoveData);
                    builder.setMessage(R.string.msgMoveDataToApplicationCheckExplanation);
                    builder.setPositiveButton(R.string.actionContinue, (dialog, which) -> {
                        restartMainActivity();
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    restartMainActivity();
                }
            }
        }

        @Override
        public void onProgressAnnotated(final String annotation) {
            runOnUiThread(() -> mFileNameView.setText(annotation));
        }
    }

    private void restartMainActivity() {
        Intent iLaunch = new Intent(Intent.ACTION_MAIN);
        iLaunch.addCategory(Intent.CATEGORY_LAUNCHER);
        iLaunch.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
        iLaunch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(iLaunch);
        finish();
    }

    private void setFinished() {
        mActionButton.setTag(true);
    }

    private void showError(final String message) {
        runOnUiThread(() -> {
            mFileNameView.setText(message);
            mProgressBar.setVisibility(View.GONE);
            mActionButton.setText(R.string.close);
            mActionButton.setTag(false);
        });
    }

    public static class DataMoveFragment extends Fragment {
        private HandlerThread mBackgroundThread;
        private Handler mBackgroundHandler;
        private MonitoredInputStream mInputStream;
        private ProgressListener mProgressListener;
        private int mProgress;
        private int mDivider = 1;
        private File mDestination;
        private final Object lock = new Object();
        private boolean mStopped;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);

            mBackgroundThread = new HandlerThread("DataMoveThread");
            mBackgroundThread.setPriority(Thread.MAX_PRIORITY);
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            logger.error("onAttach context");
            DataMoveActivity activity = (DataMoveActivity) getActivity();
            if (MainActivity.NEW_EXTERNAL_STORAGE.equals(Configuration.getNewExternalStorage())) {
                activity.askForPermission();
            } else if (MainActivity.NEW_APPLICATION_STORAGE.equals(Configuration.getNewExternalStorage()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !mHasFolderPermission) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.actionMoveData);
                builder.setMessage(R.string.msgMoveDataToApplicationStoragePermissionExplanation);
                builder.setPositiveButton(R.string.actionContinue, (dialog, which) -> activity.askForFolderPermission());
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                startDataMove(null);
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

        public void setProgressHandler(ProgressListener listener) {
            mProgressListener = listener;
        }

        private void moveData(Uri uri) {
            MapTrek application = MapTrek.getApplication();
            DataMoveActivity activity = (DataMoveActivity) getActivity();

            mStopped = false;

            String currentStorage = Configuration.getExternalStorage();
            File source;
            if (currentStorage != null)
                source = new File(currentStorage);
            else
                source = application.getExternalFilesDir(null);

            boolean moveHome = false;
            String newStorage = Configuration.getNewExternalStorage();
            if (MainActivity.NEW_SD_STORAGE.equals(newStorage)) {
                mDestination = application.getSDCardDirectory();
            } else if (MainActivity.NEW_EXTERNAL_STORAGE.equals(newStorage)) {
                mDestination = application.getExternalDirectory();
            } else {
                mDestination = application.getExternalFilesDir(null);
                moveHome = true;
            }

            if (mDestination == null) {
                activity.showError(getString(R.string.msgFailedToMoveData, getString(R.string.msgCannotCreateDestinationFolder)));
                return;
            }

            for (String dir : directories) {
                File destinationDir = new File(mDestination, dir);
                if (moveHome) {
                    // remove everything
                    emptyDirectory(destinationDir);
                } else {
                    // check if mDestination is empty to prevent data overwrite
                    if (destinationDir.isDirectory() && destinationDir.list().length > 0) {
                        activity.showError(getString(R.string.msgFailedToMoveData, getString(R.string.msgDestinationFolderIsNotEmpty)));
                        return;
                    }
                    if (!destinationDir.exists() && !destinationDir.mkdirs()) {
                        logger.error("Failed to create dir {}", destinationDir.getAbsolutePath());
                        activity.showError(getString(R.string.msgFailedToMoveData, getString(R.string.msgCannotCreateDestinationFolder)));
                        return;
                    }
                }
            }

            // check if there is enough empty space
            long size = 0L;
            if (uri != null) {
                Uri rootUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
                size = getDirectoryEntriesSize(rootUri);
            } else {
                for (String dir : directories) {
                    File sourcePath = new File(source, dir);
                    size += getDirectorySize(sourcePath);
                }
            }

            logger.error("Required space: {}", size);
            if (mDestination.getUsableSpace() < size * 1.2) {
                logger.error("No space: {}", mDestination.getUsableSpace());
                activity.showError(getString(R.string.msgFailedToMoveData, getString(R.string.msgNotEnoughSpaceOnDestinationStorage)));
                return;
            }

            if (size > Integer.MAX_VALUE) {
                mDivider = 8;
                size = size >> mDivider;
            }

            // copy files
            mProgress = 0;
            mProgressListener.onProgressStarted((int) size);
            mProgressListener.onProgressAnnotated(getString(R.string.msgMovingFiles));

            try {
                if (uri != null) {
                    Uri rootUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
                    copyDirectoryEntries(rootUri, mDestination);
                } else {
                    for (String dir : directories) {
                        File sourcePath = new File(source, dir);
                        if (sourcePath.exists())
                            copyDirectory(sourcePath, new File(mDestination, dir));
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to move data", e);
                activity.showError(getString(R.string.msgFailedToMoveData, getString(R.string.msgFileMoveError)));
                return;
            }

            synchronized (lock) {
                if (mStopped)
                    return;
                activity.setFinished();
                if (mProgressListener != null) {
                    Bundle data = new Bundle(2);
                    data.putString("source", source.getAbsolutePath());
                    data.putString("destination", mDestination.getAbsolutePath());
                    mProgressListener.onProgressFinished(data);
                }
            }
        }

        public void startDataMove(Uri uri) {
            final Message m = Message.obtain(mBackgroundHandler, () -> this.moveData(uri));
            mBackgroundHandler.sendMessage(m);
        }

        public void stopDataMove() {
            synchronized (lock) {
                mStopped = true;
                if (mInputStream != null) {
                    try {
                        mInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // FIXME Stop on destination not empty - still removing destination
                // if (mDestination != null)
                //     emptyDirectory(mDestination);
                Configuration.setNewExternalStorage(null);
                Configuration.commit();
            }
        }

        private void copyDirectory(File source, File destination) throws IOException {
            if (mStopped)
                return;

            if (source.isDirectory()) {
                if (!destination.exists() && !destination.mkdirs()) {
                    throw new IOException("Cannot create dir " + destination.getAbsolutePath());
                }

                String[] children = source.list();
                if (children != null)
                    for (String child : children)
                        copyDirectory(new File(source, child), new File(destination, child));
            } else {
                // make sure the directory we plan to store the recording in exists
                File directory = destination.getParentFile();
                if (directory != null && !directory.exists() && !directory.mkdirs()) {
                    throw new IOException("Cannot create dir " + directory.getAbsolutePath());
                }

                logger.debug("Copy: [{}][{}]", source.getAbsolutePath(), destination.getAbsolutePath());

                try {
                    mInputStream = new MonitoredInputStream(new FileInputStream(source));

                    mInputStream.addChangeListener(location -> {
                        if (mProgressListener != null) {
                            int progress = mDivider > 1 ? (int) location >> mDivider : (int) location;
                            mProgressListener.onProgressChanged(mProgress + progress);
                        }
                    });

                    FileUtils.copyStreamToFile(mInputStream, destination);
                    mProgress += source.length();
                } catch (java.io.FileNotFoundException ignore) {
                    logger.error("No file");
                }
            }
        }

        long getDirectoryEntriesSize(Uri childrenUri) {
            ContentResolver contentResolver = getActivity().getContentResolver();
            long totalSize = 0L;
            Cursor c = contentResolver.query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
            }, null, null, null);
            try {
                while (c != null && c.moveToNext()) {
                    if (mStopped)
                        break;
                    final String docId = c.getString(0);
                    final String mime = c.getString(1);
                    final int size = c.getInt(2);
                    if (isDirectory(mime)) {
                        final Uri childrenNode = DocumentsContract.buildChildDocumentsUriUsingTree(childrenUri, docId);
                        totalSize += getDirectoryEntriesSize(childrenNode);
                    } else {
                        totalSize += size;
                    }
                }
            } finally {
                closeQuietly(c);
            }
            return totalSize;
        }

        void copyDirectoryEntries(Uri childrenUri, File destination) throws IOException {
            ContentResolver contentResolver = getActivity().getContentResolver();

            Cursor c = contentResolver.query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
            }, null, null, null);
            try {
                while (c != null && c.moveToNext()) {
                    if (mStopped)
                        break;
                    final String docId = c.getString(0);
                    final String name = c.getString(1);
                    final String mime = c.getString(2);
                    final int size = c.getInt(3);
                    File destinationChild = new File(destination, name);
                    if (isDirectory(mime)) {
                        if (!destinationChild.exists() && !destinationChild.mkdirs()) {
                            throw new IOException("Cannot create dir " + destinationChild.getAbsolutePath());
                        }
                        final Uri childrenNode = DocumentsContract.buildChildDocumentsUriUsingTree(childrenUri, docId);
                        copyDirectoryEntries(childrenNode, destinationChild);
                    } else {
                        final Uri node = DocumentsContract.buildDocumentUriUsingTree(childrenUri, docId);
                        logger.debug("Copy: [{}][{}]", node, destinationChild.getAbsolutePath());

                        mInputStream = new MonitoredInputStream(contentResolver.openInputStream(node));

                        mInputStream.addChangeListener(location -> {
                            if (mProgressListener != null) {
                                int progress = mDivider > 1 ? (int) location >> mDivider : (int) location;
                                mProgressListener.onProgressChanged(mProgress + progress);
                            }
                        });

                        FileUtils.copyStreamToFile(mInputStream, destinationChild);
                        mProgress += size;
                    }
                }
            } finally {
                closeQuietly(c);
            }
        }

        // Util method to check if the mime type is a directory
        private static boolean isDirectory(String mimeType) {
            return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
        }

        // Util method to close a closeable
        private static void closeQuietly(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception ignore) {
                    // ignore exception
                }
            }
        }
    }

    private static long getDirectorySize(File source) {
        long size = 0L;
        if (source.isDirectory()) {
            File[] children = source.listFiles();
            if (children != null)
                for (File child : children)
                    size += getDirectorySize(child);
        } else {
            size = source.length();
        }
        return size;
    }

    private static void emptyDirectory(File source) {
        if (source.isDirectory()) {
            String[] children = source.list();
            if (children != null)
                for (String child : children)
                    emptyDirectory(new File(source, child));
        }
        logger.debug("Delete: [{}]", source.getAbsolutePath());
        //noinspection ResultOfMethodCallIgnored
        source.delete();
    }
}
