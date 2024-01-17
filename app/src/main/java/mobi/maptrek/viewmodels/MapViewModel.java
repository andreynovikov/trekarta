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

package mobi.maptrek.viewmodels;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.oscim.core.GeoPoint;

public class MapViewModel extends ViewModel {
    public static class MarkerState {
        private final GeoPoint coordinates;
        private final String name;
        private final boolean amenity;
        private final boolean shown;

        public MarkerState(GeoPoint coordinates, String name, boolean amenity, boolean shown) {
            this.coordinates = coordinates;
            this.name = name;
            this.amenity = amenity;
            this.shown = shown;
        }

        public GeoPoint getCoordinates() {
            return coordinates;
        }

        public String getName() {
            return name;
        }

        public boolean isAmenity() {
            return amenity;
        }

        public boolean isShown() {
            return shown;
        }
    }

    private final MutableLiveData<Location> location = new MutableLiveData<>(new Location("unknown"));
    public LiveData<Location> getLocation() {
        return location;
    }
    public void setLocation(Location location) {
        this.location.setValue(location);
    }

    public void showMarker(@NonNull GeoPoint coordinates, String name, boolean amenity) {
        markerState.setValue(new MarkerState(coordinates, name, amenity, true));
    }

    public void removeMarker() {
        markerState.setValue(new MarkerState(null, null, false, false));
    }

    private final MutableLiveData<MarkerState> markerState = new MutableLiveData<>(new MarkerState(null, null, false, false));
    public LiveData<MarkerState> getMarkerState() {
        return markerState;
    }
}
