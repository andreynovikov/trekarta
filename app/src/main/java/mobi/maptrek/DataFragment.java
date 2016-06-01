package mobi.maptrek;

import android.app.Fragment;
import android.os.Bundle;

import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;

import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;

public class DataFragment extends Fragment {

    private MapIndex mMapIndex;
    private Waypoint mEditedWaypoint;
    private WaypointDbDataSource mWaypointDbDataSource;
    private MapFile mBitmapLayerMap;
    private MultiMapFileTileSource mMapFileSource;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public MapIndex getMapIndex() {
        return mMapIndex;
    }

    public void setMapIndex(MapIndex mapIndex) {
        mMapIndex = mapIndex;
    }

    public Waypoint getEditedWaypoint() {
        return mEditedWaypoint;
    }

    public void setEditedWaypoint(Waypoint waypoint) {
        mEditedWaypoint = waypoint;
    }

    public WaypointDbDataSource getWaypointDbDataSource() {
        return mWaypointDbDataSource;
    }

    public void setWaypointDbDataSource(WaypointDbDataSource waypointDbDataSource) {
        mWaypointDbDataSource = waypointDbDataSource;
    }

    public MapFile getBitmapLayerMap() {
        return mBitmapLayerMap;
    }

    public void setBitmapLayerMap(MapFile bitmapLayerMap) {
        mBitmapLayerMap = bitmapLayerMap;
    }

    public MultiMapFileTileSource getMapFileSource() {
        return mMapFileSource;
    }

    public void setMapFileSource(MultiMapFileTileSource mapFileSource) {
        mMapFileSource = mapFileSource;
    }
}