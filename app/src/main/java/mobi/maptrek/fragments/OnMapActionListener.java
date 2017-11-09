package mobi.maptrek.fragments;

import mobi.maptrek.maps.MapFile;

public interface OnMapActionListener {
    void onMapSelected(MapFile map);
    void onHideMapObjects(boolean hide);
    void onTransparencyChanged(int transparency);
    void onBeginMapManagement();
    void onFinishMapManagement();
    void onManageNativeMaps(boolean hillshadesEnabled);
}
