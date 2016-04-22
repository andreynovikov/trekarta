package mobi.maptrek.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.oscim.core.GeoPoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import mobi.maptrek.R;
import mobi.maptrek.map.MapFile;

public class MapList extends ListFragment {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";

    private MapListAdapter mAdapter;
    private ArrayList<MapFile> mMaps;
    private MapFile mActiveMap;
    private OnMapActionListener mListener;

    private GeoPoint mLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.menu_list_with_empty_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        double latitude = getArguments().getDouble(ARG_LATITUDE);
        double longitude = getArguments().getDouble(ARG_LONGITUDE);

        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble(ARG_LATITUDE);
            longitude = savedInstanceState.getDouble(ARG_LONGITUDE);
        }

        mLocation = new GeoPoint(latitude, longitude);

        TextView emptyView = (TextView) getListView().getEmptyView();
        if (emptyView != null)
            emptyView.setText(R.string.msg_empty_map_list);

        mAdapter = new MapListAdapter(getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnMapActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMapActionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Collections.sort(mMaps, new MapComparator());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMaps.clear();
        mListener = null;
    }

    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
        MapFile map = mAdapter.getItem(position);
        mListener.onMapSelected(map);
    }

    public void setMaps(Collection<MapFile> maps, MapFile active) {
        mMaps = new ArrayList<>(maps.size());
        for (MapFile map : maps)
            mMaps.add(map);
        mActiveMap = active;
    }

    public class MapListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public MapListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public MapFile getItem(int position) {
            return mMaps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mMaps.get(position).name.hashCode();
        }

        @Override
        public int getCount() {
            return mMaps.size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            MapListItemHolder itemHolder;
            final MapFile mapFile = getItem(position);

            if (convertView == null) {
                itemHolder = new MapListItemHolder();
                convertView = mInflater.inflate(R.layout.list_item_map, parent, false);
                itemHolder.name = (TextView) convertView.findViewById(R.id.name);
                itemHolder.map = (ImageView) convertView.findViewById(R.id.map);
                itemHolder.indicator = convertView.findViewById(R.id.indicator);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (MapListItemHolder) convertView.getTag();
            }

            itemHolder.name.setText(mapFile.name);

            if (mapFile == mActiveMap) {
                itemHolder.indicator.setBackgroundColor(getResources().getColor(R.color.colorAccent, getContext().getTheme()));
            } else {
                itemHolder.indicator.setBackgroundColor(Color.TRANSPARENT);
            }
            //if (mapFile != mActiveMap)
            //    mapFile.tileSource.open();

            //map.setBaseMap(new BitmapTileLayer(map, mapFile.tileSource));

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    private static class MapListItemHolder {
        TextView name;
        ImageView map;
        View indicator;
    }

    private class MapComparator implements Comparator<MapFile>, Serializable {
        @Override
        public int compare(MapFile o1, MapFile o2) {
            boolean c1 = o1.boundingBox.contains(mLocation);
            boolean c2 = o2.boundingBox.contains(mLocation);
            int res = Boolean.compare(c1, c2);
            if (res != 0)
                return res;
            // Larger max zoom is better
            res = Integer.compare(o1.tileSource.getZoomLevelMax(), o2.tileSource.getZoomLevelMax());
            if (res != 0)
                return res;
            // Larger min zoom is "better" too
            res = Integer.compare(o1.tileSource.getZoomLevelMin(), o2.tileSource.getZoomLevelMin());
            if (res != 0)
                return res;
            //TODO Compare covering area - smaller is better
            return 0;
        }
    }

}
