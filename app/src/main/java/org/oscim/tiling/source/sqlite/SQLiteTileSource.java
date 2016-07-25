package org.oscim.tiling.source.sqlite;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.BoundingBox;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.ITileDecoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class SQLiteTileSource extends TileSource {
    public static final byte[] MAGIC = "SQLite format".getBytes();

    SQLiteDatabase mDatabase;
    Class<? extends SQLiteTileDatabase> mTileDatabase;
    BoundingBox mBoundingBox;

    public boolean setMapFile(String filename) {
        setOption("path", filename);

        File file = new File(filename);

        if (!file.exists()) {
            return false;
        } else if (!file.isFile()) {
            return false;
        } else if (!file.canRead()) {
            return false;
        }

        return true;
    }

    @Override
    public ITileDataSource getDataSource() {
        try {
            Constructor con = mTileDatabase.getConstructor(SQLiteTileSource.class, ITileDecoder.class);
            return (ITileDataSource) con.newInstance(this, new BitmapTileDecoder());
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public OpenResult open() {
        if (!options.containsKey("path"))
            return new OpenResult("no map path set");

        File file = new File(options.get("path"));

        // check if the path exists and is readable
        if (!file.exists()) {
            return new OpenResult("path does not exist: " + file);
        } else if (!file.isFile()) {
            return new OpenResult("not a path: " + file);
        } else if (!file.canRead()) {
            return new OpenResult("cannot read path: " + file);
        }

        mDatabase = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        OpenResult openResult = RMapsDatabase.initialize(this, mDatabase);

        if (openResult.isSuccess()) {
            mTileDatabase = RMapsDatabase.class;
        } else {
            openResult = MBTilesDatabase.initialize(this, mDatabase);
            if (openResult.isSuccess()) {
                mTileDatabase = MBTilesDatabase.class;
            } else {
                close();
                return openResult;
            }
        }

        if (getOption("name") == null) {
            // Construct name
            // 1. remove extension
            String name = file.getName().toLowerCase();
            int e = name.lastIndexOf(".sqlitedb");
            if (e > 0)
                name = name.substring(0, e);
            e = name.lastIndexOf(".mbtiles");
            if (e > 0)
                name = name.substring(0, e);
            // 2. capitalizeFirst first letter
            StringBuilder nameSb = new StringBuilder(name);
            nameSb.setCharAt(0, Character.toUpperCase(nameSb.charAt(0)));
            nameSb.append(" (");
            nameSb.append(String.valueOf(getZoomLevelMin()));
            nameSb.append("-");
            nameSb.append(String.valueOf(getZoomLevelMax()));
            nameSb.append(")");
            setOption("name", nameSb.toString());
        }

        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {
        mDatabase.close();
    }

    public void setMinZoom(int minZoom) {
        mZoomMin = minZoom;
    }

    public void setMaxZoom(int maxZoom) {
        mZoomMax = maxZoom;
    }

    public void setName(String name) {
        setOption("name", name);
    }

    public SQLiteMapInfo getMapInfo() {
        return new SQLiteMapInfo(options.get("name"), mBoundingBox);
    }

    public class BitmapTileDecoder implements ITileDecoder {
        @Override
        public boolean decode(Tile tile, ITileDataSink sink, InputStream is) throws IOException {

            Bitmap bitmap = CanvasAdapter.decodeBitmap(is);
            if (!bitmap.isValid()) {
                Log.d("BitmapTileSource", tile + " invalid bitmap");
                return false;
            }
            sink.setTileImage(bitmap);

            return true;
        }
    }

}
