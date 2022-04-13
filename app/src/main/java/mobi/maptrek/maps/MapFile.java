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

package mobi.maptrek.maps;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MercatorProjection;
import org.oscim.layers.tile.TileLayer;
import org.oscim.tiling.TileSource;

public class MapFile {
    public String name;
    public String id;
    public BoundingBox boundingBox;
    public TileSource tileSource;
    public transient TileLayer tileLayer;
    public double[] polygonPoints;

    MapFile() {
    }

    public MapFile(String name, String id) {
        this.name = name;
        this.id = id;
    }

    /**
     * Checks if map contains given point
     */
    public boolean contains(double x, double y) {
        if (polygonPoints == null) {
            GeoPoint geoPoint = new GeoPoint(MercatorProjection.toLatitude(y), MercatorProjection.toLongitude(x));
            return boundingBox.contains(geoPoint);
        }
        // http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
        //  Note that division by zero is avoided because the division is protected
        //  by the "if" clause which surrounds it.
        int j = polygonPoints.length - 2;
        boolean inside = false;

        for (int i = 0; i < polygonPoints.length; i += 2) {
            double ix = polygonPoints[i];
            double iy = polygonPoints[i + 1];
            double jx = polygonPoints[j];
            double jy = polygonPoints[j + 1];
            if (iy < y && jy >= y || jy < y && iy >= y) {
                if (ix + (y - iy) * 1. / (jy - iy) * (jx - ix) < x) {
                    inside = !inside;
                }
            }
            j = i;
        }

        return inside;
    }


}
