package mobi.maptrek.fragments;

import mobi.maptrek.MapSelectionListener;
import mobi.maptrek.maps.MapFile;

public interface OnMapActionListener {
    void onMapSelected(MapFile map);
    void onBeginMapManagement(MapSelectionListener listener);
    void onFinishMapManagement();
    void onManageSelectedMaps(MapSelectionListener.ACTION[][] actionState);
}
