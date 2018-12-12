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

package mobi.maptrek.data;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import mobi.maptrek.util.Geo;

public class Route {
    public String name;
    public String description;
    public boolean show;
    public int width;

    public double distance;
    public boolean removed = false;

    private final ArrayList<Waypoint> waypoints = new ArrayList<>();
    private Waypoint lastWaypoint;

    public Route() {
        this("", "", false);
    }

    public Route(String name, String description, boolean show) {
        this.name = name;
        this.description = description;
        this.show = show;
        distance = 0;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void addWaypoint(Waypoint waypoint) {
        if (lastWaypoint != null) {
            distance += lastWaypoint.coordinates.vincentyDistance(waypoint.coordinates);
        }
        lastWaypoint = waypoint;
        waypoints.add(lastWaypoint);
    }

    public void addWaypoint(int pos, Waypoint waypoint) {
        waypoints.add(pos, waypoint);
        lastWaypoint = waypoints.get(waypoints.size() - 1);
        distance = distanceBetween(0, waypoints.size() - 1);
    }

    public Waypoint addWaypoint(String name, double lat, double lon) {
        Waypoint waypoint = new Waypoint(name, lat, lon);
        addWaypoint(waypoint);
        return waypoint;
    }

    public void insertWaypoint(Waypoint waypoint) {
        if (waypoints.size() < 2) {
            addWaypoint(waypoint);
            return;
        }
        int after = waypoints.size() - 1;
        double xtk = Double.MAX_VALUE;
        synchronized (waypoints) {
            for (int i = 0; i < waypoints.size() - 1; i++) {
                double distance = waypoint.coordinates.vincentyDistance(waypoints.get(i + 1).coordinates);
                double bearing1 = waypoint.coordinates.bearingTo(waypoints.get(i + 1).coordinates);
                double dtk1 = waypoints.get(i).coordinates.bearingTo(waypoints.get(i + 1).coordinates);
                double cxtk1 = Math.abs(Geo.xtk(distance, dtk1, bearing1));
                double bearing2 = waypoint.coordinates.bearingTo(waypoints.get(i).coordinates);
                double dtk2 = waypoints.get(i + 1).coordinates.bearingTo(waypoints.get(i).coordinates);
                double cxtk2 = Math.abs(Geo.xtk(distance, dtk2, bearing2));

                if (cxtk2 != Double.POSITIVE_INFINITY && cxtk1 < xtk) {
                    xtk = cxtk1;
                    after = i;
                }
            }
        }
        waypoints.add(after + 1, waypoint);
        lastWaypoint = waypoints.get(waypoints.size() - 1);
        distance = distanceBetween(0, waypoints.size() - 1);
    }

    public Waypoint insertWaypoint(String name, double lat, double lon) {
        Waypoint waypoint = new Waypoint(name, lat, lon);
        insertWaypoint(waypoint);
        return waypoint;
    }

    public void insertWaypoint(int after, Waypoint waypoint) {
        waypoints.add(after + 1, waypoint);
        lastWaypoint = waypoints.get(waypoints.size() - 1);
        distance = distanceBetween(0, waypoints.size() - 1);
    }

    public Waypoint insertWaypoint(int after, String name, double lat, double lon) {
        Waypoint waypoint = new Waypoint(name, lat, lon);
        insertWaypoint(after, waypoint);
        return waypoint;
    }

    public void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint);
        if (waypoints.size() > 0) {
            lastWaypoint = waypoints.get(waypoints.size() - 1);
            distance = distanceBetween(0, waypoints.size() - 1);
        } else {
            lastWaypoint = null;
            distance = 0f;
        }
    }

    public Waypoint getWaypoint(int index) throws IndexOutOfBoundsException {
        return waypoints.get(index);
    }

    public int length() {
        return waypoints.size();
    }

    public void clear() {
        synchronized (waypoints) {
            waypoints.clear();
        }
        lastWaypoint = null;
        distance = 0;
    }

    public double distanceBetween(int first, int last) {
        double dist = 0.0;
        synchronized (waypoints) {
            for (int i = first; i < last; i++) {
                dist += waypoints.get(i).coordinates.vincentyDistance(waypoints.get(i + 1).coordinates);
            }
        }
        return dist;
    }

    public double course(int prev, int next) {
        synchronized (waypoints) {
            return waypoints.get(prev).coordinates.bearingTo(waypoints.get(next).coordinates);
        }
    }

    public Waypoint getNearestWaypoint(GeoPoint point) {
        synchronized (waypoints) {
            int index = waypoints.size() - 1;
            double distance = point.vincentyDistance(waypoints.get(index).coordinates);
            for (int i = index - 1; i >= 0; i--) {
                double d = point.vincentyDistance(waypoints.get(i).coordinates);
                if (d < distance) {
                    distance = d;
                    index = i;
                }
            }
            return waypoints.get(index);
        }
    }
}
