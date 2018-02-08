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

package mobi.maptrek.fragments;

import org.oscim.core.GeoPoint;

import java.util.Set;

import mobi.maptrek.data.Waypoint;

public interface OnWaypointActionListener {
    void onWaypointCreate(GeoPoint point, String name, boolean locked, boolean customize);

    /**
     * Position map so that waypoint is visible
     */
    void onWaypointView(Waypoint waypoint);

    void onWaypointFocus(Waypoint waypoint);

    void onWaypointDetails(Waypoint waypoint, boolean full);

    void onWaypointNavigate(Waypoint waypoint);

    void onWaypointShare(Waypoint waypoint);

    void onWaypointSave(Waypoint waypoint);

    void onWaypointDelete(Waypoint waypoint);

    void onWaypointsDelete(Set<Waypoint> waypoints);
}
