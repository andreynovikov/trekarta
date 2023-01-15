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

package mobi.maptrek.data.source;

import android.database.Cursor;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import org.oscim.core.GeoPoint;

import java.util.HashSet;
import java.util.Set;

/**
 * A set of map data objects.
 */
public abstract class DataSource {
    @IntDef({TYPE_WAYPOINT, TYPE_TRACK, TYPE_ROUTE})
    public @interface DataType {
    }

    public static final int TYPE_WAYPOINT = 0;
    public static final int TYPE_TRACK = 1;
    public static final int TYPE_ROUTE = 2;

    public String name;
    int mLatitudeE6 = 0;
    int mLongitudeE6 = 0;
    private boolean loaded = false;
    private boolean loadable = true;
    private boolean visible = false;
    private final Set<DataSourceUpdateListener> mListeners = new HashSet<>();

    public DataSource() {
    }

    public void setReferenceLocation(@Nullable GeoPoint coordinates) {
        if (coordinates != null) {
            mLatitudeE6 = coordinates.latitudeE6;
            mLongitudeE6 = coordinates.longitudeE6;
        } else {
            mLatitudeE6 = 0;
            mLongitudeE6 = 0;
        }
    }

    /**
     * Returns whether the source contains only one track and nothing more.
     *
     * @return <code>true</code> if this is single track source, <code>false</code> otherwise.
     */
    public abstract boolean isNativeTrack();

    /**
     * Returns whether the source contains only one item and nothing more.
     *
     * @return <code>true</code> if this is single item source, <code>false</code> otherwise.
     */
    public abstract boolean isIndividual();

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

    public boolean isLoadable() {
        return loadable;
    }

    public void setLoadable(boolean loadable) {
        this.loadable = loadable;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public abstract Cursor getCursor();

    @DataType
    public abstract int getDataType(int position);

    public void addListener(DataSourceUpdateListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(DataSourceUpdateListener listener) {
        mListeners.remove(listener);
    }

    public void notifyListeners() {
        for (DataSourceUpdateListener listener : mListeners) {
            listener.onDataSourceUpdated();
        }
    }
}
