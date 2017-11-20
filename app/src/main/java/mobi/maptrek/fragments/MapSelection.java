package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import mobi.maptrek.Configuration;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.util.HelperUtils;

public class MapSelection extends Fragment implements OnBackPressedListener, Index.MapStateListener {
    private static final Logger logger = LoggerFactory.getLogger(MapSelection.class);

    private static final long INDEX_CACHE_TIMEOUT = 24 * 3600 * 1000L; // One day
    private static final long HILLSHADE_CACHE_TIMEOUT = 60 * 24 * 3600 * 1000L; // Two months

    private OnMapActionListener mListener;
    private FragmentHolder mFragmentHolder;
    private FloatingActionButton mFloatingButton;
    private Index mMapIndex;
    private View mDownloadCheckboxHolder;
    private View mHillshadesCheckboxHolder;
    private CheckBox mDownloadBasemap;
    private CheckBox mDownloadHillshades;
    private TextView mMessageView;
    private TextView mStatusView;
    private TextView mCounterView;
    private Resources mResources;
    private boolean mIsDownloadingIndex;
    private File mCacheFile;
    private File mHillshadeCacheFile;
    private int mCounter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI(mMapIndex.getMapStats());
        if (!mMapIndex.hasDownloadSizes() && mCacheFile.exists()) {
            mIsDownloadingIndex = true;
            new LoadMapIndex().execute();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_map_selection, container, false);
        mHillshadesCheckboxHolder = rootView.findViewById(R.id.hillshadesCheckboxHolder);
        mDownloadHillshades = (CheckBox) rootView.findViewById(R.id.downloadHillshades);
        mDownloadHillshades.setChecked(Configuration.getHillshadesEnabled());
        mDownloadHillshades.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mMapIndex.accountHillshades(isChecked);
                updateUI(mMapIndex.getMapStats());
            }
        });
        mDownloadCheckboxHolder = rootView.findViewById(R.id.downloadCheckboxHolder);
        mDownloadBasemap = (CheckBox) rootView.findViewById(R.id.downloadBasemap);
        mDownloadBasemap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUI(mMapIndex.getMapStats());
            }
        });
        mMessageView = (TextView) rootView.findViewById(R.id.message);
        mMessageView.setText(mResources.getQuantityString(R.plurals.itemsSelected, 0, 0));
        mStatusView = (TextView) rootView.findViewById(R.id.status);
        mCounterView = (TextView) rootView.findViewById(R.id.count);

        if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_ACTIVE_MAPS_SIZE)) {
            ViewTreeObserver vto = rootView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (mMapIndex.getMapDatabaseSize() > (1 << 22)) { // 4 GB
                        Rect r = new Rect();
                        mCounterView.getGlobalVisibleRect(r);
                        r.left = r.right - r.width() / 3; // focus on size
                        HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_ACTIVE_MAPS_SIZE, R.string.advice_active_maps_size, r);
                    }
                }
            });
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListener.onBeginMapManagement();

        mFloatingButton = mFragmentHolder.enableActionButton();
        mFloatingButton.setImageResource(R.drawable.ic_file_download);
        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDownloadBasemap.isChecked()) {
                    mMapIndex.downloadBaseMap();
                }
                if (mCounter > 0) {
                    mListener.onManageNativeMaps(mDownloadHillshades.isChecked());
                }
                if (mDownloadBasemap.isChecked() || mCounter > 0) {
                    mListener.onFinishMapManagement();
                }
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
        mHillshadeCacheFile = new File(cacheDir, "hillshadeIndex");

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
    public void onMapSelected(final int x, final int y, Index.ACTION action, Index.IndexStats stats) {
        if (action == Index.ACTION.CANCEL) {
            final Activity activity = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.msgCancelDownload);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mMapIndex.cancelDownload(x, y);
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
        updateUI(stats);
        if (action == Index.ACTION.DOWNLOAD && !mMapIndex.hasDownloadSizes() && !mIsDownloadingIndex) {
            mIsDownloadingIndex = true;
            new LoadMapIndex().execute();
        }
    }

    public void setMapIndex(Index mapIndex) {
        mMapIndex = mapIndex;
        mMapIndex.accountHillshades(Configuration.getHillshadesEnabled());
    }

    private void updateUI(Index.IndexStats stats) {
        if (!isVisible())
            return;

        if (mMapIndex.isBaseMapOutdated()) {
            mDownloadBasemap.setText(getString(R.string.downloadBasemap, Formatter.formatFileSize(getContext(), mMapIndex.getBaseMapSize())));
            mDownloadCheckboxHolder.setVisibility(View.VISIBLE);
        }

        mCounter = stats.download + stats.remove;
        mMessageView.setText(mResources.getQuantityString(R.plurals.itemsSelected, mCounter, mCounter));
        // can be null when fragment is not yet visible
        if (mFloatingButton != null) {
            if (mDownloadBasemap.isChecked() || stats.download > 0) {
                mFloatingButton.setImageResource(R.drawable.ic_file_download);
                mFloatingButton.setVisibility(View.VISIBLE);
                mHillshadesCheckboxHolder.setVisibility(View.VISIBLE);
            } else if (stats.remove > 0) {
                mFloatingButton.setImageResource(R.drawable.ic_delete);
                mFloatingButton.setVisibility(View.VISIBLE);
                mHillshadesCheckboxHolder.setVisibility(View.GONE);
            } else {
                mFloatingButton.setVisibility(View.GONE);
                mHillshadesCheckboxHolder.setVisibility(View.GONE);
            }
        }
        if (stats.downloadSize > 0L) {
            mStatusView.setVisibility(View.VISIBLE);
            mStatusView.setText(getString(R.string.msgDownloadSize, Formatter.formatFileSize(getContext(), stats.downloadSize)));
        } else if (!mIsDownloadingIndex) {
            mStatusView.setVisibility(View.GONE);
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (stats.loaded > 0) {
            stringBuilder.append(mResources.getQuantityString(R.plurals.loadedAreas, stats.loaded, stats.loaded));
            stringBuilder.append(" (");
            stringBuilder.append(Formatter.formatFileSize(getContext(), mMapIndex.getMapDatabaseSize()));
            stringBuilder.append(")");
        }
        if (stats.downloading > 0) {
            if (stringBuilder.length() > 0)
                stringBuilder.append(", ");
            stringBuilder.append(mResources.getQuantityString(R.plurals.downloading, stats.downloading, stats.downloading));
        }
        if (stringBuilder.length() > 0) {
            mCounterView.setVisibility(View.VISIBLE);
            mCounterView.setText(stringBuilder);
        } else {
            mCounterView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onHasDownloadSizes() {
    }

    @Override
    public void onStatsChanged() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI(mMapIndex.getMapStats());
            }
        });
    }

    @Override
    public void onHillshadeAccountingChanged(boolean account) {
    }

    private class LoadMapIndex extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            mStatusView.setVisibility(View.VISIBLE);
            mStatusView.setText(R.string.msgEstimateDownloadSize);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            InputStream in;
            DataInputStream data;
            OutputStream out;
            DataOutputStream dataOut = null;
            long now = System.currentTimeMillis();
            boolean validCache = mCacheFile.lastModified() + INDEX_CACHE_TIMEOUT > now;
            boolean validHillshadeCache = mHillshadeCacheFile.lastModified() + HILLSHADE_CACHE_TIMEOUT > now;
            int divider = validHillshadeCache ? 1 : 2;
            int progress = 0;
            // load map index
            try {
                if (validCache) {
                    in = new FileInputStream(mCacheFile);
                } else {
                    URL url = new URL(Index.getIndexUri().toString() + "?" + mFragmentHolder.getStatsString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    out = new FileOutputStream(mCacheFile);
                    dataOut = new DataOutputStream(new BufferedOutputStream(out));
                }
                data = new DataInputStream(new BufferedInputStream(in));

                for (int x = 0; x < 128; x++)
                    for (int y = 0; y < 128; y++) {
                        short date = data.readShort();
                        int size = data.readInt();
                        if (!validCache) {
                            dataOut.writeShort(date);
                            dataOut.writeInt(size);
                        }
                        mMapIndex.setNativeMapStatus(x, y, date, size);
                        int p = (int) ((x * 128 + y) / 163.84 / divider);
                        if (p > progress) {
                            progress = p;
                            publishProgress(progress);
                        }
                    }
                short date = data.readShort();
                int size = data.readInt();
                mMapIndex.setBaseMapStatus(date, size);
                if (!validCache) {
                    dataOut.writeShort(date);
                    dataOut.writeInt(size);
                    dataOut.close();
                }
            } catch (Exception e) {
                logger.error("Failed to load index", e);
                // remove cache on any error
                //noinspection ResultOfMethodCallIgnored
                mCacheFile.delete();
                return false;
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            // load hillshade index
            try {
                if (validHillshadeCache) {
                    in = new FileInputStream(mHillshadeCacheFile);
                } else {
                    URL url = new URL(Index.getHillshadeIndexUri().toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    out = new FileOutputStream(mHillshadeCacheFile);
                    dataOut = new DataOutputStream(new BufferedOutputStream(out));
                }
                data = new DataInputStream(new BufferedInputStream(in));

                for (int x = 0; x < 128; x++)
                    for (int y = 0; y < 128; y++) {
                        byte version = data.readByte();
                        int size = data.readInt();
                        if (!validHillshadeCache) {
                            dataOut.writeByte(version);
                            dataOut.writeInt(size);
                        }
                        mMapIndex.setHillshadeStatus(x, y, version, size);
                        int p = (int) ((x * 128 + y) / 163.84 / divider);
                        if (p > progress) {
                            progress = p;
                            publishProgress(progress);
                        }
                    }
                if (!validHillshadeCache) {
                    dataOut.close();
                }
            } catch (Exception e) {
                logger.error("Failed to load hillshade index", e);
                // remove cache on any error
                //noinspection ResultOfMethodCallIgnored
                mHillshadeCacheFile.delete();
                return false;
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mIsDownloadingIndex = false;
            if (result) {
                boolean expired = mCacheFile.lastModified() + INDEX_CACHE_TIMEOUT < System.currentTimeMillis();
                mMapIndex.setHasDownloadSizes(true, expired);
                updateUI(mMapIndex.getMapStats());
            } else {
                mStatusView.setText(R.string.msgIndexDownloadFailed);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (isVisible())
                mStatusView.setText(getString(R.string.msgEstimateDownloadSizePlaceholder, getString(R.string.msgEstimateDownloadSize), values[0]));
        }
    }
}
