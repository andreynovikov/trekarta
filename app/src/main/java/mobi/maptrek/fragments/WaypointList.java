package mobi.maptrek.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.oscim.core.GeoPoint;

import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.WaypointDataSource;
import mobi.maptrek.data.WaypointDataSourceUpdateListener;
import mobi.maptrek.util.MarkerFactory;
import mobi.maptrek.util.StringFormatter;

public class WaypointList extends ListFragment implements WaypointDataSourceUpdateListener {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";

    private WaypointListAdapter mAdapter;
    private WaypointDataSource mDataSource;
    private OnWaypointActionListener mListener;

    private double mLatitude;
    private double mLongitude;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_with_empty_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLatitude = getArguments().getDouble(ARG_LATITUDE);
        mLongitude = getArguments().getDouble(ARG_LONGITUDE);

        if (savedInstanceState != null) {
            mLatitude = savedInstanceState.getDouble(ARG_LATITUDE);
            mLongitude = savedInstanceState.getDouble(ARG_LONGITUDE);
        }

        TextView emptyView = (TextView) getListView().getEmptyView();
        if (emptyView != null)
            emptyView.setText(R.string.msg_empty_waypoint_list);

        mAdapter = new WaypointListAdapter(getActivity(), mDataSource.getCursor(), 0);
        setListAdapter(mAdapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnWaypointActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnWaypointActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDataSource.removeListener(this);
        mDataSource = null;
        mListener = null;
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        Waypoint waypoint = mDataSource.cursorToWaypoint(cursor);
        mListener.onWaypointDetails(waypoint, true);
    }

    public void setDataSource(WaypointDataSource dataSource) {
        mDataSource = dataSource;
        mDataSource.addListener(this);
        //mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDataSourceUpdated() {
        if (mAdapter != null) {
            mAdapter.changeCursor(mDataSource.getCursor());
        }
    }

    public class WaypointListAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public WaypointListAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.list_item_waypoint, parent, false);
            WaypointItemHolder holder = new WaypointItemHolder();
            holder.name = (TextView) view.findViewById(R.id.name);
            holder.distance = (TextView) view.findViewById(R.id.distance);
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.viewButton = (ImageView) view.findViewById(R.id.view);
            holder.navigateButton = (ImageView) view.findViewById(R.id.navigate);
            holder.navigateButton.setVisibility(View.GONE);
            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            WaypointItemHolder holder = (WaypointItemHolder) view.getTag();
            final Waypoint waypoint = mDataSource.cursorToWaypoint(cursor);
            double dist = GeoPoint.distance(mLatitude, mLongitude, waypoint.latitude, waypoint.longitude);
            double bearing = GeoPoint.bearing(mLatitude, mLongitude, waypoint.latitude, waypoint.longitude);
            String distance = StringFormatter.distanceH(dist) + " " + StringFormatter.angleH(bearing);
            int color = waypoint.color;
            if (color == 0)
                color = MarkerFactory.DEFAULT_COLOR;
            holder.name.setText(waypoint.name);
            holder.distance.setText(distance);
            Drawable background = holder.icon.getBackground().mutate();
            if (background instanceof ShapeDrawable) {
                ((ShapeDrawable) background).getPaint().setColor(color);
            } else if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            }
            holder.viewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onWaypointView(waypoint);
                    getActivity().onBackPressed();
                }
            });
            holder.navigateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onWaypointNavigate(waypoint);
                    getActivity().onBackPressed();
                }
            });
        }
    }

    private static class WaypointItemHolder {
        TextView name;
        TextView distance;
        ImageView icon;
        ImageView viewButton;
        ImageView navigateButton;
    }
}
