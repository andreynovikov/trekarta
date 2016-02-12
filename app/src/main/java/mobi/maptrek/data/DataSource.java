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
    public String name;
    @NonNull
    public List<Track> tracks = new ArrayList<>();
    @NonNull
    public List<Waypoint> waypoints = new ArrayList<>();
    private String mNewName;
    private boolean loaded = false;
    private boolean visible = true;

    public DataSource() {
    }

    /**
     * Creates new empty data source with specified file name (without extension)
     * @param name Source name
     */
    public DataSource(String name) {
        //TODO This is tricky and not straightforward
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
        //FIXME Must properly handle not loaded sources
        return !isLoaded() || waypoints.isEmpty() && tracks.size() == 1;
    }

    /**
     * Marks source to be renamed on next save, does not actually save the source.
     *
     * @param name New file name (without extension)
     */
    public void rename(String name) {
        this.name = name;
        mNewName = name;
    }

    @Nullable
    public String getNewName() {
        return mNewName;
    }

    /**
     * Returns whether the source is loaded from file (contains data).
     *
     * @return <code>true</code> if data is loaded, <code>false</code> if it is just a stub.
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Mark data source as loaded (populated with data).
     */
    public void setLoaded() {
        loaded = true;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
