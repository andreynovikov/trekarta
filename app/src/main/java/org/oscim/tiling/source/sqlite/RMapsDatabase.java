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

public class RMapsDatabase extends SQLiteTileDatabase {
    private static final String SQL_CREATE_TILES = "CREATE TABLE IF NOT EXISTS tiles (x INTEGER, y INTEGER, z INTEGER, s INTEGER, image BLOB, PRIMARY KEY (x,y,z,s));";
    private static final String SQL_CREATE_INFO = "CREATE TABLE IF NOT EXISTS info (maxzoom INTEGER, minzoom INTEGER, params VARCHAR);";
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

    static TileSource.OpenResult initialize(SQLiteTileSource tileSource, SQLiteDatabase database) {
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

    public static class RMapsDatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        public RMapsDatabaseHelper(Context context, File file) {
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
