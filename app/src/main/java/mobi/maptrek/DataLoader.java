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

package mobi.maptrek;

import android.content.Context;
import android.os.FileObserver;
import android.util.Pair;

import androidx.loader.content.AsyncTaskLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.io.DataFilenameFilter;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.util.MonitoredInputStream;
import mobi.maptrek.util.ProgressListener;

//TODO Document class
// http://www.androiddesignpatterns.com/2012/08/implementing-loaders.html

class DataLoader extends AsyncTaskLoader<List<FileDataSource>> {
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    private static final String DO_NOT_LOAD_FLAG = ".do_not_load";

    // We hold a reference to the Loader’s data here.
    private List<FileDataSource> mData;
    private final Set<String> mFiles = new HashSet<>();

    private ProgressListener mProgressListener;
    private FileObserver mObserver;

    DataLoader(Context ctx) {
        // Loaders may be used across multiple Activities (assuming they aren't
        // bound to the LoaderManager), so NEVER hold a reference to the context
        // directly. Doing so will cause you to leak an entire Activity's context.
        // The superclass constructor will store a reference to the Application
        // Context instead, and can be retrieved with a call to getContext().
        super(ctx);
    }

    void setProgressHandler(ProgressListener listener) {
        mProgressListener = listener;
    }

    void renameSource(FileDataSource source, File thatFile) {
        File thisFile = new File(source.path);
        if (thisFile.renameTo(thatFile)) {
            synchronized (mFiles) {
                mFiles.remove(source.path);
                source.path = thatFile.getAbsolutePath();
                mFiles.add(source.path);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void markDataSourceLoadable(FileDataSource source, boolean loadable) {
        // Actual data changes will be performed by FileObserver which will detect flag change
        source.setLoadable(loadable);
        File flag = new File(source.path + DO_NOT_LOAD_FLAG);
        if (loadable)
            flag.delete();
        else
            try {
                flag.createNewFile();
                logger.debug("contains: {}", mData.contains(source));
            } catch (IOException e) {
                logger.error("Failed to create flag", e);
            }
    }

    @Override
    public List<FileDataSource> loadInBackground() {
        // This method is called on a background thread and should generate a
        // new set of data to be delivered back to the client.
        logger.debug("loadInBackground()");
        File dataDir = getContext().getExternalFilesDir("data");
        if (dataDir == null)
            return null;
        File[] files = dataDir.listFiles(new DataFilenameFilter());
        if (files == null)
            return null;
        List<FileDataSource> data = new ArrayList<>();
        List<Pair<File, Boolean>> loadFiles = new ArrayList<>();

        int maxProgress = 0;
        for (File file : files) {
            String path = file.getAbsolutePath();
            synchronized (mFiles) {
                if (!mFiles.contains(path)) {
                    File loadFlagFile = new File(path + DO_NOT_LOAD_FLAG);
                    boolean loadFlag = loadFlagFile.exists();
                    if (!loadFlag)
                        maxProgress = (int) (maxProgress + file.length());
                    loadFiles.add(new Pair<>(file, loadFlag));
                }
            }
        }

        if (mProgressListener != null)
            mProgressListener.onProgressStarted(maxProgress);

        int progress = 0;

        for (Pair<File, Boolean> pair : loadFiles) {
            if (isLoadInBackgroundCanceled()) {
                logger.debug("loadInBackgroundCanceled");
                return null;
            }
            File file = pair.first;
            boolean loadFlag = pair.second;

            logger.debug("  {} -> {}", (loadFlag ? "skip" : "load"), file.getName());

            if (loadFlag) {
                FileDataSource source = new FileDataSource();
                source.name = file.getName().substring(0, file.getName().lastIndexOf("."));
                source.path = file.getAbsolutePath();
                data.add(source);
            } else {
                try {
                    MonitoredInputStream inputStream = new MonitoredInputStream(new FileInputStream(file));
                    final int finalProgress = progress;
                    inputStream.addChangeListener(location -> {
                        if (mProgressListener != null) {
                            //TODO Divide progress by 1024
                            mProgressListener.onProgressChanged(finalProgress + (int) location);
                        }
                    });
                    Manager manager = Manager.getDataManager(file.getName());
                    if (manager != null) {
                        FileDataSource source = manager.loadData(inputStream, file.getAbsolutePath());
                        source.path = file.getAbsolutePath();
                        if (source.name == null || "".equals(source.name)) {
                            String fileName = file.getName();
                            source.name = fileName.substring(0, fileName.lastIndexOf("."));
                        }
                        source.setLoaded();
                        data.add(source);

                        if (manager instanceof TrackManager) {
                            //noinspection ResultOfMethodCallIgnored
                            file.setLastModified(source.tracks.get(0).getLastPoint().time);
                        }
                    }
                } catch (Exception e) {
                    //TODO Notify user about a problem
                    logger.error("File error: " + file.getAbsolutePath(), e);
                }
                progress += file.length();
            }
        }
        return data;
    }

    @Override
    public void deliverResult(List<FileDataSource> data) {
        logger.debug("deliverResult()");

        if (mProgressListener != null) {
            mProgressListener.onProgressFinished();
        }

        if (isReset() || data == null) {
            return;
        }

        synchronized (mFiles) {
            if (mData == null) {
                mData = data;
            } else {
                // Somewhere under the hood it looks if the data has changed by comparing
                // object instances, so we need a new object
                ArrayList<FileDataSource> newData = new ArrayList<>(mData.size());
                newData.addAll(mData);
                newData.addAll(data);
                mData = newData;
            }

            for (FileDataSource source : data) {
                mFiles.add(source.path);
            }
        }

        if (isStarted())
            super.deliverResult(mData);
    }

    @Override
    protected void onStartLoading() {
        logger.debug("onStartLoading()");
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(new ArrayList<>());
        }

        // Begin monitoring the underlying data source.
        if (mObserver == null) {
            final File dir = getContext().getExternalFilesDir("data");
            if (dir == null)
                return;

            mObserver = new FileObserver(dir.getAbsolutePath(), FileObserver.CLOSE_WRITE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO | FileObserver.DELETE) {
                @Override
                public void onEvent(int event, String path) {
                    if (event == 0x8000) // Undocumented, sent on stop watching
                        return;
                    if (path == null) // Undocumented, unexplainable
                        return;
                    path = dir.getAbsolutePath() + File.separator + path;
                    logger.debug("{}: {}", path, event);
                    boolean loadFlag = false;
                    if (path.endsWith(DO_NOT_LOAD_FLAG)) {
                        if (event == FileObserver.CLOSE_WRITE)
                            return;
                        path = path.substring(0, path.indexOf(DO_NOT_LOAD_FLAG));
                        loadFlag = true;
                    }
                    synchronized (mFiles) {
                        boolean loadedSource = false;
                        for (Iterator<FileDataSource> i = mData.iterator(); i.hasNext(); ) {
                            FileDataSource source = i.next();
                            if (source.path.equals(path)) {
                                if (loadFlag && source.isLoaded())
                                    loadedSource = true;
                                else
                                    i.remove();
                            }
                        }
                        if (!loadedSource) {
                            mFiles.remove(path);
                            DataLoader.this.onContentChanged();
                        }
                    }
                }
            };
            mObserver.startWatching();
        }

        if (takeContentChanged() || mData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        logger.debug("onStopLoading()");
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
    protected void onReset() {
        logger.debug("onReset()");
        // Ensure the loader has been stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        mFiles.clear();
        if (mData != null)
            mData.clear();
        mData = null;

        // The Loader is being reset, so we should stop monitoring for changes.
        if (mObserver != null) {
            mObserver.stopWatching();
            mObserver = null;
        }
    }

    @Override
    public void onCanceled(List<FileDataSource> data) {
        logger.debug("onCanceled()");
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);
    }
}