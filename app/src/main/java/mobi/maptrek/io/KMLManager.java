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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.data.style.TrackStyle;
import mobi.maptrek.io.kml.KmlParser;
import mobi.maptrek.io.kml.KmlSerializer;
import mobi.maptrek.util.ProgressListener;

public class KMLManager extends Manager {
    public static final String EXTENSION = ".kml";
    public static final String ZIP_EXTENSION = ".kmz";

    @NonNull
    @Override
    public FileDataSource loadData(InputStream inputStream, String filePath) throws Exception {
        if (filePath.endsWith(ZIP_EXTENSION)) {
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry zipEntry;
            boolean hasDocument = false;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                logger.error(zipEntry.getName());
                if (zipEntry.getName().endsWith(EXTENSION)) {
                    inputStream = zipInputStream;
                    hasDocument = true;
                    break;
                } else {
                    zipInputStream.closeEntry();
                }
            }
            if (!hasDocument) {
                zipInputStream.close();
                throw new FileNotFoundException("KMZ file does not contain KML document");
            }
        }
        FileDataSource dataSource = KmlParser.parse(inputStream);
        // We do not need to close zipInputStream because it is closed by parser

        int hash = filePath.hashCode() * 31;
        int i = 1;
        // TODO - Generate names if they are missing
        for (Waypoint waypoint : dataSource.waypoints) {
            waypoint._id = 31 * (hash + waypoint.name.hashCode()) + i;
            waypoint.source = dataSource;
            if (waypoint.style.color == 0)
                waypoint.style.color = MarkerStyle.DEFAULT_COLOR;
            i++;
        }
        for (Track track : dataSource.tracks) {
            track.id = 31 * (hash + track.name.hashCode()) + i;
            track.source = dataSource;
            if (track.style.color == 0)
                track.style.color = TrackStyle.DEFAULT_COLOR;
            if (track.style.width == 0f)
                track.style.width = TrackStyle.DEFAULT_WIDTH;
            i++;
        }
        return dataSource;
    }

    @Override
    public void saveData(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws Exception {
        KmlSerializer.serialize(outputStream, source, progressListener);
    }

    @NonNull
    @Override
    public String getExtension() {
        return EXTENSION;
    }
}
