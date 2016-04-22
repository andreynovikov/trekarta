package mobi.maptrek;

import android.app.Fragment;
import android.os.Bundle;

import mobi.maptrek.data.Waypoint;
import mobi.maptrek.map.MapIndex;

public class DataFragment extends Fragment {

    private MapIndex mMapIndex;
    private Waypoint mEditedWaypoint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public void setMapIndex(MapIndex mapIndex) {
        mMapIndex = mapIndex;
    }

    public MapIndex getMapIndex() {
        return mMapIndex;
    }

    public void setEditedWaypoint(Waypoint waypoint) {
        mEditedWaypoint = waypoint;
    }

    public Waypoint getEditedWaypoint() {
        return mEditedWaypoint;
    }
}