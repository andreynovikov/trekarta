package mobi.maptrek;

public interface MapSelectionListener {
    void onMapSelected(int x, int y);
    void registerMapSelectionState(boolean[][] selectedState);
}
