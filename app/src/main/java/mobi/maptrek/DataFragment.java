package mobi.maptrek;

import android.app.Fragment;
import android.os.Bundle;

import mobi.maptrek.data.Waypoint;

public class DataFragment extends Fragment {

    private Waypoint mEditedWaypoint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public void setEditedWaypoint(Waypoint waypoint) {
        mEditedWaypoint = waypoint;
    }

    public Waypoint getEditedWaypoint() {
        return mEditedWaypoint;
    }
}