package mobi.maptrek.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import mobi.maptrek.data.DataSource;
import mobi.maptrek.data.Track;
import mobi.maptrek.util.KmlFiles;

public class KMLManager extends Manager {
    public static final String EXTENSION = ".kml";

    @NonNull
    @Override
    public DataSource loadData(InputStream inputStream, String fileName) throws Exception {
        DataSource source = new DataSource(fileName);
        List<Track> tracks = KmlFiles.loadTracksFromFile(inputStream, fileName);
        for (Track track : tracks)
            track.source = source;
        source.tracks.addAll(tracks);
        return source;
    }

    @Override
    public void saveData(OutputStream outputStream, DataSource source, @Nullable ProgressListener progressListener) throws Exception {
        KmlFiles.saveTrackToFile(outputStream, source.tracks.get(0), progressListener);
    }

    @NonNull
    @Override
    public String getExtension() {
        return EXTENSION;
    }
}
