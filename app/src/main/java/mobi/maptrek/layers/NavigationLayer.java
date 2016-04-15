/*
 * Copyright 2012 osmdroid authors: Viesturs Zarins, Martin Pearman
 * Copyright 2012 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

package mobi.maptrek.layers;

import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.layers.vector.AbstractVectorLayer;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.theme.styles.LineStyle;

/**
 * This class draws a bearing navigation line.
 */
public class NavigationLayer extends AbstractVectorLayer {
    private final GeoPoint mDestination;

    /**
     * Line style
     */
    LineStyle mLineStyle;
    private double mLatitude;
    private double mLongitude;

    public NavigationLayer(Map map, GeoPoint destination, int lineColor, float lineWidth) {
        super(map);
        mDestination = destination;
        mLineStyle = new LineStyle(lineColor, lineWidth, Cap.BUTT);
    }

    public void setPosition(double lat, double lon) {
        mLatitude = lat;
        mLongitude = lon;
        update();
    }

    @Override
    protected void processFeatures(Task t, BoundingBox b) {
        int level = 0;

        LineBucket ll = t.buckets.getLineBucket(level * 3 + 2);
        if (ll.line == null) {
            ll.line = mLineStyle;
            ll.heightOffset = level * 4;
            ll.setDropDistance(0);
        }

        mGeom.clear();
        mGeom.startLine();

        double scale = t.position.scale * Tile.SIZE / UNSCALE_COORD;

        float x = (float) ((MercatorProjection.longitudeToX(mLongitude) - t.position.x) * scale);
        float y = (float) ((MercatorProjection.latitudeToY(mLatitude) - t.position.y) * scale);

        mGeom.addPoint(x, y);

        x = (float) ((MercatorProjection.longitudeToX(mDestination.getLongitude()) - t.position.x) * scale);
        y = (float) ((MercatorProjection.latitudeToY(mDestination.getLatitude()) - t.position.y) * scale);

        mGeom.addPoint(x, y);

        ll.addLine(mGeom);
    }
}
