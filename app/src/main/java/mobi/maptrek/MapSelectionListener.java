package mobi.maptrek;

public interface MapSelectionListener {
    enum ACTION {NONE, DOWNLOAD, REMOVE}

    void onMapSelected(int x, int y, ACTION action);

    void registerMapSelectionState(ACTION[][] actionState);
}
