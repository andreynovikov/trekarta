package mobi.maptrek.fragments;

import mobi.maptrek.MapSelectionListener;
import mobi.maptrek.maps.MapFile;

public interface OnMapActionListener {
    void onMapSelected(MapFile map);
    void onBeginMapSelection(MapSelectionListener listener);
    void onFinishMapSelection();
    void onDownloadSelectedMaps(boolean[][] mSelectionState);
}
