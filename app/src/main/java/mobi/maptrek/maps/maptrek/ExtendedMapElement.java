package mobi.maptrek.maps.maptrek;

import org.oscim.core.MapElement;

public class ExtendedMapElement extends MapElement {
    public long id = 0L;
    public int buildingHeight = 0;
    public int buildingMinHeight = 0;
    public int buildingColor = 0;
    public int roofColor = 0;
    public int elevation = 0;
    boolean hasLabelPosition = true;
    public MapTrekDatabase database;
    public int kind = 0;

    void clearData() {
        id = 0L;
        layer = 5;
        kind = 0;
        hasLabelPosition = true;
        labelPosition = null;
        database = null;
        elevation = 0;
        buildingHeight = 0;
        buildingMinHeight = 0;
        buildingColor = 0;
        roofColor = 0;
    }

    boolean isBuilding() {
        return (kind & 0x00000004) > 0;
    }
}
