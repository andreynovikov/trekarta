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

package mobi.maptrek.layers;

import org.oscim.core.GeoPoint;
import org.oscim.map.Map;

import java.util.List;

import mobi.maptrek.data.Route;
import mobi.maptrek.layers.marker.MarkerSymbol;

public class NavigationLayer extends RouteLayer {
    private GeoPoint mPosition;

    public NavigationLayer(Map map, int lineColor, float lineWidth, MarkerSymbol pointSymbol, MarkerSymbol startSymbol, MarkerSymbol endSymbol) {
        super(map, new Route(), pointSymbol, startSymbol, endSymbol);
        LINE_ALPHA = 0x66;
        setLineStyle(lineColor, lineWidth);
    }

    public void setRemainingPoints(List<GeoPoint> points, GeoPoint previous) {
        //addGreatCircle(mPosition, mDestination);
        if (mPosition != null)
            points.add(0, mPosition);
        else if (previous != null)
            points.add(0, previous);
        setPoints(points);
    }

    public void setPosition(double lat, double lon) {
        boolean hadPosition = mPosition != null;
        mPosition = new GeoPoint(lat, lon);
        if (mPoints.isEmpty())
            return;
        synchronized (mPoints) {
            if (hadPosition || mPoints.size() > 1)
                mPoints.set(0, mPosition);
            else
                mPoints.add(0, mPosition);
            updatePoints();
        }
    }
}