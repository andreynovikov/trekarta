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
import org.oscim.core.MapPosition;
import org.oscim.core.Point;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileSet;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.VectorTileLoader;
import org.oscim.map.Map;
import org.oscim.utils.Utils;

import java.util.ArrayList;

import mobi.maptrek.MapTrek;
import mobi.maptrek.maps.maptrek.MapTrekTileSource;

public class MapTrekTileLayer extends VectorTileLayer implements GestureListener {
    public static final String POI_DATA = MapTrekTileLayer.class.getSimpleName();

    private static final int MAX_ZOOMLEVEL = 17;
    private static final int MIN_ZOOMLEVEL = 2;
    private static final int CACHE_LIMIT = 150;

    private double mScale;
    private TileSet mTileSet = new TileSet();
    private OnAmenityGestureListener mOnAmenityGestureListener;
    private final double mFingerTipSize;

    public static class AmenityTileData extends MapTile.TileData {
        public final ArrayList<Pair<Point, Long>> amenities = new ArrayList<>();

        @Override
        protected void dispose() {
            amenities.clear();
        }
    }

    public MapTrekTileLayer(Map map, MapTrekTileSource tileSource, OnAmenityGestureListener listener) {
        super(map, CACHE_LIMIT);
        mTileManager.setZoomLevel(MIN_ZOOMLEVEL, MAX_ZOOMLEVEL);
        mOnAmenityGestureListener = listener;
        mFingerTipSize = MapTrek.ydpi * 0.08d;
        setTileSource(tileSource);
    }

    @Override
    protected TileLoader createLoader() {
        return new MapTrekTileLayer.OsmTileLoader(this);
    }

    private static class OsmTileLoader extends VectorTileLoader {
        private final TagSet mFilteredTags;

        OsmTileLoader(VectorTileLayer tileLayer) {
            super(tileLayer);
            mFilteredTags = new TagSet();
        }

        /* Replace tags that should only be matched by key in RenderTheme
         * to avoid caching RenderInstructions for each way of the same type
         * only with different name.
         * Maybe this should be done within RenderTheme, also allowing
         * to set these replacement rules in theme file. */
        private static final TagReplacement[] mTagReplacement = {
                new TagReplacement(Tag.KEY_NAME),
                new TagReplacement(Tag.KEY_HOUSE_NUMBER),
                new TagReplacement(Tag.KEY_REF),
                new TagReplacement(Tag.KEY_HEIGHT),
                new TagReplacement(Tag.KEY_MIN_HEIGHT)
        };

        protected TagSet filterTags(TagSet tagSet) {
            Tag[] tags = tagSet.getTags();

            mFilteredTags.clear();

            O:
            for (int i = 0, n = tagSet.size(); i < n; i++) {
                Tag t = tags[i];

                for (TagReplacement replacement : mTagReplacement) {
                    if (Utils.equals(t.key, replacement.key)) {
                        mFilteredTags.add(replacement.tag);
                        continue O;
                    }
                }

                mFilteredTags.add(t);
            }

            return mFilteredTags;
        }
    }

    @Override
    public void onMapEvent(Event ev, MapPosition pos) {
        super.onMapEvent(ev, pos);
        mScale = pos.scale;

        mTileSet.releaseTiles();

        if (ev == Map.CLEAR_EVENT) {
            mTileSet = new TileSet();
        }

        tileRenderer().getVisibleTiles(mTileSet);
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
