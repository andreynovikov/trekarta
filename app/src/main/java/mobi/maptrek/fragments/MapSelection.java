/*
 * Copyright 2023 Andrey Novikov
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

package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import mobi.maptrek.Configuration;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.util.HelperUtils;

public class MapSelection extends Fragment implements Index.MapStateListener {
    private static final Logger logger = LoggerFactory.getLogger(MapSelection.class);

    private static final long INDEX_CACHE_TIMEOUT = 24 * 3600 * 1000L; // One day
    private static final long INDEX_CACHE_EXPIRATION = 60 * 24 * 3600 * 1000L; // Two months
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
    private ImageButton mHelpButton;
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
        mDownloadHillshades = rootView.findViewById(R.id.downloadHillshades);
        mDownloadHillshades.setChecked(Configuration.getHillshadesEnabled());
        mDownloadHillshades.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mMapIndex.accountHillshades(isChecked);
            updateUI(mMapIndex.getMapStats());
        });
        mDownloadCheckboxHolder = rootView.findViewById(R.id.downloadCheckboxHolder);
        mDownloadBasemap = rootView.findViewById(R.id.downloadBasemap);
        mDownloadBasemap.setOnCheckedChangeListener((buttonView, isChecked) -> updateUI(mMapIndex.getMapStats()));
        mMessageView = rootView.findViewById(R.id.message);
        mMessageView.setText(mResources.getQuantityString(R.plurals.itemsSelected, 0, 0));
        mStatusView = rootView.findViewById(R.id.status);
        mCounterView = rootView.findViewById(R.id.count);
        mHelpButton = rootView.findViewById(R.id.helpButton);
        mHelpButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.msgMapSelectionExplanation);
            builder.setPositiveButton(R.string.ok, null);
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_ACTIVE_MAPS_SIZE)) {
            ViewTreeObserver vto = rootView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (mMapIndex.getMapDatabaseSize() > (1L << 32)) { // 4 GB
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListener.onBeginMapManagement();

        mFloatingButton = mFragmentHolder.enableActionButton();
        mFloatingButton.setImageResource(R.drawable.ic_file_download);
        mFloatingButton.setOnClickListener(v -> {
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
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnMapActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnMapActionListener");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement FragmentHolder");
        }
        mResources = getResources();

        File cacheDir = context.getExternalCacheDir();
        mCacheFile = new File(cacheDir, "mapIndex");
        mHillshadeCacheFile = new File(cacheDir, "hillshadeIndex");

        mMapIndex.addMapStateListener(this);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBackPressedCallback.remove();
        mMapIndex.removeMapStateListener(this);
        mFragmentHolder = null;
        mListener = null;
        mResources = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            mFragmentHolder.disableActionButton();
            mListener.onFinishMapManagement();
            this.remove();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    };

    @Override
    public void onMapSelected(final int x, final int y, Index.ACTION action, Index.IndexStats stats) {
        if (action == Index.ACTION.CANCEL) {
            final Activity activity = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.msgCancelDownload);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> mMapIndex.cancelDownload(x, y));
            builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
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

        if (mMapIndex.isBaseMapOutdated() || mMapIndex.getBaseMapVersion() == 0) {
            @StringRes int msgId = mMapIndex.getBaseMapVersion() > 0 ? R.string.downloadUpdatedBasemap : R.string.downloadBasemap;
            mDownloadBasemap.setText(getString(msgId, Formatter.formatFileSize(getContext(), mMapIndex.getBaseMapSize())));
            mDownloadCheckboxHolder.setVisibility(View.VISIBLE);
        }

        mCounter = stats.download + stats.remove;
        mMessageView.setText(mResources.getQuantityString(R.plurals.itemsSelected, mCounter, mCounter));
        // can be null when fragment is not yet visible
        if (mFloatingButton != null) {
            if (mDownloadBasemap.isChecked() || stats.download > 0) {
                mFloatingButton.setImageResource(R.drawable.ic_file_download);
                ((View)mFloatingButton).setVisibility(View.VISIBLE);
                mHelpButton.setVisibility(View.INVISIBLE);
                if (stats.download > 0)
                    mHillshadesCheckboxHolder.setVisibility(View.VISIBLE);
            } else if (stats.remove > 0) {
                mFloatingButton.setImageResource(R.drawable.ic_delete);
                ((View)mFloatingButton).setVisibility(View.VISIBLE);
                mHelpButton.setVisibility(View.INVISIBLE);
                mHillshadesCheckboxHolder.setVisibility(View.GONE);
            } else {
                ((View)mFloatingButton).setVisibility(View.GONE);
                mHelpButton.setVisibility(View.VISIBLE);
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
        requireActivity().runOnUiThread(() -> updateUI(mMapIndex.getMapStats()));
    }

    @Override
    public void onHillshadeAccountingChanged(boolean account) {
    }

    private class LoadMapIndex extends AsyncTask<Void, Integer, Boolean> {

        private int mProgress;
        private int mDivider;

        @Override
        protected void onPreExecute() {
            mStatusView.setVisibility(View.VISIBLE);
            mStatusView.setText(R.string.msgEstimateDownloadSize);
            mProgress = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            long now = System.currentTimeMillis();
            boolean validCache = mCacheFile.lastModified() + INDEX_CACHE_TIMEOUT > now;
            boolean validHillshadeCache = mHillshadeCacheFile.lastModified() + HILLSHADE_CACHE_TIMEOUT > now;
            mDivider = validHillshadeCache ? 1 : 2;
            // load map index
            try {
                boolean loaded = false;
                InputStream in;
                if (!validCache) {
                    URL url = new URL(Index.getIndexUri().toString() + "?" + mFragmentHolder.getStatsString());
                    HttpURLConnection urlConnection = null;
                    try {
                        urlConnection = (HttpURLConnection) url.openConnection();
                        in = urlConnection.getInputStream();
                        File tmpFile = new File(mCacheFile.getAbsoluteFile() + "_tmp");
                        OutputStream out = new FileOutputStream(tmpFile);
                        loadMapIndex(in, out);
                        loaded = tmpFile.renameTo(mCacheFile);
                    } catch (IOException e) {
                        logger.error("Failed to download map index", e);
                    } finally {
                        if (urlConnection != null)
                            urlConnection.disconnect();
                    }
                }
                if (!loaded) {
                    in = new FileInputStream(mCacheFile);
                    loadMapIndex(in, null);
                }
            } catch (Exception e) {
                logger.error("Failed to load map index", e);
                // remove cache on any error
                //noinspection ResultOfMethodCallIgnored
                mCacheFile.delete();
                return false;
            }
            // load hillshade index
            try {
                boolean loaded = false;
                InputStream in;
                if (!validHillshadeCache) {
                    URL url = new URL(Index.getHillshadeIndexUri().toString());
                    HttpURLConnection urlConnection = null;
                    try {
                        urlConnection = (HttpURLConnection) url.openConnection();
                        in = urlConnection.getInputStream();
                        File tmpFile = new File(mHillshadeCacheFile.getAbsoluteFile() + "_tmp");
                        OutputStream out = new FileOutputStream(mHillshadeCacheFile);
                        loadHillshadesIndex(in, out);
                        loaded = tmpFile.renameTo(mHillshadeCacheFile);
                    } catch (IOException e) {
                        logger.error("Failed to download hillshades index", e);
                    } finally {
                        if (urlConnection != null)
                            urlConnection.disconnect();
                    }
                }
                if (!loaded) {
                    in = new FileInputStream(mHillshadeCacheFile);
                    loadHillshadesIndex(in, null);
                }
            } catch (Exception e) {
                logger.error("Failed to load hillshades index", e);
                // remove cache on any error
                //noinspection ResultOfMethodCallIgnored
                mHillshadeCacheFile.delete();
                return false;
            }
            return true;
        }

        private void loadMapIndex(InputStream in, OutputStream out) throws IOException {
            DataInputStream data = new DataInputStream(new BufferedInputStream(in));
            DataOutputStream dataOut = null;
            if (out != null)
                dataOut = new DataOutputStream(new BufferedOutputStream(out));

            for (int x = 0; x < 128; x++)
                for (int y = 0; y < 128; y++) {
                    short date = data.readShort();
                    int size = data.readInt();
                    if (dataOut != null) {
                        dataOut.writeShort(date);
                        dataOut.writeInt(size);
                    }
                    mMapIndex.setNativeMapStatus(x, y, date, size);
                    int p = (int) ((x * 128 + y) / 163.84 / mDivider);
                    if (p > mProgress) {
                        mProgress = p;
                        publishProgress(mProgress);
                    }
                }
            short date = data.readShort();
            int size = data.readInt();
            mMapIndex.setBaseMapStatus(date, size);
            if (dataOut != null) {
                dataOut.writeShort(date);
                dataOut.writeInt(size);
                dataOut.close();
            }
        }

        private void loadHillshadesIndex(InputStream in, OutputStream out) throws IOException {
            DataInputStream data = new DataInputStream(new BufferedInputStream(in));
            DataOutputStream dataOut = null;
            if (out != null)
                dataOut = new DataOutputStream(new BufferedOutputStream(out));

            for (int x = 0; x < 128; x++)
                for (int y = 0; y < 128; y++) {
                    byte version = data.readByte();
                    int size = data.readInt();
                    if (dataOut != null) {
                        dataOut.writeByte(version);
                        dataOut.writeInt(size);
                    }
                    mMapIndex.setHillshadeStatus(x, y, version, size);
                    int p = (int) ((x * 128 + y) / 163.84 / mDivider);
                    if (p > mProgress) {
                        mProgress = p;
                        publishProgress(mProgress);
                    }
                }
            if (dataOut != null) {
                dataOut.close();
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mIsDownloadingIndex = false;
            if (result) {
                boolean expired = mCacheFile.lastModified() + INDEX_CACHE_EXPIRATION < System.currentTimeMillis();
                mMapIndex.setHasDownloadSizes(expired);
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
