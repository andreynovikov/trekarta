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

import mobi.maptrek.data.Place;

public interface OnPlaceActionListener {
    void onPlaceCreate(GeoPoint point, String name, boolean locked, boolean customize);

    /**
     * Position map so that place is visible
     */
    void onPlaceView(Place place);

    void onPlaceFocus(Place place);

    void onPlaceDetails(Place place, boolean full);

    void onPlaceNavigate(Place place);

    void onPlaceShare(Place place);

    void onPlaceSave(Place place);

    void onPlaceDelete(Place place);

    void onPlacesDelete(Set<Place> places);
}
