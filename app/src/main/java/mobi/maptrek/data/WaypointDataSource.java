package mobi.maptrek.data;

import android.database.Cursor;

import java.util.List;

public interface WaypointDataSource {
    void saveWaypoint(Waypoint waypoint);
    void deleteWaypoint(Waypoint waypoint);
    List<Waypoint> getWaypoints();
    Cursor getCursor();
    Waypoint cursorToWaypoint(Cursor cursor);
    void addListener(WaypointDataSourceUpdateListener listener);
    void removeListener(WaypointDataSourceUpdateListener listener);
}
