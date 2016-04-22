package mobi.maptrek.map;

import org.oscim.core.BoundingBox;
import org.oscim.layers.tile.TileLayer;
import org.oscim.tiling.TileSource;

public class MapFile {
    public String name;
    public BoundingBox boundingBox;
    // If we would store index in file, use path and type instead
    public TileSource tileSource;
    public transient TileLayer tileLayer;
}
