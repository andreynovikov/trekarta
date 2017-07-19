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

    void clearData() {
        id = 0L;
        layer = 5;
        hasLabelPosition = true;
        labelPosition = null;
        database = null;
        elevation = 0;
        buildingHeight = 0;
        buildingMinHeight = 0;
        buildingColor = 0;
        roofColor = 0;
    }
}
