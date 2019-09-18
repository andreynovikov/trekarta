/*
 * Copyright 2019 Andrey Novikov
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

package mobi.maptrek.layers;

import android.util.Pair;

import org.oscim.core.Box;
import org.oscim.core.MapElement;
import org.oscim.core.MapPosition;
import org.oscim.core.Point;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.MapTile.TileData;
import org.oscim.layers.tile.TileSet;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import mobi.maptrek.MapTrek;
import mobi.maptrek.maps.maptrek.ExtendedMapElement;

import static org.oscim.core.GeometryBuffer.GeometryType.POINT;
import static org.oscim.core.GeometryBuffer.GeometryType.POLY;

public class AmenityLayer extends Layer implements Map.UpdateListener, GestureListener {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(AmenityLayer.class);

    private final static String POI_DATA = AmenityLayer.class.getSimpleName();

    private VectorTileLayer mTileLayer;
    private double mScale;
    private TileSet mTileSet = new TileSet();
    private OnAmenityGestureListener mOnAmenityGestureListener;
    private final double mFingerTipSize;

    static class AmenityTileData extends TileData {
        final ArrayList<Pair<Point, Long>> amenities = new ArrayList<>();

        @Override
        protected void dispose() {
            amenities.clear();
        }
    }

    private AmenityTileData get(MapTile tile) {
        AmenityTileData td = (AmenityTileData) tile.getData(POI_DATA);
        if (td == null) {
            td = new AmenityTileData();
            tile.addData(POI_DATA, td);
        }
        return td;
    }

    public AmenityLayer(Map map, VectorTileLayer tileLayer, OnAmenityGestureListener listener) {
        super(map);
        tileLayer.addHook(new VectorTileLayer.TileLoaderThemeHook() {

            @Override
            public boolean process(MapTile tile, RenderBuckets buckets, MapElement element, RenderStyle style, int level) {
                if (!(element instanceof ExtendedMapElement))
                    return false;

                ExtendedMapElement extendedElement = (ExtendedMapElement) element;
                // skip places, roads, buildings, barriers
                if (extendedElement.id == 0L || (extendedElement.kind & 0x0FFBFFF8) == 0)
                    return false;

                // skip amenities without symbols
                if (!(style instanceof SymbolStyle))
                    return false;
                SymbolStyle symbol = (SymbolStyle) style.current();
                if (symbol.bitmap == null)
                    return false;

                if (element.type == POINT) {
                    for (int i = 0, n = element.getNumPoints(); i < n; i++) {
                        PointF p = element.getPoint(i);
                        addAmenity(p, tile, extendedElement.id);
                    }
                } else if (element.type == POLY) {
                    PointF centroid = element.labelPosition;
                    if (centroid == null)
                        return false;

                    if (centroid.x < 0 || centroid.x > Tile.SIZE || centroid.y < 0 || centroid.y > Tile.SIZE)
                        return false;
                    addAmenity(centroid, tile, extendedElement.id);
                }

                return false;
            }

            @Override
            public void complete(MapTile tile, boolean success) {
            }
        });

        mTileLayer = tileLayer;
        mOnAmenityGestureListener = listener;
        mFingerTipSize = MapTrek.ydpi * 0.08d;
    }

    private void addAmenity(PointF point, MapTile tile, long id) {
        if (point.x < 0 || point.x > Tile.SIZE || point.y < 0 || point.y > Tile.SIZE)
            return;

        AmenityTileData td = get(tile);

        double x = tile.x + point.x / tile.mapSize;
        double y = tile.y + point.y / tile.mapSize;
        td.amenities.add(new Pair<>(new Point(x, y), id));
    }

    @Override
    public void onMapEvent(Event ev, MapPosition pos) {
        mScale = pos.scale;

        if (ev == Map.CLEAR_EVENT) {
            mTileSet = new TileSet();
        }

        mTileSet.releaseTiles();
        mTileLayer.tileRenderer().getVisibleTiles(mTileSet);
    }

    @Override
    public boolean onGesture(Gesture g, MotionEvent e) {
        if (!(g instanceof Gesture.Tap))
            return false;

        Point point = new Point();
        mMap.viewport().fromScreenPoint(e.getX(), e.getY(), point);

        Box box = mMap.viewport().getBBox(null, 128);

        double cs = mScale * Tile.SIZE;
        double distance = mFingerTipSize * mFingerTipSize / cs / cs;

        long nearest = 0L;

        for (int i = 0; i < mTileSet.cnt; i++) {
            MapTile t = mTileSet.tiles[i];
            AmenityTileData td = (AmenityTileData) t.getData(POI_DATA);
            if (td == null || td.amenities.isEmpty())
                continue;

            double dist = distance;

            int size = td.amenities.size();
            for (int j = 0; j < size; j++) {
                Pair<Point, Long> amenity = td.amenities.get(j);

                if (!box.contains(amenity.first))
                    continue;

                double dx = amenity.first.x - point.x;
                double dy = amenity.first.y - point.y;

                double d = dx * dx + dy * dy;
                if (d > dist)
                    continue;

                dist = d;
                nearest = amenity.second;
            }

        }

        if (nearest > 0L) {
            return mOnAmenityGestureListener != null &&
                mOnAmenityGestureListener.onAmenitySingleTapUp(nearest);
        }
        return false;
    }

    public interface OnAmenityGestureListener {
        boolean onAmenitySingleTapUp(long amenityId);
    }
}
