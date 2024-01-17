/*
 * Copyright 2024 Andrey Novikov
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
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.Configuration;
import mobi.maptrek.R;
import mobi.maptrek.databinding.FragmentMapSelectionBinding;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.viewmodels.MapIndexViewModel;

public class MapSelection extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(MapSelection.class);

    private OnMapActionListener mListener;
    private FragmentHolder mFragmentHolder;
    private FloatingActionButton mFloatingButton;
    private Resources resources;

    private MapIndexViewModel mapIndexViewModel;
    private FragmentMapSelectionBinding viewBinding;
    private String statsString;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentMapSelectionBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapIndexViewModel = new ViewModelProvider(requireActivity()).get(MapIndexViewModel.class);
        mapIndexViewModel.getNativeIndexState().observe(getViewLifecycleOwner(), indexStats -> {
            int count = indexStats.download + indexStats.remove;
            logger.debug("Selected maps count: {}", count);
            viewBinding.message.setText(resources.getQuantityString(R.plurals.itemsSelected, count, count));
            if (indexStats.downloadSize > 0L) {
                viewBinding.status.setVisibility(View.VISIBLE);
                viewBinding.status.setText(getString(R.string.msgDownloadSize, Formatter.formatFileSize(getContext(), indexStats.downloadSize)));
            } else {
                viewBinding.status.setVisibility(View.GONE);
            }
            updateUI(indexStats);
        });
        mapIndexViewModel.getBaseMapState().observe(getViewLifecycleOwner(), baseMapState -> {
            if (baseMapState.outdated || baseMapState.version == 0) {
                @StringRes int msgId = baseMapState.version > 0 ? R.string.downloadUpdatedBasemap : R.string.downloadBasemap;
                viewBinding.downloadBasemap.setText(getString(msgId, Formatter.formatFileSize(getContext(), baseMapState.size)));
                viewBinding.downloadCheckboxHolder.setVisibility(View.VISIBLE);
            } else {
                viewBinding.downloadCheckboxHolder.setVisibility(View.GONE);
            }
        });
        mapIndexViewModel.getActionState().observe(getViewLifecycleOwner(), actionState -> {
            if (actionState == null)
                return;
            if (actionState.action == Index.ACTION.CANCEL) {
                final Activity activity = getActivity();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.msgCancelDownload);
                builder.setPositiveButton(R.string.yes, (dialog, which) -> mapIndexViewModel.nativeIndex.cancelDownload(actionState.x, actionState.y));
                builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            } else if (actionState.action == Index.ACTION.DOWNLOAD) {
                mapIndexViewModel.loadMapIndexes(statsString);
            }
        });
        mapIndexViewModel.getIndexDownloadProgressState().observe(getViewLifecycleOwner(), progress -> {
            viewBinding.progress.setVisibility(progress == -1 ? View.GONE : View.VISIBLE);
            if (progress == -2) { // error
                viewBinding.progress.setText(R.string.msgIndexDownloadFailed);
            } else if (progress == 0) {
                viewBinding.progress.setText(R.string.msgEstimateDownloadSize);
            } else {
                viewBinding.progress.setText(getString(R.string.msgEstimateDownloadSizePlaceholder, getString(R.string.msgEstimateDownloadSize), progress));
            }
        });

        viewBinding.downloadHillshades.setChecked(Configuration.getHillshadesEnabled());
        //noinspection CodeBlock2Expr
        viewBinding.downloadHillshades.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mapIndexViewModel.nativeIndex.accountHillshades(isChecked);
        });
        //noinspection CodeBlock2Expr
        viewBinding.downloadBasemap.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateUI(mapIndexViewModel.nativeIndex.getMapStats());
        });
        viewBinding.message.setText(resources.getQuantityString(R.plurals.itemsSelected, 0, 0));
        viewBinding.helpButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.msgMapSelectionExplanation);
            builder.setPositiveButton(R.string.ok, null);
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_ACTIVE_MAPS_SIZE)) {
            ViewTreeObserver vto = view.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (mapIndexViewModel.nativeIndex.getMapDatabaseSize() > (1L << 32)) { // 4 GB
                        Rect r = new Rect();
                        if (!viewBinding.count.getGlobalVisibleRect(r))
                            return;
                        r.left = r.right - r.width() / 3; // focus on size
                        HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_ACTIVE_MAPS_SIZE, R.string.advice_active_maps_size, r);
                    }
                }
            });
        }

        mListener.onBeginMapManagement();

        mFloatingButton = mFragmentHolder.enableActionButton();
        mFloatingButton.setImageResource(R.drawable.ic_file_download);
        mFloatingButton.setOnClickListener(v -> {
            if (viewBinding.downloadBasemap.isChecked()) {
                mapIndexViewModel.nativeIndex.downloadBaseMap();
            }
            Index.IndexStats indexStats = mapIndexViewModel.getNativeIndexState().getValue();
            if (indexStats != null && indexStats.download + indexStats.remove > 0) {
                mapIndexViewModel.nativeIndex.manageNativeMaps(viewBinding.downloadHillshades.isChecked());
            }
            mapIndexViewModel.nativeIndex.clearSelections();
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
        resources = getResources();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        backPressedCallback.remove();
        mFragmentHolder = null;
        mListener = null;
        resources = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        statsString = mFragmentHolder.getStatsString();
        if (mapIndexViewModel.cacheFile.exists()) {
            mapIndexViewModel.loadMapIndexes(statsString);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFragmentHolder.disableActionButton();
        mListener.onFinishMapManagement();
    }

    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            mapIndexViewModel.nativeIndex.clearSelections();
            this.remove();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    };

    private void updateUI(Index.IndexStats stats) {
        if (viewBinding.downloadBasemap.isChecked() || stats.download > 0) {
            mFloatingButton.setImageResource(R.drawable.ic_file_download);
            mFloatingButton.setVisibility(View.VISIBLE);
            viewBinding.helpButton.setVisibility(View.INVISIBLE);
            if (stats.download > 0)
                viewBinding.hillshadesCheckboxHolder.setVisibility(View.VISIBLE);
        } else if (stats.remove > 0) {
            mFloatingButton.setImageResource(R.drawable.ic_delete);
            mFloatingButton.setVisibility(View.VISIBLE);
            viewBinding.helpButton.setVisibility(View.INVISIBLE);
            viewBinding.hillshadesCheckboxHolder.setVisibility(View.GONE);
        } else {
            mFloatingButton.setVisibility(View.GONE);
            viewBinding.helpButton.setVisibility(View.VISIBLE);
            viewBinding.hillshadesCheckboxHolder.setVisibility(View.GONE);
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (stats.loaded > 0) {
            stringBuilder.append(resources.getQuantityString(R.plurals.loadedAreas, stats.loaded, stats.loaded));
            stringBuilder.append(" (");
            stringBuilder.append(Formatter.formatFileSize(getContext(), mapIndexViewModel.nativeIndex.getMapDatabaseSize()));
            stringBuilder.append(")");
        }
        if (stats.downloading > 0) {
            if (stringBuilder.length() > 0)
                stringBuilder.append(", ");
            stringBuilder.append(resources.getQuantityString(R.plurals.downloading, stats.downloading, stats.downloading));
        }
        if (stringBuilder.length() > 0) {
            viewBinding.count.setVisibility(View.VISIBLE);
            viewBinding.count.setText(stringBuilder);
        } else {
            viewBinding.count.setVisibility(View.GONE);
        }
    }
}
