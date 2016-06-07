package mobi.maptrek;

import org.oscim.core.GeoPoint;
import org.oscim.map.Map;

public interface MapHolder {
    Map getMap();

    void updateMapViewArea();

    void disableLocations();

    void setMapLocation(GeoPoint point);
}
