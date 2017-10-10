package mobi.maptrek;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.oscim.core.GeoPoint;
import org.oscim.map.Map;

public interface MapHolder {
    Map getMap();

    void updateMapViewArea();

    void disableLocations();

    void disableTracking();

    void shareLocation(@NonNull GeoPoint coordinates, @Nullable String name);

    void navigateTo(@NonNull GeoPoint coordinates, @Nullable String name);

    boolean isNavigatingTo(@NonNull GeoPoint coordinates);

    void stopNavigation();

    /**
     * Adds location state change listener and then calls listener with current state.
     */
    void addLocationStateChangeListener(LocationStateChangeListener listener);

    void removeLocationStateChangeListener(LocationStateChangeListener listener);

    void addLocationChangeListener(LocationChangeListener listener);

    void removeLocationChangeListener(LocationChangeListener listener);

    void setMapLocation(@NonNull GeoPoint point);

    void showMarker(@NonNull GeoPoint point, @Nullable String name);

    void removeMarker();
}
