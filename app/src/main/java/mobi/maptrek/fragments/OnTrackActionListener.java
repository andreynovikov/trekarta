package mobi.maptrek.fragments;

import java.util.Set;

import mobi.maptrek.data.Track;

public interface OnTrackActionListener {
    void onTrackView(Track track);

    void onTrackDetails(Track track);

    void onTrackShare(Track track);

    void onTrackSave(Track track);

    void onTrackDelete(Track track);

    void onTracksDelete(Set<Track> tracks);
}
