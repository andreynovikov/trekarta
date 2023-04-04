/*
 * Copyright 2018 Andrey Novikov
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

package mobi.maptrek.io;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import mobi.maptrek.MapTrek;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.ProgressListener;

public abstract class Manager {
    protected static final Logger logger = LoggerFactory.getLogger(Manager.class);

    /**
     * Returns manager for specified file. Manager is selected by file extension. No file format
     * check is performed. Returns <code>null</code> for unknown file formats.
     *
     * @param file File name
     * @return File read and write manager
     */
    @Nullable
    public static Manager getDataManager(String file) {
        if (file.toLowerCase().endsWith(TrackManager.EXTENSION)) {
            return new TrackManager();
        }
        if (file.toLowerCase().endsWith(GPXManager.EXTENSION)) {
            return new GPXManager();
        }
        if (file.toLowerCase().endsWith(KMLManager.EXTENSION)) {
            return new KMLManager();
        }
        if (file.toLowerCase().endsWith(KMLManager.ZIP_EXTENSION)) {
            return new KMLManager();
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
    private static Manager getDataManager(FileDataSource source) {
        // FIXME Method not suitable for exporting data
        if (source.path == null && source.isIndividual() && source.getTracksCount() == 1)
            return new TrackManager();
        if (source.path.toLowerCase().endsWith(GPXManager.EXTENSION))
            return new GPXManager();
        if (source.path.toLowerCase().endsWith(KMLManager.EXTENSION))
            return new KMLManager();
        if (source.path.toLowerCase().endsWith(TrackManager.EXTENSION))
            return new TrackManager();
        if (source.path.toLowerCase().endsWith(RouteManager.EXTENSION))
            return new RouteManager();
        return null;
    }

    public static void save(FileDataSource source) {
        save(source, null);
    }

    public static void save(FileDataSource source, OnSaveListener saveListener) {
        save(source, saveListener, null);
    }

    public static void save(FileDataSource source, OnSaveListener saveListener, ProgressListener progressListener) {
        Manager manager = Manager.getDataManager(source);
        assert manager != null : "Failed to get IO manager for " + source.path;
        manager.saveData(source, saveListener, progressListener);
    }

    /**
     * Loads data from file (input stream). File name is used only for reference.
     * @param inputStream <code>InputStream</code> with waypoints
     * @param filePath File path associated with that <code>InputStream</code>
     * @return <code>DataSource</code> filled with file data
     * @throws Exception if IO or parsing error occurred
     */
    @NonNull
    public abstract FileDataSource loadData(InputStream inputStream, String filePath) throws Exception;

    public abstract void saveData(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws Exception;

    /**
     * Returns file extension with leading dot.
     */
    @NonNull
    public abstract String getExtension();

    protected final void saveData(FileDataSource source, @Nullable OnSaveListener saveListener, @Nullable ProgressListener progressListener) {
        File file;
        if (source.path == null) {
            String name = source.name != null && !"".equals(source.name) ? source.name : "data_source_" + System.currentTimeMillis();
            file = MapTrek.getApplication().getExternalFilesDir("data");
            file = new File(file, FileUtils.sanitizeFilename(name) + getExtension());
        } else {
            file = new File(source.path);
        }
        new Thread(new SaveRunnable(file, source, saveListener, progressListener)).start();
    }

    private class SaveRunnable implements Runnable {
        private final File mFile;
        private final FileDataSource mDataSource;
        private final OnSaveListener mSaveListener;
        private final ProgressListener mProgressListener;

        SaveRunnable(final File file, final FileDataSource source, @Nullable final OnSaveListener saveListener, @Nullable final ProgressListener progressListener) {
            mFile = file;
            mDataSource = source;
            mSaveListener = saveListener;
            mProgressListener = progressListener;
        }

        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                // Do not allow simultaneous operations on one source
                synchronized (mDataSource) {
                    logger.debug("Saving data source...");
                    // Save to temporary file for two reasons: to preserve original file in case of error
                    // and to hide it from data loader until it is completely saved
                    File newFile = new File(mFile.getParent(), System.currentTimeMillis() + ".tmp");
                    Manager.this.saveData(new FileOutputStream(newFile, false), mDataSource, mProgressListener);
                    if (mFile.exists() && !mFile.delete() || !newFile.renameTo(mFile)) {
                        logger.error("Can not rename data source file after save");
                        if (mSaveListener != null)
                            mSaveListener.onError(mDataSource, new Exception("Can not rename data source file after save"));
                    } else {
                        mDataSource.path = mFile.getAbsolutePath();
                        if (mSaveListener != null)
                            mSaveListener.onSaved(mDataSource);
                        logger.debug("Done");
                    }
                }
            } catch (Exception e) {
                logger.error("Can not save data source", e);
                if (mSaveListener != null)
                    mSaveListener.onError(mDataSource, e);
            }
        }
    }

    public interface OnSaveListener {
        void onSaved(FileDataSource source);
        void onError(FileDataSource source, Exception e);
    }
}
