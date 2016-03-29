package org.oscim.android.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.oscim.core.Tile;
import org.oscim.tiling.ITileCache;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class PreCachedTileCache implements ITileCache {
    private TileCache mTileCache;
    private final SQLiteDatabase mDatabase;

    public PreCachedTileCache(Context context, SQLiteDatabase database, String cacheDirectory, String dbName) {
        mDatabase = database;
        mTileCache = new TileCache(context, cacheDirectory, dbName);
    }

    @Override
    public TileWriter writeTile(Tile tile) {
        return mTileCache.writeTile(tile);
    }

    @Override
    public TileReader getTile(Tile tile) {
        String[] mQueryValues = new String[3];
        mQueryValues[0] = String.valueOf(tile.zoomLevel);
        mQueryValues[1] = String.valueOf(tile.tileX);
        mQueryValues[2] = String.valueOf(tile.tileY);

        Cursor cursor = mDatabase.rawQuery("SELECT " + TileCache.COLUMN_DATA +
                " FROM " + TileCache.TABLE_NAME +
                " WHERE z=? AND x=? AND y=?", mQueryValues);

        if (!cursor.moveToFirst()) {
            cursor.close();
            return mTileCache.getTile(tile);
        }

        InputStream in = new ByteArrayInputStream(cursor.getBlob(0));
        cursor.close();

        return new CacheTileReader(tile, in);
    }

    @Override
    public void setCacheSize(long size) {
        mTileCache.setCacheSize(size);
    }

    public void dispose() {
        if (mDatabase.isOpen())
            mDatabase.close();
        mTileCache.dispose();
    }
}
