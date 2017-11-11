package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.oscim.core.GeoPoint;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import mobi.maptrek.R;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.view.BitmapTileMapPreviewView;

public class MapList extends Fragment {
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_ZOOM_LEVEL = "zoom";
    public static final String ARG_HIDE_OBJECTS = "hide";
    public static final String ARG_TRANSPARENCY = "transparency";

    private TextView mEmptyView;
    private View mMapListHeader;
    private Switch mHideSwitch;
    private SeekBar mTransparencySeekBar;
    private LinearLayout mMapList;
    private ArrayList<MapFile> mMaps = new ArrayList<>();
    private MapFile mActiveMap;
    private OnMapActionListener mListener;
    private FragmentHolder mFragmentHolder;
    private LayoutInflater mInflater;

    private GeoPoint mLocation;
    private int mZoomLevel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_map_list, container, false);

        mEmptyView = (TextView) rootView.findViewById(android.R.id.empty);
        mMapListHeader = rootView.findViewById(R.id.mapListHeader);
        mHideSwitch = (Switch) rootView.findViewById(R.id.hideSwitch);
        mTransparencySeekBar = (SeekBar) rootView.findViewById(R.id.transparencySeekBar);
        mMapList = (LinearLayout) rootView.findViewById(R.id.mapList);

        mHideSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mListener.onHideMapObjects(isChecked);
            }
        });
        mTransparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    mListener.onTransparencyChanged(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle arguments = getArguments();
        double latitude = arguments.getDouble(ARG_LATITUDE);
        double longitude = arguments.getDouble(ARG_LONGITUDE);
        mZoomLevel = arguments.getInt(ARG_ZOOM_LEVEL);
        boolean hideObjects = arguments.getBoolean(ARG_HIDE_OBJECTS);
        int transparency = arguments.getInt(ARG_TRANSPARENCY);

        mLocation = new GeoPoint(latitude, longitude);

        mMapList.removeAllViews();
        if (mMaps.size() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mMapListHeader.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mMapListHeader.setVisibility(View.VISIBLE);
            for (MapFile map : mMaps)
                addMap(map);
        }
        mHideSwitch.setChecked(hideObjects);
        mTransparencySeekBar.setProgress(transparency);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapList.removeAllViews();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnMapActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMapActionListener");
        }
        mFragmentHolder = (FragmentHolder) context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder = null;
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMaps.clear();
    }

    public void setMaps(Collection<MapFile> maps, MapFile active) {
        mMaps.clear();
        for (MapFile map : maps)
            mMaps.add(map);
        mActiveMap = active;
        Collections.sort(mMaps, new MapComparator());
    }

    private void addMap(final MapFile mapFile) {
        final View mapView = mInflater.inflate(R.layout.list_item_map, mMapList, false);
        TextView name = (TextView) mapView.findViewById(R.id.name);
        final BitmapTileMapPreviewView map = (BitmapTileMapPreviewView) mapView.findViewById(R.id.map);
        View indicator = mapView.findViewById(R.id.indicator);
        name.setText(mapFile.name);
        map.setTileSource(mapFile.tileSource, mActiveMap == mapFile);
        if (mapFile.boundingBox.contains(mLocation)) {
            int zoomLevel = mapFile.tileSource.getZoomLevelMax();
            int minZoomLevel = mapFile.tileSource.getZoomLevelMin();
            if (mapFile.tileSource instanceof SQLiteTileSource) {
                minZoomLevel = ((SQLiteTileSource) mapFile.tileSource).sourceZoomMin;
            }
            if (mapFile.tileSource.getOption("path") == null ||
                    (mZoomLevel < zoomLevel && mZoomLevel > minZoomLevel))
                zoomLevel = mZoomLevel;
            map.setLocation(mLocation, zoomLevel);
        } else {
            map.setLocation(mapFile.boundingBox.getCenterPoint(), mapFile.tileSource.getZoomLevelMax());
        }
        if (mapFile == mActiveMap) {
            indicator.setBackgroundColor(getResources().getColor(R.color.colorAccent, getContext().getTheme()));
        } else {
            indicator.setBackgroundColor(Color.TRANSPARENT);
        }
        mapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                map.setShouldNotCloseDataSource();
                mListener.onMapSelected(mapFile);
                mFragmentHolder.popCurrent();
            }
        });
        mMapList.addView(mapView);
    }

    private class MapComparator implements Comparator<MapFile>, Serializable {
        @Override
        public int compare(MapFile o1, MapFile o2) {
            /*
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
            */
            return o1.name.compareTo(o2.name);
        }
    }

}
