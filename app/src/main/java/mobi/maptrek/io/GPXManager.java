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

package mobi.maptrek.io;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

import mobi.maptrek.data.Route;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.io.gpx.GpxParser;
import mobi.maptrek.io.gpx.GpxSerializer;
import mobi.maptrek.util.ProgressListener;

public class GPXManager extends Manager {
    public static final String EXTENSION = ".gpx";

    @NonNull
    @Override
    public FileDataSource loadData(InputStream inputStream, String filePath) throws Exception {
        FileDataSource dataSource = GpxParser.parse(inputStream);
        int hash = filePath.hashCode() * 31;
        int i = 1;
        int j = 1;
        for (Waypoint waypoint : dataSource.waypoints) {
            if (waypoint.name == null)
                waypoint.name = "Waypoint " + j;
            waypoint._id = 31L * (hash + waypoint.name.hashCode()) + i;
            waypoint.source = dataSource;
            i++;
            j++;
        }
        j = 1;
        for (Track track : dataSource.tracks) {
            if (track.name == null)
                track.name = "Track " + j;
            track.id = 31 * (hash + track.name.hashCode()) + i;
            track.source = dataSource;
            i++;
            j++;
        }
        j = 1;
        for (Route route : dataSource.routes) {
            if (route.name == null)
                route.name = "Route " + j;
            route.id = 31 * (hash + route.name.hashCode()) + i;
            route.source = dataSource;
            i++;
            j++;
        }
        return dataSource;
    }

    @Override
    public void saveData(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws Exception {
        GpxSerializer.serialize(outputStream, source, progressListener);
    }

    @NonNull
    @Override
    public String getExtension() {
        return EXTENSION;
    }
}
