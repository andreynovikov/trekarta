package mobi.maptrek.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import mobi.maptrek.data.DataSource;
import mobi.maptrek.data.Track;
import mobi.maptrek.util.GpxFiles;

public class GPXManager extends Manager {
    public static final String EXTENSION = ".gpx";

    @NonNull
    @Override
    public DataSource loadData(InputStream inputStream, String fileName) throws Exception {
        DataSource source = new DataSource();
        List<Track> tracks = GpxFiles.loadTracksFromFile(inputStream, fileName);
        for (Track track : tracks)
            track.source = source;
        source.tracks.addAll(tracks);
        return source;
    }

    @Override
    public void saveData(OutputStream outputStream, DataSource source, @Nullable ProgressListener progressListener) throws Exception {
        GpxFiles.saveTrackToFile(outputStream, source.tracks.get(0), progressListener);
    }

    @NonNull
    @Override
    public String getExtension() {
        return EXTENSION;
    }
}
