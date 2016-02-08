package mobi.maptrek;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mobi.maptrek.data.DataSource;
import mobi.maptrek.io.Manager;
import mobi.maptrek.util.DataFilenameFilter;
import mobi.maptrek.util.MonitoredInputStream;

// http://www.androiddesignpatterns.com/2012/08/implementing-loaders.html

public class DataLoader extends AsyncTaskLoader<List<DataSource>> {

    // We hold a reference to the Loader’s data here.
    private List<DataSource> mData;
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

    @Override
    public List<DataSource> loadInBackground() {
        // This method is called on a background thread and should generate a
        // new set of data to be delivered back to the client.
        Log.i("DataLoader", "loadInBackground()");
        Context ctx = getContext();
        File dataDir = ctx.getExternalFilesDir("data");
        if (dataDir == null)
            return null;
        File[] files = dataDir.listFiles(new DataFilenameFilter());
        if (files == null)
            return null;
        List<DataSource> data = new ArrayList<>();
        List<File> loadFiles = new ArrayList<>();

        int maxProgress = 0;
        for (File file : files) {
            String path = file.getAbsolutePath();
            synchronized (mFiles) {
                if (!mFiles.contains(path)) {
                    maxProgress += file.length();
                    loadFiles.add(file);
                }
            }
        }

        if (mProgressListener != null)
            mProgressListener.onProgressStarted(maxProgress);

        int progress = 0;

        for (File file : loadFiles) {
            if (isLoadInBackgroundCanceled()) {
                Log.i("DataLoader", "loadInBackgroundCanceled");
                return null;
            }
            Log.d("DataLoader", "  load -> " + file.getName());

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
                    DataSource source = manager.loadData(inputStream, file.getName());
                    source.path = file.getAbsolutePath();
                    data.add(source);
                }
            } catch (Exception e) {
                //TODO Notify user about a problem
                e.printStackTrace();
            }
            progress += file.length();
        }
        return data;
    }

    @Override
    public void deliverResult(List<DataSource> data) {
        Log.i("DataLoader", "deliverResult()");

        if (mProgressListener != null) {
            mProgressListener.onProgressFinished();
        }

        if (isReset()) {
            return;
        }

        synchronized (mFiles) {
            if (mData == null)
                mData = data;
            else
                mData.addAll(data);

            for (DataSource source : data) {
                mFiles.add(source.path);
            }
        }

        if (isStarted()) {
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStartLoading() {
        Log.i("DataLoader", "onStartLoading()");
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
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
                    path = dir.getAbsolutePath() + File.separator + path;
                    Log.i("DataLoader", path + ": " + event);
                    synchronized (mFiles) {
                        mFiles.remove(path);
                        for(Iterator<DataSource> i = mData.iterator(); i.hasNext();) {
                            DataSource source = i.next();
                            if (source.path.equals(path))
                                i.remove();
                        }
                    }
                    DataLoader.this.onContentChanged();
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
        Log.i("DataLoader", "onStopLoading()");
        // The Loader is in a stopped state, so we should attempt to cancel the
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
    protected void onReset() {
        Log.i("DataLoader", "onReset()");
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
    public void onCanceled(List<DataSource> data) {
        Log.i("DataLoader", "onCanceled()");
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);
    }
}