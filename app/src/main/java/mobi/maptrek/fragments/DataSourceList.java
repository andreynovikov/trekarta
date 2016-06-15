package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mobi.maptrek.DataHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.TrackDataSource;
import mobi.maptrek.data.source.WaypointDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.util.StringFormatter;

public class DataSourceList extends ListFragment {
    public static final String ARG_NATIVE_TRACKS = "nativeTracks";

    private DataSourceListAdapter mAdapter;
    private OnTrackActionListener mTrackActionListener;
    private DataHolder mDataHolder;
    private List<DataSource> mData = new ArrayList<>();
    private boolean mNativeTracks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_with_empty_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mNativeTracks = getArguments().getBoolean(ARG_NATIVE_TRACKS);

        if (mNativeTracks) {
            TextView emptyView = (TextView) getListView().getEmptyView();
            if (emptyView != null)
                emptyView.setText(R.string.msg_empty_track_list);
        }

        mAdapter = new DataSourceListAdapter(getActivity());
        setListAdapter(mAdapter);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final DataSource dataSource = mAdapter.getItem(position);
                if (dataSource instanceof WaypointDbDataSource)
                    return false;
                PopupMenu popup = new PopupMenu(getContext(), view);
                popup.inflate(R.menu.context_menu_track_list);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_delete:
                                mDataHolder.onDataSourceDelete(dataSource);
                                return true;
                        }
                        return false;
                    }
                });
                popup.show();
                return true;
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mDataHolder = (DataHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DataHolder");
        }
        try {
            mTrackActionListener = (OnTrackActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTrackActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mData.clear();
        mDataHolder = null;
        mTrackActionListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        DataSource source = mAdapter.getItem(position);
        if (source.isNativeTrack()) {
            Track track = ((TrackDataSource) source).getTracks().get(0);
            mTrackActionListener.onTrackDetails(track);
        } else {
            mDataHolder.onDataSourceSelected(source);
        }
    }

    public void updateData() {
        mData.clear();

        if (!mNativeTracks)
            mData.add(mDataHolder.getWaypointDataSource());

        List<FileDataSource> data = mDataHolder.getData();
        // TODO Preserve position after source is loaded and name changes
        Collections.sort(data, new Comparator<DataSource>() {
            @Override
            public int compare(DataSource lhs, DataSource rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });
        for (FileDataSource source : data) {
            if (mNativeTracks ^ !source.isNativeTrack()) {
                mData.add(source);
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    public class DataSourceListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private int mAccentColor;
        private int mDisabledColor;

        public DataSourceListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            Activity activity = getActivity();
            mAccentColor = activity.getColor(R.color.colorAccent);
            mDisabledColor = activity.getColor(R.color.colorPrimary);
        }

        @Override
        public DataSource getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mData.get(position).hashCode();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public boolean isEnabled(int position) {
            DataSource dataSource = getItem(position);
            return dataSource.isLoaded();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            DataSourceListItemHolder itemHolder;
            final DataSource dataSource = getItem(position);

            if (convertView == null) {
                itemHolder = new DataSourceListItemHolder();
                convertView = mInflater.inflate(R.layout.list_item_data_source, parent, false);
                itemHolder.name = (TextView) convertView.findViewById(R.id.name);
                itemHolder.description = (TextView) convertView.findViewById(R.id.description);
                itemHolder.filename = (TextView) convertView.findViewById(R.id.filename);
                itemHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                itemHolder.action = (ImageView) convertView.findViewById(R.id.action);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (DataSourceListItemHolder) convertView.getTag();
            }

            itemHolder.name.setText(dataSource.name);
            Resources resources = getResources();

            int color = mAccentColor;
            if (dataSource instanceof WaypointDbDataSource) {
                int count = ((WaypointDataSource) dataSource).getWaypointsCount();
                itemHolder.description.setText(resources.getQuantityString(R.plurals.waypointsCount, count, count));
                itemHolder.filename.setText("");
                itemHolder.action.setVisibility(View.GONE);
                itemHolder.action.setOnClickListener(null);
            } else {
                File file = new File(((FileDataSource) dataSource).path);
                itemHolder.filename.setText(file.getName());
                if (dataSource.isLoaded()) {
                    if (mNativeTracks) {
                        Track track = ((FileDataSource) dataSource).tracks.get(0);
                        String distance = StringFormatter.distanceH(track.getDistance());
                        itemHolder.description.setText(distance);
                        color = track.style.color;
                    } else {
                        int waypointsCount = ((FileDataSource) dataSource).waypoints.size();
                        int tracksCount = ((FileDataSource) dataSource).tracks.size();
                        StringBuilder sb = new StringBuilder();
                        if (waypointsCount > 0) {
                            sb.append(resources.getQuantityString(R.plurals.waypointsCount, waypointsCount, waypointsCount));
                            if (tracksCount > 0)
                                sb.append(", ");
                        }
                        if (tracksCount > 0) {
                            sb.append(resources.getQuantityString(R.plurals.tracksCount, tracksCount, tracksCount));
                        }
                        itemHolder.description.setText(sb);
                    }
                } else {
                    String size = Formatter.formatShortFileSize(getContext(), file.length());
                    itemHolder.description.setText(size);
                    color = mDisabledColor;
                }
                final boolean shown = dataSource.isVisible();
                if (shown)
                    itemHolder.action.setImageResource(R.drawable.ic_visibility);
                else
                    itemHolder.action.setImageResource(R.drawable.ic_visibility_off);
                itemHolder.action.setVisibility(View.VISIBLE);
                itemHolder.action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDataHolder.setDataSourceAvailability((FileDataSource) getItem(position), !shown);
                        notifyDataSetChanged();
                    }
                });
            }

            //TODO Set appropriate icons: track, waypoints, file format, etc
            Drawable background = itemHolder.icon.getBackground().mutate();
            if (background instanceof ShapeDrawable) {
                ((ShapeDrawable) background).getPaint().setColor(color);
            } else if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            }

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    private static class DataSourceListItemHolder {
        TextView name;
        TextView description;
        TextView filename;
        ImageView icon;
        ImageView action;
    }
}
