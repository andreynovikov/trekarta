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

import mobi.maptrek.data.Place;
import mobi.maptrek.data.Route;
import mobi.maptrek.data.Track;

public class MemoryDataSource extends DataSource implements PlaceDataSource, TrackDataSource, RouteDataSource {
    @NonNull
    public List<Place> places = new ArrayList<>();
    @NonNull
    public List<Track> tracks = new ArrayList<>();
    @NonNull
    public List<Route> routes = new ArrayList<>();

    @Override
    public int getFormat() {
        return DataSource.FORMAT_NONE;
    }

    @Override
    public boolean isNativeTrack() {
        return false;
    }

    @Override
    public boolean isNativeRoute() {
        return false;
    }

    @Override
    public boolean isIndividual() {
        return (places.size() + tracks.size() + routes.size()) == 1;
    }

    @NonNull
    @Override
    public List<Place> getPlaces() {
        return places;
    }

    @Override
    public int getPlacesCount() {
        return places.size();
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
        if (position < places.size())
            return TYPE_PLACE;
        if (position < places.size() + tracks.size())
            return TYPE_TRACK;
        if (position < places.size() + tracks.size() + routes.size())
            return TYPE_ROUTE;
        throw new IndexOutOfBoundsException("Wrong index: " + position);
    }

    @Override
    public Place cursorToPlace(Cursor cursor) {
        return places.get(cursor.getInt(1));
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
            return places.size() + tracks.size() + routes.size();
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
            if (position < places.size())
                return position;
            if (position < places.size() + tracks.size())
                return position - places.size();
            return position - places.size() - tracks.size();
        }

        @Override
        public long getLong(int column) {
            checkPosition();
            if (column != 0)
                return 0;
            int position = getPosition();
            if (position < places.size())
                return places.get(position)._id;
            if (position < places.size() + tracks.size())
                return tracks.get(position - places.size()).id;
            return routes.get(position - places.size() - tracks.size()).id;
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
