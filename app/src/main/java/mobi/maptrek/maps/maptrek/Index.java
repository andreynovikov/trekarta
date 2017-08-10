package mobi.maptrek.maps.maptrek;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import mobi.maptrek.BasePaymentActivity;
import mobi.maptrek.R;

import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_FEATURES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_FEATURE_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_MAPS;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.ALL_COLUMNS_TILES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_INFO_VALUE;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_DATE;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_DOWNLOADING;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_X;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.COLUMN_MAPS_Y;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_FEATURES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_FEATURE_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_INFO;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_MAPS;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_NAMES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.TABLE_TILES;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.WHERE_INFO_NAME;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.WHERE_MAPS_PRESENT;
import static mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper.WHERE_MAPS_XY;

public class Index {
    private static final Logger logger = LoggerFactory.getLogger(Index.class);

    public enum ACTION {NONE, DOWNLOAD, CANCEL, REMOVE}

    private final Context mContext;
    private SQLiteDatabase mDatabase;
    private final DownloadManager mDownloadManager;
    private MapStatus[][] mMaps = new MapStatus[128][128];
    private boolean mHasDownloadSizes;
    private int mMapsLimit = BasePaymentActivity.MAPS_LIMIT;
    private int mLoadedMaps = 0;

    private final Set<WeakReference<MapStateListener>> mMapStateListeners = new HashSet<>();

    public Index(Context context, SQLiteDatabase database) {
        mContext = context;
        mDatabase = database;
        mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        Cursor cursor = mDatabase.query(TABLE_MAPS, ALL_COLUMNS_MAPS, WHERE_MAPS_PRESENT, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int x = cursor.getInt(cursor.getColumnIndex(COLUMN_MAPS_X));
            int y = cursor.getInt(cursor.getColumnIndex(COLUMN_MAPS_Y));
            short date = cursor.getShort(cursor.getColumnIndex(COLUMN_MAPS_DATE));
            long downloading = cursor.getLong(cursor.getColumnIndex(COLUMN_MAPS_DOWNLOADING));
            MapStatus mapStatus = getNativeMap(x, y);
            mapStatus.created = date;
            logger.debug("index({}, {}, {})", x, y, date);
            int status = checkDownloadStatus(downloading);
            if (status == DownloadManager.STATUS_PAUSED
                    || status == DownloadManager.STATUS_PENDING
                    || status == DownloadManager.STATUS_RUNNING) {
                mapStatus.downloading = downloading;
                logger.debug("  downloading: {}", downloading);
            } else {
                mapStatus.downloading = 0L;
                setDownloading(x, y, 0L);
                logger.debug("  cleared");
            }
            if (date > 0L)
                mLoadedMaps++;
            cursor.moveToNext();
        }
        cursor.close();
    }

    /**
     * Returns native map for a specified square.
     */
    @SuppressLint("DefaultLocale")
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
                if (mHasDownloadSizes)
                    stats.downloadSize -= mapStatus.downloadSize;
                if (stats.remaining >= 0)
                    stats.remaining++;
            }
            if (action == ACTION.REMOVE) {
                stats.remove--;
                if (stats.remaining > 0)
                    stats.remaining--;
            }
        } else if (action != ACTION.DOWNLOAD || stats.remaining != 0) {
            mapStatus.action = action;
            if (action == ACTION.DOWNLOAD) {
                stats.download++;
                if (mHasDownloadSizes)
                    stats.downloadSize += mapStatus.downloadSize;
                if (stats.remaining > 0)
                    stats.remaining--;
            }
            if (action == ACTION.REMOVE) {
                stats.remove++;
                if (stats.remaining >= 0)
                    stats.remaining++;
            }
        }
        for (WeakReference<MapStateListener> weakRef : mMapStateListeners) {
            MapStateListener mapStateListener = weakRef.get();
            if (mapStateListener != null) {
                mapStateListener.onMapSelected(x, y, mapStatus.action, stats);
            }
        }
    }

    private void removeNativeMap(int x, int y) {
        if (mMaps[x][y] == null)
            return;
        if (mMaps[x][y].created == 0)
            return;
    }

    public void setNativeMapStatus(int x, int y, short date, long size) {
        if (mMaps[x][y] == null)
            getNativeMap(x, y);
        mMaps[x][y].downloadCreated = date;
        mMaps[x][y].downloadSize = size;
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
        map.downloading = 0L;
        setDownloading(x, y, 0L);
        selectNativeMap(x, y, ACTION.NONE);
    }

    public boolean isDownloading(int x, int y) {
        return mMaps[x][y] != null && mMaps[x][y].downloading != 0L;
    }

    public boolean processDownloadedMap(int x, int y, String filePath) {
        File mapFile = new File(filePath);
        try {
            logger.error("Start import from {}", mapFile.getName());
            SQLiteDatabase database = SQLiteDatabase.openDatabase(filePath, null, SQLiteDatabase.OPEN_READONLY);
            //TODO Perform data move

            // copy names
            SQLiteStatement statement = mDatabase.compileStatement("REPLACE INTO " + TABLE_NAMES + " VALUES (?,?);");
            Cursor cursor = database.query(TABLE_NAMES, ALL_COLUMNS_NAMES, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                statement.clearBindings();
                statement.bindLong(1, cursor.getLong(0));
                statement.bindString(2, cursor.getString(1));
                statement.execute();
                cursor.moveToNext();
            }
            cursor.close();
            logger.error("  imported names");

            // copy features
            statement = mDatabase.compileStatement("REPLACE INTO " + TABLE_FEATURES + " VALUES (?,?,?,?);");
            cursor = database.query(TABLE_FEATURES, ALL_COLUMNS_FEATURES, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                statement.clearBindings();
                statement.bindLong(1, cursor.getLong(0));
                statement.bindLong(2, cursor.getInt(1));
                statement.bindDouble(3, cursor.getDouble(2));
                statement.bindDouble(4, cursor.getDouble(3));
                statement.execute();
                cursor.moveToNext();
            }
            cursor.close();
            logger.error("  imported features");

            // copy feature names
            statement = mDatabase.compileStatement("REPLACE INTO " + TABLE_FEATURE_NAMES + " VALUES (?,?,?);");
            cursor = database.query(TABLE_FEATURE_NAMES, ALL_COLUMNS_FEATURE_NAMES, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                statement.clearBindings();
                statement.bindLong(1, cursor.getLong(0));
                statement.bindLong(2, cursor.getInt(1));
                statement.bindLong(3, cursor.getLong(2));
                statement.execute();
                cursor.moveToNext();
            }
            cursor.close();
            logger.error("  imported feature names");

            // copy tiles
            statement = mDatabase.compileStatement("REPLACE INTO " + TABLE_TILES + " VALUES (?,?,?,?);");
            //mDatabase.beginTransaction();

            cursor = database.query(TABLE_TILES, ALL_COLUMNS_TILES, null, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                statement.clearBindings();
                statement.bindLong(1, cursor.getInt(0));
                statement.bindLong(2, cursor.getInt(1));
                statement.bindLong(3, cursor.getInt(2));
                statement.bindBlob(4, cursor.getBlob(3));
                statement.execute();

                cursor.moveToNext();
            }
            cursor.close();
            //mDatabase.setTransactionSuccessful();
            logger.error("  imported tiles");

            short date = 0;
            cursor = database.query(TABLE_INFO, new String[]{COLUMN_INFO_VALUE}, WHERE_INFO_NAME, new String[]{"timestamp"}, null, null, null);
            if (cursor.moveToFirst()) {
                date = Short.valueOf(cursor.getString(0));
            }
            cursor.close();
            database.close();
            setDownloaded(x, y, date);
        } catch (SQLiteException e) {
            logger.error("Import failed", e);
            return false;
        } finally {
            //mDatabase.endTransaction();
            //noinspection ResultOfMethodCallIgnored
            mapFile.delete();
        }
        return true;
    }

    public static int getNativeKey(int x, int y) {
        return (x << 7) + y;
    }

    public IndexStats getMapStats() {
        IndexStats stats = new IndexStats();
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++) {
                MapStatus mapStatus = getNativeMap(x, y);
                if (mapStatus.action == ACTION.DOWNLOAD) {
                    stats.download++;
                    if (mHasDownloadSizes)
                        stats.downloadSize += mapStatus.downloadSize;
                }
                if (mapStatus.action == ACTION.REMOVE)
                    stats.remove++;
                if (mapStatus.downloading != 0L)
                    stats.downloading++;
            }
        stats.loaded = mLoadedMaps;
        if (mMapsLimit != Integer.MAX_VALUE)
            stats.remaining = mMapsLimit - stats.loaded - stats.download - stats.downloading + stats.remove;
        else
            stats.remaining = -1;

        return stats;
    }

    public boolean manageNativeMaps() {
        boolean removed = false;
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++) {
                MapStatus mapStatus = getNativeMap(x, y);
                if (mapStatus.action == ACTION.NONE)
                    continue;
                if (mapStatus.action == ACTION.REMOVE) {
                    removeNativeMap(x, y);
                    mapStatus.action = ACTION.NONE;
                    removed = true;
                    continue;
                }
                String fileName = String.format(Locale.ENGLISH, "%d-%d.mtiles", x, y);
                Uri uri = new Uri.Builder()
                        .scheme("http")
                        .authority("maptrek.mobi")
                        .appendPath("maps")
                        .appendPath(String.valueOf(x))
                        .appendPath(fileName)
                        .build();
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setTitle(mContext.getString(R.string.mapTitle, x, y));
                request.setDescription(mContext.getString(R.string.app_name));
                File root = new File(mDatabase.getPath()).getParentFile();
                File file = new File(root, fileName);
                if (file.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
                request.setDestinationInExternalFilesDir(mContext, root.getName(), fileName);
                request.setVisibleInDownloadsUi(false);
                setDownloading(x, y, mDownloadManager.enqueue(request));
                mapStatus.action = ACTION.NONE;
            }
        return removed;
    }

    private void setDownloaded(int x, int y, short date) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MAPS_DATE, date);
        values.put(COLUMN_MAPS_DOWNLOADING, 0L);
        int updated = mDatabase.update(TABLE_MAPS, values, WHERE_MAPS_XY,
                new String[]{String.valueOf(x), String.valueOf(y)});
        if (updated == 0) {
            values.put(COLUMN_MAPS_X, x);
            values.put(COLUMN_MAPS_Y, y);
            mDatabase.insert(TABLE_MAPS, null, values);
        }
        getNativeMap(x, y).created = date;
    }

    private void setDownloading(int x, int y, long enqueue) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MAPS_DOWNLOADING, enqueue);
        int updated = mDatabase.update(TABLE_MAPS, values, WHERE_MAPS_XY,
                new String[]{String.valueOf(x), String.valueOf(y)});
        if (updated == 0) {
            values.put(COLUMN_MAPS_X, x);
            values.put(COLUMN_MAPS_Y, y);
            mDatabase.insert(TABLE_MAPS, null, values);
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

    public void setHasDownloadSizes(boolean hasSizes) {
        mHasDownloadSizes = hasSizes;
        if (hasSizes) {
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

    @SuppressLint("DefaultLocale")
    public static Uri getIndexUri() {
        return new Uri.Builder()
                .scheme("http")
                .authority("maptrek.mobi")
                .appendPath("maps")
                .appendPath("nativeindex")
                .build();
    }

    public void setLimit(int limit) {
        //TODO Implement
    }

    public static class MapStatus {
        public short created = 0;
        public short downloadCreated;
        public long downloadSize;
        public long downloading;
        public ACTION action = ACTION.NONE;
    }

    public static class IndexStats {
        public int loaded = 0;
        public int download = 0;
        public int remove = 0;
        public int downloading = 0;
        public int remaining = 0;
        public long downloadSize = 0L;
    }

    public interface MapStateListener {
        void onHasDownloadSizes();

        void onStatsChanged(Index.IndexStats stats);

        void onMapSelected(int x, int y, Index.ACTION action, Index.IndexStats stats);
    }
}
