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

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import mobi.maptrek.Configuration;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.maps.MapService;
import mobi.maptrek.util.ProgressListener;

import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_FEATURES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_FEATURES_V2;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_FEATURES_WO_XY;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_FEATURES_V1;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_FEATURE_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_MAPS;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_TILES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_ENUM1;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_FLAGS;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_ID;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_KIND;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_TYPE;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_LAT;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_LON;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_OPENING_HOURS;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_PHONE;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_WIKIPEDIA;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_FEATURES_WEBSITE;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_INFO_VALUE;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_DATE;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_DOWNLOADING;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_HILLSHADE_DOWNLOADING;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_VERSION;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_X;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_Y;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_NAMES_NAME;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.SQL_REMOVE_FEATURES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.SQL_REMOVE_FEATURE_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.SQL_REMOVE_GONE_FEATURES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.SQL_REMOVE_MAP_FEATURES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.SQL_REMOVE_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.SQL_REMOVE_NAMES_FTS;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.SQL_REMOVE_TILES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.SQL_SELECT_UNUSED_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_FEATURES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_FEATURE_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_INFO;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_MAPS;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_MAP_FEATURES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_NAMES_FTS;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_TILES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.WHERE_INFO_NAME;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.WHERE_MAPS_PRESENT;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.WHERE_MAPS_XY;

public class Index {
    private static final Logger logger = LoggerFactory.getLogger(Index.class);

    public static final String WORLDMAP_FILENAME = "world.mtiles";
    public static final String BASEMAP_FILENAME = "basemap.mtiles";
    public static final int BASEMAP_SIZE_STUB = 45;
    public static final String HILLSHADE_FILENAME = "hillshade.mbtiles";
    private static final long HUGE_MAP_THRESHOLD = 400 * 1024 * 1024; // 400MB

    public enum ACTION {NONE, DOWNLOAD, CANCEL, REMOVE}

    private final Context mContext;
    private final SQLiteDatabase mMapsDatabase;
    private final SQLiteDatabase mHillshadeDatabase;
    private final DownloadManager mDownloadManager;
    private final MapStatus[][] mMaps = new MapStatus[128][128];
    private boolean mHasDownloadSizes;
    private boolean mExpiredDownloadSizes;
    private boolean mAccountHillshades;
    private int mLoadedMaps = 0;
    private boolean mHasHillshades;
    private short mBaseMapDownloadVersion = 0;
    private short mBaseMapVersion = 0;
    private long mBaseMapDownloadSize = 0L;

    private final Set<WeakReference<MapStateListener>> mMapStateListeners = new HashSet<>();

    public Index(Context context, SQLiteDatabase mapsDatabase, SQLiteDatabase hillshadesDatabase) {
        mContext = context;
        mMapsDatabase = mapsDatabase;
        mHillshadeDatabase = hillshadesDatabase;
        mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        try {
            Cursor cursor = mMapsDatabase.query(TABLE_MAPS, ALL_COLUMNS_MAPS, WHERE_MAPS_PRESENT, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                int x = cursor.getInt(cursor.getColumnIndex(COLUMN_MAPS_X));
                int y = cursor.getInt(cursor.getColumnIndex(COLUMN_MAPS_Y));
                short date = cursor.getShort(cursor.getColumnIndex(COLUMN_MAPS_DATE));
                byte version = (byte) cursor.getShort(cursor.getColumnIndex(COLUMN_MAPS_VERSION));
                logger.debug("index({}, {}, {}, {})", x, y, date, version);
                if (x == -1 && y == -1) {
                    mBaseMapVersion = date;
                    cursor.moveToNext();
                    continue;
                }
                long downloading = cursor.getLong(cursor.getColumnIndex(COLUMN_MAPS_DOWNLOADING));
                long hillshadeDownloading = cursor.getLong(cursor.getColumnIndex(COLUMN_MAPS_HILLSHADE_DOWNLOADING));
                MapStatus mapStatus = getNativeMap(x, y);
                mapStatus.created = date;
                mapStatus.hillshadeVersion = version;
                int status = checkDownloadStatus(downloading);
                if (status == DownloadManager.STATUS_PAUSED
                        || status == DownloadManager.STATUS_PENDING
                        || status == DownloadManager.STATUS_RUNNING) {
                    mapStatus.downloading = downloading;
                    logger.debug("  map downloading: {}", downloading);
                } else {
                    downloading = 0L;
                    setDownloading(x, y, downloading, hillshadeDownloading);
                    logger.debug("  cleared");
                }
                status = checkDownloadStatus(hillshadeDownloading);
                if (status == DownloadManager.STATUS_PAUSED
                        || status == DownloadManager.STATUS_PENDING
                        || status == DownloadManager.STATUS_RUNNING) {
                    mapStatus.hillshadeDownloading = hillshadeDownloading;
                    logger.debug("  hillshade downloading: {}", downloading);
                } else {
                    hillshadeDownloading = 0L;
                    setDownloading(x, y, downloading, hillshadeDownloading);
                    logger.debug("  cleared");
                }
                if (date > 0)
                    mLoadedMaps++;
                cursor.moveToNext();
            }
            cursor.close();
        } catch (SQLiteException e) {
            logger.error("Failed to read map index", e);
            mMapsDatabase.execSQL(MapTrekDatabaseHelper.SQL_CREATE_MAPS);
            mMapsDatabase.execSQL(MapTrekDatabaseHelper.SQL_INDEX_MAPS);
        }
        mHasHillshades = DatabaseUtils.queryNumEntries(mHillshadeDatabase, TABLE_TILES) > 0;

        //TODO Remove old basemap file
    }

    public short getBaseMapVersion() {
        return mBaseMapVersion;
    }

    public boolean isBaseMapOutdated() {
        return mBaseMapVersion > 0 && mBaseMapVersion < mBaseMapDownloadVersion;
    }

    public boolean hasHillshades() {
        return mHasHillshades;
    }

    public long getBaseMapSize() {
        return mBaseMapDownloadSize > 0L ? mBaseMapDownloadSize : BASEMAP_SIZE_STUB * 1024 * 1024;
    }

    public long getMapDatabaseSize() {
        long size = new File(mMapsDatabase.getPath()).length();
        size += new File(mHillshadeDatabase.getPath()).length();
        return size;
    }

    public void setBaseMapStatus(short date, int size) {
        mBaseMapDownloadVersion = date;
        mBaseMapDownloadSize = size;
    }

    /**
     * Returns native map for a specified square.
     */
    @NonNull
    public MapStatus getNativeMap(int x, int y) {
        if (mMaps[x][y] == null) {
            mMaps[x][y] = new MapStatus();
        }
        return mMaps[x][y];
    }

    public void selectNativeMap(int x, int y, ACTION action) {
        IndexStats stats = getMapStats();
        MapStatus mapStatus = getNativeMap(x, y);
        if (mapStatus.action == action) {
            mapStatus.action = ACTION.NONE;
            if (action == ACTION.DOWNLOAD) {
                stats.download--;
                if (mHasDownloadSizes) {
                    stats.downloadSize -= mapStatus.downloadSize;
                    if (mAccountHillshades)
                        stats.downloadSize -= mapStatus.hillshadeDownloadSize;
                }
            }
            if (action == ACTION.REMOVE) {
                stats.remove--;
            }
        } else if (action == ACTION.DOWNLOAD) {
            mapStatus.action = action;
            stats.download++;
            if (mHasDownloadSizes) {
                stats.downloadSize += mapStatus.downloadSize;
                if (mAccountHillshades)
                    stats.downloadSize += mapStatus.hillshadeDownloadSize;
            }
        } else if (action == ACTION.REMOVE) {
            mapStatus.action = action;
            stats.remove++;
        }
        for (WeakReference<MapStateListener> weakRef : mMapStateListeners) {
            MapStateListener mapStateListener = weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onMapSelected(x, y, mapStatus.action, stats);
            }
        }
    }

    public void removeNativeMap(int x, int y, @Nullable ProgressListener progressListener) {
        if (mMaps[x][y] == null)
            return;
        if (mMaps[x][y].created == 0)
            return;

        boolean hillshades = mMaps[x][y].hillshadeVersion != 0L;

        logger.error("Removing map: {} {}", x, y);
        if (progressListener != null)
            progressListener.onProgressStarted(100);
        try {
            // remove tiles
            SQLiteStatement statement = mMapsDatabase.compileStatement(SQL_REMOVE_TILES);
            SQLiteStatement hillshadeStatement = mHillshadeDatabase.compileStatement(SQL_REMOVE_TILES);
            for (int z = 8; z < 15; z++) {
                int s = z - 7;
                int cmin = x << s;
                int cmax = ((x + 1) << s) - 1;
                int rmin = y << s;
                int rmax = ((y + 1) << s) - 1;
                statement.clearBindings();
                statement.bindLong(1, z);
                statement.bindLong(2, cmin);
                statement.bindLong(3, cmax);
                statement.bindLong(4, rmin);
                statement.bindLong(5, rmax);
                statement.executeUpdateDelete();
                if (hillshades && z < 13) {
                    hillshadeStatement.clearBindings();
                    hillshadeStatement.bindLong(1, z);
                    hillshadeStatement.bindLong(2, cmin);
                    hillshadeStatement.bindLong(3, cmax);
                    hillshadeStatement.bindLong(4, rmin);
                    hillshadeStatement.bindLong(5, rmax);
                    hillshadeStatement.executeUpdateDelete();
                }
            }
            if (progressListener != null)
                progressListener.onProgressChanged(10);
            logger.error("  removed tiles");
            // remove features
            statement = mMapsDatabase.compileStatement(SQL_REMOVE_FEATURES);
            statement.bindLong(1, x);
            statement.bindLong(2, y);
            statement.executeUpdateDelete();
            if (progressListener != null)
                progressListener.onProgressChanged(20);
            logger.error("  removed features");
            // remove feature references
            statement = mMapsDatabase.compileStatement(SQL_REMOVE_MAP_FEATURES);
            statement.bindLong(1, x);
            statement.bindLong(2, y);
            statement.executeUpdateDelete();
            if (progressListener != null)
                progressListener.onProgressChanged(30);
            logger.error("  removed feature references");
            // remove feature names
            statement = mMapsDatabase.compileStatement(SQL_REMOVE_FEATURE_NAMES);
            statement.executeUpdateDelete();
            if (progressListener != null)
                progressListener.onProgressChanged(40);
            logger.error("  removed feature names");
            // remove names
            if (MapTrekDatabaseHelper.hasFullTextIndex(mMapsDatabase)) {
                ArrayList<Long> ids = new ArrayList<>();
                Cursor cursor = mMapsDatabase.rawQuery(SQL_SELECT_UNUSED_NAMES, null);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    ids.add(cursor.getLong(0));
                    cursor.moveToNext();
                }
                cursor.close();
                if (ids.size() > 0) {
                    StringBuilder sql = new StringBuilder();
                    sql.append(SQL_REMOVE_NAMES_FTS);
                    String sep = "";
                    for (Long id : ids) {
                        sql.append(sep);
                        sql.append(id);
                        sep = ",";
                    }
                    sql.append(")");
                    statement = mMapsDatabase.compileStatement(sql.toString());
                    statement.executeUpdateDelete();
                }
                if (progressListener != null)
                    progressListener.onProgressChanged(60);
                logger.error("  removed names fts");
            }
            statement = mMapsDatabase.compileStatement(SQL_REMOVE_NAMES);
            statement.executeUpdateDelete();
            if (progressListener != null)
                progressListener.onProgressChanged(100);
            logger.error("  removed names");
            setDownloaded(x, y, (short) 0);
            setHillshadeDownloaded(x, y, (byte) 0);
            if (progressListener != null)
                progressListener.onProgressFinished();
        } catch (Exception e) {
            logger.error("Query error", e);
        }
    }

    public void setNativeMapStatus(int x, int y, short date, long size) {
        if (mMaps[x][y] == null)
            getNativeMap(x, y);
        mMaps[x][y].downloadCreated = date;
        mMaps[x][y].downloadSize = size;
    }

    public void accountHillshades(boolean account) {
        mAccountHillshades = account;
        for (WeakReference<MapStateListener> weakRef : mMapStateListeners) {
            MapStateListener mapStateListener = weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onHillshadeAccountingChanged(account);
            }
        }
    }

    public void setHillshadeStatus(int x, int y, byte version, long size) {
        if (mMaps[x][y] == null)
            getNativeMap(x, y);
        mMaps[x][y].hillshadeDownloadVersion = version;
        mMaps[x][y].hillshadeDownloadSize = size;
    }

    public void clearSelections() {
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++)
                if (mMaps[x][y] != null)
                    mMaps[x][y].action = ACTION.NONE;
    }

    public void cancelDownload(int x, int y) {
        MapStatus map = getNativeMap(x, y);
        mDownloadManager.remove(map.downloading);
        if (map.hillshadeDownloading != 0L)
            mDownloadManager.remove(map.hillshadeDownloading);
        setDownloading(x, y, 0L, 0L);
        selectNativeMap(x, y, ACTION.NONE);
    }

    public boolean isDownloading(int x, int y) {
        return mMaps[x][y] != null && mMaps[x][y].downloading != 0L;
    }

    public boolean processDownloadedMap(int x, int y, String filePath, @Nullable ProgressListener progressListener) {
        File mapFile = new File(filePath);
        try {
            logger.error("Importing from {}", mapFile.getName());
            boolean huge = mapFile.length() > HUGE_MAP_THRESHOLD;
            SQLiteDatabase database = SQLiteDatabase.openDatabase(filePath, null, SQLiteDatabase.OPEN_READONLY);

            short version = 0;
            short date = 0;
            Cursor cursor = database.query(TABLE_INFO, new String[]{COLUMN_INFO_VALUE}, WHERE_INFO_NAME, new String[]{"version"}, null, null, null);
            if (cursor.moveToFirst()) {
                version = Short.parseShort(cursor.getString(0));
            }
            cursor.close();
            cursor = database.query(TABLE_INFO, new String[]{COLUMN_INFO_VALUE}, WHERE_INFO_NAME, new String[]{"timestamp"}, null, null, null);
            if (cursor.moveToFirst()) {
                date = Short.parseShort(cursor.getString(0));
            }
            cursor.close();
            logger.error("Version: {} Date: {}", version, date);

            int total = 2000, progress = 0, step = 0;
            if (progressListener != null) {
                total += DatabaseUtils.queryNumEntries(database, TABLE_NAMES);
                total += DatabaseUtils.queryNumEntries(database, TABLE_FEATURES);
                total += DatabaseUtils.queryNumEntries(database, TABLE_FEATURE_NAMES);
                total += DatabaseUtils.queryNumEntries(database, TABLE_TILES);
                step = total / 100;
                progressListener.onProgressStarted(total);
            }
            boolean hasFts = MapTrekDatabaseHelper.hasFullTextIndex(mMapsDatabase);

            // copy names
            SQLiteStatement statement = mMapsDatabase.compileStatement("REPLACE INTO " + TABLE_NAMES + " VALUES (?,?)");
            SQLiteStatement statementFts = null;
            if (hasFts) {
                statementFts = mMapsDatabase.compileStatement("INSERT INTO " + TABLE_NAMES_FTS
                        + " (docid, " + COLUMN_NAMES_NAME + ") VALUES (?,?)");
            }
            mMapsDatabase.beginTransaction();
            cursor = database.query(TABLE_NAMES, ALL_COLUMNS_NAMES, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                statement.clearBindings();
                statement.bindLong(1, cursor.getLong(0));
                statement.bindString(2, cursor.getString(1));
                statement.execute();
                if (statementFts != null) {
                    statementFts.clearBindings();
                    statementFts.bindLong(1, cursor.getLong(0));
                    statementFts.bindString(2, cursor.getString(1));
                    statementFts.execute();
                }
                if (progressListener != null) {
                    progress++;
                    progressListener.onProgressChanged(progress);
                    if (huge && progress % step == 0) {
                        mMapsDatabase.setTransactionSuccessful();
                        mMapsDatabase.endTransaction();
                        mMapsDatabase.beginTransaction();
                    }
                }
                cursor.moveToNext();
            }
            cursor.close();
            mMapsDatabase.setTransactionSuccessful();
            mMapsDatabase.endTransaction();
            logger.error("  imported names");

            // remove old features references
            statement = mMapsDatabase.compileStatement(SQL_REMOVE_MAP_FEATURES);
            statement.bindLong(1, x);
            statement.bindLong(2, y);
            mMapsDatabase.beginTransaction();
            int num = statement.executeUpdateDelete();
            if (progressListener != null) {
                progress += 1000;
                progressListener.onProgressChanged(progress);
            }
            logger.error("  removed feature references: {}", num);

            // copy features
            statement = mMapsDatabase.compileStatement("REPLACE INTO " + TABLE_FEATURES + " (" +
                    TextUtils.join(", ", ALL_COLUMNS_FEATURES) + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
            SQLiteStatement extraStatement = mMapsDatabase.compileStatement("INSERT INTO " + TABLE_MAP_FEATURES + " VALUES (?,?,?)");
            extraStatement.bindLong(1, x);
            extraStatement.bindLong(2, y);
            String[] COLUMNS_FEATURES = version == 1 ? ALL_COLUMNS_FEATURES_V1 : version == 2 ? ALL_COLUMNS_FEATURES_V2 : ALL_COLUMNS_FEATURES_WO_XY;
            int[] xy = new int[] {0, 0};
            cursor = database.query(TABLE_FEATURES, COLUMNS_FEATURES, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                statement.clearBindings();
                statement.bindLong(1, cursor.getLong(cursor.getColumnIndex(COLUMN_FEATURES_ID)));
                statement.bindLong(2, cursor.getInt(cursor.getColumnIndex(COLUMN_FEATURES_KIND)));
                if (version == 1) {
                    statement.bindNull(3);
                } else {
                    statement.bindLong(3, cursor.getInt(cursor.getColumnIndex(COLUMN_FEATURES_TYPE)));
                }
                int latColumnIndex = cursor.getColumnIndex(COLUMN_FEATURES_LAT);
                int lonColumnIndex = cursor.getColumnIndex(COLUMN_FEATURES_LON);
                boolean isPlaced = !(cursor.isNull(latColumnIndex) || cursor.isNull(lonColumnIndex));
                if (isPlaced) {
                    double lat = cursor.getDouble(latColumnIndex);
                    double lon = cursor.getDouble(lonColumnIndex);
                    statement.bindDouble(6, lat);
                    statement.bindDouble(7, lon);
                    if (version > 1) {
                        MapTrekDatabaseHelper.getFourteenthTileXY(lat, lon, xy);
                        statement.bindLong(4, xy[0]);
                        statement.bindLong(5, xy[1]);
                    }
                } else {
                    statement.bindNull(4);
                    statement.bindNull(5);
                    statement.bindNull(6);
                    statement.bindNull(7);
                }
                if (version == 1 || !isPlaced) {
                    statement.bindNull(8);
                    statement.bindNull(9);
                    statement.bindNull(10);
                    statement.bindNull(11);
                } else {
                    int hoursColumnIndex = cursor.getColumnIndex(COLUMN_FEATURES_OPENING_HOURS);
                    if (cursor.isNull(hoursColumnIndex)) {
                        statement.bindNull(8);
                    } else {
                        statement.bindString(8, cursor.getString(hoursColumnIndex));
                    }
                    int phoneColumnIndex = cursor.getColumnIndex(COLUMN_FEATURES_PHONE);
                    if (cursor.isNull(phoneColumnIndex)) {
                        statement.bindNull(9);
                    } else {
                        statement.bindString(9, cursor.getString(phoneColumnIndex));
                    }
                    int wikiColumnIndex = cursor.getColumnIndex(COLUMN_FEATURES_WIKIPEDIA);
                    if (cursor.isNull(wikiColumnIndex)) {
                        statement.bindNull(10);
                    } else {
                        statement.bindString(10, cursor.getString(wikiColumnIndex));
                    }
                    int siteColumnIndex = cursor.getColumnIndex(COLUMN_FEATURES_WEBSITE);
                    if (cursor.isNull(siteColumnIndex)) {
                        statement.bindNull(11);
                    } else {
                        statement.bindString(11, cursor.getString(siteColumnIndex));
                    }
                }
                if (version < 3 || !isPlaced) {
                    statement.bindNull(12);
                    statement.bindNull(13);
                } else {
                    int flagsColumnIndex = cursor.getColumnIndex(COLUMN_FEATURES_FLAGS);
                    if (cursor.isNull(flagsColumnIndex)) {
                        statement.bindNull(12);
                    } else {
                        statement.bindLong(12, cursor.getLong(flagsColumnIndex));
                    }
                    int enum1ColumnIndex = cursor.getColumnIndex(COLUMN_FEATURES_ENUM1);
                    if (cursor.isNull(enum1ColumnIndex)) {
                        statement.bindNull(13);
                    } else {
                        statement.bindLong(13, cursor.getLong(enum1ColumnIndex));
                    }
                }
                statement.execute();
                extraStatement.bindLong(3, cursor.getLong(0));
                extraStatement.execute();
                if (progressListener != null) {
                    progress++;
                    progressListener.onProgressChanged(progress);
                    if (huge && progress % step == 0) {
                        mMapsDatabase.setTransactionSuccessful();
                        mMapsDatabase.endTransaction();
                        mMapsDatabase.beginTransaction();
                    }
                }
                cursor.moveToNext();
            }
            cursor.close();
            mMapsDatabase.setTransactionSuccessful();
            mMapsDatabase.endTransaction();
            logger.error("  imported features");

            // copy feature names
            statement = mMapsDatabase.compileStatement("REPLACE INTO " + TABLE_FEATURE_NAMES + " VALUES (?,?,?)");
            mMapsDatabase.beginTransaction();
            cursor = database.query(TABLE_FEATURE_NAMES, ALL_COLUMNS_FEATURE_NAMES, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                statement.clearBindings();
                statement.bindLong(1, cursor.getLong(0));
                statement.bindLong(2, cursor.getInt(1));
                statement.bindLong(3, cursor.getLong(2));
                statement.execute();
                if (progressListener != null) {
                    progress++;
                    progressListener.onProgressChanged(progress);
                    if (huge && progress % step == 0) {
                        mMapsDatabase.setTransactionSuccessful();
                        mMapsDatabase.endTransaction();
                        mMapsDatabase.beginTransaction();
                    }
                }
                cursor.moveToNext();
            }
            cursor.close();
            mMapsDatabase.setTransactionSuccessful();
            mMapsDatabase.endTransaction();
            logger.error("  imported feature names");

            // remove gone features
            statement = mMapsDatabase.compileStatement(SQL_REMOVE_GONE_FEATURES);
            statement.bindLong(1, x);
            statement.bindLong(2, x + 1);
            statement.bindLong(3, y);
            statement.bindLong(4, y + 1);
            mMapsDatabase.beginTransaction();
            num = statement.executeUpdateDelete();
            if (progressListener != null) {
                progress += 1000;
                progressListener.onProgressChanged(progress);
            }
            mMapsDatabase.setTransactionSuccessful();
            mMapsDatabase.endTransaction();
            logger.error("  removed gone features: {}", num);

            // copy tiles
            statement = mMapsDatabase.compileStatement("REPLACE INTO " + TABLE_TILES + " VALUES (?,?,?,?)");
            mMapsDatabase.beginTransaction();
            cursor = database.query(TABLE_TILES, ALL_COLUMNS_TILES, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                statement.clearBindings();
                statement.bindLong(1, cursor.getInt(0));
                statement.bindLong(2, cursor.getInt(1));
                statement.bindLong(3, cursor.getInt(2));
                statement.bindBlob(4, cursor.getBlob(3));
                statement.execute();
                if (progressListener != null) {
                    progress++;
                    progressListener.onProgressChanged(progress);
                    if (huge && progress % step == 0) {
                        mMapsDatabase.setTransactionSuccessful();
                        mMapsDatabase.endTransaction();
                        mMapsDatabase.beginTransaction();
                    }
                }
                cursor.moveToNext();
            }
            cursor.close();
            mMapsDatabase.setTransactionSuccessful();
            mMapsDatabase.endTransaction();
            logger.error("  imported tiles");

            database.close();
            setDownloaded(x, y, date);
            logger.error("Import complete");
        } catch (SQLiteException e) {
            MapTrek.getApplication().registerException(e);
            logger.error("Import failed", e);
            setDownloading(x, y, 0L, 0L);
            return false;
        } finally {
            if (mMapsDatabase.inTransaction())
                mMapsDatabase.endTransaction();
            if (progressListener != null)
                progressListener.onProgressFinished();
            //noinspection ResultOfMethodCallIgnored
            mapFile.delete();
        }
        return true;
    }

    public boolean processDownloadedHillshade(int x, int y, String filePath, @Nullable ProgressListener progressListener) {
        File mapFile = new File(filePath);
        try {
            logger.error("Importing from {}", mapFile.getName());
            SQLiteDatabase database = SQLiteDatabase.openDatabase(filePath, null, SQLiteDatabase.OPEN_READONLY);

            int total = 0, progress = 0;
            if (progressListener != null) {
                total += DatabaseUtils.queryNumEntries(database, TABLE_TILES);
                progressListener.onProgressStarted(total);
            }

            // copy tiles
            SQLiteStatement statement = mHillshadeDatabase.compileStatement("REPLACE INTO " + TABLE_TILES + " VALUES (?,?,?,?)");
            mHillshadeDatabase.beginTransaction();
            Cursor cursor = database.query(TABLE_TILES, ALL_COLUMNS_TILES, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                statement.clearBindings();
                statement.bindLong(1, cursor.getInt(0));
                statement.bindLong(2, cursor.getInt(1));
                statement.bindLong(3, cursor.getInt(2));
                statement.bindBlob(4, cursor.getBlob(3));
                statement.execute();
                if (progressListener != null) {
                    progress++;
                    progressListener.onProgressChanged(progress);
                }
                cursor.moveToNext();
            }
            cursor.close();
            mHillshadeDatabase.setTransactionSuccessful();
            mHillshadeDatabase.endTransaction();
            logger.error("  imported tiles");

            byte version = 0;
            cursor = database.query(TABLE_INFO, new String[]{COLUMN_INFO_VALUE}, WHERE_INFO_NAME, new String[]{"version"}, null, null, null);
            if (cursor.moveToFirst())
                version = (byte) Integer.valueOf(cursor.getString(0)).intValue();
            cursor.close();
            database.close();
            if (!mHasHillshades) {
                Configuration.setHillshadesEnabled(true);
                mHasHillshades = true;
            }
            setHillshadeDownloaded(x, y, version);
        } catch (SQLiteException e) {
            MapTrek.getApplication().registerException(e);
            logger.error("Import failed", e);
            return false;
        } finally {
            if (mHillshadeDatabase.inTransaction())
                mHillshadeDatabase.endTransaction();
            if (progressListener != null)
                progressListener.onProgressFinished();
            //noinspection ResultOfMethodCallIgnored
            mapFile.delete();
        }
        return true;
    }

    public IndexStats getMapStats() {
        IndexStats stats = new IndexStats();
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++) {
                MapStatus mapStatus = getNativeMap(x, y);
                if (mapStatus.action == ACTION.DOWNLOAD) {
                    stats.download++;
                    if (mHasDownloadSizes) {
                        stats.downloadSize += mapStatus.downloadSize;
                        if (mAccountHillshades)
                            stats.downloadSize += mapStatus.hillshadeDownloadSize;
                    }
                }
                if (mapStatus.action == ACTION.REMOVE)
                    stats.remove++;
                if (mapStatus.downloading != 0L)
                    stats.downloading++;
            }
        stats.loaded = mLoadedMaps;

        return stats;
    }

    public void downloadBaseMap() {
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority("trekarta.info")
                .appendPath("maps")
                .appendPath(BASEMAP_FILENAME)
                .build();
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(mContext.getString(R.string.baseMapTitle));
        request.setDescription(mContext.getString(R.string.app_name));
        File root = new File(mMapsDatabase.getPath()).getParentFile();
        File file = new File(root, BASEMAP_FILENAME);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        request.setDestinationInExternalFilesDir(mContext, root.getName(), BASEMAP_FILENAME);
        request.setVisibleInDownloadsUi(false);
        mDownloadManager.enqueue(request);
    }

    public void manageNativeMaps(boolean hillshadesEnabled) {
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++) {
                MapStatus mapStatus = getNativeMap(x, y);
                if (mapStatus.action == ACTION.NONE)
                    continue;
                if (mapStatus.action == ACTION.REMOVE) {
                    Intent deleteIntent = new Intent(Intent.ACTION_DELETE, null, mContext, MapService.class);
                    deleteIntent.putExtra(MapService.EXTRA_X, x);
                    deleteIntent.putExtra(MapService.EXTRA_Y, y);
                    mContext.startService(deleteIntent);
                    mapStatus.action = ACTION.NONE;
                    continue;
                }
                long mapDownloadId = requestDownload(x, y, false);
                long hillshadeDownloadId = 0L;
                if (hillshadesEnabled && mapStatus.hillshadeDownloadVersion > mapStatus.hillshadeVersion)
                    hillshadeDownloadId = requestDownload(x, y, true);
                setDownloading(x, y, mapDownloadId, hillshadeDownloadId);
                mapStatus.action = ACTION.NONE;
            }
    }

    private long requestDownload(int x, int y, boolean hillshade) {
        String ext = hillshade ? "mbtiles" : "mtiles";
        String fileName = String.format(Locale.ENGLISH, "%d-%d.%s", x, y, ext);
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority("trekarta.info")
                .appendPath(hillshade ? "hillshades" : "maps")
                .appendPath(String.valueOf(x))
                .appendPath(fileName)
                .build();
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(mContext.getString(hillshade ? R.string.hillshadeTitle : R.string.mapTitle, x, y));
        request.setDescription(mContext.getString(R.string.app_name));
        File root = new File(mMapsDatabase.getPath()).getParentFile();
        File file = new File(root, fileName);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        request.setDestinationInExternalFilesDir(mContext, root.getName(), fileName);
        request.setVisibleInDownloadsUi(false);
        return mDownloadManager.enqueue(request);
    }

    private void setDownloaded(int x, int y, short date) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MAPS_DATE, date);
        values.put(COLUMN_MAPS_DOWNLOADING, 0L);
        int updated = mMapsDatabase.update(TABLE_MAPS, values, WHERE_MAPS_XY,
                new String[]{String.valueOf(x), String.valueOf(y)});
        if (updated == 0) {
            values.put(COLUMN_MAPS_X, x);
            values.put(COLUMN_MAPS_Y, y);
            mMapsDatabase.insert(TABLE_MAPS, null, values);
        }
        if (x == -1 && y == -1) {
            mBaseMapVersion = date;
        } else if (x >= 0 && y >= 0) {
            MapStatus mapStatus = getNativeMap(x, y);
            mapStatus.created = date;
            mapStatus.downloading = 0L;
            for (WeakReference<MapStateListener> weakRef : mMapStateListeners) {
                MapStateListener mapStateListener = weakRef.get();
                if (mapStateListener != null) {
                    mapStateListener.onStatsChanged();
                }
            }
        }
    }

    private void setHillshadeDownloaded(int x, int y, byte version) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MAPS_VERSION, version);
        values.put(COLUMN_MAPS_HILLSHADE_DOWNLOADING, 0L);
        int updated = mMapsDatabase.update(TABLE_MAPS, values, WHERE_MAPS_XY,
                new String[]{String.valueOf(x), String.valueOf(y)});
        if (updated == 0) {
            values.put(COLUMN_MAPS_X, x);
            values.put(COLUMN_MAPS_Y, y);
            mMapsDatabase.insert(TABLE_MAPS, null, values);
        }
        MapStatus mapStatus = getNativeMap(x, y);
        mapStatus.hillshadeVersion = version;
        mapStatus.hillshadeDownloading = 0L;
        for (WeakReference<MapStateListener> weakRef : mMapStateListeners) {
            MapStateListener mapStateListener = weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onStatsChanged();
            }
        }
    }

    private void setDownloading(final int x, final int y, final long enqueue, long hillshadeEnquire) {
        // do not block if another map is being imported
        new Thread(() -> {
            ContentValues values = new ContentValues();
            values.put(COLUMN_MAPS_DOWNLOADING, enqueue);
            int updated = mMapsDatabase.update(TABLE_MAPS, values, WHERE_MAPS_XY,
                    new String[]{String.valueOf(x), String.valueOf(y)});
            if (updated == 0) {
                values.put(COLUMN_MAPS_X, x);
                values.put(COLUMN_MAPS_Y, y);
                mMapsDatabase.insert(TABLE_MAPS, null, values);
            }
        }).start();
        if (x < 0 || y < 0)
            return;
        MapStatus mapStatus = getNativeMap(x, y);
        mapStatus.downloading = enqueue;
        mapStatus.hillshadeDownloading = hillshadeEnquire;
        for (WeakReference<MapStateListener> weakRef : mMapStateListeners) {
            MapStateListener mapStateListener = weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onStatsChanged();
            }
        }
    }

    private int checkDownloadStatus(long enqueue) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(enqueue);
        Cursor c = mDownloadManager.query(query);
        int status = 0;
        if (c.moveToFirst())
            status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
        c.close();
        return status;
    }

    public boolean hasDownloadSizes() {
        return mHasDownloadSizes;
    }

    public boolean expiredDownloadSizes() {
        return mExpiredDownloadSizes;
    }

    public void setHasDownloadSizes(boolean expired) {
        mHasDownloadSizes = true;
        mExpiredDownloadSizes = expired;
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++) {
                MapStatus mapStatus = getNativeMap(x, y);
                if (mapStatus.action == ACTION.DOWNLOAD) {
                    if (mapStatus.downloadSize == 0L)
                        selectNativeMap(x, y, ACTION.NONE);
                }
            }
        for (WeakReference<MapStateListener> weakRef : mMapStateListeners) {
            MapStateListener mapStateListener = weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onHasDownloadSizes();
            }
        }
    }

    public void addMapStateListener(MapStateListener listener) {
        mMapStateListeners.add(new WeakReference<>(listener));
    }

    public void removeMapStateListener(MapStateListener listener) {
        for (Iterator<WeakReference<MapStateListener>> iterator = mMapStateListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<MapStateListener> weakRef = iterator.next();
            if (weakRef.get() == listener) {
                iterator.remove();
            }
        }
    }

    public static Uri getIndexUri() {
        return new Uri.Builder()
                .scheme("https")
                .authority("trekarta.info")
                .appendPath("maps")
                .appendPath("index")
                .build();
    }

    public static Uri getHillshadeIndexUri() {
        return new Uri.Builder()
                .scheme("https")
                .authority("trekarta.info")
                .appendPath("hillshades")
                .appendPath("index")
                .build();
    }

    public int getMapsCount() {
        return mLoadedMaps;
    }

    @SuppressWarnings("WeakerAccess")
    public static class MapStatus {
        public short created = 0;
        public short downloadCreated;
        public long downloadSize;
        public long downloading;
        public byte hillshadeVersion = 0;
        public byte hillshadeDownloadVersion;
        public long hillshadeDownloadSize;
        public long hillshadeDownloading;
        public ACTION action = ACTION.NONE;
    }

    public static class IndexStats {
        public int loaded = 0;
        public int download = 0;
        public int remove = 0;
        public int downloading = 0;
        public long downloadSize = 0L;
    }

    public interface MapStateListener {
        void onHasDownloadSizes();

        void onStatsChanged();

        void onHillshadeAccountingChanged(boolean account);

        void onMapSelected(int x, int y, Index.ACTION action, Index.IndexStats stats);
    }
}
