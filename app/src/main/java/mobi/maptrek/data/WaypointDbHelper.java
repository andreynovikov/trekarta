package mobi.maptrek.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;

public class WaypointDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "waypoint";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LATE6 = "latitude";
    public static final String COLUMN_LONE6 = "longitude";
    public static final String COLUMN_ALTITUDE = "altitude";
    public static final String COLUMN_PROXIMITY = "proximity";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_COLOR = "color";
    public static final String COLUMN_ICON = "icon";

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
                    + COLUMN_ICON + " TEXT"
                    + ");";

    public WaypointDbHelper(Context context, File file) {
        super(context, file.getAbsolutePath(), null, DATABASE_VERSION);
        if (!file.exists())
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_WAYPOINT_SCHEMA);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}