package mobi.maptrek.location;

import mobi.maptrek.data.MapObject;

public interface INavigationService {
    boolean isNavigating();
    boolean isNavigatingViaRoute();
    MapObject getWaypoint();
    float getDistance();
    float getBearing();
    float getTurn();
    float getVmg();
    float getXtk();
    int getEte();
}
