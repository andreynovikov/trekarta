package mobi.maptrek.maps.maptrek;

import org.oscim.core.MapElement;

public class ExtendedMapElement extends MapElement {
    public long id = 0L;
    public int buildingHeight = 0;
    public int buildingMinHeight = 0;
    public int buildingColor = 0;
    public int roofColor = 0;
    public int elevation = 0;
    public boolean hasLabelPosition = true;

    public void clearData() {
        layer = 5;
        hasLabelPosition = true;
        labelPosition = null;

        id = 0L;
        elevation = 0;
        buildingHeight = 0;
        buildingMinHeight = 0;
        buildingColor = 0;
        roofColor = 0;
    }
}
