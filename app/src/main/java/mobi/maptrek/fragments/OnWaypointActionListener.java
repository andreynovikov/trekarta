package mobi.maptrek.fragments;

import org.oscim.core.GeoPoint;

import java.util.Set;

import mobi.maptrek.data.Waypoint;

public interface OnWaypointActionListener {
    void onWaypointCreate(GeoPoint point, String name, boolean locked, boolean customize);

    /**
     * Position map so that waypoint is visible
     */
    void onWaypointView(Waypoint waypoint);

    void onWaypointFocus(Waypoint waypoint);

    void onWaypointDetails(Waypoint waypoint, boolean full);

    void onWaypointNavigate(Waypoint waypoint);

    void onWaypointShare(Waypoint waypoint);

    void onWaypointSave(Waypoint waypoint);

    void onWaypointDelete(Waypoint waypoint);

    void onWaypointsDelete(Set<Waypoint> waypoints);
}
