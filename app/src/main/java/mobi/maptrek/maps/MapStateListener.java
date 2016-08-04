package mobi.maptrek.maps;

public interface MapStateListener {

    void onHasDownloadSizes();

    void onMapSelected(int x, int y, MapIndex.ACTION action);
}
