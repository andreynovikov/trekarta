package mobi.maptrek.io;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import mobi.maptrek.data.DataSource;
import mobi.maptrek.util.FileUtils;

public abstract class Manager {
    private static final String TAG = "IO Manager";
    private Context mContext;

    /**
     * Returns manager for specified file. Manager is selected by file extension. No file format
     * check is performed. Returns <code>null</code> for unknown file formats.
     *
     * @param file File name
     * @return File read and write manager
     */
    @Nullable
    public static Manager getDataManager(Context context, String file) {
        if (file.toLowerCase().endsWith(GPXManager.EXTENSION)) {
            return new GPXManager().setContext(context);
        }
        if (file.toLowerCase().endsWith(KMLManager.EXTENSION)) {
            return new KMLManager().setContext(context);
        }
        return null;
    }

    /**
     * Returns manager for specified data source. Manager is selected by source file extension.
     * No file format check is performed. Returns GPX manager if source has no associated file.
     * Returns <code>null</code> for unknown file formats.
     *
     * @param source Data source
     * @return File read and write manager
     */
    @Nullable
    private static Manager getDataManager(Context context, DataSource source) {
        // GPX is the default manager
        if (source.path == null)
            return new GPXManager().setContext(context);
        if (source.path.toLowerCase().endsWith(GPXManager.EXTENSION)) {
            return new GPXManager().setContext(context);
        }
        if (source.path.toLowerCase().endsWith(KMLManager.EXTENSION)) {
            return new KMLManager().setContext(context);
        }
        return null;
    }

    public static void save(Context context, DataSource source) {
        save(context, source, null);
    }

    public static void save(Context context, DataSource source, OnSaveListener saveListener) {
        save(context, source, saveListener, null);
    }

    public static void save(Context context, DataSource source, OnSaveListener saveListener, ProgressListener progressListener) {
        Manager manager = Manager.getDataManager(context, source);
        assert manager != null : "Failed to get IO manager for " + source.path;
        manager.saveData(source, saveListener, progressListener);
    }

    /**
     * Loads data from file (input stream). File name is used only for reference.
     * @param inputStream <code>InputStream</code> with waypoints
     * @param fileName File name associated with that <code>InputStream</code>
     * @return <code>DataSource</code> filled with file data
     * @throws Exception if IO or parsing error occurred
     */
    @NonNull
    public abstract DataSource loadData(InputStream inputStream, String fileName) throws Exception;

    public abstract void saveData(OutputStream outputStream, DataSource source, @Nullable ProgressListener progressHandler) throws Exception;

    /**
     * Returns file extension with leading dot.
     */
    @NonNull
    public abstract String getExtension();

    protected final void saveData(DataSource source, @Nullable OnSaveListener saveListener, @Nullable ProgressListener progressListener) {
        File file;
        if (source.path == null) {
            file = mContext.getExternalFilesDir("data");
            file = new File(file, "data_source_" + System.currentTimeMillis() + getExtension());
        } else {
            file = new File(source.path);
        }
        new Thread(new SaveRunnable(file, source, saveListener, progressListener)).start();
    }

    private class SaveRunnable implements Runnable {
        private final File mFile;
        private final DataSource mDataSource;
        private final OnSaveListener mSaveListener;
        private final ProgressListener mProgressListener;

        SaveRunnable(final File file, final DataSource source, @Nullable final OnSaveListener saveListener, @Nullable final ProgressListener progressListener) {
            this.mFile = file;
            this.mDataSource = source;
            this.mSaveListener = saveListener;
            this.mProgressListener = progressListener;
        }

        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                Log.i(TAG, "Saving data source...");
                // Save to temporary file for two reasons: to preserve original file in case of error
                // and to hide it from data loader until it is completely saved
                File newFile = new File(mFile.getParent(), System.currentTimeMillis() + ".tmp");
                Manager.this.saveData(new FileOutputStream(newFile, false), mDataSource, mProgressListener);
                String newName = mDataSource.getNewName();
                File saveFile = mFile;
                if (newName != null)
                    saveFile = new File(mFile.getParent(), FileUtils.sanitizeFilename(newName) + Manager.this.getExtension());
                if (saveFile.exists() && !saveFile.delete() || !newFile.renameTo(saveFile)) {
                    Log.e(TAG, "Can not rename data source file after save");
                    if (mSaveListener != null)
                        mSaveListener.onError(mDataSource, new Exception("Can not rename data source file after save"));
                } else {
                    mDataSource.path = saveFile.getAbsolutePath();
                    if (mSaveListener != null)
                        mSaveListener.onSaved(mDataSource);
                    Log.i(TAG, "Done");
                }
            } catch (Exception e) {
                Log.e(TAG, "Can not save data source", e);
                if (mSaveListener != null)
                    mSaveListener.onError(mDataSource, e);
            }
        }
    }

    protected final Manager setContext(Context context) {
        mContext = context;
        return this;
    }

    public interface OnSaveListener {
        void onSaved(DataSource source);
        void onError(DataSource source, Exception e);
    }
    /**
     * Callback interface for progress monitoring.
     */
    public interface ProgressListener {
        /**
         * Called when operation is about to start and maximum progress is known.
         * @param length Maximum progress
         */
        void onProgressStarted(int length);

        /**
         * Called on operation progress.
         * @param progress Current progress
         */
        void onProgressChanged(int progress);

        /**
         * Called when operation has ended, is not called if error (exception) has occurred.
         */
        void onProgressFinished();
    }
}
