package mobi.maptrek.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.oscim.core.GeoPoint;

import java.util.HashSet;

import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.DataSourceUpdateListener;
import mobi.maptrek.data.source.TrackDataSource;
import mobi.maptrek.data.source.WaypointDataSource;
import mobi.maptrek.util.StringFormatter;

public class DataList extends ListFragment implements DataSourceUpdateListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_EMPTY_MESSAGE = "msg";
    public static final String ARG_HEIGHT = "hgt";

    private DataListAdapter mAdapter;
    private DataSource mDataSource;
    private boolean mIsMultiDataSource;
    private OnWaypointActionListener mWaypointActionListener;
    private OnTrackActionListener mTrackActionListener;
    private FragmentHolder mFragmentHolder;

    private double mLatitude;
    private double mLongitude;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_with_empty_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle arguments = getArguments();
        mLatitude = arguments.getDouble(ARG_LATITUDE);
        mLongitude = arguments.getDouble(ARG_LONGITUDE);
        boolean emptyMessage = arguments.getBoolean(ARG_EMPTY_MESSAGE);
        int minHeight = arguments.getInt(ARG_HEIGHT, 0);

        if (savedInstanceState != null) {
            mLatitude = savedInstanceState.getDouble(ARG_LATITUDE);
            mLongitude = savedInstanceState.getDouble(ARG_LONGITUDE);
        }

        if (emptyMessage) {
            TextView emptyView = (TextView) getListView().getEmptyView();
            if (emptyView != null)
                emptyView.setText(R.string.msg_empty_waypoint_list);
        }

        mAdapter = new DataListAdapter(getActivity(), mDataSource.getCursor(), 0);
        setListAdapter(mAdapter);

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(mMultiChoiceModeListener);

        View rootView = getView();
        if (rootView != null)
            rootView.setMinimumHeight(minHeight);

        if (emptyMessage) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            listView.addFooterView(inflater.inflate(R.layout.list_footer_data_source, listView, false));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mWaypointActionListener = (OnWaypointActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnWaypointActionListener");
        }
        try {
            mTrackActionListener = (OnTrackActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTrackActionListener");
        }
        mFragmentHolder = (FragmentHolder) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDataSource.removeListener(this);
        mWaypointActionListener = null;
        mTrackActionListener = null;
        mFragmentHolder = null;
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        int itemType = mDataSource.getDataType(position);
        if (itemType == DataSource.TYPE_WAYPOINT) {
            Waypoint waypoint = ((WaypointDataSource) mDataSource).cursorToWaypoint(cursor);
            mWaypointActionListener.onWaypointDetails(waypoint, true);
        } else if (itemType == DataSource.TYPE_TRACK) {
            Track track = ((TrackDataSource) mDataSource).cursorToTrack(cursor);
            mTrackActionListener.onTrackDetails(track);
        }
    }

    public void setDataSource(DataSource dataSource) {
        mDataSource = dataSource;
        mIsMultiDataSource = mDataSource instanceof WaypointDataSource &&
                mDataSource instanceof TrackDataSource &&
                ((WaypointDataSource) mDataSource).getWaypointsCount() > 0 &&
                ((TrackDataSource) mDataSource).getTracksCount() > 0;
        mDataSource.addListener(this);
    }

    @Override
    public void onDataSourceUpdated() {
        if (mAdapter != null) {
            mAdapter.changeCursor(mDataSource.getCursor());
            mIsMultiDataSource = mDataSource instanceof WaypointDataSource &&
                    mDataSource instanceof TrackDataSource &&
                    ((WaypointDataSource) mDataSource).getWaypointsCount() > 0 &&
                    ((TrackDataSource) mDataSource).getTracksCount() > 0;
        }
    }

    private void deleteSelectedItems() {
        SparseBooleanArray positions = getListView().getCheckedItemPositions();
        HashSet<Waypoint> waypoints = new HashSet<>();
        HashSet<Track> tracks = new HashSet<>();
        for (int position = 0; position < mAdapter.getCount(); position++) {
            if (positions.get(position)) {
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                int type = mDataSource.getDataType(position);
                if (type == DataSource.TYPE_WAYPOINT) {
                    Waypoint waypoint = ((WaypointDataSource) mDataSource).cursorToWaypoint(cursor);
                    waypoints.add(waypoint);
                } else if (type == DataSource.TYPE_TRACK) {
                    Track track = ((TrackDataSource) mDataSource).cursorToTrack(cursor);
                    tracks.add(track);
                }
            }
        }
        if (waypoints.size() > 0)
            mWaypointActionListener.onWaypointsDelete(waypoints);
        if (tracks.size() > 0)
            mTrackActionListener.onTracksDelete(tracks);
    }

    public class DataListAdapter extends CursorAdapter {
        private static final int STATE_UNKNOWN = 0;
        private static final int STATE_SECTIONED_CELL = 1;
        private static final int STATE_REGULAR_CELL = 2;
        @ColorInt
        private int mAccentColor;
        private int[] mCellStates;

        private LayoutInflater mInflater;

        public DataListAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mAccentColor = getResources().getColor(R.color.colorAccent, context.getTheme());
            mCellStates = cursor == null ? null : new int[cursor.getCount()];
        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            mCellStates = cursor == null ? null : new int[cursor.getCount()];
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            int viewType = getItemViewType(cursor.getPosition());
            View view = null;
            if (viewType == DataSource.TYPE_WAYPOINT) {
                view = mInflater.inflate(R.layout.list_item_waypoint, parent, false);
            } else if (viewType == DataSource.TYPE_TRACK) {
                view = mInflater.inflate(R.layout.list_item_track, parent, false);
            }
            if (view != null) {
                ItemHolder holder = new ItemHolder();
                holder.separator = (TextView) view.findViewById(R.id.separator);
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.distance = (TextView) view.findViewById(R.id.distance);
                holder.icon = (ImageView) view.findViewById(R.id.icon);
                holder.viewButton = (ImageView) view.findViewById(R.id.view);
                view.setTag(holder);
            }
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final int position = cursor.getPosition();
            int viewType = getItemViewType(position);
            ItemHolder holder = (ItemHolder) view.getTag();

            boolean needSeparator = false;

            switch (mCellStates[position]) {
                case STATE_SECTIONED_CELL:
                    needSeparator = true;
                    break;

                case STATE_REGULAR_CELL:
                    needSeparator = false;
                    break;

                case STATE_UNKNOWN:
                default:
                    if (mIsMultiDataSource && position == 0) {
                        needSeparator = true;
                    } else if (mIsMultiDataSource) {
                        int prevViewType = getItemViewType(position - 1);
                        if (prevViewType != viewType)
                            needSeparator = true;
                    }
                    mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
                    break;
            }

            if (needSeparator) {
                holder.separator.setText(getText(viewType == DataSource.TYPE_WAYPOINT ? R.string.waypoints : R.string.tracks));
                holder.separator.setVisibility(View.VISIBLE);
            } else {
                holder.separator.setVisibility(View.GONE);
            }

            boolean isChecked = getListView().isItemChecked(position);
            boolean hasChecked = getListView().getCheckedItemCount() > 0;
            @DrawableRes int icon = R.drawable.ic_info_outline;
            @ColorInt int color = R.color.colorPrimaryDark;

            if (viewType == DataSource.TYPE_WAYPOINT) {
                final Waypoint waypoint = ((WaypointDataSource) mDataSource).cursorToWaypoint(cursor);
                double dist = GeoPoint.distance(mLatitude, mLongitude, waypoint.latitude, waypoint.longitude);
                double bearing = GeoPoint.bearing(mLatitude, mLongitude, waypoint.latitude, waypoint.longitude);
                String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
                holder.name.setText(waypoint.name);
                holder.distance.setText(distance);
                holder.viewButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mWaypointActionListener.onWaypointView(waypoint);
                        mFragmentHolder.popAll();
                    }
                });
                icon = R.drawable.ic_point;
                color = waypoint.style.color;
            } else if (viewType == DataSource.TYPE_TRACK) {
                final Track track = ((TrackDataSource) mDataSource).cursorToTrack(cursor);
                String distance = StringFormatter.distanceH(track.getDistance());
                holder.name.setText(track.name);
                holder.distance.setText(distance);
                holder.viewButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTrackActionListener.onTrackView(track);
                        mFragmentHolder.popAll();
                    }
                });
                icon = R.drawable.ic_track;
                color = track.style.color;
            }
            if (hasChecked) {
                holder.viewButton.setVisibility(View.GONE);
            } else {
                holder.viewButton.setVisibility(View.VISIBLE);
            }
            if (isChecked) {
                icon = R.drawable.ic_done;
                color = mAccentColor;
            }
            holder.icon.setImageResource(icon);
            Drawable background = holder.icon.getBackground().mutate();
            if (background instanceof ShapeDrawable) {
                ((ShapeDrawable) background).getPaint().setColor(color);
            } else if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            }

        }

        @DataSource.DataType
        @Override
        public int getItemViewType(int position) {
            return mDataSource.getDataType(position);
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }
    }

    private static class ItemHolder {
        public TextView separator;
        TextView name;
        TextView distance;
        ImageView icon;
        ImageView viewButton;
    }

    private AbsListView.MultiChoiceModeListener mMultiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            ListView listView = getListView();
            int count = listView.getCheckedItemCount();
            mode.setTitle(getResources().getQuantityString(R.plurals.itemsSelected, count, count));
            // Update (redraw) list item view
            int start = listView.getFirstVisiblePosition();
            for (int i = start, j = listView.getLastVisiblePosition(); i <= j; i++) {
                if (position == i) {
                    View view = listView.getChildAt(i - start);
                    listView.getAdapter().getView(i, view, listView);
                    break;
                }
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteSelectedItems();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_menu_waypoint_list, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };
}
