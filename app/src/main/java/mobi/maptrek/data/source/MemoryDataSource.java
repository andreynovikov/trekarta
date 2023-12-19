/*
 * Copyright 2018 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.data.source;

import android.database.AbstractCursor;
import android.database.Cursor;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import mobi.maptrek.data.Route;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;

public class MemoryDataSource extends DataSource implements WaypointDataSource, TrackDataSource, RouteDataSource {
    @NonNull
    public List<Waypoint> waypoints = new ArrayList<>();
    @NonNull
    public List<Track> tracks = new ArrayList<>();
    @NonNull
    public List<Route> routes = new ArrayList<>();

    @Override
    public boolean isNativeTrack() {
        return false;
    }

    @Override
    public boolean isIndividual() {
        return (waypoints.size() + tracks.size() + routes.size()) == 1;
    }

    @NonNull
    @Override
    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    @Override
    public int getWaypointsCount() {
        return waypoints.size();
    }

    @Override
    public Cursor getCursor() {
        return new DataCursor();
    }

    @DataType
    @Override
    public int getDataType(int position) {
        if (position < 0)
            throw new IndexOutOfBoundsException("Wrong index: " + position);
        if (position < waypoints.size())
            return TYPE_WAYPOINT;
        if (position < waypoints.size() + tracks.size())
            return TYPE_TRACK;
        if (position < waypoints.size() + tracks.size() + routes.size())
            return TYPE_ROUTE;
        throw new IndexOutOfBoundsException("Wrong index: " + position);
    }

    @Override
    public Waypoint cursorToWaypoint(Cursor cursor) {
        return waypoints.get(cursor.getInt(1));
    }

    @Override
    public Track cursorToTrack(Cursor cursor) {
        return tracks.get(cursor.getInt(1));
    }

    @Override
    public Route cursorToRoute(Cursor cursor) {
        return routes.get(cursor.getInt(1));
    }

    @NonNull
    @Override
    public List<Track> getTracks() {
        return tracks;
    }

    @Override
    public int getTracksCount() {
        return tracks.size();
    }

    @NonNull
    @Override
    public List<Route> getRoutes() {
        return routes;
    }

    @Override
    public int getRoutesCount() {
        return routes.size();
    }

    /**
     * Helper cursor that does not hold data but only a reference (index) to actual data lists.
     */
    public class DataCursor extends AbstractCursor {

        @Override
        public int getCount() {
            return waypoints.size() + tracks.size() + routes.size();
        }

        @Override
        public String[] getColumnNames() {
            return new String[]{"_id", "index"};
        }

        @Override
        public String getString(int column) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public short getShort(int column) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public int getInt(int column) {
            checkPosition();
            if (column != 1)
                return 0;
            int position = getPosition();
            if (position < waypoints.size())
                return position;
            if (position < waypoints.size() + tracks.size())
                return position - waypoints.size();
            return position - waypoints.size() - tracks.size();
        }

        @Override
        public long getLong(int column) {
            checkPosition();
            if (column != 0)
                return 0;
            int position = getPosition();
            if (position < waypoints.size())
                return waypoints.get(position)._id;
            if (position < waypoints.size() + tracks.size())
                return tracks.get(position - waypoints.size()).id;
            return routes.get(position - waypoints.size() - tracks.size()).id;
        }

        @Override
        public float getFloat(int column) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public double getDouble(int column) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean isNull(int column) {
            return column == 0;
        }
    }
}
