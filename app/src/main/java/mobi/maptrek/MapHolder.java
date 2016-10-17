package mobi.maptrek;

import org.oscim.core.GeoPoint;
import org.oscim.map.Map;

public interface MapHolder {
    Map getMap();

    void updateMapViewArea();

    void disableLocations();

    void disableTracking();

    boolean isNavigatingTo(GeoPoint coordinates);

    void stopNavigation();

    /**
     * Adds location state change listener and then calls listener with current state.
     */
    void addLocationStateChangeListener(LocationStateChangeListener listener);

    void removeLocationStateChangeListener(LocationStateChangeListener listener);

    void addLocationChangeListener(LocationChangeListener listener);

    void removeLocationChangeListener(LocationChangeListener listener);

    void setMapLocation(GeoPoint point);

    void showMarker(GeoPoint point, String name);

    void removeMarker();
}
