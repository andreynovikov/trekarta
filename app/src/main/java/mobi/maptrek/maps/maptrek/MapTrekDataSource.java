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

package mobi.maptrek.maps.maptrek;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomDataSink;
import org.oscim.tiling.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

import mobi.maptrek.layers.MapTrekTileLayer;

import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_ENUM1;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_FLAGS;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_ID;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_KIND;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_LAT;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_LON;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_TYPE;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_X;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_Y;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_TILES_DATA;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_FEATURES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_TILES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.WHERE_TILE_ZXY;
import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;
import static org.oscim.tiling.QueryResult.TILE_NOT_FOUND;


class MapTrekDataSource implements ITileDataSource {
    private static final Logger logger = LoggerFactory.getLogger(MapTrekDataSource.class);

    private static final String SQL_GET_TILE = "SELECT " + COLUMN_TILES_DATA + " FROM " + TABLE_TILES + " WHERE " + WHERE_TILE_ZXY;

    private static final int MAX_NATIVE_ZOOM = 14;

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
        int zoomDiff = z - MAX_NATIVE_ZOOM;
        int limitedZoomDiff = Math.min(zoomDiff, 17 - MAX_NATIVE_ZOOM);
        if (z > MAX_NATIVE_ZOOM) {
            x = x >> zoomDiff;
            y = y >> zoomDiff;
            z = MAX_NATIVE_ZOOM;
        }

        String[] tileArgs = {String.valueOf(z), String.valueOf(x), String.valueOf(y)};
        QueryResult result = tile.zoomLevel > 7 ? TILE_NOT_FOUND : SUCCESS;
        try (Cursor c = mDatabase.rawQuery(SQL_GET_TILE, tileArgs)) {
            if (c.moveToFirst()) {
                byte[] bytes = c.getBlob(0);
                ITileDataSink dataSink = new NativeTileDataSink(sink, tile);

                if (zoomDiff > 0) {
                    MapTile mapTile = new MapTile(tile.node, x, y, MAX_NATIVE_ZOOM);
                    dataSink = new OverzoomDataSink(dataSink, mapTile, tile);
                }

                boolean ok = mTileDecoder.decode(tile, dataSink, new ByteArrayInputStream(bytes));
                result = ok ? SUCCESS : FAILED;
            }
        } catch (Exception e) {
            logger.error("Query error", e);
            result = FAILED;
        }

        if (Tags.highlightedTypes.size() >= 0 && z > 7) {
            int dz = MAX_NATIVE_ZOOM - z;
            int min_x = x << dz;
            int min_y = y << dz;
            int max_x = ((x + 1) << dz) - 1;
            int max_y = ((y + 1) << dz) - 1;

            String sql = "SELECT DISTINCT " + COLUMN_FEATURES_ID + ", " + COLUMN_FEATURES_KIND
                    + ", " + COLUMN_FEATURES_TYPE + ", " + COLUMN_FEATURES_LAT + ", "
                    + COLUMN_FEATURES_LON + " FROM " + TABLE_FEATURES + " WHERE "
                    + COLUMN_FEATURES_TYPE + " = ? AND "
                    + COLUMN_FEATURES_X + " >= ? AND " + COLUMN_FEATURES_X + " <= ? AND "
                    + COLUMN_FEATURES_Y + " >= ? AND " + COLUMN_FEATURES_Y + " <= ?";
            for(int i = 0; i < Tags.highlightedTypes.size(); ++i) {
                String[] featureArgs = new String[]{String.valueOf(Tags.highlightedTypes.get(i)),
                        String.valueOf(min_x), String.valueOf(max_x),
                        String.valueOf(min_y), String.valueOf(max_y)};
                addFeaturesToTile(tile, sink, sql, featureArgs);
            }
        } else if (limitedZoomDiff >= 0 && Tags.typeSelectors[limitedZoomDiff].length() > 0) {
            String sql = "SELECT DISTINCT " + COLUMN_FEATURES_ID + ", " + COLUMN_FEATURES_KIND
                    + ", " + COLUMN_FEATURES_TYPE + ", " + COLUMN_FEATURES_LAT + ", "
                    + COLUMN_FEATURES_LON + ", " + COLUMN_FEATURES_FLAGS  + ", "
                    + COLUMN_FEATURES_ENUM1 + " FROM " + TABLE_FEATURES + " WHERE "
                    + COLUMN_FEATURES_TYPE + " IN (" + Tags.typeSelectors[limitedZoomDiff] + ") AND "
                    + COLUMN_FEATURES_X + " = ? AND " + COLUMN_FEATURES_Y + " = ?";

            String[] featureArgs;
            if (limitedZoomDiff == 0) {
                featureArgs = new String[]{String.valueOf(x), String.valueOf(y)};
            } else {
                sql += " AND " + COLUMN_FEATURES_LAT + " >= ? AND " + COLUMN_FEATURES_LON
                        + " >= ? AND " + COLUMN_FEATURES_LAT + " <= ? AND " + COLUMN_FEATURES_LON
                        + " <= ?";
                BoundingBox bb = tile.getBoundingBox();
                featureArgs = new String[]{String.valueOf(x), String.valueOf(y),
                        String.valueOf(bb.getMinLatitude()), String.valueOf(bb.getMinLongitude()),
                        String.valueOf(bb.getMaxLatitude()), String.valueOf(bb.getMaxLongitude())};
            }
            addFeaturesToTile(tile, sink, sql, featureArgs);
        }

        sink.completed(result);
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

    private void addFeaturesToTile(MapTile tile, ITileDataSink sink, String sql, String[] args) {
        MapTrekTileLayer.AmenityTileData td = get(tile);

        try (Cursor c = mDatabase.rawQuery(sql, args)) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                ExtendedMapElement element = new ExtendedMapElement(1, 1);
                element.id = c.getLong(0);
                element.kind = c.getInt(1);
                if (!c.isNull(5))
                    Tags.setFlags(c.getInt(5), element);
                int type = c.getInt(2);
                Tags.setTypeTag(type, element.tags);
                if (!c.isNull(6))
                    Tags.setExtra(type, c.getInt(6), element.tags);
                element.database = this;

                double px = MercatorProjection.longitudeToX(c.getDouble(4));
                double py = MercatorProjection.latitudeToY(c.getDouble(3));

                td.amenities.add(new Pair<>(new Point(px, py), element.id));

                px = (px - tile.x) * tile.mapSize;
                py = (py - tile.y) * tile.mapSize;

                element.startPoints();
                element.addPoint((float) px, (float) py);

                sink.process(element);
                c.moveToNext();
            }
        } catch (Exception e) {
            logger.error("Query error", e);
        }
    }

    private class NativeTileDataSink implements ITileDataSink {
        private final Tile tile;
        ITileDataSink mapDataSink;
        QueryResult result;
        float scale;

        NativeTileDataSink(ITileDataSink mapDataSink, Tile tile) {
            this.mapDataSink = mapDataSink;
            this.tile = tile;
            this.scale = 1 << (tile.zoomLevel - MAX_NATIVE_ZOOM);
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
            }
            if (element.isLine() && element.tags.contains("power", "line"))
                element.layer += 5; // place power lines above everything

            // Convert tree points to polygons
            if (tile.zoomLevel > 14 && element.type == GeometryBuffer.GeometryType.POINT && element.tags.contains(TAG_TREE)) {
                float x = element.getPointX(0);
                float y = element.getPointY(0);
                GeometryBuffer geom = GeometryBuffer.makeCircle(x, y, 0.8f * scale, 10);
                element.ensurePointSize(geom.getNumPoints(), false);
                element.type = GeometryBuffer.GeometryType.POLY;
                System.arraycopy(geom.points, 0, element.points, 0, geom.points.length);
                element.index[0] = geom.points.length;
                if (element.index.length > 1)
                    element.index[1] = -1;
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

    private MapTrekTileLayer.AmenityTileData get(MapTile tile) {
        MapTrekTileLayer.AmenityTileData td = (MapTrekTileLayer.AmenityTileData) tile.getData(MapTrekTileLayer.POI_DATA);
        if (td == null) {
            td = new MapTrekTileLayer.AmenityTileData();
            tile.addData(MapTrekTileLayer.POI_DATA, td);
        }
        return td;
    }
}
