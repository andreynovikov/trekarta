package mobi.maptrek.maps.maptrek;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.oscim.backend.canvas.Bitmap;
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

import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;
import static org.oscim.tiling.QueryResult.TILE_NOT_FOUND;


class MapTrekDatabase implements ITileDataSource {
    private static final Logger logger = LoggerFactory.getLogger(MapTrekDatabase.class);

    static final String SQL_CREATE_INFO = "CREATE TABLE IF NOT EXISTS metadata (name TEXT, value TEXT, PRIMARY KEY (name))";
    static final String SQL_CREATE_TILES = "CREATE TABLE IF NOT EXISTS tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB, PRIMARY KEY (zoom_level, tile_column, tile_row))";
    static final String SQL_CREATE_NAMES = "CREATE TABLE IF NOT EXISTS names (ref INTEGER, name TEXT, PRIMARY KEY (ref))";
    static final String SQL_CREATE_FEATURES = "CREATE TABLE IF NOT EXISTS features (id INTEGER, name INTEGER, kind INTEGER, lat REAL, lon REAL, PRIMARY KEY (id))";
    @SuppressWarnings("unused")
    private static final String SQL_GET_PARAM = "SELECT value FROM metadata WHERE name = ?";
    private static final String SQL_GET_TILE = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";
    private static final String SQL_GET_NAME = "SELECT name FROM names WHERE ref = (SELECT name FROM features WHERE id = ?)";

    private static final int MAX_NATIVE_ZOOM = 14;
    private static final int CLIP_BUFFER = 32;

    private final MapTrekTileDecoder mTileDecoder;
    private final SQLiteDatabase mDatabase;

    MapTrekDatabase(SQLiteDatabase database) {
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
        int yi = (1 << z) - y - 1;
        String[] args = {String.valueOf(z), String.valueOf(x), String.valueOf(yi)};
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

    private class NativeTileDataSink implements ITileDataSink {
        private final Tile tile;
        private final int scale;
        private int dx;
        private int dy;
        private TileClipper mTileClipper;
        ITileDataSink mapDataSink;
        QueryResult result;

        NativeTileDataSink(ITileDataSink mapDataSink, Tile tile, int dz, int x, int y) {
            this.mapDataSink = mapDataSink;
            this.tile = tile;
            scale = 1 << dz;
            if (scale != 1) {
                dx = (tile.tileX - (x << dz)) * Tile.SIZE;
                dy = (tile.tileY - (y << dz)) * Tile.SIZE;
                mTileClipper = new TileClipper(1f * dx / scale - CLIP_BUFFER, 1f * dy / scale - CLIP_BUFFER,
                        1f * (dx + Tile.SIZE) / scale + CLIP_BUFFER, 1f * (dy + Tile.SIZE) / scale +  CLIP_BUFFER);
            }
        }

        @Override
        public void process(MapElement el) {
            ExtendedMapElement element = (ExtendedMapElement) el;
            //TODO replace with building_part flag
            if (tile.zoomLevel < 17 && element.tags.containsKey("building:part") && !element.tags.containsKey("building"))
                return;
            if (scale != 1) {
                if (!mTileClipper.clip(element))
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
                String id = String.valueOf(element.id);
                String[] args = {id};
                try (Cursor c = mDatabase.rawQuery(SQL_GET_NAME, args)) {
                    if (c.moveToFirst()) {
                        String name = c.getString(0);
                        element.tags.add(new Tag(Tag.KEY_NAME, name, false));
                    }
                } catch (Exception e) {
                    logger.error("Query error", e);
                }
            }
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
