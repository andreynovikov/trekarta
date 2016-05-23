package mobi.maptrek.fragments;

import java.util.Set;

import mobi.maptrek.data.Track;

public interface OnTrackActionListener {
    void onTrackView(Track track);

    void onTrackDetails(Track track);

    void onTracksDelete(Set<Track> tracks);
}
