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

public class ExtendedMapElement extends MapElement {
    public long id = 0L;
    public int buildingHeight = 0;
    public int buildingMinHeight = 0;
    public int buildingColor = 0;
    public int roofColor = 0;
    public int elevation = 0;
    boolean hasLabelPosition = true;
    public MapTrekDataSource database;
    public int kind = 0;
    boolean isContour = false;
    boolean isBuilding = false;
    boolean isBuildingPart = false;

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
        isContour = false;
        isBuilding = false;
        isBuildingPart = false;
    }
}
