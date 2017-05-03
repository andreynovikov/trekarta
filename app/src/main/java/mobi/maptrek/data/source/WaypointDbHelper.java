package mobi.maptrek.data.source;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;

class WaypointDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;

    static final String TABLE_NAME = "waypoint";
    static final String COLUMN_ID = "_id";
    static final String COLUMN_NAME = "name";
    static final String COLUMN_LATE6 = "latitude";
    static final String COLUMN_LONE6 = "longitude";
    static final String COLUMN_ALTITUDE = "altitude";
    static final String COLUMN_PROXIMITY = "proximity";
    static final String COLUMN_DESCRIPTION = "description";
    static final String COLUMN_DATE = "date";
    static final String COLUMN_COLOR = "color";
    static final String COLUMN_ICON = "icon";
    static final String COLUMN_LOCKED = "locked";

    private static final String SQL_CREATE_WAYPOINT_SCHEMA =
            "CREATE TABLE "
                    + TABLE_NAME + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY,"
                    + COLUMN_NAME + " TEXT NOT NULL,"
                    + COLUMN_LATE6 + " INTEGER NOT NULL,"
                    + COLUMN_LONE6 + " INTEGER NOT NULL,"
                    + COLUMN_ALTITUDE + " INTEGER,"
                    + COLUMN_PROXIMITY + " INTEGER,"
                    + COLUMN_DESCRIPTION + " TEXT,"
                    + COLUMN_DATE + " LONG,"
                    + COLUMN_COLOR + " INTEGER,"
                    + COLUMN_ICON + " TEXT,"
                    + COLUMN_LOCKED + " INTEGER"
                    + ");";

    private static final String ALTER_WAYPOINTS_1 =
            "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_LOCKED + " INTEGER;";

    WaypointDbHelper(Context context, File file) {
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
        db.execSQL(SQL_CREATE_WAYPOINT_SCHEMA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(ALTER_WAYPOINTS_1);
        }
    }
}