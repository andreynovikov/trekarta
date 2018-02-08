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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.oscim.utils.geom.TileClipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_TILES_DATA;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_TILES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.WHERE_TILE_ZXY;
import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;
import static org.oscim.tiling.QueryResult.TILE_NOT_FOUND;


class MapTrekDataSource implements ITileDataSource {
    private static final Logger logger = LoggerFactory.getLogger(MapTrekDataSource.class);

    private static final String SQL_GET_TILE = "SELECT " + COLUMN_TILES_DATA + " FROM " + TABLE_TILES + " WHERE " + WHERE_TILE_ZXY;

    private static final int MAX_NATIVE_ZOOM = 14;
    private static final int CLIP_BUFFER = 32;
    private static final int BUILDING_CLIP_BUFFER = 4;

    private static final Tag TAG_TREE = new Tag("natural", "tree");

    private final MapTrekTileDecoder mTileDecoder;
    private final SQLiteDatabase mDatabase;
    private boolean mContoursEnabled = true;

    MapTrekDataSource(SQLiteDatabase database) {
        mDatabase = database;
        mTileDecoder = new MapTrekTileDecoder();
    }

    @Override
    public void query(MapTile tile, ITileDataSink sink) {
        int x = tile.tileX;
        int y = tile.tileY;
        int z = tile.zoomLevel;
        int dz = z - MAX_NATIVE_ZOOM;
        if (z > MAX_NATIVE_ZOOM) {
            x = x >> dz;
            y = y >> dz;
            z = MAX_NATIVE_ZOOM;
        }
        String[] args = {String.valueOf(z), String.valueOf(x), String.valueOf(y)};
        QueryResult result = tile.zoomLevel > 7 ? TILE_NOT_FOUND : SUCCESS;
        try (Cursor c = mDatabase.rawQuery(SQL_GET_TILE, args)) {
            if (c.moveToFirst()) {
                byte[] bytes = c.getBlob(0);
                NativeTileDataSink proxyDataSink = new NativeTileDataSink(sink, tile, dz, x, y);
                boolean ok = mTileDecoder.decode(tile, proxyDataSink, new ByteArrayInputStream(bytes));
                result = ok ? SUCCESS : FAILED;
            }
        } catch (Exception e) {
            logger.error("Query error", e);
            result = FAILED;
        } finally {
            sink.completed(result);
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void cancel() {
    }

    String getName(int lang, long elementId) {
        return MapTrekDatabaseHelper.getFeatureName(lang, elementId, mDatabase);
    }

    void setContoursEnabled(boolean enabled) {
        mContoursEnabled = enabled;
    }

    private class NativeTileDataSink implements ITileDataSink {
        private final Tile tile;
        private int scale;
        private int dx;
        private int dy;
        private TileClipper mTileClipper;
        private TileClipper mBuildingTileClipper;
        ITileDataSink mapDataSink;
        QueryResult result;

        NativeTileDataSink(ITileDataSink mapDataSink, Tile tile, int dz, int x, int y) {
            this.mapDataSink = mapDataSink;
            this.tile = tile;
            scale = 1;
            if (dz > 0) {
                scale = 1 << dz;
                dx = (tile.tileX - (x << dz)) * Tile.SIZE;
                dy = (tile.tileY - (y << dz)) * Tile.SIZE;
                mTileClipper = new TileClipper((dx - CLIP_BUFFER) / scale, (dy - CLIP_BUFFER) / scale,
                        (dx + Tile.SIZE + CLIP_BUFFER) / scale, (dy + Tile.SIZE + CLIP_BUFFER) / scale);
                mBuildingTileClipper = new TileClipper((dx - BUILDING_CLIP_BUFFER) / scale, (dy - BUILDING_CLIP_BUFFER) / scale,
                        (dx + Tile.SIZE + BUILDING_CLIP_BUFFER) / scale, (dy + Tile.SIZE + BUILDING_CLIP_BUFFER) / scale);
                /*
                mBuildingTileClipper = new TileClipper(dx / scale, dy / scale,
                        (dx + Tile.SIZE) / scale, (dy + Tile.SIZE) / scale);
                        */
            }
        }

        @Override
        public void process(MapElement el) {
            ExtendedMapElement element = (ExtendedMapElement) el;

            if (tile.zoomLevel < 17 && element.isBuildingPart && !element.isBuilding)
                return;
            if (!mContoursEnabled && element.isContour)
                return;
            if (element.layer < 5) {
                //TODO Find a better solution to hide subway platforms
                if ("platform".equals(element.tags.getValue("railway")))
                    return;
                //TODO Properly process tunnels (requires changes to VTM)
                if (element.tags.containsKey("tunnel"))
                    element.layer = 5;
            }

            // Convert tree points to polygons
            if (tile.zoomLevel > 15 && element.type == GeometryBuffer.GeometryType.POINT && element.tags.contains(TAG_TREE)) {
                float x = element.getPointX(0);
                float y = element.getPointY(0);
                GeometryBuffer geom = GeometryBuffer.makeCircle(x, y, 1.1f, 10);
                element.ensurePointSize(geom.getNumPoints(), false);
                element.type = GeometryBuffer.GeometryType.POLY;
                System.arraycopy(geom.points, 0, element.points, 0, geom.points.length);
                element.index[0] = geom.points.length;
                if (element.index.length > 1)
                    element.index[1] = -1;
            }

            if (scale != 1) {
                TileClipper clipper = element.isBuildingPart && !element.isBuilding ? mBuildingTileClipper : mTileClipper;
                if (!clipper.clip(element))
                    return;
                element.scale(scale, scale);
                element.translate(-dx, -dy);
                if (element.hasLabelPosition && element.labelPosition != null) {
                    element.labelPosition.x = element.labelPosition.x * scale - dx;
                    element.labelPosition.y = element.labelPosition.y * scale - dy;
                    if (element.labelPosition.x < 0 || element.labelPosition.x > Tile.SIZE
                            || element.labelPosition.y < 0 || element.labelPosition.y > Tile.SIZE)
                        element.labelPosition = null;
                }
            }
            if (element.id != 0L) {
                element.database = MapTrekDataSource.this;
            }
            /*
            if (tile.zoomLevel == 17 && tile.tileX == 79237 && tile.tileY == 40978 && element.isLine() && element.tags.containsKey("highway")) {
                logger.error(element.id + ": " + element.tags.toString());
            }
            if (element.id == ((84126396L << 2) + 2) || element.id == ((84126388L << 2) + 2)) {
                logger.error(tile.toString() + ": " + element.id + ": " + element.layer + " "
                        + element.kind + " " + element.tags.toString());
            }
            */
            mapDataSink.process(element);
        }

        @Override
        public void setTileImage(Bitmap bitmap) {
            // There should be no bitmaps in vector data sources
            // but we will put it here for convenience
            mapDataSink.setTileImage(bitmap);
        }

        @Override
        public void completed(QueryResult result) {
            this.result = result;
        }
    }

}
