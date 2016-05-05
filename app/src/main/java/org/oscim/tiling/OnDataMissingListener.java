package org.oscim.tiling;

import org.oscim.layers.tile.MapTile;

public interface OnDataMissingListener {
    void onDataMissing(MapTile tile);
}
