package mobi.maptrek.data.source;

import android.database.Cursor;

import java.util.List;

import mobi.maptrek.data.Waypoint;

public interface WaypointDataSource {
    void saveWaypoint(Waypoint waypoint);

    void deleteWaypoint(Waypoint waypoint);

    List<Waypoint> getWaypoints();

    int getWaypointsCount();

    Waypoint cursorToWaypoint(Cursor cursor);
}
