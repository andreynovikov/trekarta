package mobi.maptrek.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of data objects commonly representing unique file.
 */
public class DataSource {
    public String path;
    @NonNull
    public List<Track> tracks = new ArrayList<>();
    @NonNull
    public List<Waypoint> waypoints = new ArrayList<>();

    private String mNewName;

    public DataSource() {
    }

    /**
     * Creates new data source with specified file name (without extension)
     * @param name
     */
    public DataSource(String name) {
        rename(name);
    }

    /**
     * Returns whether the source contains any data.
     *
     * @return <code>true</code> if this source has no data entries, <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return tracks.isEmpty() && waypoints.isEmpty();
    }

    /**
     * Returns whether the source contains only one track and nothing more.
     *
     * @return <code>true</code> if this is single track source, <code>false</code> otherwise.
     */
    public boolean isSingleTrack() {
        return waypoints.isEmpty() && tracks.size() == 1;
    }

    /**
     * Marks source to be renamed on next save, does not actually save the source.
     *
     * @param name New file name (without extension)
     */
    public void rename(String name) {
        mNewName = name;
    }

    @Nullable
    public String getNewName() {
        return mNewName;
    }
}
