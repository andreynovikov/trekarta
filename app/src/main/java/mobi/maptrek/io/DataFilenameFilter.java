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

package mobi.maptrek.io;

import java.io.File;
import java.io.FilenameFilter;

public class DataFilenameFilter implements FilenameFilter {
    @Override
    public boolean accept(final File dir, final String filename) {
        String lc = filename.toLowerCase();
        return lc.endsWith(TrackManager.EXTENSION) || lc.endsWith(RouteManager.EXTENSION)
                || lc.endsWith(GPXManager.EXTENSION)
                || lc.endsWith(KMLManager.EXTENSION) || lc.endsWith(KMLManager.ZIP_EXTENSION);
    }
}
