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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StableIdKeyProvider;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;

import org.oscim.core.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.TreeMap;

import mobi.maptrek.Configuration;
import mobi.maptrek.DataHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Route;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.DataSourceUpdateListener;
import mobi.maptrek.data.source.MemoryDataSource;
import mobi.maptrek.data.source.RouteDataSource;
import mobi.maptrek.data.source.TrackDataSource;
import mobi.maptrek.data.source.WaypointDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.databinding.ListWithEmptyViewBinding;
import mobi.maptrek.dialogs.CoordinatesInput;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.JosmCoordinatesParser;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.viewmodels.DataSourceViewModel;
import mobi.maptrek.viewmodels.MapViewModel;

public class DataList extends Fragment implements CoordinatesInput.CoordinatesInputDialogCallback {
    private static final Logger logger = LoggerFactory.getLogger(DataList.class);
    private static final String lineSeparator = System.getProperty("line.separator", "\n");

    private OnWaypointActionListener mWaypointActionListener;
    private OnTrackActionListener mTrackActionListener;
    private OnRouteActionListener mRouteActionListener;
    private FragmentHolder mFragmentHolder;
    private DataHolder mDataHolder;
    private FloatingActionButton mFloatingButton;

    private GeoPoint coordinates = null;

    private SelectionTracker<Long> selectionTracker;
    private ActionMode actionMode;
    private DataSourceViewModel dataSourceViewModel;
    private ListWithEmptyViewBinding viewBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = ListWithEmptyViewBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataSourceViewModel = new ViewModelProvider(requireActivity()).get(DataSourceViewModel.class);
        dataSourceViewModel.selectedDataSource.observe(getViewLifecycleOwner(), dataSource -> {
            logger.debug("dataSource changed");
            setDataSource(dataSource, savedInstanceState);
            if (dataSource instanceof WaypointDbDataSource) {
                mFloatingButton = mFragmentHolder.enableListActionButton(R.drawable.ic_add_location, v -> {
                    if (!isAdded()) // automated testing presses buttons too quickly
                        return;
                    CoordinatesInput.Builder builder = new CoordinatesInput.Builder();
                    CoordinatesInput coordinatesInput = builder.setCallbacks(DataList.this)
                            .setTitle(getString(R.string.titleCoordinatesInput))
                            .create();
                    coordinatesInput.show(getParentFragmentManager(), "pointCoordinatesInput");
                });
            } else {
                mFragmentHolder.disableListActionButton();
                mFloatingButton = null;
            }
        });

        MapViewModel mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        mapViewModel.currentLocation.observe(getViewLifecycleOwner(), location -> {
            logger.debug("location changed");
            DataSource dataSource = dataSourceViewModel.selectedDataSource.getValue();
            if (dataSource == null)
                return;
            if ("unknown".equals(location.getProvider())) {
                coordinates = null;
            } else {
                coordinates = new GeoPoint(location.getLatitude(), location.getLongitude());
            }
            if (selectionTracker == null || !selectionTracker.hasSelection()) {
                dataSource.setReferenceLocation(coordinates);
                //noinspection rawtypes
                RecyclerView.Adapter adapter = viewBinding.list.getAdapter();
                if (adapter != null)
                    adapter.notifyDataSetChanged();
            }
        });

        // selection tracker does not start without adapter
        DataList.DataListAdapter adapter = new DataList.DataListAdapter(new MemoryDataSource(), true);
        viewBinding.list.setAdapter(adapter);

        // check if we are open above another fragment and make us fully overlap it
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent.getChildCount() > 1) {
            int height = 0;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child != view) {
                    int childHeight = child.getHeight();
                    if (height < childHeight)
                        height = childHeight;
                }
            }
            view.setMinimumHeight(height);
        }

        if (HelperUtils.needsTargetedAdvice(Configuration.ADVICE_VIEW_DATA_ITEM)) {
            ViewTreeObserver vto = view.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    DataSource dataSource = dataSourceViewModel.selectedDataSource.getValue();
                    if (dataSource == null)
                        return;
                    Cursor cursor = dataSource.getCursor();
                    if (cursor.getCount() > 0) {
                        View view = viewBinding.list.getChildAt(0).findViewById(R.id.view);
                        if (view == null && viewBinding.list.getChildCount() > 1) // looks like it is a separator
                            view = viewBinding.list.getChildAt(1).findViewById(R.id.view);
                        if (view == null)
                            return;
                        Rect r = new Rect();
                        view.getGlobalVisibleRect(r);
                        if (r.isEmpty())
                            return;
                        HelperUtils.showTargetedAdvice(getActivity(), Configuration.ADVICE_VIEW_DATA_ITEM, R.string.advice_view_data_item, r);
                    }
                    cursor.close();
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        selectionTracker.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        mFragmentHolder.disableListActionButton();
        mFloatingButton = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mWaypointActionListener = (OnWaypointActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnWaypointActionListener");
        }
        try {
            mTrackActionListener = (OnTrackActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnTrackActionListener");
        }
        try {
            mRouteActionListener = (OnRouteActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnRouteActionListener");
        }
        try {
            mDataHolder = (DataHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement DataHolder");
        }
        mFragmentHolder = (FragmentHolder) context;
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);

        if (selectionTracker != null && selectionTracker.hasSelection() && actionMode == null) {
            actionMode = requireActivity().startActionMode(new ActionModeController(), ActionMode.TYPE_FLOATING);
            int count = selectionTracker.getSelection().size();
            actionMode.setTitle(getResources().getQuantityString(R.plurals.itemsSelected, count, count)); // not used in floating mode, left for reference
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        backPressedCallback.remove();
        mWaypointActionListener = null;
        mTrackActionListener = null;
        mRouteActionListener = null;
        mFragmentHolder = null;
        mDataHolder = null;
    }

    @Override
    public void onDestroyView() {
        closeAdapterCursor();
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        CoordinatesInput coordinatesInput = (CoordinatesInput) getParentFragmentManager().findFragmentByTag("pointCoordinatesInput");
        if (coordinatesInput != null) {
            coordinatesInput.setCallback(this);
        }
    }

    public void setDataSource(DataSource dataSource, Bundle savedInstanceState) {
        boolean extraSources = dataSourceViewModel.hasExtraDataSources();
        StringBuilder stringBuilder = new StringBuilder();
        if (dataSourceViewModel.waypointDbDataSource == dataSource) {
            stringBuilder.append(getString(R.string.msgEmptyPlaceList));
            if (!extraSources) {
                stringBuilder.append(lineSeparator);
                stringBuilder.append(lineSeparator);
                stringBuilder.append(getString(R.string.msgNoFileDataSources));
            }
        } else {
            stringBuilder.append(getString(R.string.msgEmptyDataSource));
        }
        viewBinding.empty.setText(stringBuilder.toString());
        closeAdapterCursor();
        boolean showFooter = dataSourceViewModel.waypointDbDataSource == dataSource && !extraSources;
        DataListAdapter adapter = new DataListAdapter(dataSource, showFooter);
        viewBinding.list.setAdapter(adapter);

        selectionTracker = new SelectionTracker.Builder<>(
                "data-selection",
                viewBinding.list,
                new StableIdKeyProvider(viewBinding.list),
                new DataDetailsLookup(viewBinding.list),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(selectionPredicate).build();
        selectionTracker.addObserver(selectionObserver);
        if (savedInstanceState != null)
            selectionTracker.onRestoreInstanceState(savedInstanceState);
    }

    private void closeAdapterCursor() {
        DataListAdapter adapter = (DataListAdapter) viewBinding.list.getAdapter();
        if (adapter != null)
            adapter.closeCursor();
    }

    private void shareSelectedItems() {
        HashSet<Waypoint> waypoints = new HashSet<>();
        HashSet<Track> tracks = new HashSet<>();
        HashSet<Route> routes = new HashSet<>();
        populateSelectedItems(waypoints, tracks, routes);
        MemoryDataSource dataSource = new MemoryDataSource();
        dataSource.waypoints.addAll(waypoints);
        dataSource.tracks.addAll(tracks);
        dataSource.routes.addAll(routes);
        mDataHolder.onDataSourceShare(dataSource);
    }

    private void deleteSelectedItems() {
        HashSet<Waypoint> waypoints = new HashSet<>();
        HashSet<Track> tracks = new HashSet<>();
        HashSet<Route> routes = new HashSet<>();
        populateSelectedItems(waypoints, tracks, routes);
        if (waypoints.size() > 0)
            mWaypointActionListener.onWaypointsDelete(waypoints);
        if (tracks.size() > 0)
            mTrackActionListener.onTracksDelete(tracks);
        if (routes.size() > 0)
            mRouteActionListener.onRoutesDelete(routes);
    }

    private void populateSelectedItems(HashSet<Waypoint> waypoints, HashSet<Track> tracks, HashSet<Route> routes) {
        DataSource dataSource = dataSourceViewModel.selectedDataSource.getValue();
        if (dataSource == null)
            return;
        Cursor cursor = dataSource.getCursor();
        for (int position = 0; position < cursor.getCount(); position++) {
            cursor.moveToPosition(position);
            int type = dataSource.getDataType(position);
            if (type == DataSource.TYPE_WAYPOINT) {
                Waypoint waypoint = ((WaypointDataSource) dataSource).cursorToWaypoint(cursor);
                if (selectionTracker.isSelected(waypoint._id))
                    waypoints.add(waypoint);
            } else if (type == DataSource.TYPE_TRACK) {
                Track track = ((TrackDataSource) dataSource).cursorToTrack(cursor);
                if (selectionTracker.isSelected((long) track.id))
                    tracks.add(track);
            } else if (type == DataSource.TYPE_ROUTE) {
                Route route = ((RouteDataSource) dataSource).cursorToRoute(cursor);
                if (selectionTracker.isSelected((long) route.id))
                    routes.add(route);
            }
        }
        cursor.close();
    }

    @Override
    public void onTextInputPositiveClick(String id, String inputText) {
        String[] lines = inputText.split(Objects.requireNonNull(lineSeparator));
        boolean errors = false;
        for (String line : lines) {
            if (line.length() == 0)
                continue;
            try {
                JosmCoordinatesParser.Result result = JosmCoordinatesParser.parseWithResult(line);
                String name = null;
                if (result.offset < line.length())
                    name = line.substring(result.offset).trim();
                if (name == null || "".equals(name))
                    name = getString(R.string.place_name, Configuration.getPointsCounter());
                mWaypointActionListener.onWaypointCreate(result.coordinates, name, true, false);
            } catch (IllegalArgumentException e) {
                errors = true;
            }
        }
        if (errors)
            HelperUtils.showError(getString(R.string.msgParseMultipleCoordinatesFailed), mFragmentHolder.getCoordinatorLayout());
    }

    @Override
    public void onTextInputNegativeClick(String id) {
    }

    private void updateListViews() {
        LinearLayoutManager lm = (LinearLayoutManager) viewBinding.list.getLayoutManager();
        if (lm == null)
            return;
        boolean hasSelection = selectionTracker.hasSelection();
        int firstVisible = lm.findFirstVisibleItemPosition();
        int lastVisible = lm.findLastVisibleItemPosition();
        for (int i = firstVisible; i <= lastVisible; i++) {
            View visibleView = lm.findViewByPosition(i);
            if (visibleView == null)
                continue;
            View viewButton = visibleView.findViewById(R.id.view);
            View checkbox = visibleView.findViewById(R.id.checkbox);
            if (viewButton == null || checkbox == null)
                continue;
            if (hasSelection) {
                viewButton.setVisibility(View.GONE);
                checkbox.setVisibility(View.VISIBLE);
            } else {
                viewButton.setVisibility(View.VISIBLE);
                checkbox.setVisibility(View.GONE);
            }
        }
    }

    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (actionMode != null) {
                actionMode.finish();
                actionMode = null;
            }
        }
    };

    private final SelectionTracker.SelectionPredicate<Long> selectionPredicate = new SelectionTracker.SelectionPredicate<Long>() {
        @Override
        public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
            return key > 0;
        }

        @Override
        public boolean canSetStateAtPosition(int position, boolean nextState) {
            DataListAdapter adapter = (DataListAdapter) viewBinding.list.getAdapter();
            if (adapter != null)
                return adapter.canSetStateForPosition(position);
            else
                return false;
        }

        @Override
        public boolean canSelectMultiple() {
            return true;
        }
    };

    SelectionTracker.SelectionObserver<Long> selectionObserver = new SelectionTracker.SelectionObserver<Long>() {
        @Override
        public void onSelectionRefresh() {
            super.onSelectionRefresh();
            if (!selectionTracker.hasSelection() && actionMode != null) {
                actionMode.finish();
                actionMode = null;
            }
        }

        @Override
        public void onSelectionChanged() {
            super.onSelectionChanged();
            if (selectionTracker.hasSelection()) {
                if (actionMode == null)
                    actionMode = requireActivity().startActionMode(new ActionModeController(), ActionMode.TYPE_FLOATING);
                int count = selectionTracker.getSelection().size();
                actionMode.setTitle(getResources().getQuantityString(R.plurals.itemsSelected, count, count)); // not used in floating mode, left for reference
            } else if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
            }
        }

        @Override
        public void onSelectionRestored() {
            super.onSelectionRestored();
            logger.debug("onSelectionRestored {}", selectionTracker.getSelection().size());
        }
    };

    public class DataListAdapter extends RecyclerView.Adapter<DataListAdapter.BindableViewHolder> implements DataSourceUpdateListener {
        private final static int TYPE_HEADER = 99;
        private final static int TYPE_FOOTER = 98;

        private final DataSource dataSource;
        private Cursor cursor;
        private final boolean showFooter;
        @ColorInt
        private final int darkColor;
        private final TreeMap<Integer, Integer> headers = new TreeMap<>();

        protected DataListAdapter(DataSource dataSource, boolean showFooter) {
            this.dataSource = dataSource;
            this.cursor = dataSource.getCursor();
            dataSource.addListener(this);
            this.showFooter = showFooter;
            darkColor = getResources().getColor(R.color.colorPrimaryDark, requireContext().getTheme());
            setHasStableIds(true);
            calculateHeaders();
            setEmptyView();
        }

        protected void calculateHeaders() {
            headers.clear();
            if (dataSource instanceof WaypointDbDataSource) // shortcut for no headers
                return;
            int wptCount = 0, trkCount = 0, rteCount = 0;
            if (dataSource instanceof WaypointDataSource)
                wptCount = ((WaypointDataSource) dataSource).getWaypointsCount();
            if (dataSource instanceof TrackDataSource)
                trkCount = ((TrackDataSource) dataSource).getTracksCount();
            if (dataSource instanceof RouteDataSource)
                rteCount = ((RouteDataSource) dataSource).getRoutesCount();
            if (wptCount > 0 && (trkCount > 0 || rteCount > 0))
                headers.put(0, DataSource.TYPE_WAYPOINT);
            if (trkCount > 0 && (headers.size() > 0 || rteCount > 0))
                headers.put(wptCount + headers.size(), DataSource.TYPE_TRACK);
            if (rteCount > 0 && headers.size() > 0)
                headers.put(wptCount + trkCount + headers.size(), DataSource.TYPE_ROUTE);
        }

        private void setEmptyView() {
            if (cursor.getCount() == 0)
                viewBinding.empty.setVisibility(View.VISIBLE);
            else
                viewBinding.empty.setVisibility(View.GONE);
        }

        private int adjustPosition(int position) {
            if (headers.isEmpty())
                return position;
            int i = 0;
            for (int pos : headers.navigableKeySet()) {
                if (pos > position)
                    break;
                i++;
            }
            return position - i;
        }

        @NonNull
        @Override
        public DataListAdapter.BindableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = 0;
            // https://github.com/nicbell/material-lists
            switch (viewType) {
                case DataSource.TYPE_WAYPOINT:
                    layout = R.layout.list_item_waypoint;
                    break;
                case DataSource.TYPE_TRACK:
                    layout = R.layout.list_item_track;
                    break;
                case DataSource.TYPE_ROUTE:
                    layout = R.layout.list_item_route;
                    break;
                case TYPE_HEADER:
                    layout = R.layout.list_item_header;
                    break;
                case TYPE_FOOTER:
                    layout = R.layout.list_footer_data_source;
            }
            View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            if (viewType == TYPE_HEADER)
                return new HeaderViewHolder(view);
            else if (viewType == TYPE_FOOTER)
                return new FooterViewHolder(view);
            else
                return new DataViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BindableViewHolder holder, int position) {
            holder.bindView(position);
        }

        @Override
        public int getItemViewType(int position) {
            if (headers.containsKey(position))
                return TYPE_HEADER;
            position = adjustPosition(position);
            if (position == cursor.getCount())
                return TYPE_FOOTER;
            return dataSource.getDataType(position);
        }

        @Override
        public long getItemId(int position) {
            if (headers.containsKey(position))
                return -TYPE_HEADER-position;
            position = adjustPosition(position);
            if (position == cursor.getCount())
                return -TYPE_FOOTER;
            cursor.moveToPosition(position);
            int viewType = dataSource.getDataType(position);
            if (viewType == DataSource.TYPE_WAYPOINT) {
                final Waypoint waypoint = ((WaypointDataSource) dataSource).cursorToWaypoint(cursor);
                return waypoint._id;
            } else if (viewType == DataSource.TYPE_TRACK) {
                final Track track = ((TrackDataSource) dataSource).cursorToTrack(cursor);
                return track.id;
            } else if (viewType == DataSource.TYPE_ROUTE) {
                final Route route = ((RouteDataSource) dataSource).cursorToRoute(cursor);
                return route.id;
            }
            return -1;
        }

        @Override
        public int getItemCount() {
            int count = cursor.getCount();
            if (count > 0 && showFooter) // footer is shown only if list contains data
                count++;
            return count + headers.size();
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onDataSourceUpdated() {
            logger.debug("onDataSourceUpdated");
            cursor.close();
            cursor = dataSource.getCursor();
            calculateHeaders();
            setEmptyView();
            notifyDataSetChanged();
        }

        public boolean canSetStateForPosition(int position) {
            if (headers.containsKey(position))
                return false;
            return position != cursor.getCount() + headers.size();
        }

        public void closeCursor() {
            dataSource.removeListener(this);
            cursor.close();
        }

        abstract class BindableViewHolder extends RecyclerView.ViewHolder {
            public BindableViewHolder(@NonNull View view) {
                super(view);
            }
            abstract void bindView(int position);
            abstract ItemDetailsLookup.ItemDetails<Long> getItemDetails();
        }

        class HeaderViewHolder extends BindableViewHolder {
            MaterialTextView title;
            public HeaderViewHolder(@NonNull View view) {
                super(view);
                title = view.findViewById(R.id.title);
            }

            @Override
            void bindView(int position) {
                Integer type = headers.get(position);
                if (type == null)
                    return;
                int string = 0;
                switch (type) {
                    case DataSource.TYPE_WAYPOINT:
                        string = R.string.places;
                        break;
                    case DataSource.TYPE_TRACK:
                        string = R.string.tracks;
                        break;
                    case DataSource.TYPE_ROUTE:
                        string = R.string.routes;
                        break;
                }
                title.setText(getText(string));
            }

            @Override
            ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
                int position = getBindingAdapterPosition();
                return new DataItemDetails(position, (long) (-TYPE_HEADER-position));
            }
        }

        class FooterViewHolder extends BindableViewHolder {
            public FooterViewHolder(@NonNull View view) {
                super(view);
            }

            @Override
            void bindView(int position) {
                // do nothing
            }

            @Override
            ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
                int position = getBindingAdapterPosition();
                return new DataItemDetails(position, (long) (-TYPE_FOOTER));
            }
        }

        class DataViewHolder extends BindableViewHolder {
            long id;
            MaterialTextView name;
            MaterialTextView distance;
            AppCompatImageView icon;
            AppCompatImageView viewButton;
            CheckBox checkbox;

            DataViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.name);
                distance = view.findViewById(R.id.distance);
                icon = view.findViewById(R.id.icon);
                viewButton = view.findViewById(R.id.view);
                checkbox = view.findViewById(R.id.checkbox);
            }

            @SuppressLint("SetTextI18n")
            @Override
            void bindView(int position) {
                int viewType = getItemViewType();
                position = adjustPosition(position);
                cursor.moveToPosition(position);

                @DrawableRes int iconRes = R.drawable.ic_info_outline;
                @ColorInt int color = darkColor;

                if (viewType == DataSource.TYPE_WAYPOINT) {
                    final Waypoint waypoint = ((WaypointDataSource) dataSource).cursorToWaypoint(cursor);
                    id = waypoint._id;
                    name.setText(waypoint.name);
                    if (coordinates != null) {
                        double dist = coordinates.vincentyDistance(waypoint.coordinates);
                        double bearing = coordinates.bearingTo(waypoint.coordinates);
                        distance.setText(StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing));
                        distance.setVisibility(View.VISIBLE);
                    } else {
                        distance.setVisibility(View.GONE);
                    }
                    viewButton.setOnClickListener(v -> {
                        mWaypointActionListener.onWaypointView(waypoint);
                        mFragmentHolder.disableListActionButton();
                        mFragmentHolder.popAll();
                    });
                    iconRes = R.drawable.ic_point;
                    color = waypoint.style.color;
                    itemView.setOnClickListener(v -> mWaypointActionListener.onWaypointDetails(waypoint, true));
                } else if (viewType == DataSource.TYPE_TRACK) {
                    final Track track = ((TrackDataSource) dataSource).cursorToTrack(cursor);
                    id = track.id;
                    name.setText(track.name);
                    distance.setText(StringFormatter.distanceH(track.getDistance()));
                    viewButton.setOnClickListener(v -> {
                        mTrackActionListener.onTrackView(track);
                        mFragmentHolder.disableListActionButton();
                        mFragmentHolder.popAll();
                    });
                    iconRes = R.drawable.ic_track;
                    color = track.style.color;
                    itemView.setOnClickListener(v -> mTrackActionListener.onTrackDetails(track));
                } else if (viewType == DataSource.TYPE_ROUTE) {
                    final Route route = ((RouteDataSource) dataSource).cursorToRoute(cursor);
                    id = route.id;
                    name.setText(route.name);
                    distance.setText(StringFormatter.distanceH(route.getTotalDistance()));
                    viewButton.setOnClickListener(v -> {
                        mRouteActionListener.onRouteView(route);
                        mFragmentHolder.disableListActionButton();
                        mFragmentHolder.popAll();
                    });
                    iconRes = R.drawable.ic_route;
                    color = route.style.color;
                    itemView.setOnClickListener(v -> mRouteActionListener.onRouteDetails(route));
                }
                icon.setImageResource(iconRes);
                icon.setImageTintList(ColorStateList.valueOf(color));
                if (selectionTracker.hasSelection()) {
                    viewButton.setVisibility(View.GONE);
                    checkbox.setVisibility(View.VISIBLE);
                } else {
                    viewButton.setVisibility(View.VISIBLE);
                    checkbox.setVisibility(View.GONE);
                }
                checkbox.setChecked(selectionTracker.isSelected(id));
            }

            public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
                return new DataItemDetails(getBindingAdapterPosition(), id);
            }
        }
    }

    private static class DataDetailsLookup extends ItemDetailsLookup<Long> {
        private final RecyclerView recyclerView;

        public DataDetailsLookup(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                return ((DataListAdapter.BindableViewHolder) viewHolder).getItemDetails();
            }
            return null;
        }
    }

    public static class DataItemDetails extends ItemDetailsLookup.ItemDetails<Long> {
        private final int position;
        private final Long key;

        public DataItemDetails(int position, Long id) {
            this.position = position;
            this.key = id;
        }

        @Override
        public int getPosition() {
            return position;
        }

        @Nullable
        @Override
        public Long getSelectionKey() {
            return key;
        }
    }

    private class ActionModeController implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.context_menu_waypoint_list, menu);
            if (mFloatingButton != null)
                mFloatingButton.setVisibility(View.GONE);
            updateListViews();
            backPressedCallback.setEnabled(true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if (!isAdded())
                return false;

            int itemId = menuItem.getItemId();
            if (itemId == R.id.action_share) {
                shareSelectedItems();
                actionMode.finish();
                return true;
            }
            if (itemId == R.id.action_delete) {
                deleteSelectedItems();
                actionMode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            selectionTracker.clearSelection();
            updateListViews();
            backPressedCallback.setEnabled(false);
            if (mFloatingButton != null)
                mFloatingButton.setVisibility(View.VISIBLE);
        }
    }
}
