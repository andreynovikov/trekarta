package mobi.maptrek;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.FileObserver;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.DataFilenameFilter;
import mobi.maptrek.util.MonitoredInputStream;

//TODO Document class
// http://www.androiddesignpatterns.com/2012/08/implementing-loaders.html

public class DataLoader extends AsyncTaskLoader<List<FileDataSource>> {
    private static final String TAG = "DataLoader";
    private static final String DO_NOT_LOAD_FLAG = ".do_not_load";

    // We hold a reference to the Loader’s data here.
    private List<FileDataSource> mData;
    private final Set<String> mFiles = new HashSet<>();

    private Manager.ProgressListener mProgressListener;
    private FileObserver mObserver;

    public DataLoader(Context ctx) {
        // Loaders may be used across multiple Activities (assuming they aren't
        // bound to the LoaderManager), so NEVER hold a reference to the context
        // directly. Doing so will cause you to leak an entire Activity's context.
        // The superclass constructor will store a reference to the Application
        // Context instead, and can be retrieved with a call to getContext().
        super(ctx);
    }

    public void setProgressHandler(Manager.ProgressListener listener) {
        mProgressListener = listener;
    }


    public void renameSource(FileDataSource source, File thatFile) {
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
    public void markDataSourceLoadable(FileDataSource source, boolean loadable) {
        // Actual data changes will be performed by FileObserver which will detect flag change
        File flag = new File(source.path + DO_NOT_LOAD_FLAG);
        if (loadable)
            flag.delete();
        else
            try {
                flag.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    public List<FileDataSource> loadInBackground() {
        // This method is called on a background thread and should generate a
        // new set of data to be delivered back to the client.
        Log.i(TAG, "loadInBackground()");
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
                        maxProgress += file.length();
                    loadFiles.add(new Pair<>(file, loadFlag));
                }
            }
        }

        if (mProgressListener != null)
            mProgressListener.onProgressStarted(maxProgress);

        int progress = 0;

        for (Pair<File,Boolean> pair: loadFiles) {
            if (isLoadInBackgroundCanceled()) {
                Log.i(TAG, "loadInBackgroundCanceled");
                return null;
            }
            File file = pair.first;
            boolean loadFlag = pair.second;

            Log.d(TAG, "  " + (loadFlag ? "skip" : "load") + " -> " + file.getName());

            if (loadFlag) {
                FileDataSource source = new FileDataSource();
                source.name = file.getName().substring(0, file.getName().lastIndexOf("."));
                source.path = file.getAbsolutePath();
                data.add(source);
            } else {
                try {
                    MonitoredInputStream inputStream = new MonitoredInputStream(new FileInputStream(file));
                    final int finalProgress = progress;
                    inputStream.addChangeListener(new MonitoredInputStream.ChangeListener() {
                        @Override
                        public void stateChanged(long location) {
                            if (mProgressListener != null) {
                                //TODO Divide progress by 1024
                                mProgressListener.onProgressChanged(finalProgress + (int) location);
                            }
                        }
                    });
                    Manager manager = Manager.getDataManager(getContext(), file.getName());
                    if (manager != null) {
                        FileDataSource source = manager.loadData(inputStream, file.getAbsolutePath());
                        source.path = file.getAbsolutePath();
                        if (source.name == null || "".equals(source.name)) {
                            String fileName = file.getName();
                            source.name = fileName.substring(0, fileName.lastIndexOf("."));
                        }
                        source.setLoaded();
                        data.add(source);
                    }
                } catch (Exception e) {
                    //TODO Notify user about a problem
                    Log.e(TAG, "File error: " + file.getAbsolutePath(), e);
                }
                progress += file.length();
            }
        }
        return data;
    }

    @Override
    public void deliverResult(List<FileDataSource> data) {
        Log.i(TAG, "deliverResult()");

        if (mProgressListener != null) {
            mProgressListener.onProgressFinished();
        }

        if (isReset()) {
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
        Log.i(TAG, "onStartLoading()");
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(new ArrayList<FileDataSource>());
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
                    Log.i(TAG, path + ": " + event);
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
        Log.i(TAG, "onStopLoading()");
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
    protected void onReset() {
        Log.i(TAG, "onReset()");
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
        Log.i(TAG, "onCanceled()");
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);
    }
}