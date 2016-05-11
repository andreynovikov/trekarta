package mobi.maptrek.data;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import mobi.maptrek.io.TrackManager;

public class FileDataSource extends DataSource implements WaypointDataSource, TrackDataSource {
    public String path;
    @NonNull
    public List<Waypoint> waypoints = new ArrayList<>();
    @NonNull
    public List<Track> tracks = new ArrayList<>();

    public boolean isSingleTrack() {
        return (!isLoaded() && path.endsWith(TrackManager.EXTENSION)) ||
                (waypoints.isEmpty() && tracks.size() == 1);
    }

    @Override
    public void saveWaypoint(Waypoint waypoint) {

    }

    @Override
    public void deleteWaypoint(Waypoint waypoint) {

    }

    @NonNull
    @Override
    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    @Override
    public Cursor getCursor() {
        return null;
    }

    @Override
    public Waypoint cursorToWaypoint(Cursor cursor) {
        return null;
    }

    @Override
    public void addListener(WaypointDataSourceUpdateListener listener) {

    }

    @Override
    public void removeListener(WaypointDataSourceUpdateListener listener) {

    }

    @Override
    public void saveTrack(Track track) {

    }

    @Override
    public void deleteTrack(Track track) {

    }

    @NonNull
    @Override
    public List<Track> getTracks() {
        return tracks;
    }
}
