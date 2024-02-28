/*
 * Copyright 2019 Andrey Novikov
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
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.PathLayer;
import org.oscim.map.Map;
import org.oscim.theme.styles.LineStyle;

import mobi.maptrek.data.Route;
import mobi.maptrek.data.Track;

public class RouteLayer extends PathLayer implements Route.UpdateListener {
    private final Route mRoute;
    private Track mTrack;

    public RouteLayer(Map map, Route route) {
        this(map, route.style.color, route.style.width, route);
    }

    public RouteLayer(Map map, int lineColor, float lineWidth, Route route) {
        super(map, new LineStyle(Color.setA(lineColor, 0x80), lineWidth, Paint.Cap.ROUND));
        mRoute = route;
        mRoute.setUpdateListener(this);
        onRouteChanged();
    }

    public RouteLayer(Map mMap, Route route, Track track) {
        this(mMap, route);
        mTrack = track;
        setPoints(track.points);
    }

    public void setWidth(int width) {
        mLineStyle = new LineStyle(mLineStyle.color, width, mLineStyle.cap);
        update();
    }

    @Override
    public void onDetach() {
        mRoute.removeUpdateListener();
    }

    @Override
    public void onRouteChanged() {
        setPoints(mRoute.getCoordinates());
    }

    public Route getRoute() {
        return mRoute;
    }

    public Track getTrack() {
        return mTrack;
    }
}
