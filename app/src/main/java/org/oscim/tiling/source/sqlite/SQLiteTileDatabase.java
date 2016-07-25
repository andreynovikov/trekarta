package org.oscim.tiling.source.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.ITileDecoder;

import java.io.ByteArrayInputStream;

import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;

public abstract class SQLiteTileDatabase implements ITileDataSource {
    private final SQLiteTileSource mSQLiteTileSource;
    protected final ITileDecoder mTileDecoder;

    public SQLiteTileDatabase(SQLiteTileSource tileSource, ITileDecoder tileDecoder) {
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
        String[] args = {String.valueOf(tile.tileX), String.valueOf(tile.tileY), String.valueOf(tile.zoomLevel)};
        boolean ok = false;
        try (Cursor c = mSQLiteTileSource.mDatabase.rawQuery(getTileQuery(args), args)) {
            if (c.moveToFirst()) {
                byte[] bytes = c.getBlob(0);
                ok = mTileDecoder.decode(tile, sink, new ByteArrayInputStream(bytes));
            }
        } catch (Exception e) {
            Log.e("SQLiteTileDatabase", mSQLiteTileSource.getMapInfo().name, e);
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
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
