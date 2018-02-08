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

import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;

import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.oscim.tiling.source.ITileDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;

abstract class SQLiteTileDatabase implements ITileDataSource {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteTileDatabase.class);

    private final SQLiteTileSource mSQLiteTileSource;
    private final ITileDecoder mTileDecoder;

    SQLiteTileDatabase(SQLiteTileSource tileSource, ITileDecoder tileDecoder) {
        mSQLiteTileSource = tileSource;
        mTileDecoder = tileDecoder;
    }

    /**
     * Returns tile query that expects three parameters (in that particular order):
     * x, y, zoom
     *
     * @param args argument list, for optional modification
     * @return SQL query in
     */
    protected abstract String getTileQuery(String[] args);

    @Override
    public void query(MapTile tile, ITileDataSink sink) {
        if (tile.zoomLevel < mSQLiteTileSource.sourceZoomMin) {
            sink.completed(SUCCESS);
            return;
        }
        String[] args = {String.valueOf(tile.tileX), String.valueOf(tile.tileY), String.valueOf(tile.zoomLevel)};
        boolean ok = false;
        try (Cursor c = mSQLiteTileSource.mDatabase.rawQuery(getTileQuery(args), args)) {
            if (c.moveToFirst()) {
                byte[] bytes = c.getBlob(0);
                ok = mTileDecoder.decode(tile, sink, new ByteArrayInputStream(bytes));
            }
        } catch (Exception e) {
            logger.error(mSQLiteTileSource.getMapInfo().name, e);
        } finally {
            sink.completed(ok ? SUCCESS : FAILED);
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void cancel() {
    }

    protected static int getInt(SQLiteDatabase database, String query, String[] args) {
        int r = 0;
        try (Cursor c = database.rawQuery(query, args)) {
            c.moveToFirst();
            r = c.getInt(0);
            return r;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return r;
    }

    protected static String getString(SQLiteDatabase database, String query, String[] args) {
        try (Cursor c = database.rawQuery(query, args)) {
            c.moveToFirst();
            return c.getString(0);
        } catch (CursorIndexOutOfBoundsException ignore) {
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
