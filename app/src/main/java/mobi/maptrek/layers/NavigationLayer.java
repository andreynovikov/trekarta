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

package mobi.maptrek.layers;

import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeoPoint;
import org.oscim.layers.PathLayer;
import org.oscim.map.Map;
import org.oscim.theme.styles.LineStyle;

/**
 * This class draws a great circle navigation line.
 */
public class NavigationLayer extends PathLayer {
    private GeoPoint mDestination;
    private GeoPoint mPosition;

    public NavigationLayer(Map map, int lineColor, float lineWidth) {
        super(map, Color.setA(lineColor, 0x66), lineWidth);
    }

    public void setLineStyle(int lineColor, float lineWidth) {
        super.setStyle(new LineStyle(Color.setA(lineColor, 0x66), lineWidth, Cap.BUTT));
    }

    public void setDestination(GeoPoint destination) {
        synchronized (mPoints) {
            mDestination = destination;
            clearPath();
            if (mPosition != null) {
                addPoint(mPosition);
                addGreatCircle(mPosition, mDestination);
                addPoint(mDestination);
            }
        }
    }

    public GeoPoint getDestination() {
        return mDestination;
    }

    public void setPosition(double lat, double lon) {
        synchronized (mPoints) {
            mPosition = new GeoPoint(lat, lon);
            clearPath();
            addPoint(mPosition);
            addGreatCircle(mPosition, mDestination);
            addPoint(mDestination);
        }
    }
}