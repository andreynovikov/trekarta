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

package org.oscim.tiling.source.sqlite;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.oscim.core.BoundingBox;
import org.oscim.core.MercatorProjection;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.ITileDecoder;

import java.io.File;
import java.io.IOException;

// https://github.com/mapbox/mbtiles-spec/blob/master/1.2/spec.md
public class MBTilesDatabase extends SQLiteTileDatabase {
    private static final String SQL_CREATE_TILES = "CREATE TABLE IF NOT EXISTS tiles(zoom_level integer, tile_column integer, tile_row integer, tile_data blob, PRIMARY KEY (zoom_level, tile_column, tile_row))";
    private static final String SQL_CREATE_INFO = "CREATE TABLE IF NOT EXISTS metadata(name text, value text, PRIMARY KEY(name))";
    private static final String SQL_SELECT_PARAM = "SELECT value FROM metadata WHERE name = ?";
    private static final String SQL_GET_IMAGE = "SELECT tile_data FROM tiles WHERE tile_column = ? AND tile_row = ? AND zoom_level = ?";
    private static final String SQL_GET_MIN_ZOOM = "SELECT MIN(zoom_level) FROM tiles;";
    private static final String SQL_GET_MAX_ZOOM = "SELECT MAX(zoom_level) FROM tiles;";
    private static final String SQL_GET_MIN_X = "SELECT MIN(tile_column) FROM tiles WHERE zoom_level = ?";
    private static final String SQL_GET_MIN_Y = "SELECT MIN(tile_row) FROM tiles WHERE zoom_level = ?";
    private static final String SQL_GET_MAX_X = "SELECT MAX(tile_column) FROM tiles WHERE zoom_level = ?";
    private static final String SQL_GET_MAX_Y = "SELECT MAX(tile_row) FROM tiles WHERE zoom_level = ?";

    private boolean tmsSchema;

    public MBTilesDatabase(SQLiteTileSource tileSource, ITileDecoder tileDecoder) {
        super(tileSource, tileDecoder);
        tmsSchema = "tms".equals(tileSource.getOption("schema"));
    }

    @Override
    protected String getTileQuery(String[] args) {
        if (tmsSchema) {
            int zoom = Integer.valueOf(args[2]);
            int y = (1 << zoom) - Integer.valueOf(args[1]) - 1;
            args[1] = String.valueOf(y);
        }
        return SQL_GET_IMAGE;
    }

    static TileSource.OpenResult initialize(SQLiteTileSource tileSource, SQLiteDatabase database) {
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

            String format = getString(database, SQL_SELECT_PARAM, new String[]{"format"});
            if (format != null)
                tileSource.setOption("format", format);

            String schema = getString(database, SQL_SELECT_PARAM, new String[]{"tile_row_type"});
            boolean tmsSchema = schema == null || !("xyz".equals(schema) || "osm".equals(schema));
            tileSource.setOption("schema", tmsSchema ? "tms" : "xyz");
        } catch (SQLException e) {
            return new TileSource.OpenResult(e.toString());
        }
        return TileSource.OpenResult.SUCCESS;
    }

    public static class MBTilesDatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        public MBTilesDatabaseHelper(Context context, File file) {
            super(context, file.getAbsolutePath(), null, DATABASE_VERSION);
            if (!file.exists())
                try {
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_INFO);
            db.execSQL(SQL_CREATE_TILES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}
