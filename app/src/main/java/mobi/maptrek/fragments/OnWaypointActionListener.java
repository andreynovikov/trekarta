package mobi.maptrek.fragments;

import java.util.Set;

import mobi.maptrek.data.Waypoint;

public interface OnWaypointActionListener {
    /**
     * Position map so that waypoint is visible
     */
    void onWaypointView(Waypoint waypoint);

    void onWaypointDetails(Waypoint waypoint, boolean full);

    void onWaypointNavigate(Waypoint waypoint);

    void onWaypointShare(Waypoint waypoint);

    void onWaypointSave(Waypoint waypoint);

    void onWaypointDelete(Waypoint waypoint);

    void onWaypointsDelete(Set<Waypoint> waypoints);
}
