package mobi.maptrek.data.source;

import java.util.List;

import mobi.maptrek.data.Track;

public interface TrackDataSource {
    void saveTrack(Track track);
    void deleteTrack(Track track);
    List<Track> getTracks();
}
