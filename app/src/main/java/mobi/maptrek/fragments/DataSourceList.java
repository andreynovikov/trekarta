package mobi.maptrek.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mobi.maptrek.MainActivity;
import mobi.maptrek.R;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.Track;
import mobi.maptrek.util.StringFormatter;

public class DataSourceList extends ListFragment {
    private DataSourceListAdapter mAdapter;
    private MainActivity mActivity;
    private List<FileDataSource> mData = new ArrayList<>();

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

        TextView emptyView = (TextView) getListView().getEmptyView();
        if (emptyView != null)
            emptyView.setText(R.string.msg_empty_track_list);

        mAdapter = new DataSourceListAdapter(getActivity());
        //mAdapter = new SwipeActionAdapter(new DataSourceListAdapter(getActivity()));
        setListAdapter(mAdapter);
        //mAdapter.setListView(getListView());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mActivity = (MainActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must be MainActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mData.clear();
        mActivity = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        initData();
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        //final Track track = application.getTrack(position);
        //trackActionsCallback.onTrackDetails(track);
    }

    public void initData() {
        mData.clear();
        List<FileDataSource> data = mActivity.getData();
        if (data == null)
            return;
        for (FileDataSource source : data) {
            if (source.isSingleTrack())
                mData.add(source);
        }
        //TODO Sort by record time (modification time?)
        Collections.sort(mData, new Comparator<FileDataSource>() {
            @Override
            public int compare(FileDataSource lhs, FileDataSource rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });
        mAdapter.notifyDataSetChanged();
    }

    public class DataSourceListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public DataSourceListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public FileDataSource getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mData.get(position).path.hashCode();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            DataSourceListItemHolder itemHolder;
            final FileDataSource dataSource = getItem(position);

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

            if (dataSource.isLoaded()) {
                Track track = dataSource.tracks.get(0);
                String distance = StringFormatter.distanceH(track.getDistance());
                itemHolder.description.setText(distance);
                if (track.style.color != -1) {
                    Drawable background = itemHolder.icon.getBackground().mutate();
                    if (background instanceof ShapeDrawable) {
                        ((ShapeDrawable) background).getPaint().setColor(track.style.color);
                    } else if (background instanceof GradientDrawable) {
                        ((GradientDrawable) background).setColor(track.style.color);
                    }
                }
            } else {
                itemHolder.description.setText(R.string.unknown);
                Drawable background = itemHolder.icon.getBackground().mutate();
                int color = getActivity().getColor(R.color.colorPrimary);
                if (background instanceof ShapeDrawable) {
                    ((ShapeDrawable) background).getPaint().setColor(color);
                } else if (background instanceof GradientDrawable) {
                    ((GradientDrawable) background).setColor(color);
                }
            }
            final boolean shown = dataSource.isLoaded() && dataSource.isVisible();
            if (shown)
                itemHolder.action.setImageResource(R.drawable.ic_visibility);
            else
                itemHolder.action.setImageResource(R.drawable.ic_visibility_off);
            itemHolder.action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.w("DSL", "Pos: " + position + " l: " + shown);
                    mActivity.setDataSourceAvailability(getItem(position), !shown);
                    notifyDataSetChanged();
                }
            });

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
