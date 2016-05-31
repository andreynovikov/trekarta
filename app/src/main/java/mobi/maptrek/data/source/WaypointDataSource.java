package mobi.maptrek.data.source;

import android.database.Cursor;

import java.util.List;

import mobi.maptrek.data.Waypoint;

public interface WaypointDataSource {
    List<Waypoint> getWaypoints();

    int getWaypointsCount();

    Waypoint cursorToWaypoint(Cursor cursor);
}
