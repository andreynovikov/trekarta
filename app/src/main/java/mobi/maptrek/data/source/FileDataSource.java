/*
 * Copyright 2024 Andrey Novikov
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

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import mobi.maptrek.io.GPXManager;
import mobi.maptrek.io.KMLManager;
import mobi.maptrek.io.RouteManager;
import mobi.maptrek.io.TrackManager;

public class FileDataSource extends MemoryDataSource {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FORMAT_NATIVE, FORMAT_GPX, FORMAT_KML})
    public @interface Format {
    }

    /**
     * Native format is used only for single track sharing
     */
    public static final int FORMAT_NATIVE = 0;
    public static final int FORMAT_GPX = 1;
    public static final int FORMAT_KML = 2;

    public String path;
    // Native format helper data
    public long propertiesOffset;

    @Override
    public int getFormat() {
        if (path == null)
            return DataSource.FORMAT_NONE;
        if (path.endsWith(TrackManager.EXTENSION))
            return FORMAT_NATIVE;
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(KMLManager.EXTENSION))
            return FORMAT_KML;
        if (lowerPath.endsWith(GPXManager.EXTENSION))
            return FORMAT_GPX;
        return DataSource.FORMAT_NONE;
    }

    @Override
    public boolean isNativeTrack() {
        return path != null && path.endsWith(TrackManager.EXTENSION);
    }

    @Override
    public boolean isNativeRoute() {
        return path != null && path.endsWith(RouteManager.EXTENSION);
    }
}
