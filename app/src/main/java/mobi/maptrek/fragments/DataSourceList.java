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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Locale;

import mobi.maptrek.DataHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.MemoryDataSource;
import mobi.maptrek.data.source.WaypointDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.databinding.ListWithEmptyViewBinding;
import mobi.maptrek.location.BaseLocationService.TRACKING_STATE;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.viewmodels.DataSourceViewModel;
import mobi.maptrek.viewmodels.TrackViewModel;

public class DataSourceList extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceList.class);

    private boolean fabShown;
    private int accentColor;
    private int disabledColor;
    private OnTrackActionListener mTrackActionListener;
    private FragmentHolder mFragmentHolder;
    private DataHolder mDataHolder;
    private DataSourceViewModel dataSourceViewModel;
    private TrackViewModel trackViewModel;
    private ListWithEmptyViewBinding viewBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = ListWithEmptyViewBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DefaultItemAnimator animator = ((DefaultItemAnimator) viewBinding.list.getItemAnimator());
        DataSourceListAdapter adapter = new DataSourceListAdapter();
        viewBinding.list.setAdapter(adapter);
        adapter.registerAdapterDataObserver(adapterDataObserver);

        dataSourceViewModel = new ViewModelProvider(requireActivity()).get(DataSourceViewModel.class);
        dataSourceViewModel.getNativeTracksState().observe(getViewLifecycleOwner(), nativeTracks -> {
            adapter.setNativeTracksMode(nativeTracks);
            if (nativeTracks)
                viewBinding.empty.setText(R.string.msgEmptyTrackList);
            else
                viewBinding.empty.setText(null);
        });
        dataSourceViewModel.getDataSourcesState().observe(getViewLifecycleOwner(), adapter::submitList);

        trackViewModel = new ViewModelProvider(requireActivity()).get(TrackViewModel.class);
        trackViewModel.currentTrack.observe(getViewLifecycleOwner(), adapter::setCurrentTrack);
        trackViewModel.trackingState.observe(getViewLifecycleOwner(), trackingState -> {
            logger.debug("current track state: {}", trackingState);
            adapter.notifyItemChanged(0);
            if (animator != null)
                animator.setSupportsChangeAnimations(trackingState != TRACKING_STATE.TRACKING); // remove list item blinking
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RecyclerView.Adapter<?> adapter = viewBinding.list.getAdapter();
        if (adapter != null)
            adapter.unregisterAdapterDataObserver(adapterDataObserver);
    }

    private void updateFloatingButtonState(boolean show) {
        if (show && !fabShown) {
            mFragmentHolder.enableListActionButton(R.drawable.ic_record, v -> trackViewModel.trackingCommand.setValue(TRACKING_STATE.PENDING));
            fabShown = true;
        } else  if (!show && fabShown) {
            mFragmentHolder.disableListActionButton();
            fabShown = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mFragmentHolder.disableListActionButton();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        accentColor = context.getColor(R.color.colorAccent);
        disabledColor = context.getColor(R.color.colorPrimary);
        try {
            mTrackActionListener = (OnTrackActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnTrackActionListener");
        }
        try {
            mDataHolder = (DataHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement DataHolder");
        }
        mFragmentHolder = (FragmentHolder) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mTrackActionListener = null;
        mFragmentHolder = null;
        mDataHolder = null;
    }

    public class DataSourceListAdapter extends ListAdapter<DataSource, DataSourceListAdapter.BindableViewHolder> {
        private final Resources resources;
        private boolean nativeTracksMode;
        private MemoryDataSource currentTrack;
        private long since = 0;
        private boolean fromFirstPoint = false;

        protected DataSourceListAdapter() {
            super(DIFF_CALLBACK);
            resources = getResources();
        }

        @Override
        protected DataSource getItem(int position) {
            if (nativeTracksMode && currentTrack != null) {
                if (position == 0)
                    return currentTrack;
                position--;
            }
            return super.getItem(position);
        }

        @Override
        public int getItemCount() {
            int count = super.getItemCount();
            if (nativeTracksMode && currentTrack != null)
                count++;
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            if (nativeTracksMode && currentTrack != null && position == 0)
                return 1;
            return 0;
        }

        @Override
        public void onCurrentListChanged(@NonNull List<DataSource> previousList, @NonNull List<DataSource> currentList) {
            updateEmptyView();
        }

        @NonNull
        @Override
        public BindableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            @LayoutRes int layout = viewType > 0 ? R.layout.list_item_current_track : R.layout.list_item_data_source;
            View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return viewType > 0 ? new CurrentTrackViewHolder(view) : new DataSourceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BindableViewHolder holder, int position) {
            DataSource dataSource = getItem(position);
            holder.bindView(dataSource, position);
        }

        public void setNativeTracksMode(boolean nativeTracks) {
            if (currentTrack != null) {
                if (nativeTracks && !nativeTracksMode) {
                    notifyItemInserted(0);
                    updateFloatingButtonState(false);
                }
                if (!nativeTracks && nativeTracksMode) {
                    notifyItemRemoved(0);
                    updateFloatingButtonState(true);
                }
            } else if (nativeTracks) {
                updateFloatingButtonState(true);
            }
            nativeTracksMode = nativeTracks;
            updateEmptyView();
        }

        public void setCurrentTrack(Track track) {
            if (track != null) {
                if (currentTrack == null) {
                    currentTrack = new MemoryDataSource();
                    currentTrack.tracks.add(track);
                    if (track.points.isEmpty()) {
                        since = System.currentTimeMillis();
                        fromFirstPoint = false;
                    } else {
                        since = track.points.get(0).time;
                        fromFirstPoint = true;
                    }
                    if (nativeTracksMode) {
                        notifyItemInserted(0);
                        updateFloatingButtonState(false);
                    }
                } else {
                    if (currentTrack.tracks.get(0) != track)
                        currentTrack.tracks.set(0, track);
                    if (nativeTracksMode) {
                        if (!fromFirstPoint && !track.points.isEmpty()) {
                            since = track.points.get(0).time;
                            fromFirstPoint = true;
                        }
                        notifyItemChanged(0);
                    }
                }
            } else if (currentTrack != null) {
                currentTrack.tracks.clear();
                currentTrack = null;
                if (nativeTracksMode) {
                    notifyItemRemoved(0);
                    updateFloatingButtonState(true);
                }
            }
            updateEmptyView();
        }

        private void updateEmptyView() {
            viewBinding.empty.setVisibility(this.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }

        abstract class BindableViewHolder extends RecyclerView.ViewHolder {
            public BindableViewHolder(@NonNull View view) {
                super(view);
            }
            abstract void bindView(DataSource dataSource, int position);
        }

        class DataSourceViewHolder extends BindableViewHolder {
            MaterialTextView name;
            MaterialTextView description;
            AppCompatImageView icon;
            AppCompatImageView action;

            DataSourceViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.name);
                description = view.findViewById(R.id.description);
                icon = view.findViewById(R.id.icon);
                action = view.findViewById(R.id.action);
            }

            @Override
            void bindView(DataSource dataSource, int position) {
                name.setText(dataSource.name);

                @ColorInt int color = accentColor;
                if (dataSource instanceof WaypointDbDataSource) {
                    int count = ((WaypointDataSource) dataSource).getWaypointsCount();
                    description.setText(resources.getQuantityString(R.plurals.placesCount, count, count));
                    icon.setImageResource(R.drawable.ic_points);
                    action.setVisibility(View.GONE);
                    action.setOnClickListener(null);
                    itemView.setOnClickListener(v -> dataSourceViewModel.selectDataSource(dataSource, DataSourceViewModel.MODE_SELECTOR));
                } else {
                    File file = new File(((FileDataSource) dataSource).path);
                    if (dataSource.isLoaded()) {
                        if (nativeTracksMode) {
                            Track track = ((FileDataSource) dataSource).tracks.get(0);
                            String distance = StringFormatter.distanceH(track.getDistance());
                            description.setText(distance);
                            icon.setImageResource(R.drawable.ic_track);
                            color = track.style.color;
                        } else {
                            int waypointsCount = ((FileDataSource) dataSource).waypoints.size();
                            int tracksCount = ((FileDataSource) dataSource).tracks.size();
                            int routesCount = ((FileDataSource) dataSource).routes.size();
                            StringBuilder sb = new StringBuilder();
                            if (waypointsCount > 0) {
                                sb.append(resources.getQuantityString(R.plurals.placesCount, waypointsCount, waypointsCount));
                                if (tracksCount > 0 || routesCount > 0)
                                    sb.append(", ");
                            }
                            if (tracksCount > 0) {
                                sb.append(resources.getQuantityString(R.plurals.tracksCount, tracksCount, tracksCount));
                                if (routesCount > 0)
                                    sb.append(", ");
                            }
                            if (routesCount > 0) {
                                sb.append(resources.getQuantityString(R.plurals.routesCount, routesCount, routesCount));
                            }
                            description.setText(sb);
                            if (waypointsCount > 0 && tracksCount == 0 && routesCount == 0)
                                icon.setImageResource(R.drawable.ic_points);
                            else if (tracksCount > 0 && waypointsCount == 0 && routesCount == 0)
                                icon.setImageResource(R.drawable.ic_tracks);
                            else if (routesCount > 0 && waypointsCount == 0 && tracksCount == 0)
                                icon.setImageResource(R.drawable.ic_routes);
                            else
                                icon.setImageResource(R.drawable.ic_dataset);
                        }
                        itemView.setOnClickListener(v -> dataSourceViewModel.selectDataSource(dataSource, DataSourceViewModel.MODE_SELECTOR));
                    } else {
                        String size = Formatter.formatShortFileSize(getContext(), file.length());
                        description.setText(String.format(Locale.ENGLISH, "%s – %s", size, file.getName()));
                        if (nativeTracksMode)
                            icon.setImageResource(R.drawable.ic_track);
                        else
                            icon.setImageResource(R.drawable.ic_dataset);
                        color = disabledColor;
                        itemView.setOnClickListener(v -> Snackbar.make(viewBinding.getRoot(), R.string.msgDataSourceNotLoaded, Snackbar.LENGTH_SHORT)
                                .setAction(R.string.actionEnable, view -> {
                                    mDataHolder.setDataSourceAvailability((FileDataSource) dataSource, true);
                                    notifyItemChanged(position);
                                })
                                .setAnchorView(v)
                                .show());
                    }
                    final boolean shown = dataSource.isVisible();
                    if (shown)
                        action.setImageResource(R.drawable.ic_visibility);
                    else
                        action.setImageResource(R.drawable.ic_visibility_off);
                    action.setVisibility(View.VISIBLE);
                    action.setOnClickListener(v -> {
                        mDataHolder.setDataSourceAvailability((FileDataSource) dataSource, !shown);
                        notifyItemChanged(position);
                    });
                }
                icon.setImageTintList(ColorStateList.valueOf(color));
                itemView.setOnLongClickListener(v -> {
                    PopupMenu popup = new PopupMenu(getContext(), v);
                    popup.inflate(R.menu.context_menu_data_list);
                    if (dataSource instanceof WaypointDbDataSource)
                        popup.getMenu().findItem(R.id.action_delete).setVisible(false);
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.action_share) {
                            mDataHolder.onDataSourceShare(dataSource);
                            return true;
                        }
                        if (itemId == R.id.action_delete) {
                            mDataHolder.onDataSourceDelete(dataSource);
                            return true;
                        }
                        return false;
                    });
                    popup.show();
                    return true;
                });
            }
        }

        class CurrentTrackViewHolder extends BindableViewHolder {
            MaterialTextView description;
            AppCompatImageView icon;
            AppCompatImageView resumeAction;
            AppCompatImageView pauseAction;
            AppCompatImageView stopAction;

            CurrentTrackViewHolder(View view) {
                super(view);
                description = view.findViewById(R.id.description);
                icon = view.findViewById(R.id.icon);
                resumeAction = view.findViewById(R.id.resume_action);
                pauseAction = view.findViewById(R.id.pause_action);
                stopAction = view.findViewById(R.id.stop_action);
            }

            @Override
            void bindView(DataSource dataSource, int position) {
                Track track = ((MemoryDataSource) dataSource).tracks.get(0);
                String timeTracked = (String) DateUtils.getRelativeTimeSpanString(itemView.getContext(), since);
                String distanceTracked = StringFormatter.distanceH(track.getDistance());
                description.setText(getString(R.string.msgTracked, distanceTracked, timeTracked));
                description.setVisibility(View.VISIBLE);
                itemView.setOnClickListener(v -> mTrackActionListener.onTrackDetails(track));

                TRACKING_STATE trackingState = trackViewModel.trackingState.getValue();
                @ColorInt int color = disabledColor;
                if (trackingState == TRACKING_STATE.TRACKING) {
                    pauseAction.setOnClickListener(v -> trackViewModel.trackingCommand.setValue(TRACKING_STATE.PAUSED));
                    pauseAction.setVisibility(View.VISIBLE);
                    resumeAction.setVisibility(View.GONE);
                    color = accentColor;
                } else if (trackingState == TRACKING_STATE.PAUSED) {
                    resumeAction.setOnClickListener(v -> trackViewModel.trackingCommand.setValue(TRACKING_STATE.PENDING));
                    resumeAction.setVisibility(View.VISIBLE);
                    pauseAction.setVisibility(View.GONE);
                }
                icon.setImageTintList(ColorStateList.valueOf(color));
                stopAction.setOnClickListener(v -> trackViewModel.trackingCommand.setValue(TRACKING_STATE.DISABLED));
            }
        }
    }

    private final RecyclerView.AdapterDataObserver adapterDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (positionStart == 0 && itemCount == 1)
                viewBinding.list.scrollToPosition(positionStart);
        }
    };

    public static final DiffUtil.ItemCallback<DataSource> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DataSource>() {
                @Override
                public boolean areItemsTheSame(@NonNull DataSource oldSource, @NonNull DataSource newSource) {
                    return oldSource == newSource;
                }

                @Override
                public boolean areContentsTheSame(@NonNull DataSource oldSource, @NonNull DataSource newSource) {
                    return false; // TODO: define proper comparison respecting visible and loaded states
                }
            };

}
