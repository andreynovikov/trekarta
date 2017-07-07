/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 devemux86
 * Copyright 2017 Andrey Novikov
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
package mobi.maptrek.layers.building;

import org.oscim.backend.canvas.Color;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.ExtrusionBucket;
import org.oscim.renderer.bucket.ExtrusionBuckets;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.theme.styles.ExtrusionStyle;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.utils.pool.Inlist;

import mobi.maptrek.maps.maptrek.ExtendedMapElement;

public class BuildingLayer extends org.oscim.layers.tile.buildings.BuildingLayer {
    public BuildingLayer(Map map, VectorTileLayer tileLayer) {
        super(map, tileLayer);
    }

    /**
     * TileLoaderThemeHook
     */
    @Override
    public boolean process(MapTile tile, RenderBuckets buckets, MapElement el,
                           RenderStyle style, int level) {

        if (!(style instanceof ExtrusionStyle))
            return false;

        ExtrusionStyle extrusion = (ExtrusionStyle) style;

        ExtendedMapElement element = (ExtendedMapElement) el;

        int height = element.buildingHeight > 0 ? element.buildingHeight : 12 * 100; // 12m default
        int minHeight = element.buildingMinHeight;

        float[] colors = extrusion.colors;

        if (element.buildingColor != 0 || element.roofColor != 0) {
            // As defined in style
            float alpha = 0.9f;
            colors = new float[16];
            System.arraycopy(extrusion.colors, 0, colors, 0, colors.length);
            if (element.roofColor != 0) {
                colors[0] = alpha * Color.rToFloat(element.roofColor);
                colors[1] = alpha * Color.gToFloat(element.roofColor);
                colors[2] = alpha * Color.bToFloat(element.roofColor);
                colors[3] = alpha;
            }
            if (element.buildingColor != 0) {
                colors[4] = alpha * Color.rToFloat(element.buildingColor);
                colors[5] = alpha * Color.gToFloat(element.buildingColor);
                colors[6] = alpha * Color.bToFloat(element.buildingColor);
                colors[7] = alpha;
                colors[8] = alpha * Color.rToFloat(element.buildingColor);
                colors[9] = alpha * Color.gToFloat(element.buildingColor);
                colors[10] = alpha * Color.bToFloat(element.buildingColor);
                colors[11] = alpha;
            }
        }

        ExtrusionBuckets ebs = get(tile);

        for (ExtrusionBucket b = ebs.buckets; b != null; b = b.next()) {
            if (b.colors == colors) {
                b.add(element, height, minHeight);
                return true;
            }
        }

        double lat = MercatorProjection.toLatitude(tile.y);
        float groundScale = (float) MercatorProjection
                .groundResolution(lat, 1 << tile.zoomLevel);

        ebs.buckets = Inlist.push(ebs.buckets,
                new ExtrusionBucket(0, groundScale,
                        colors));

        ebs.buckets.add(element, height, minHeight);

        return true;
    }
}
