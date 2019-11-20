/*
 * Copyright 2018 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.maps.maptrek;

import org.oscim.core.MapElement;
import org.oscim.theme.IRenderTheme;

public class ExtendedMapElement extends MapElement {
    public long id = 0L;
    public int buildingHeight = 0;
    public int buildingMinHeight = 0;
    public int buildingColor = 0;
    public int roofHeight = 0;
    public int roofColor = 0;
    public String roofShape = null;
    public float roofDirection = -1;
    public boolean roofOrientationAcross = false;
    public int elevation = 0;
    public long featureArea = 0;
    boolean hasLabelPosition = true;
    public MapTrekDataSource database;
    public int kind = 0;
    boolean isContour = false;
    boolean isBuilding = false;
    boolean isBuildingPart = false;

    public ExtendedMapElement() {
        super();
    }

    public ExtendedMapElement(int numPoints, int numIndices) {
        super(numPoints, numIndices);
    }

    public ExtendedMapElement(ExtendedMapElement element) {
        super(element);
        this.id = element.id;
        this.buildingHeight = element.buildingHeight;
        this.buildingMinHeight = element.buildingMinHeight;
        this.buildingColor = element.buildingColor;
        this.roofHeight = element.roofHeight;
        this.roofColor = element.roofColor;
        this.roofShape = element.roofShape;
        this.roofDirection = element.roofDirection;
        this.roofOrientationAcross = element.roofOrientationAcross;
        this.elevation = element.elevation;
        this.featureArea = element.featureArea;
        this.hasLabelPosition = element.hasLabelPosition;
        this.database = element.database;
        this.kind = element.kind;
        this.isContour = element.isContour;
        this.isBuilding = element.isBuilding;
        this.isBuildingPart = element.isBuildingPart;
    }

    /**
     * @return height in meters, if present
     */
    @Override
    public Float getHeight(IRenderTheme theme) {
        if (isBuildingPart)
            return (float) buildingHeight * 0.01f;
        return null;
    }

    /**
     * @return minimum height in meters, if present
     */
    public Float getMinHeight(IRenderTheme theme) {
        if (isBuildingPart)
            return (float) buildingMinHeight * 0.01f;
        return null;
    }

    /**
     * @return true if this is a building, else false.
     */
    public boolean isBuilding() { // TODO from themes (with overzoom ref)
        return isBuilding;
    }

    /**
     * @return true if this is a building part, else false.
     */
    public boolean isBuildingPart() { // TODO from themes (with overzoom ref)
        return isBuildingPart && !isBuilding;
    }

    @Override
    public String toString() {
        return id + " (" + kind + "): " + super.toString() + '\n';
    }

    void clearData() {
        id = 0L;
        layer = 5;
        kind = 0;
        hasLabelPosition = true;
        labelPosition = null;
        database = null;
        elevation = 0;
        featureArea = 0;
        buildingHeight = 0;
        buildingMinHeight = 0;
        buildingColor = 0;
        roofHeight = 0;
        roofColor = 0;
        roofShape = null;
        roofDirection = -1;
        roofOrientationAcross = false;
        isContour = false;
        isBuilding = false;
        isBuildingPart = false;
    }
}
