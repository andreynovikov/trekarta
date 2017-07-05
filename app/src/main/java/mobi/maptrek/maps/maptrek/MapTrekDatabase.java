package mobi.maptrek.maps.maptrek;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
        int s = 1, dx = 0, dy = 0;
        if (z > MAX_NATIVE_ZOOM) {
            int dz = z - MAX_NATIVE_ZOOM;
            x = x >> dz;
            y = y >> dz;
            z = MAX_NATIVE_ZOOM;
            s = 1 << dz;
            dx = (tile.tileX - (x << dz)) * Tile.SIZE;
            dy = (tile.tileY - (y << dz)) * Tile.SIZE;
        }
        y = (1 << z) - y - 1;
        String[] args = {String.valueOf(z), String.valueOf(x), String.valueOf(y)};
        QueryResult result = tile.zoomLevel > 7 ? TILE_NOT_FOUND : SUCCESS;
        try (Cursor c = mDatabase.rawQuery(SQL_GET_TILE, args)) {
            if (c.moveToFirst()) {
                byte[] bytes = c.getBlob(0);
                NativeTileDataSink proxyDataSink = new NativeTileDataSink(sink, tile.zoomLevel, s, dx, dy);
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
        private final int zoom;
        private final int scale;
        private final int dx;
        private final int dy;
        private final boolean rescale;
        private TileClipper mTileClipper;
        ITileDataSink mapDataSink;
        QueryResult result;

        NativeTileDataSink(ITileDataSink mapDataSink, int zoom, int scale, int dx, int dy) {
            this.mapDataSink = mapDataSink;
            this.zoom = zoom;
            this.scale = scale;
            this.dx = dx;
            this.dy = dy;
            rescale = scale != 1;
            if (rescale) {
                dx -= CLIP_BUFFER;
                dy -= CLIP_BUFFER;
                int tileSize = Tile.SIZE + 2 * CLIP_BUFFER;
                mTileClipper = new TileClipper(1f * dx / scale, 1f * dy / scale, 1f * (dx + tileSize) / scale, 1f * (dy + tileSize) / scale);
            }
        }

        @Override
        public void process(MapElement element) {
            //if (zoom == 9)
            //    Log.e("MTD", zoom + "/" + dx + "/" + dy + " " + element.toString());
            //if (element.tags.containsKey("waterway")) {
            //    logger.error(element.toString());
            //}
            if (zoom < 17 && element.tags.containsKey("building:part") && !element.tags.containsKey("building"))
                return;
            if (rescale) {
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
            String id = element.tags.getValue("id");
            if (id != null) {
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
