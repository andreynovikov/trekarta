/*
 * Copyright 2018-2019 Gustl22
 * Copyright 2018-2019 devemux86
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
 */
package org.oscim.layers.tile.buildings;

import org.oscim.backend.canvas.Color;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Map;
import org.oscim.theme.styles.ExtrusionStyle;
import org.oscim.utils.ExtrusionUtils;
import org.oscim.utils.geom.GeometryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mobi.maptrek.maps.maptrek.ExtendedMapElement;

import static org.oscim.renderer.MapRenderer.COORD_SCALE;

/**
 * A layer to display S3DB elements.
 */
public class S3DBLayer extends BuildingLayer {

    private static final Logger log = LoggerFactory.getLogger(S3DBLayer.class);

    private final float TILE_SCALE = (ExtrusionUtils.REF_TILE_SIZE / (Tile.SIZE * COORD_SCALE));

    private boolean mColored = true;
    private boolean mTransparent = true;

    public S3DBLayer(Map map, VectorTileLayer tileLayer) {
        this(map, tileLayer, false);
    }

    public S3DBLayer(Map map, VectorTileLayer tileLayer, boolean shadow) {
        this(map, tileLayer, MIN_ZOOM, map.viewport().getMaxZoomLevel(), shadow);
    }

    /**
     * @param map       The map data to add
     * @param tileLayer The vector tile layer which contains the tiles and map elements
     * @param zoomMin   The minimum zoom at which the layer appears
     * @param zoomMax   The maximum zoom at which the layer appears
     * @param shadow    Declare if using shadow renderer
     */
    public S3DBLayer(Map map, VectorTileLayer tileLayer, int zoomMin, int zoomMax, boolean shadow) {
        super(map, tileLayer, zoomMin, zoomMax, true, shadow);
    }

    public boolean isColored() {
        return mColored;
    }

    /**
     * @param colored true: use colour written in '*:colour' tag,
     *                false: use colours of extrusion style
     */
    public void setColored(boolean colored) {
        mColored = colored;
    }

    public boolean isTransparent() {
        return mTransparent;
    }

    /**
     * @param transparent if true coloured buildings blend transparency of extrusion style
     *                    (only in combination with isColored = true)
     */
    public void setTransparent(boolean transparent) {
        mTransparent = transparent;
    }

    @Override
    public void complete(MapTile tile, boolean success) {
        super.complete(tile, success);
        // Do stuff here
    }

    @Override
    public void processElement(MapElement element, ExtrusionStyle extrusion, MapTile tile) {
        if (!(element instanceof ExtendedMapElement))
            return;

        ExtendedMapElement extendedElement = (ExtendedMapElement) element;

        float groundScale = tile.getGroundScale();

        int maxHeight = extendedElement.buildingHeight;
        int minHeight = extendedElement.buildingMinHeight;
        int roofHeight = extendedElement.roofHeight;

        /*
        } else if ((v = getValue(element, Tag.KEY_ROOF_ANGLE)) != null) {
            Box bb = null;
            for (int k = 0; k < element.index[0]; k += 2) {
                float p1 = element.points[k];
                float p2 = element.points[k + 1];
                if (bb == null)
                    bb = new Box(p1, p2);
                else {
                    bb.add(p1, p2);
                }
            }
            if (bb != null) {
                float minSize = (int) Math.min(bb.getHeight(), bb.getWidth()) * groundScale; // depends on lat
                // Angle is simplified, 40 is an estimated constant
                roofHeight = (int) ((Float.parseFloat(v) / 45.f) * (minSize * 40));
            }
        */
        if (maxHeight == 0)
            maxHeight = extrusion.defaultHeight * 100;

        if (roofHeight == 0 && extendedElement.roofShape != null && !extendedElement.roofShape.equals(Tag.VALUE_FLAT)) {
            int h = maxHeight - minHeight;
            if (h > BUILDING_LEVEL_HEIGHT)
                roofHeight = BUILDING_LEVEL_HEIGHT;
            else
                roofHeight = h >> 1;
        }

        int bColor = 0;

        if (mColored) {
            bColor = extendedElement.buildingColor;
            if (bColor != 0 && mTransparent) {
                // Multiply alpha channel of extrusion style
                bColor = ExtrusionStyle.blendAlpha(bColor, Color.aToFloat(extrusion.colorSide));
            }
        }

        if (bColor == 0)
            bColor = extrusion.colorSide;

        // Scale x, y and z axis
        ExtrusionUtils.mapPolyCoordScale(element);
        float minHeightS = ExtrusionUtils.mapGroundScale(minHeight, groundScale) * TILE_SCALE;
        float maxHeightS = ExtrusionUtils.mapGroundScale(maxHeight, groundScale) * TILE_SCALE;
        float minRoofHeightS = ExtrusionUtils.mapGroundScale(maxHeight - roofHeight, groundScale) * TILE_SCALE;

        // Process building and roof
        processRoof(extendedElement, tile, minRoofHeightS, maxHeightS, bColor, extrusion);
        if (S3DBUtils.calcOutlines(element, minHeightS, minRoofHeightS)) {
            get(tile).addMeshElement(element, groundScale, bColor);
        }
    }

    @Override
    protected void processElements(MapTile tile) {
        if (!mBuildings.containsKey(tile.hashCode()))
            return;

        List<BuildingElement> tileBuildings = mBuildings.get(tile.hashCode());
        Set<BuildingElement> rootBuildings = new HashSet<>();
        for (BuildingElement partBuilding : tileBuildings) {
            if (!partBuilding.element.isBuildingPart())
                continue;

            if (!(partBuilding.element instanceof ExtendedMapElement))
                continue;
            ExtendedMapElement partElement = (ExtendedMapElement) partBuilding.element;

            String refId = getValue(partBuilding.element, Tag.KEY_REF);
            if (!RAW_DATA && refId == null)
                continue;

            // Search buildings which inherit parts
            for (BuildingElement rootBuilding : tileBuildings) {
                if (rootBuilding.element.isBuildingPart())
                    continue;
                if (!(rootBuilding.element instanceof ExtendedMapElement))
                    continue;
                ExtendedMapElement rootElement = (ExtendedMapElement) rootBuilding.element;

                if (RAW_DATA) {
                    float[] center = GeometryUtils.center(partBuilding.element.points, 0, partBuilding.element.pointNextPos, null);
                    if (!GeometryUtils.pointInPoly(center[0], center[1], rootBuilding.element.points, rootBuilding.element.index[0], 0))
                        continue;
                } else if (!refId.equals(rootBuilding.element.tags.getValue(Tag.KEY_ID)))
                    continue;

                if ((rootElement.roofShape != null)
                        && (partElement.roofShape == null)) {
                    partElement.roofShape = rootElement.roofShape;
                }

                if (mColored) {
                    if (partElement.buildingColor == 0)
                        partElement.buildingColor = rootElement.buildingColor;
                    if (partElement.roofColor == 0)
                        partElement.roofColor = rootElement.roofColor;
                }
                rootBuildings.add(rootBuilding);
                break;
            }
        }

        tileBuildings.removeAll(rootBuildings); // root buildings aren't rendered

        for (BuildingElement buildingElement : tileBuildings) {
            processElement(buildingElement.element, buildingElement.style, tile);
        }
        mBuildings.remove(tile.hashCode());
    }

    /**
     * Process the roof parts of building.
     *
     * @param element       the MapElement which needs a roof
     * @param tile          the tile which contains map element
     * @param minHeight     the height of the underlying building
     * @param maxHeight     the height of the roof + minHeight (whole building)
     * @param buildingColor the color of main building
     * @param extrusion     the extrusion style
     */
    private void processRoof(ExtendedMapElement element, MapTile tile, float minHeight, float maxHeight,
                             int buildingColor, ExtrusionStyle extrusion) {
        int roofColor = extrusion.colorTop;

        if (mColored) {
            if (element.roofColor != 0)
                roofColor = element.roofColor;
        }

        boolean roofOrientationAcross = element.roofOrientationAcross;

        float groundScale = tile.getGroundScale();

        GeometryBuffer gElement = new GeometryBuffer(element);
        GeometryBuffer specialParts = null;

        if (mTransparent) {
            // Use transparency of default roof color
            roofColor = ExtrusionStyle.blendAlpha(roofColor, Color.aToFloat(extrusion.colorTop));
        }

        String roofShape = element.roofShape != null ? element.roofShape : Tag.VALUE_FLAT;
        boolean success;
        switch (roofShape) {
            case Tag.VALUE_DOME:
            case Tag.VALUE_ONION:
                success = S3DBUtils.calcCircleMesh(gElement, minHeight, maxHeight, element.roofShape);
                break;
            case Tag.VALUE_ROUND:
            case Tag.VALUE_SALTBOX:
            case Tag.VALUE_GABLED:
            case Tag.VALUE_GAMBREL:
                specialParts = new GeometryBuffer(0, 0); // No data in GeometryBuffer needed
                success = S3DBUtils.calcRidgeMesh(gElement, minHeight, maxHeight, roofOrientationAcross, roofShape, specialParts);
                break;
            case Tag.VALUE_MANSARD:
            case Tag.VALUE_HALF_HIPPED:
            case Tag.VALUE_HIPPED:
                success = S3DBUtils.calcRidgeMesh(gElement, minHeight, maxHeight, roofOrientationAcross, roofShape, null);
                break;
            case Tag.VALUE_SKILLION:
                // ROOF_SLOPE_DIRECTION is not supported yet
                float roofDegree = 0;
                if (element.roofDirection != -1) {
                    roofDegree = element.roofDirection;
                }
                specialParts = new GeometryBuffer(element);
                success = S3DBUtils.calcSkillionMesh(gElement, minHeight, maxHeight, roofDegree, specialParts);
                break;
            case Tag.VALUE_PYRAMIDAL:
                success = S3DBUtils.calcPyramidalMesh(gElement, minHeight, maxHeight);
                break;
            case Tag.VALUE_FLAT:
            default:
                success = S3DBUtils.calcFlatMesh(gElement, minHeight);
                break;
        }

        if (success) {
            get(tile).addMeshElement(gElement, groundScale, roofColor);
            if (specialParts != null) {
                get(tile).addMeshElement(specialParts, groundScale, buildingColor);
            }
        } else {
            log.debug("Roof calculation failed: " + element.toString());
        }
    }
}
