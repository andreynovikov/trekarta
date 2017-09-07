package mobi.maptrek.maps.maptrek;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
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
    private static final String SQL_GET_NAME = "SELECT names.name, lang FROM names INNER JOIN feature_names ON (ref = feature_names.name) WHERE id = ? AND lang IN (0, ?) ORDER BY lang";

    private static final int MAX_NATIVE_ZOOM = 14;
    private static final int CLIP_BUFFER = 32;

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

    String[] getNames(int lang, long elementId) {
        String[] args = {String.valueOf(elementId), String.valueOf(lang)};
        try (Cursor c = mDatabase.rawQuery(SQL_GET_NAME, args)) {
            String result[] = new String[c.getCount()];
            int i = 0;
            if (c.moveToFirst())
                do {
                    result[i] = c.getString(0);
                    i++;
                } while (c.moveToNext());
            return result;
        } catch (Exception e) {
            logger.error("Query error", e);
        }
        return null;
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
                mBuildingTileClipper = new TileClipper(dx / scale, dy / scale,
                        (dx + Tile.SIZE) / scale, (dy + Tile.SIZE) / scale);
            }
        }

        @Override
        public void process(MapElement el) {
            ExtendedMapElement element = (ExtendedMapElement) el;
            //TODO replace with building_part flag
            if (tile.zoomLevel < 17 && element.tags.containsKey("building:part") && !element.tags.containsKey("building"))
                return;
            if (!mContoursEnabled && element.tags.containsKey("contour"))
                return;
            if (scale != 1) {
                TileClipper clipper = element.isBuilding() ? mBuildingTileClipper : mTileClipper;
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
            if (tile.zoomLevel < 8) {
                logger.error(element.tags.toString());
            }
            */
            /*
            if (tile.zoomLevel == 17 && tile.tileX == 79237 && tile.tileY == 40978 && element.isLine() && element.tags.containsKey("highway")) {
                logger.error(element.id + ": " + element.tags.toString());
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
