/*
 * Copyright 2022 Andrey Novikov
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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
    private final List<DataSource> mData = new ArrayList<>();
    private boolean mNativeTracks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_with_empty_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null)
            mNativeTracks = arguments.getBoolean(ARG_NATIVE_TRACKS);

        if (mNativeTracks) {
            TextView emptyView = (TextView) getListView().getEmptyView();
            if (emptyView != null)
                emptyView.setText(R.string.msgEmptyTrackList);
        }

        mAdapter = new DataSourceListAdapter(requireContext());
        setListAdapter(mAdapter);
        updateData();

        getListView().setOnItemLongClickListener((parent, v, position, id) -> {
            final DataSource dataSource = mAdapter.getItem(position);
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mDataHolder = (DataHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement DataHolder");
        }
        try {
            mTrackActionListener = (OnTrackActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnTrackActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDataHolder = null;
        mTrackActionListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mData.clear();
    }

    @Override
    public void onListItemClick(@NonNull ListView lv, @NonNull View v, int position, long id) {
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
        Collections.sort(data, (lhs, rhs) -> {
            if (mNativeTracks) {
                // Newer tracks first
                File lf = new File(lhs.path);
                File rf = new File(rhs.path);
                return Long.compare(rf.lastModified(), lf.lastModified());
            }
            return lhs.name.compareTo(rhs.name);
        });
        for (FileDataSource source : data) {
            if (mNativeTracks ^ !source.isNativeTrack()) {
                mData.add(source);
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    private class DataSourceListAdapter extends BaseAdapter {
        private final int mAccentColor;
        private final int mDisabledColor;

        DataSourceListAdapter(Context context) {
            mAccentColor = context.getColor(R.color.colorAccentLight);
            mDisabledColor = context.getColor(R.color.colorPrimary);
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
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_data_source, parent, false);
                itemHolder.name = convertView.findViewById(R.id.name);
                itemHolder.description = convertView.findViewById(R.id.description);
                itemHolder.filename = convertView.findViewById(R.id.filename);
                itemHolder.icon = convertView.findViewById(R.id.icon);
                itemHolder.action = convertView.findViewById(R.id.action);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (DataSourceListItemHolder) convertView.getTag();
            }

            itemHolder.name.setText(dataSource.name);
            Resources resources = getResources();

            int color = mAccentColor;
            if (dataSource instanceof WaypointDbDataSource) {
                int count = ((WaypointDataSource) dataSource).getWaypointsCount();
                itemHolder.description.setText(resources.getQuantityString(R.plurals.placesCount, count, count));
                itemHolder.filename.setText("");
                itemHolder.icon.setImageResource(R.drawable.ic_points);
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
                        itemHolder.icon.setImageResource(R.drawable.ic_track);
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
                        itemHolder.description.setText(sb);
                        if (waypointsCount > 0 && tracksCount > 0)
                            itemHolder.icon.setImageResource(R.drawable.ic_dataset);
                        else if (waypointsCount > 0)
                            itemHolder.icon.setImageResource(R.drawable.ic_points);
                        else if (tracksCount > 0)
                            itemHolder.icon.setImageResource(R.drawable.ic_tracks);
                    }
                } else {
                    String size = Formatter.formatShortFileSize(getContext(), file.length());
                    itemHolder.description.setText(size);
                    if (mNativeTracks)
                        itemHolder.icon.setImageResource(R.drawable.ic_track);
                    else
                        itemHolder.icon.setImageResource(R.drawable.ic_dataset);
                    color = mDisabledColor;
                }
                final boolean shown = dataSource.isVisible();
                if (shown)
                    itemHolder.action.setImageResource(R.drawable.ic_visibility);
                else
                    itemHolder.action.setImageResource(R.drawable.ic_visibility_off);
                itemHolder.action.setVisibility(View.VISIBLE);
                itemHolder.action.setOnClickListener(v -> {
                    mDataHolder.setDataSourceAvailability((FileDataSource) getItem(position), !shown);
                    notifyDataSetChanged();
                });
            }

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
