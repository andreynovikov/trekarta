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
import org.oscim.core.GeoPoint;
import org.oscim.layers.PathLayer;
import org.oscim.map.Map;

import java.util.ArrayList;
import java.util.List;

import mobi.maptrek.data.Route;
import mobi.maptrek.data.Waypoint;

public class RouteLayer extends PathLayer {
    private final Route mRoute;

    public RouteLayer(Map map, Route route) {
        super(map, Color.RED, 5);
        mRoute = route;
        setRoutePoints();
    }

    public void addWaypoint(Waypoint waypoint) {
        mRoute.addWaypoint(waypoint);
        addPoint(waypoint.coordinates);
    }

    public void insertWaypoint(Waypoint waypoint) {
        mRoute.insertWaypoint(waypoint);
        setRoutePoints();
    }

    public void removeWaypoint(Waypoint waypoint) {
        mRoute.removeWaypoint(waypoint);
        setRoutePoints();
    }

    private void setRoutePoints() {
        List<GeoPoint> points = new ArrayList<>(mRoute.length());
        for (Waypoint waypoint : mRoute.getWaypoints())
            points.add(waypoint.coordinates);
        setPoints(points);
    }
}
