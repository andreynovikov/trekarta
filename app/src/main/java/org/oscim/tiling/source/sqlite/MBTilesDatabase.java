package org.oscim.tiling.source.sqlite;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import org.oscim.core.BoundingBox;
import org.oscim.core.MercatorProjection;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.ITileDecoder;

// https://github.com/mapbox/mbtiles-spec/blob/master/1.2/spec.md
public class MBTilesDatabase extends SQLiteTileDatabase {
    @SuppressWarnings("unused")
    private static final String SQL_CREATE_TILES = "CREATE TABLE IF NOT EXISTS tiles(zoom_level integer, tile_column integer, tile_row integer, tile_data blob, PRIMARY KEY (zoom_level, tile_column, tile_row))";
    @SuppressWarnings("unused")
    private static final String SQL_CREATE_INFO = "CREATE TABLE IF NOT EXISTS metadata(name text, value text)";
    private static final String SQL_SELECT_PARAM = "SELECT value FROM metadata WHERE name = ?";
    private static final String SQL_GET_IMAGE = "SELECT tile_data FROM tiles WHERE tile_column = ? AND tile_row = ? AND zoom_level = ?";
    private static final String SQL_GET_MIN_ZOOM = "SELECT MIN(zoom_level) FROM tiles;";
    private static final String SQL_GET_MAX_ZOOM = "SELECT MAX(zoom_level) FROM tiles;";
    private static final String SQL_GET_MIN_X = "SELECT MIN(tile_column) FROM tiles WHERE zoom_level = ?";
    private static final String SQL_GET_MIN_Y = "SELECT MIN(tile_row) FROM tiles WHERE zoom_level = ?";
    private static final String SQL_GET_MAX_X = "SELECT MAX(tile_column) FROM tiles WHERE zoom_level = ?";
    private static final String SQL_GET_MAX_Y = "SELECT MAX(tile_row) FROM tiles WHERE zoom_level = ?";

    public MBTilesDatabase(SQLiteTileSource tileSource, ITileDecoder tileDecoder) {
        super(tileSource, tileDecoder);
    }

    @Override
    protected String getTileQuery(String[] args) {
        int zoom = Integer.valueOf(args[2]);
        int y = (1 << zoom) - Integer.valueOf(args[1]) - 1;
        args[1] = String.valueOf(y);
        return SQL_GET_IMAGE;
    }

    protected static TileSource.OpenResult initialize(SQLiteTileSource tileSource, SQLiteDatabase database) {
        try {
            int minZoom = (int) database.compileStatement(SQL_GET_MIN_ZOOM).simpleQueryForLong();
            int maxZoom = (int) database.compileStatement(SQL_GET_MAX_ZOOM).simpleQueryForLong();
            tileSource.setMinZoom(minZoom);
            tileSource.setMaxZoom(maxZoom);

            boolean validBounds = false;
            String bounds = getString(database, SQL_SELECT_PARAM, new String[]{"bounds"});
            if (bounds != null) {
                // left, bottom, right, top
                String[] edges = bounds.split(",\\s*");
                try {
                    tileSource.mBoundingBox = new BoundingBox(
                            Double.valueOf(edges[1]),
                            Double.valueOf(edges[0]),
                            Double.valueOf(edges[3]),
                            Double.valueOf(edges[2])
                    );
                    validBounds = true;
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
            if (!validBounds) {
                String[] args = {String.valueOf(maxZoom)};
                int minX = getInt(database, SQL_GET_MIN_X, args);
                int minY = getInt(database, SQL_GET_MIN_Y, args);
                int maxX = getInt(database, SQL_GET_MAX_X, args) + 1;
                int maxY = getInt(database, SQL_GET_MAX_Y, args) + 1;

                double scale = 1 << maxZoom;
                tileSource.mBoundingBox = new BoundingBox(
                        MercatorProjection.toLatitude(maxY / scale),
                        MercatorProjection.toLongitude(minX / scale),
                        MercatorProjection.toLatitude(minY / scale),
                        MercatorProjection.toLongitude(maxX / scale)
                );
            }

            String name = getString(database, SQL_SELECT_PARAM, new String[]{"name"});
            tileSource.setName(name);
        } catch (SQLException e) {
            return new TileSource.OpenResult(e.getMessage());
        }
        return TileSource.OpenResult.SUCCESS;
    }
}
