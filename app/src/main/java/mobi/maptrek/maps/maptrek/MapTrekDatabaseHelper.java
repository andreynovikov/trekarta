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

package mobi.maptrek.maps.maptrek;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import androidx.annotation.Nullable;

import com.ibm.icu.text.IDNA;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import mobi.maptrek.data.Amenity;
import mobi.maptrek.util.OpeningHoursLocalizer;
import mobi.maptrek.util.Smaz;

public class MapTrekDatabaseHelper extends SQLiteOpenHelper {
    private static final Logger logger = LoggerFactory.getLogger(MapTrekDatabaseHelper.class);

    private static final int DATABASE_VERSION = 7;

    static final String TABLE_MAPS = "maps";
    static final String TABLE_MAP_FEATURES = "map_features";
    static final String TABLE_INFO = "metadata";
    static final String TABLE_TILES = "tiles";
    static final String TABLE_NAMES = "names";
    static final String TABLE_NAMES_FTS = "names_fts";
    static final String TABLE_FEATURES = "features";
    static final String TABLE_FEATURE_NAMES = "feature_names";

    static final String COLUMN_MAPS_X = "x";
    static final String COLUMN_MAPS_Y = "y";
    static final String COLUMN_MAPS_DATE = "date";
    static final String COLUMN_MAPS_VERSION = "version";
    static final String COLUMN_MAPS_DOWNLOADING = "downloading";
    static final String COLUMN_MAPS_HILLSHADE_DOWNLOADING = "hillshade_downloading";

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

    static final String COLUMN_FEATURES_ID = "id";
    static final String COLUMN_FEATURES_KIND = "kind";
    static final String COLUMN_FEATURES_TYPE = "type";
    static final String COLUMN_FEATURES_X = "x";
    static final String COLUMN_FEATURES_Y = "y";
    static final String COLUMN_FEATURES_LAT = "lat";
    static final String COLUMN_FEATURES_LON = "lon";
    static final String COLUMN_FEATURES_OPENING_HOURS = "opening_hours";
    static final String COLUMN_FEATURES_PHONE = "phone";
    static final String COLUMN_FEATURES_WIKIPEDIA = "wikipedia";
    static final String COLUMN_FEATURES_WEBSITE = "website";
    static final String COLUMN_FEATURES_FLAGS = "flags";
    static final String COLUMN_FEATURES_ENUM1 = "enum1";

    private static final String COLUMN_FEATURES_NAMES_LANG = "lang";
    private static final String COLUMN_FEATURES_NAMES_NAME = "name";

    static final String SQL_CREATE_MAPS =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE_MAPS + " ("
                    + COLUMN_MAPS_X + " INTEGER NOT NULL, "
                    + COLUMN_MAPS_Y + " INTEGER NOT NULL, "
                    + COLUMN_MAPS_DATE + " INTEGER NOT NULL DEFAULT 0, "
                    + COLUMN_MAPS_VERSION + " INTEGER NOT NULL DEFAULT 0, "
                    + COLUMN_MAPS_DOWNLOADING + " INTEGER NOT NULL DEFAULT 0, "
                    + COLUMN_MAPS_HILLSHADE_DOWNLOADING + " INTEGER NOT NULL DEFAULT 0"
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
                    + COLUMN_FEATURES_TYPE + " INTEGER, "
                    + COLUMN_FEATURES_X + " INTEGER, "
                    + COLUMN_FEATURES_Y + " INTEGER, "
                    + COLUMN_FEATURES_LAT + " REAL, "
                    + COLUMN_FEATURES_LON + " REAL, "
                    + COLUMN_FEATURES_OPENING_HOURS + " TEXT, "
                    + COLUMN_FEATURES_PHONE + " TEXT, "
                    + COLUMN_FEATURES_WIKIPEDIA + " TEXT, "
                    + COLUMN_FEATURES_WEBSITE + " TEXT, "
                    + COLUMN_FEATURES_FLAGS + " INTEGER, "
                    + COLUMN_FEATURES_ENUM1 + " INTEGER"
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
            COLUMN_MAPS_VERSION,
            COLUMN_MAPS_DOWNLOADING,
            COLUMN_MAPS_HILLSHADE_DOWNLOADING
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

    static final String[] ALL_COLUMNS_FEATURES_V1 = {
            COLUMN_FEATURES_ID,
            COLUMN_FEATURES_KIND,
            COLUMN_FEATURES_LAT,
            COLUMN_FEATURES_LON
    };

    static final String[] ALL_COLUMNS_FEATURES_V2 = {
            COLUMN_FEATURES_ID,
            COLUMN_FEATURES_KIND,
            COLUMN_FEATURES_LAT,
            COLUMN_FEATURES_LON,
            COLUMN_FEATURES_OPENING_HOURS,
            COLUMN_FEATURES_PHONE,
            COLUMN_FEATURES_WIKIPEDIA,
            COLUMN_FEATURES_WEBSITE
    };

    static final String[] ALL_COLUMNS_FEATURES_WO_XY = {
            COLUMN_FEATURES_ID,
            COLUMN_FEATURES_KIND,
            COLUMN_FEATURES_TYPE,
            COLUMN_FEATURES_LAT,
            COLUMN_FEATURES_LON,
            COLUMN_FEATURES_OPENING_HOURS,
            COLUMN_FEATURES_PHONE,
            COLUMN_FEATURES_WIKIPEDIA,
            COLUMN_FEATURES_WEBSITE,
            COLUMN_FEATURES_FLAGS,
            COLUMN_FEATURES_ENUM1
    };

    static final String[] ALL_COLUMNS_FEATURES = {
            COLUMN_FEATURES_ID,
            COLUMN_FEATURES_KIND,
            COLUMN_FEATURES_TYPE,
            COLUMN_FEATURES_X,
            COLUMN_FEATURES_Y,
            COLUMN_FEATURES_LAT,
            COLUMN_FEATURES_LON,
            COLUMN_FEATURES_OPENING_HOURS,
            COLUMN_FEATURES_PHONE,
            COLUMN_FEATURES_WIKIPEDIA,
            COLUMN_FEATURES_WEBSITE,
            COLUMN_FEATURES_FLAGS,
            COLUMN_FEATURES_ENUM1
    };

    static final String[] ALL_COLUMNS_FEATURE_NAMES = {
            COLUMN_FEATURES_ID,
            COLUMN_FEATURES_NAMES_LANG,
            COLUMN_FEATURES_NAMES_NAME
    };

    private static final String SQL_GET_NAME = "SELECT names.name, lang FROM names INNER JOIN feature_names ON (ref = feature_names.name) WHERE id = ? AND lang IN (0, ?) ORDER BY lang";

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
    private static final String SQL_INDEX_MAP_FEATURES = "CREATE INDEX IF NOT EXISTS map_feature_ids ON map_features (feature)";
    private static final String SQL_INDEX_MAP_FEATURE_REFS = "CREATE UNIQUE INDEX IF NOT EXISTS map_feature_refs ON map_features (x, y, feature)";
    private static final String SQL_INDEX_TILES = "CREATE UNIQUE INDEX IF NOT EXISTS coord ON tiles (zoom_level, tile_column, tile_row)";
    private static final String SQL_INDEX_NAMES = "CREATE UNIQUE INDEX IF NOT EXISTS name_ref ON names (ref)";
    private static final String SQL_INDEX_FEATURES = "CREATE UNIQUE INDEX IF NOT EXISTS feature_id ON features (id)";
    private static final String SQL_INDEX_FEATURE_TYPES = "CREATE INDEX IF NOT EXISTS feature_types ON features (type, x, y)";
    private static final String SQL_INDEX_FEATURE_LANG = "CREATE UNIQUE INDEX IF NOT EXISTS feature_name_lang ON feature_names (id, lang)";
    private static final String SQL_INDEX_FEATURE_NAME = "CREATE UNIQUE INDEX IF NOT EXISTS feature_name_ref ON feature_names (id, lang, name)";
    private static final String SQL_INDEX_FEATURE_NAMES = "CREATE INDEX IF NOT EXISTS feature_names_ref ON feature_names (name)";

    static final String PRAGMA_PAGE_SIZE = "PRAGMA main.page_size = 4096";
    static final String PRAGMA_ENABLE_VACUUM = "PRAGMA main.auto_vacuum = INCREMENTAL";
    static final String PRAGMA_VACUUM = "PRAGMA main.incremental_vacuum(5000)";
    private static final String FTS_MERGE = "INSERT INTO names_fts(names_fts) VALUES('merge=300,8')";

    private static IDNA idna = null;
    static {
        try {
            idna = IDNA.getUTS46Instance(IDNA.DEFAULT);
        } catch (Exception e) {
            logger.error("Failed to initialize IDNA decoder", e);
        }
    }

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
        createWorldMapTables(db);
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
        logger.error("Create");
        db.execSQL(PRAGMA_ENABLE_VACUUM);
        db.execSQL(PRAGMA_PAGE_SIZE);
        db.execSQL(SQL_CREATE_MAPS);
        db.execSQL(SQL_CREATE_MAP_FEATURES);
        db.execSQL(SQL_CREATE_INFO);
        db.execSQL(SQL_CREATE_TILES);
        db.execSQL(SQL_CREATE_NAMES);
        db.execSQL(SQL_CREATE_FEATURES);
        db.execSQL(SQL_CREATE_FEATURE_NAMES);
        // This test is for single area map debugging only
        try {
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_FEATURES_X + " FROM " + TABLE_FEATURES + " LIMIT 1", null);
            cursor.close();
        } catch (SQLiteException ignore) {
            logger.error("  Create feature index");
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_X + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_Y + " INTEGER");
            upgradeFeatureTable(db);
        }
        db.execSQL(SQL_INDEX_MAPS);
        db.execSQL(SQL_INDEX_MAP_FEATURES);
        db.execSQL(SQL_INDEX_MAP_FEATURE_REFS);
        db.execSQL(SQL_INDEX_TILES);
        db.execSQL(SQL_INDEX_INFO);
        db.execSQL(SQL_INDEX_NAMES);
        db.execSQL(SQL_INDEX_FEATURES);
        db.execSQL(SQL_INDEX_FEATURE_TYPES);
        db.execSQL(SQL_INDEX_FEATURE_LANG);
        db.execSQL(SQL_INDEX_FEATURE_NAME);
        db.execSQL(SQL_INDEX_FEATURE_NAMES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        logger.error("Upgrade from {} to {}", oldVersion, newVersion);
        if (oldVersion < 3) {
            db.execSQL(SQL_INDEX_FEATURE_NAMES);
        }
        if (oldVersion < 4) {
            db.execSQL("DROP INDEX IF EXISTS map_feature_ids");
            db.execSQL(SQL_INDEX_MAP_FEATURES);
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_MAPS + " ADD COLUMN " + COLUMN_MAPS_VERSION + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_MAPS + " ADD COLUMN " + COLUMN_MAPS_HILLSHADE_DOWNLOADING + " INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_TYPE + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_X + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_Y + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_OPENING_HOURS + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_PHONE + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_WIKIPEDIA + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_WEBSITE + " TEXT");
            upgradeFeatureTable(db);
            db.execSQL(SQL_INDEX_FEATURE_TYPES);
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_FLAGS + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_FEATURES + " ADD COLUMN " + COLUMN_FEATURES_ENUM1 + " INTEGER");
        }
    }

    private static void createWorldMapTables(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MAPS + " LIMIT 1", null);
            cursor.close();
        } catch (SQLiteException ignore) {
            logger.error("  Create maps table");
            db.execSQL(SQL_CREATE_MAPS);
            db.execSQL(SQL_INDEX_MAPS);
        }
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MAP_FEATURES + " LIMIT 1", null);
            cursor.close();
        } catch (SQLiteException ignore) {
            logger.error("  Create map features table");
            db.execSQL(SQL_CREATE_MAP_FEATURES);
            db.execSQL(SQL_INDEX_MAP_FEATURES);
            db.execSQL(SQL_INDEX_MAP_FEATURE_REFS);
        }
    }

    private static void upgradeFeatureTable(SQLiteDatabase db) {
        logger.error("  Upgrade feature table");
        SQLiteStatement statement = db.compileStatement("UPDATE " + TABLE_FEATURES + " SET " +
                COLUMN_FEATURES_X + " = ?, " + COLUMN_FEATURES_Y + " = ? WHERE " +
                COLUMN_FEATURES_ID + " = ?");
        String SQL_SELECT_FEATURES = "SELECT " + COLUMN_FEATURES_ID + ", " + COLUMN_FEATURES_LAT +
                ", " + COLUMN_FEATURES_LON + " FROM " + TABLE_FEATURES;
        db.beginTransaction();
        Cursor cursor = db.rawQuery(SQL_SELECT_FEATURES, null);
        cursor.moveToFirst();
        int[] xy = new int[] {0, 0};
        while (!cursor.isAfterLast()) {
            long id = cursor.getLong(0);
            double lat = cursor.getDouble(1);
            double lon = cursor.getDouble(2);
            cursor.moveToNext();
            if (lat == 0 && lon == 0)
                continue;

            getFourteenthTileXY(lat, lon, xy);
            statement.bindLong(1, xy[0]);
            statement.bindLong(2, xy[1]);
            statement.bindLong(3, id);
            statement.execute();
        }
        cursor.close();
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    static void getFourteenthTileXY(double lat, double lon, int[] xy) {
        xy[0] = (int) ((lon + 180) / 360 * 16384);
        double lr = Math.toRadians(lat);
        xy[1] = (int) ((1 - Math.log(Math.tan(lr) + 1 / Math.cos(lr)) / Math.PI) / 2 * 16384);
        xy[0] = Math.min(Math.max(xy[0], 0), 16383);
        xy[1] = Math.min(Math.max(xy[1], 0), 16383);
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

    public static Amenity getAmenityData(int lang, long elementId, SQLiteDatabase db) {
        String[] args = {String.valueOf(elementId)};
        Amenity amenity = null;
        try (Cursor c = db.query(TABLE_FEATURES, ALL_COLUMNS_FEATURES_WO_XY, "id = ?", args, null, null, null)) {
            if (c.moveToFirst()) {
                int kind = c.getInt(c.getColumnIndex(COLUMN_FEATURES_KIND));
                int type = c.getInt(c.getColumnIndex(COLUMN_FEATURES_TYPE));
                double lat = c.getDouble(c.getColumnIndex(COLUMN_FEATURES_LAT));
                double lon = c.getDouble(c.getColumnIndex(COLUMN_FEATURES_LON));
                amenity = new Amenity(elementId, kind, type, lat, lon);
                amenity.name = getFeatureName(lang, elementId, db);
                String openingHours = c.getString(c.getColumnIndex(COLUMN_FEATURES_OPENING_HOURS));
                if (openingHours != null) {
                    amenity.openingHours = OpeningHoursLocalizer.localize(Smaz.decompress(openingHours, Smaz.OPENING_HOURS_DECODE), lang);
                }
                String phone = c.getString(c.getColumnIndex(COLUMN_FEATURES_PHONE));
                if (phone != null)
                    amenity.phone = Smaz.decompress(phone, Smaz.PHONE_DECODE);
                amenity.wikipedia = c.getString(c.getColumnIndex(COLUMN_FEATURES_WIKIPEDIA));
                String website = c.getString(c.getColumnIndex(COLUMN_FEATURES_WEBSITE));
                if (website != null) {
                    website = Smaz.decompress(website, Smaz.WEBSITE_DECODE);
                    URL url = new URL(website);
                    if (website.contains("xn--") && idna != null) {
                        StringBuilder sb = new StringBuilder();
                        IDNA.Info info = new IDNA.Info();
                        try {
                            if (!website.startsWith("http://") && !website.startsWith("https://"))
                                website = "http://" + website;
                            String host = url.getHost();
                            idna.nameToUnicode(host, sb, info);
                            website = website.replace(host, sb);
                        } catch (Exception e) {
                            logger.error("IDNA decode error", e);
                        }
                    }
                    String qs = url.getQuery();
                    if (qs != null)
                        website = website.replace(qs, URLDecoder.decode(qs, "UTF-8"));
                    amenity.website = website;
                }
                if (!c.isNull(c.getColumnIndex(COLUMN_FEATURES_FLAGS))) {
                    int flags = c.getInt(c.getColumnIndex(COLUMN_FEATURES_FLAGS));
                    if ((flags & 0x00000001) == 0x00000001)
                        amenity.fee = Amenity.Fee.YES;
                    if ((flags & 0x00000006) == 0x00000006)
                        amenity.wheelchair = Amenity.Wheelchair.YES;
                    else if ((flags & 0x00000004) == 0x00000004)
                        amenity.wheelchair = Amenity.Wheelchair.LIMITED;
                    else if ((flags & 0x00000002) == 0x00000002)
                        amenity.wheelchair = Amenity.Wheelchair.NO;
                }
            }
        } catch (Exception e) {
            logger.error("Query error", e);
        }
        return amenity;
    }

    static String getFeatureName(int lang, long elementId, SQLiteDatabase db) {
        String[] args = {String.valueOf(elementId), String.valueOf(lang)};
        try (Cursor c = db.rawQuery(SQL_GET_NAME, args)) {
            String[] result = new String[c.getCount()];
            int i = 0;
            if (c.moveToFirst())
                do {
                    result[i] = c.getString(0);
                    i++;
                } while (c.moveToNext());

            if (result.length > 0) {
                if (result.length == 2 && result[1] != null)
                    return result[1];
                return result[0];
            }
        } catch (Exception e) {
            logger.error("Query error", e);
        }
        return null;
    }

    public static int getLanguageId(@Nullable String lang) {
        if (lang == null)
            return 0;
        switch (lang) {
            case "en":
                return 840;
            case "de":
                return 276;
            case "ru":
                return 643;
        }
        return 0;
    }
}
