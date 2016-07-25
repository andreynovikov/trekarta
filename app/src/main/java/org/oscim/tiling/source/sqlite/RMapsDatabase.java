package org.oscim.tiling.source.sqlite;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import org.oscim.core.BoundingBox;
import org.oscim.core.MercatorProjection;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.ITileDecoder;

public class RMapsDatabase extends SQLiteTileDatabase {
    @SuppressWarnings("unused")
    private static final String SQL_CREATE_TILES = "CREATE TABLE IF NOT EXISTS tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x,y,z,s));";
    @SuppressWarnings("unused")
    private static final String SQL_CREATE_INFO = "CREATE TABLE IF NOT EXISTS info (maxzoom Int, minzoom Int, params VARCHAR);";
    @SuppressWarnings("unused")
    private static final String SQL_SELECT_PARAMS = "SELECT * FROM info";
    private static final String SQL_GET_IMAGE = "SELECT image FROM tiles WHERE x = ? AND y = ? AND z = (17 - ?)";
    private static final String SQL_GET_MIN_ZOOM = "SELECT DISTINCT 17 - z FROM tiles ORDER BY z DESC LIMIT 1;";
    private static final String SQL_GET_MAX_ZOOM = "SELECT DISTINCT 17 - z FROM tiles ORDER BY z ASC LIMIT 1;";
    private static final String SQL_GET_MIN_X = "SELECT MIN(x) FROM tiles WHERE z = ?";
    private static final String SQL_GET_MIN_Y = "SELECT MIN(y) FROM tiles WHERE z = ?";
    private static final String SQL_GET_MAX_X = "SELECT MAX(x) FROM tiles WHERE z = ?";
    private static final String SQL_GET_MAX_Y = "SELECT MAX(y) FROM tiles WHERE z = ?";

    public RMapsDatabase(SQLiteTileSource tileSource, ITileDecoder tileDecoder) {
        super(tileSource, tileDecoder);
    }

    @Override
    protected String getTileQuery(String[] args) {
        return SQL_GET_IMAGE;
    }

    protected static TileSource.OpenResult initialize(SQLiteTileSource tileSource, SQLiteDatabase database) {
        try {
            int minZoom = (int) database.compileStatement(SQL_GET_MIN_ZOOM).simpleQueryForLong();
            int maxZoom = (int) database.compileStatement(SQL_GET_MAX_ZOOM).simpleQueryForLong();
            tileSource.setMinZoom(minZoom);
            tileSource.setMaxZoom(maxZoom);

            String[] args = {String.valueOf(17 - maxZoom)};
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

            //TODO Try to fill zoom table and see what happens
        } catch (SQLException e) {
            return new TileSource.OpenResult(e.getMessage());
        }
        return TileSource.OpenResult.SUCCESS;
    }
}
