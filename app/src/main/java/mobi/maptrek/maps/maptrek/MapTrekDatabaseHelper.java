package mobi.maptrek.maps.maptrek;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MapTrekDatabaseHelper extends SQLiteOpenHelper {
    private static final Logger logger = LoggerFactory.getLogger(MapTrekDatabaseHelper.class);

    private static final int DATABASE_VERSION = 3;

    static final String TABLE_MAPS = "maps";
    static final String TABLE_MAP_FEATURES = "map_features";
    static final String TABLE_INFO = "metadata";
    static final String TABLE_TILES = "tiles";
    static final String TABLE_NAMES = "names";
    @SuppressWarnings("WeakerAccess")
    static final String TABLE_NAMES_FTS = "names_fts";
    static final String TABLE_FEATURES = "features";
    static final String TABLE_FEATURE_NAMES = "feature_names";

    static final String COLUMN_MAPS_X = "x";
    static final String COLUMN_MAPS_Y = "y";
    static final String COLUMN_MAPS_DATE = "date";
    static final String COLUMN_MAPS_DOWNLOADING = "downloading";

    private static final String COLUMN_MAP_FEATURES_COLUMN = "x";
    private static final String COLUMN_MAP_FEATURES_ROW = "y";
    private static final String COLUMN_MAP_FEATURES_FEATURE = "feature";

    @SuppressWarnings("WeakerAccess")
    static final String COLUMN_INFO_NAME = "name";
    static final String COLUMN_INFO_VALUE = "value";

    private static final String COLUMN_TILES_ZOOM_LEVEL = "zoom_level";
    private static final String COLUMN_TILES_COLUMN = "tile_column";
    private static final String COLUMN_TILES_ROW = "tile_row";
    static final String COLUMN_TILES_DATA = "tile_data";

    private static final String COLUMN_NAMES_REF = "ref";
    static final String COLUMN_NAMES_NAME = "name";

    private static final String COLUMN_FEATURES_ID = "id";
    private static final String COLUMN_FEATURES_KIND = "kind";
    private static final String COLUMN_FEATURES_LAT = "lat";
    private static final String COLUMN_FEATURES_LON = "lon";

    private static final String COLUMN_FEATURES_NAMES_LANG = "lang";
    private static final String COLUMN_FEATURES_NAMES_NAME = "name";

    static final String SQL_CREATE_MAPS =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_MAPS + " ("
                    + COLUMN_MAPS_X + " INTEGER NOT NULL, "
                    + COLUMN_MAPS_Y + " INTEGER NOT NULL, "
                    + COLUMN_MAPS_DATE + " INTEGER NOT NULL DEFAULT 0, "
                    + COLUMN_MAPS_DOWNLOADING + " INTEGER NOT NULL DEFAULT 0"
                    + ")";

    private static final String SQL_CREATE_MAP_FEATURES =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_MAP_FEATURES + " ("
                    + COLUMN_MAP_FEATURES_COLUMN + " INTEGER NOT NULL, "
                    + COLUMN_MAP_FEATURES_ROW + " INTEGER NOT NULL, "
                    + COLUMN_MAP_FEATURES_FEATURE + " INTEGER NOT NULL"
                    + ")";

    private static final String SQL_CREATE_INFO =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_INFO + " ("
                    + COLUMN_INFO_NAME + " TEXT NOT NULL, "
                    + COLUMN_INFO_VALUE + " TEXT"
                    + ")";

    private static final String SQL_CREATE_TILES =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_TILES + " ("
                    + COLUMN_TILES_ZOOM_LEVEL + " INTEGER NOT NULL, "
                    + COLUMN_TILES_COLUMN + " INTEGER NOT NULL, "
                    + COLUMN_TILES_ROW + " INTEGER NOT NULL, "
                    + COLUMN_TILES_DATA + " BLOB NOT NULL"
                    + ")";

    private static final String SQL_CREATE_NAMES =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_NAMES + " ("
                    + COLUMN_NAMES_REF + " INTEGER NOT NULL, "
                    + COLUMN_NAMES_NAME + " TEXT NOT NULL"
                    + ")";

    private static final String SQL_CREATE_NAMES_FTS =
            "CREATE VIRTUAL TABLE IF NOT EXISTS "
                    + TABLE_NAMES_FTS + " USING fts4(tokenize=unicode61, content=\""
                    + TABLE_NAMES + "\", " + COLUMN_NAMES_NAME
                    + ")";

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
                    + COLUMN_FEATURES_ID + " INTEGER NOT NULL, "
                    + COLUMN_FEATURES_NAMES_LANG + " INTEGER NOT NULL, "
                    + COLUMN_FEATURES_NAMES_NAME + " INTEGER NOT NULL"
                    + ")";

    static final String SQL_REMOVE_TILES =
            "DELETE FROM " + TABLE_TILES + " WHERE "
                    + COLUMN_TILES_ZOOM_LEVEL + " = ? AND "
                    + COLUMN_TILES_COLUMN + " >= ? AND "
                    + COLUMN_TILES_COLUMN + " <= ? AND "
                    + COLUMN_TILES_ROW + " >= ? AND "
                    + COLUMN_TILES_ROW + " <= ?";

    static final String SQL_REMOVE_FEATURES =
            "DELETE FROM " + TABLE_FEATURES + " WHERE "
                    + COLUMN_FEATURES_ID + " IN (SELECT a."
                    + COLUMN_MAP_FEATURES_FEATURE + " FROM "
                    + TABLE_MAP_FEATURES + " AS a LEFT JOIN "
                    + TABLE_MAP_FEATURES + " AS b ON (a."
                    + COLUMN_MAP_FEATURES_FEATURE + " = b."
                    + COLUMN_MAP_FEATURES_FEATURE + " AND (a."
                    + COLUMN_MAP_FEATURES_COLUMN + " != b."
                    + COLUMN_MAP_FEATURES_COLUMN + " OR a."
                    + COLUMN_MAP_FEATURES_ROW + " != b."
                    + COLUMN_MAP_FEATURES_ROW + ")) WHERE a."
                    + COLUMN_MAP_FEATURES_COLUMN + " = ? AND a."
                    + COLUMN_MAP_FEATURES_ROW + " = ? AND b."
                    + COLUMN_MAP_FEATURES_FEATURE + " IS NULL)";

    static final String SQL_REMOVE_FEATURE_NAMES =
            "DELETE FROM " + TABLE_FEATURE_NAMES + " WHERE "
                    + COLUMN_FEATURES_ID + " IN (SELECT "
                    + TABLE_FEATURE_NAMES + "." + COLUMN_FEATURES_ID + " FROM "
                    + TABLE_FEATURE_NAMES + " LEFT JOIN "
                    + TABLE_FEATURES + " ON ("
                    + TABLE_FEATURE_NAMES + "." + COLUMN_FEATURES_ID + " = "
                    + TABLE_FEATURES + "." + COLUMN_FEATURES_ID + ") WHERE "
                    + TABLE_FEATURES + "." + COLUMN_FEATURES_ID + " IS NULL)";

    static final String SQL_SELECT_UNUSED_NAMES =
            "SELECT "
                    + COLUMN_NAMES_REF + " FROM "
                    + TABLE_NAMES + " LEFT JOIN "
                    + TABLE_FEATURE_NAMES + " ON ("
                    + COLUMN_NAMES_REF + " = "
                    + TABLE_FEATURE_NAMES + "." + COLUMN_FEATURES_NAMES_NAME + ") WHERE "
                    + COLUMN_FEATURES_ID + " IS NULL";

    static final String SQL_REMOVE_NAMES =
            "DELETE FROM " + TABLE_NAMES + " WHERE "
                    + COLUMN_NAMES_REF + " IN (" + SQL_SELECT_UNUSED_NAMES + ")";

    static final String SQL_REMOVE_NAMES_FTS =
            "DELETE FROM " + TABLE_NAMES_FTS + " WHERE docid IN (";

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
    static final String WHERE_TILE_ZXY = COLUMN_TILES_ZOOM_LEVEL + " = ? AND " + COLUMN_TILES_COLUMN + " = ? AND " + COLUMN_TILES_ROW + " = ?";

    private static final String SQL_INSERT_NAMES_FTS = "INSERT INTO "
            + TABLE_NAMES_FTS + "(docid, "
            + COLUMN_NAMES_NAME + ") SELECT "
            + COLUMN_NAMES_REF + ", "
            + COLUMN_NAMES_NAME + " FROM "
            + TABLE_NAMES;

    private static final String SQL_INDEX_INFO = "CREATE UNIQUE INDEX IF NOT EXISTS property ON metadata (name)";
    static final String SQL_INDEX_MAPS = "CREATE UNIQUE INDEX IF NOT EXISTS maps_x_y ON maps (x, y)";
    private static final String SQL_INDEX_MAP_FEATURES = "CREATE UNIQUE INDEX IF NOT EXISTS map_feature_ids ON map_features (feature)";
    private static final String SQL_INDEX_MAP_FEATURE_REFS = "CREATE UNIQUE INDEX IF NOT EXISTS map_feature_refs ON map_features (x, y, feature)";
    private static final String SQL_INDEX_TILES = "CREATE UNIQUE INDEX IF NOT EXISTS coord ON tiles (zoom_level, tile_column, tile_row)";
    private static final String SQL_INDEX_NAMES = "CREATE UNIQUE INDEX IF NOT EXISTS name_ref ON names (ref)";
    private static final String SQL_INDEX_FEATURES = "CREATE UNIQUE INDEX IF NOT EXISTS feature_id ON features (id)";
    private static final String SQL_INDEX_FEATURE_LANG = "CREATE UNIQUE INDEX IF NOT EXISTS feature_name_lang ON feature_names (id, lang)";
    private static final String SQL_INDEX_FEATURE_NAME = "CREATE UNIQUE INDEX IF NOT EXISTS feature_name_ref ON feature_names (id, lang, name)";
    private static final String SQL_INDEX_FEATURE_NAMES = "CREATE INDEX IF NOT EXISTS feature_names_ref ON feature_names (name)";

    private static final String PRAGMA_PAGE_SIZE = "PRAGMA main.page_size = 4096";
    private static final String PRAGMA_ENABLE_VACUUM = "PRAGMA main.auto_vacuum = INCREMENTAL";
    private static final String PRAGMA_VACUUM = "PRAGMA main.incremental_vacuum(5000)";
    private static final String FTS_MERGE = "INSERT INTO names_fts(names_fts) VALUES('merge=300,8')";

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
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        logger.info("Vacuuming maps database");
        Cursor cursor = db.rawQuery(PRAGMA_VACUUM, null);
        if (cursor.moveToFirst())
            logger.debug("  removed {} pages", cursor.getCount());
        cursor.close();
        if (hasFullTextIndex(db)) {
            cursor = db.rawQuery(FTS_MERGE, null);
            if (cursor.moveToFirst())
                logger.debug("  merged FTS index");
            cursor.close();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PRAGMA_ENABLE_VACUUM);
        db.execSQL(PRAGMA_PAGE_SIZE);
        db.execSQL(SQL_CREATE_MAPS);
        db.execSQL(SQL_CREATE_MAP_FEATURES);
        db.execSQL(SQL_CREATE_INFO);
        db.execSQL(SQL_CREATE_TILES);
        db.execSQL(SQL_CREATE_NAMES);
        db.execSQL(SQL_CREATE_NAMES_FTS);
        db.execSQL(SQL_CREATE_FEATURES);
        db.execSQL(SQL_CREATE_FEATURE_NAMES);
        db.execSQL(SQL_INDEX_MAPS);
        db.execSQL(SQL_INDEX_MAP_FEATURES);
        db.execSQL(SQL_INDEX_MAP_FEATURE_REFS);
        db.execSQL(SQL_INDEX_TILES);
        db.execSQL(SQL_INDEX_INFO);
        db.execSQL(SQL_INDEX_NAMES);
        db.execSQL(SQL_INDEX_FEATURES);
        db.execSQL(SQL_INDEX_FEATURE_LANG);
        db.execSQL(SQL_INDEX_FEATURE_NAME);
        db.execSQL(SQL_INDEX_FEATURE_NAMES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        logger.debug("Upgrade from {} to {}", oldVersion, newVersion);
        if (oldVersion == 2) {
            db.execSQL(SQL_INDEX_FEATURE_NAMES);
        }
    }

    public static void createFtsTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_NAMES_FTS);
        logger.debug("Populate fts");
        db.execSQL(SQL_INSERT_NAMES_FTS);
        logger.debug("Finished populating fts");
    }

    public static boolean hasFullTextIndex(SQLiteDatabase db) {
        try {
            String[] selectionArgs = {"Antarctica"};
            Cursor cursor = db.rawQuery("SELECT docid FROM " + TABLE_NAMES_FTS + " WHERE " +
                    TABLE_NAMES_FTS + " MATCH ?", selectionArgs);
            cursor.close();
            return true;
        } catch (SQLiteException ignore) {
            return false;
        }
    }
}
