package mobi.maptrek.maps.maptrek;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;

public class MapTrekDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;

    static final String TABLE_MAPS = "maps";
    static final String TABLE_INFO = "metadata";
    static final String TABLE_TILES = "tiles";
    static final String TABLE_NAMES = "names";
    static final String TABLE_FEATURES = "features";
    static final String TABLE_FEATURE_NAMES = "feature_names";

    static final String COLUMN_MAPS_X = "x";
    static final String COLUMN_MAPS_Y = "y";
    static final String COLUMN_MAPS_DATE = "date";
    static final String COLUMN_MAPS_DOWNLOADING = "downloading";

    @SuppressWarnings("WeakerAccess")
    static final String COLUMN_INFO_NAME = "name";
    static final String COLUMN_INFO_VALUE = "value";

    static final String COLUMN_TILES_ZOOM_LEVEL = "zoom_level";
    static final String COLUMN_TILES_COLUMN = "tile_column";
    static final String COLUMN_TILES_ROW = "tile_row";
    static final String COLUMN_TILES_DATA = "tile_data";

    static final String COLUMN_NAMES_REF = "ref";
    static final String COLUMN_NAMES_NAME = "name";

    static final String COLUMN_FEATURES_ID = "id";
    static final String COLUMN_FEATURES_KIND = "kind";
    static final String COLUMN_FEATURES_LAT = "lat";
    static final String COLUMN_FEATURES_LON = "lon";

    static final String COLUMN_FEATURES_NAMES_LANG = "lang";
    static final String COLUMN_FEATURES_NAMES_NAME = "name";

    private static final String SQL_CREATE_MAPS =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_MAPS + " ("
                    + COLUMN_MAPS_X + " INTEGER NOT NULL, "
                    + COLUMN_MAPS_Y + " INTEGER NOT NULL, "
                    + COLUMN_MAPS_DATE + " INTEGER NOT NULL DEFAULT 0, "
                    + COLUMN_MAPS_DOWNLOADING + " INTEGER NOT NULL DEFAULT 0"
                    + ");";

    private static final String SQL_CREATE_INFO =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_INFO + " ("
                    + COLUMN_INFO_NAME + " TEXT NOT NULL, "
                    + COLUMN_INFO_VALUE + " TEXT"
                    + ");";

    private static final String SQL_CREATE_TILES =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_TILES + " ("
                    + COLUMN_TILES_ZOOM_LEVEL + " INTEGER NOT NULL, "
                    + COLUMN_TILES_COLUMN + " INTEGER NOT NULL, "
                    + COLUMN_TILES_ROW + " INTEGER NOT NULL, "
                    + COLUMN_TILES_DATA + " BLOB NOT NULL"
                    + ");";

    private static final String SQL_CREATE_NAMES =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_NAMES + " ("
                    + COLUMN_NAMES_REF + " INTEGER NOT NULL, "
                    + COLUMN_NAMES_NAME + " TEXT NOT NULL"
                    + ");";

    private static final String SQL_CREATE_FEATURES =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_FEATURES + " ("
                    + COLUMN_FEATURES_ID + " INTEGER NOT NULL, "
                    + COLUMN_FEATURES_KIND + " INTEGER, "
                    + COLUMN_FEATURES_LAT + " REAL, "
                    + COLUMN_FEATURES_LON + " REAL"
                    + ")";

    private static final String SQL_CREATE_FEATURE_NAMES =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_FEATURE_NAMES + " ("
                    + COLUMN_FEATURES_ID + "  INTEGER NOT NULL, "
                    + COLUMN_FEATURES_NAMES_LANG + " INTEGER NOT NULL, "
                    + COLUMN_FEATURES_NAMES_NAME + " INTEGER NOT NULL"
                    + ");";

    static final String[] ALL_COLUMNS_MAPS = {
            COLUMN_MAPS_X,
            COLUMN_MAPS_Y,
            COLUMN_MAPS_DATE,
            COLUMN_MAPS_DOWNLOADING
    };

    static final String[] ALL_COLUMNS_TILES = {
            COLUMN_TILES_ZOOM_LEVEL,
            COLUMN_TILES_COLUMN,
            COLUMN_TILES_ROW,
            COLUMN_TILES_DATA
    };

    static final String[] ALL_COLUMNS_NAMES = {
            COLUMN_NAMES_REF,
            COLUMN_NAMES_NAME
    };

    static final String[] ALL_COLUMNS_FEATURES = {
            COLUMN_FEATURES_ID,
            COLUMN_FEATURES_KIND,
            COLUMN_FEATURES_LAT,
            COLUMN_FEATURES_LON
    };

    static final String[] ALL_COLUMNS_FEATURE_NAMES = {
            COLUMN_FEATURES_ID,
            COLUMN_FEATURES_NAMES_LANG,
            COLUMN_FEATURES_NAMES_NAME
    };

    static final String WHERE_MAPS_XY = COLUMN_MAPS_X + " = ? AND " + COLUMN_MAPS_Y + " = ?";
    static final String WHERE_INFO_NAME = COLUMN_INFO_NAME + " = ?";
    static final String WHERE_MAPS_PRESENT = COLUMN_MAPS_DATE + " > 0 OR " + COLUMN_MAPS_DOWNLOADING + " > 0";
    static final String WHERE_FEATURES_ID = COLUMN_FEATURES_ID + " = ?";
    static final String WHERE_FEATURE_NAME = COLUMN_FEATURES_ID + " = ? AND " + COLUMN_FEATURES_NAMES_LANG + " = ?";
    static final String WHERE_TILE_ZXY = COLUMN_TILES_ZOOM_LEVEL + " = ? AND " + COLUMN_TILES_COLUMN + " = ? AND " + COLUMN_TILES_ROW + " = ?";


    private static final String SQL_INDEX_INFO = "CREATE UNIQUE INDEX IF NOT EXISTS property ON metadata (name)";
    private static final String SQL_INDEX_MAPS = "CREATE UNIQUE INDEX IF NOT EXISTS maps_x_y ON maps (x, y)";
    private static final String SQL_INDEX_TILES = "CREATE UNIQUE INDEX IF NOT EXISTS coord ON tiles (zoom_level, tile_column, tile_row)";
    private static final String SQL_INDEX_NAMES = "CREATE UNIQUE INDEX IF NOT EXISTS name_ref ON names (ref)";
    private static final String SQL_INDEX_FEATURES = "CREATE UNIQUE INDEX IF NOT EXISTS feature_id ON features (id)";
    private static final String SQL_INDEX_FEATURE_LANG = "CREATE UNIQUE INDEX IF NOT EXISTS feature_name_lang ON feature_names (id, lang)";
    private static final String SQL_INDEX_FEATURE_NAMES = "CREATE UNIQUE INDEX IF NOT EXISTS feature_name_ref ON feature_names (id, lang, name)";

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
        db.execSQL(SQL_CREATE_MAPS);
        db.execSQL(SQL_CREATE_INFO);
        db.execSQL(SQL_CREATE_TILES);
        db.execSQL(SQL_CREATE_NAMES);
        db.execSQL(SQL_CREATE_FEATURES);
        db.execSQL(SQL_CREATE_FEATURE_NAMES);
        db.execSQL(SQL_INDEX_MAPS);
        db.execSQL(SQL_INDEX_TILES);
        db.execSQL(SQL_INDEX_INFO);
        db.execSQL(SQL_INDEX_NAMES);
        db.execSQL(SQL_INDEX_FEATURES);
        db.execSQL(SQL_INDEX_FEATURE_LANG);
        db.execSQL(SQL_INDEX_FEATURE_NAMES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //FIXME Implement update
    }
}
