package mobi.maptrek.data.source;

import android.support.annotation.Nullable;

/**
 * A set of map data objects.
 */
public abstract class DataSource {
    public String name;
    private String mNewName;
    private boolean loaded = false;
    private boolean visible = true;

    public DataSource() {
    }

    /**
     * Returns whether the source contains only one track and nothing more.
     *
     * @return <code>true</code> if this is single track source, <code>false</code> otherwise.
     */
    public abstract boolean isSingleTrack();

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
