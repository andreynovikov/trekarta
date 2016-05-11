package mobi.maptrek.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

import mobi.maptrek.data.FileDataSource;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.io.kml.KmlParser;
import mobi.maptrek.io.kml.KmlSerializer;

public class KMLManager extends Manager {
    public static final String EXTENSION = ".kml";

    @NonNull
    @Override
    public FileDataSource loadData(InputStream inputStream, String filePath) throws Exception {
        FileDataSource dataSource = KmlParser.parse(inputStream);
        int hash = filePath.hashCode() * 31;
        int i = 1;
        // TODO - Generate names if they are missing
        for (Waypoint waypoint : dataSource.waypoints) {
            waypoint._id = 31 * (hash + waypoint.name.hashCode()) + i;
            i++;
        }
        for (Track track : dataSource.tracks) {
            track.id = 31 * (hash + track.name.hashCode()) + i;
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
