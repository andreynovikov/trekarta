package mobi.maptrek.maps;

public interface MapStateListener {

    void onHasDownloadSizes();

    void onStatsChanged(MapIndex.IndexStats stats);

    void onMapSelected(int x, int y, MapIndex.ACTION action, MapIndex.IndexStats stats);
}
