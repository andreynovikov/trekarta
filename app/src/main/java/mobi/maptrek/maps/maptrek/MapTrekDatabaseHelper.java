package mobi.maptrek.maps.maptrek;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;

public class MapTrekDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;

    public MapTrekDatabaseHelper(Context context, File file) {
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
        db.execSQL(MapTrekDatabase.SQL_CREATE_INFO);
        db.execSQL(MapTrekDatabase.SQL_CREATE_TILES);
        db.execSQL(MapTrekDatabase.SQL_CREATE_NAMES);
        db.execSQL(MapTrekDatabase.SQL_CREATE_FEATURES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //FIXME Implement update
    }
}
