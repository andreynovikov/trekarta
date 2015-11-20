package mobi.maptrek;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import mobi.maptrek.data.Track;
import mobi.maptrek.util.DataFilenameFilter;
import mobi.maptrek.util.GpxFiles;
import mobi.maptrek.util.KmlFiles;
import mobi.maptrek.util.MonitoredInputStream;
import mobi.maptrek.util.ProgressHandler;

//TODO Encapsulate different data types into one container
public class DataLoader extends AsyncTaskLoader<List<Track>> {

    // We hold a reference to the Loader’s data here.
    private List<Track> mData;
    private ProgressHandler mProgressHandler;

    public DataLoader(Context ctx) {
        // Loaders may be used across multiple Activities (assuming they aren't
        // bound to the LoaderManager), so NEVER hold a reference to the context
        // directly. Doing so will cause you to leak an entire Activity's context.
        // The superclass constructor will store a reference to the Application
        // Context instead, and can be retrieved with a call to getContext().
        super(ctx);
    }

    public void setProgressHandler(ProgressHandler handler) {
        mProgressHandler = handler;
    }

    @Override
    public List<Track> loadInBackground() {
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
        List<Track> data = new ArrayList<>();

        int maxProgress = 0;
        for (File file : files) {
            maxProgress += file.length();
        }

        if (mProgressHandler != null)
            mProgressHandler.onProgressStarted(maxProgress);

        int progress = 0;

        for (File file : files) {
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
                        if (mProgressHandler != null) {
                            //TODO Divide progress by 1024
                            mProgressHandler.onProgressChanged(finalProgress + (int) location);
                        }
                    }
                });
                List<Track> tracks = null;
                //TODO Refactor to use factory
                if (file.getName().toLowerCase().endsWith(".kml")) {
                    tracks = KmlFiles.loadTracksFromFile(inputStream, file.getName());
                }
                if (file.getName().toLowerCase().endsWith(".gpx")) {
                    tracks = GpxFiles.loadTracksFromFile(inputStream, file.getName());
                }
                if (tracks != null)
                    data.addAll(tracks);
            } catch (Exception e) {
                //TODO Notify user about a problem
                e.printStackTrace();
            }
            progress += file.length();
        }
        return data;
    }

    @Override
    public void deliverResult(List<Track> data) {
        Log.i("DataLoader", "deliverResult()");

        if (mProgressHandler != null) {
            mProgressHandler.onProgressFinished();
        }

        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            releaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<Track> oldData = mData;
        mData = data;

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            releaseResources(oldData);
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
            File dir = getContext().getExternalFilesDir("data");
            if (dir == null)
                return;

            mObserver = new FileObserver(dir.getAbsolutePath(), FileObserver.CLOSE_WRITE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO | FileObserver.DELETE) {
                @Override
                public void onEvent(int event, String path) {
                    if (event == 0x8000) // Undocumented, sent on stop watching
                        return;
                    DataLoader.this.onContentChanged();
                    Log.i("DataLoader", path + ": " + event);
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
        if (mData != null) {
            releaseResources(mData);
            mData = null;
        }

        // The Loader is being reset, so we should stop monitoring for changes.
        if (mObserver != null) {
            mObserver.stopWatching();
            mObserver = null;
        }
    }

    @Override
    public void onCanceled(List<Track> data) {
        Log.i("DataLoader", "onCanceled()");
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(@SuppressWarnings("UnusedParameters") List<Track> data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }

    private FileObserver mObserver;
}