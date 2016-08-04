package mobi.maptrek.fragments;

import mobi.maptrek.maps.MapFile;

public interface OnMapActionListener {
    void onMapSelected(MapFile map);
    void onBeginMapManagement();
    void onFinishMapManagement();
    void onManageNativeMaps();
}
