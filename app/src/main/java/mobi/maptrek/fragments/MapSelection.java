package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import mobi.maptrek.R;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.maps.MapStateListener;

public class MapSelection extends Fragment implements OnBackPressedListener, MapStateListener {
    private static final long INDEX_CACHE_TIMEOUT = 7 * 24 * 3600 * 1000; // One week

    private OnMapActionListener mListener;
    private FragmentHolder mFragmentHolder;
    private FloatingActionButton mFloatingButton;
    private MapIndex mMapIndex;
    private TextView mMessageView;
    private TextView mStatusView;
    private Resources mResources;
    private boolean mIsDownloadingIndex;
    private File mCacheFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mMapIndex.hasDownloadSizes() && mCacheFile.exists()
                && mCacheFile.lastModified() + INDEX_CACHE_TIMEOUT > System.currentTimeMillis()) {
            mIsDownloadingIndex = true;
            new LoadMapIndex().execute(true);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map_selection, container, false);
        mMessageView = (TextView) rootView.findViewById(R.id.message);
        mMessageView.setText(mResources.getQuantityString(R.plurals.itemsSelected, 0, 0));
        mStatusView = (TextView) rootView.findViewById(R.id.status);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListener.onBeginMapManagement();

        mFloatingButton = mFragmentHolder.enableActionButton();
        mFloatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_file_download));
        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onManageNativeMaps();
                mListener.onFinishMapManagement();
                mFragmentHolder.disableActionButton();
                mFragmentHolder.popCurrent();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnMapActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMapActionListener");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
            mFragmentHolder.addBackClickListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
        }
        mResources = getResources();

        File cacheDir = context.getExternalCacheDir();
        mCacheFile = new File(cacheDir, "mapIndex");

        mMapIndex.addMapStateListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMapIndex.removeMapStateListener(this);
        mFragmentHolder.removeBackClickListener(this);
        mFragmentHolder = null;
        mListener = null;
        mResources = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onBackClick() {
        mFragmentHolder.disableActionButton();
        mListener.onFinishMapManagement();
        return false;
    }

    @Override
    public void onMapSelected(final int x, final int y, MapIndex.ACTION action) {
        if (action == MapIndex.ACTION.CANCEL) {
            final Activity activity = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(R.string.msg_cancel_download);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mMapIndex.cancelDownload(x, y);
                    mMapIndex.selectNativeMap(x, y, MapIndex.ACTION.NONE);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }
        calculateDownloadSize();
        if (action == MapIndex.ACTION.DOWNLOAD && !mMapIndex.hasDownloadSizes() && !mIsDownloadingIndex) {
            mIsDownloadingIndex = true;
            new LoadMapIndex().execute(false);
        }
    }

    public void setMapIndex(MapIndex mapIndex) {
        mMapIndex = mapIndex;
    }

    private void calculateDownloadSize() {
        boolean hasSizes = mMapIndex.hasDownloadSizes();
        int downloadCounter = 0, removeCounter = 0;
        long size = 0L;
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++) {
                MapFile mapFile = mMapIndex.getNativeMap(x, y);
                if (mapFile.action == MapIndex.ACTION.DOWNLOAD) {
                    downloadCounter++;
                    if (hasSizes)
                        size += mapFile.downloadSize;
                }
                if (mapFile.action == MapIndex.ACTION.REMOVE) {
                    removeCounter++;
                }
            }
        int counter = downloadCounter + removeCounter;
        mMessageView.setText(mResources.getQuantityString(R.plurals.itemsSelected, counter, counter));
        // can be null when fragment is not yet visible
        if (mFloatingButton != null) {
            if (downloadCounter == 0 && removeCounter > 0)
                mFloatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_delete));
            else
                mFloatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_file_download));
        }
        if (size > 0L) {
            mStatusView.setVisibility(View.VISIBLE);
            mStatusView.setText(getString(R.string.msgDownloadSize, Formatter.formatFileSize(getContext(), size)));
        } else {
            mStatusView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onHasDownloadSizes() {
    }

    private class LoadMapIndex extends AsyncTask<Boolean, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            mStatusView.setVisibility(View.VISIBLE);
            mStatusView.setText(R.string.msgEstimateDownloadSize);
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            boolean useCache = params[0];
            HttpURLConnection urlConnection = null;
            InputStream in;
            DataInputStream data;
            OutputStream out;
            DataOutputStream dataOut = null;
            try {
                if (useCache) {
                    in = new FileInputStream(mCacheFile);
                } else {
                    URL url = new URL(MapIndex.getIndexUri().toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    out = new FileOutputStream(mCacheFile);
                    dataOut = new DataOutputStream(new BufferedOutputStream(out));
                }
                data = new DataInputStream(new BufferedInputStream(in));

                int progress = 0;
                for (int x = 0; x < 128; x++)
                    for (int y = 0; y < 128; y++) {
                        short date = data.readShort();
                        int size = data.readInt();
                        if (!useCache) {
                            dataOut.writeShort(date);
                            dataOut.writeInt(size);
                        }
                        mMapIndex.setNativeMapStatus(x, y, date, size);
                        int p = (int) ((x * 128 + y) / 163.84);
                        if (p > progress) {
                            progress = p;
                            publishProgress(progress);
                        }
                    }
                if (!useCache)
                    dataOut.close();
            } catch (Exception e) {
                Log.e("MS", "Failed to load index", e);
                // remove cache on any error
                //noinspection ResultOfMethodCallIgnored
                mCacheFile.delete();
                return false;
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mMapIndex.setHasDownloadSizes(true);
                calculateDownloadSize();
            } else {
                mStatusView.setText(R.string.msgIndexDownloadFailed);
            }
            mIsDownloadingIndex = false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mStatusView.setText(getString(R.string.msgEstimateDownloadSizePlaceholder, getString(R.string.msgEstimateDownloadSize), values[0]));
        }
    }
}
