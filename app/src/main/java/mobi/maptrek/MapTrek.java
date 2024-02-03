/*
 * Copyright 2024 Andrey Novikov
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

package mobi.maptrek;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.LongSparseArray;

import com.google.common.util.concurrent.ListenableFuture;

import org.greenrobot.eventbus.EventBus;
import org.oscim.theme.styles.TextStyle;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;
import org.oscim.utils.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.maps.MapWorker;
import mobi.maptrek.maps.maptrek.HillshadeDatabaseHelper;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.maps.maptrek.MapTrekDatabaseHelper;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.plugin.PluginRepository;
import mobi.maptrek.util.LongSparseArrayIterator;
import mobi.maptrek.util.NativeMapFilenameFilter;
import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.SafeResultReceiver;
import mobi.maptrek.util.ShieldFactory;
import mobi.maptrek.util.StringFormatter;

public class MapTrek extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MapTrek.class);
    private static final String EXCEPTION_PATH = "exception.txt";
    private static final String DEBUG_PATH = "debug.log";

    private static MapTrek mSelf;
    private File mExceptionLog;
    private DefaultExceptionHandler mExceptionHandler;

    public static float density = 1f;
    public static float ydpi = 160f;
    public static int versionCode = 0;

    public static boolean isMainActivityRunning = false;
    private boolean mMainActivityExists = false;

    private Index mIndex;
    private MapIndex mExtraMapIndex;
    private MapTrekDatabaseHelper mDetailedMapHelper;
    private SQLiteDatabase mDetailedMapDatabase;
    private HillshadeDatabaseHelper mHillshadeHelper;
    private SQLiteDatabase mHillshadeDatabase;
    private WaypointDbDataSource mWaypointDbDataSource;
    private ShieldFactory mShieldFactory;
    private OsmcSymbolFactory mOsmcSymbolFactory;
    private String mUserNotification;
    private SafeResultReceiver mResultReceiver;
    private Waypoint mEditedWaypoint;
    private List<MapFile> mBitmapLayerMaps;
    private PluginRepository mPluginRepository;

    private static final LongSparseArray<MapObject> mapObjects = new LongSparseArray<>();

    // Configure global defaults
    static {
        Parameters.CUSTOM_TILE_SIZE = true;
        Parameters.MAP_EVENT_LAYER2 = true;
        //Parameters.TEXTURE_ATLAS = true;
        Parameters.POT_TEXTURES = true;
        Parameters.TRANSPARENT_LINES = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        logger.info("Trekarta application starting");
        mSelf = this;

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        Configuration.initialize(PreferenceManager.getDefaultSharedPreferences(this));

        File cacheDir = getExternalCacheDir();
        File exportDir = new File(cacheDir, "export");
        if (!exportDir.exists())
            //noinspection ResultOfMethodCallIgnored
            exportDir.mkdir();
        mExceptionLog = new File(exportDir, EXCEPTION_PATH);
        mExceptionHandler = new DefaultExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mExceptionHandler);
        configureDebugLogging();

        registerActivityLifecycleCallbacks(new ListeningToActivityCallbacks());

        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            logger.error("Failed to get version", e);
        }

        initializeSettings();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        density = metrics.density;
        ydpi = metrics.ydpi;

        TextStyle.MAX_TEXT_WIDTH = (int) (density * 220);

        // Configure work manager to execute one job at a time to import and remove maps sequentially
        androidx.work.Configuration configuration =
                new androidx.work.Configuration.Builder()
                        .setExecutor(Executors.newFixedThreadPool(1))
                        .build();
        WorkManager.initialize(getApplicationContext(), configuration);

        if (Build.VERSION.SDK_INT > 25)
            createNotificationChannel();

        mapObjects.clear();

        AppCompatDelegate.setDefaultNightMode(Configuration.getNightModeState());

        if (BuildConfig.DEBUG) {
            findDebugMaps();
        }
    }

    private void initializeSettings() {
        Resources resources = getResources();
        int unit = Configuration.getSpeedUnit();
        StringFormatter.speedFactor = Float.parseFloat(resources.getStringArray(R.array.speed_factors)[unit]);
        StringFormatter.speedAbbr = resources.getStringArray(R.array.speed_abbreviations)[unit];
        unit = Configuration.getDistanceUnit();
        StringFormatter.distanceFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors)[unit]);
        StringFormatter.distanceAbbr = resources.getStringArray(R.array.distance_abbreviations)[unit];
        StringFormatter.distanceShortFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors_short)[unit]);
        StringFormatter.distanceShortAbbr = resources.getStringArray(R.array.distance_abbreviations_short)[unit];
        unit = Configuration.getElevationUnit();
        StringFormatter.elevationFactor = Float.parseFloat(resources.getStringArray(R.array.elevation_factors)[unit]);
        StringFormatter.elevationAbbr = resources.getStringArray(R.array.elevation_abbreviations)[unit];
        unit = Configuration.getAngleUnit();
        StringFormatter.angleFactor = Double.parseDouble(resources.getStringArray(R.array.angle_factors)[unit]);
        StringFormatter.angleAbbr = resources.getStringArray(R.array.angle_abbreviations)[unit];
        boolean precision = Configuration.getUnitPrecision();
        StringFormatter.precisionFormat = precision ? "%.1f" : "%.0f";
        StringFormatter.coordinateFormat = Configuration.getCoordinatesFormat();
        Configuration.loadKindZoomState();
        Tags.recalculateTypeZooms();
    }

    public static MapTrek getApplication() {
        return mSelf;
    }

    /** @noinspection unused*/
    public void restart() {
        logger.info("Application restart initiated");
        Configuration.commit();
        //Context ctx = getApplicationContext();
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            Intent mainIntent = Intent.makeRestartActivityTask(intent.getComponent());
            startActivity(mainIntent);
        }
        Runtime.getRuntime().exit(0);
    }

    @TargetApi(26)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("ongoing",
                getString(R.string.notificationChannelName), NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        channel.setSound(null, null);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.createNotificationChannel(channel);
    }

    public synchronized SQLiteDatabase getDetailedMapDatabase() throws IOException {
        if (mDetailedMapHelper == null) {
            File dbFile = new File(getExternalFilesDir("native"), Index.WORLDMAP_FILENAME);
            boolean fresh = !dbFile.exists();
            if (fresh)
                copyAsset("databases/" + Index.BASEMAP_FILENAME, dbFile);
            mDetailedMapHelper = new MapTrekDatabaseHelper(this, dbFile);
            mDetailedMapHelper.setWriteAheadLoggingEnabled(true);
            try {
                mDetailedMapDatabase = mDetailedMapHelper.getWritableDatabase();
            } catch (SQLiteCantOpenDatabaseException e) {
                logger.error("Failed to open map file", e);
                mDetailedMapHelper.close();
            } catch (SQLiteException e) {
                logger.error("Detailed map error", e);
                mDetailedMapHelper.close();
                if (dbFile.delete()) {
                    copyAsset("databases/" + Index.BASEMAP_FILENAME, dbFile);
                    mDetailedMapHelper = new MapTrekDatabaseHelper(this, dbFile);
                    mDetailedMapHelper.setWriteAheadLoggingEnabled(true);
                    mDetailedMapDatabase = mDetailedMapHelper.getWritableDatabase();
                    fresh = true;
                    mUserNotification = getString(R.string.msgMapDatabaseError);
                }
            }
            if (fresh && mDetailedMapDatabase != null)
                MapTrekDatabaseHelper.createFtsTable(mDetailedMapDatabase);
        }
        return mDetailedMapDatabase;
    }

    private synchronized HillshadeDatabaseHelper getHillshadeDatabaseHelper(boolean reset) {
        if (mHillshadeHelper == null) {
            File file = new File(getExternalFilesDir("native"), Index.HILLSHADE_FILENAME);
            if (reset)
                logger.error("Hillshade database deleted: {}", file.delete());
            mHillshadeHelper = new HillshadeDatabaseHelper(this, file);
            mHillshadeHelper.setWriteAheadLoggingEnabled(true);
        }
        return mHillshadeHelper;
    }

    public synchronized SQLiteDatabase getHillshadeDatabase() {
        if (mHillshadeDatabase == null) {
            try {
                mHillshadeDatabase = getHillshadeDatabaseHelper(false).getWritableDatabase();
            } catch (SQLiteCantOpenDatabaseException ignore) {
                logger.error("Failed to open hillshade file");
                mHillshadeHelper.close();
                mHillshadeHelper = null;
            } catch (SQLiteException ignore) {
                mHillshadeHelper.close();
                mHillshadeHelper = null;
                mHillshadeDatabase = getHillshadeDatabaseHelper(true).getWritableDatabase();
                mUserNotification = getString(R.string.msgHillshadeDatabaseError);
            }
        }
        return mHillshadeDatabase;
    }

    public @Nullable
    SQLiteTileSource getHillShadeTileSource() {
        SQLiteTileSource tileSource = new SQLiteTileSource(getHillshadeDatabaseHelper(false));
        TileSource.OpenResult result = tileSource.open();
        if (result.isSuccess())
            return tileSource;
        else
            return null;
    }

    public Index getMapIndex() {
        if (mIndex == null)
            try {
                mIndex = new Index(this, getDetailedMapDatabase(), getHillshadeDatabase());
            } catch (IOException e) {
                logger.error("Couldn't open map database", e);
            }
        return mIndex;
    }

    public MapIndex getExtraMapIndex() {
        if (mExtraMapIndex == null) {
            mExtraMapIndex = new MapIndex(getExternalFilesDir("maps"));
            mExtraMapIndex.initializeMapProviders(this, getPackageManager());
        }
        return mExtraMapIndex;
    }

    public synchronized WaypointDbDataSource getWaypointDbDataSource() {
        if (mWaypointDbDataSource == null) {
            File waypointsFile = new File(getExternalFilesDir("databases"), "waypoints.sqlitedb");
            mWaypointDbDataSource = new WaypointDbDataSource(this, waypointsFile);
        }
        return mWaypointDbDataSource;
    }

    public ShieldFactory getShieldFactory() {
        if (mShieldFactory == null)
            mShieldFactory = new ShieldFactory();
        return mShieldFactory;
    }

    public OsmcSymbolFactory getOsmcSymbolFactory() {
        if (mOsmcSymbolFactory == null)
            mOsmcSymbolFactory = new OsmcSymbolFactory();
        return mOsmcSymbolFactory;
    }

    public PluginRepository getPluginRepository() {
        if (mPluginRepository == null) {
            mPluginRepository = new PluginRepository(this);
            mPluginRepository.initializePlugins();
        }
        return mPluginRepository;
    }

    private void configureDebugLogging() {
        File debugLog = new File(mExceptionLog.getParentFile(), DEBUG_PATH);

        // Remove previous debug log if no exception was registered on previous run
        long size = Configuration.getExceptionSize();
        if (!mExceptionLog.exists() || mExceptionLog.length() == 0L || size == mExceptionLog.length()) {
            if (debugLog.isFile())
                //noinspection ResultOfMethodCallIgnored
                debugLog.delete();
        }

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setPattern("%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n");
        encoder.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(lc);
        fileAppender.setFile(debugLog.getAbsolutePath());
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(fileAppender);
    }

    private void findDebugMaps() {
        // Look for test maps and import them
        File dir = getExternalFilesDir("native");
        if (dir == null)
            return;
        File[] mapFiles = dir.listFiles(new NativeMapFilenameFilter());
        if (mapFiles == null)
            return;
        for (File mapFile : mapFiles) {
            if (mapFile.getName().matches("\\d+-\\d+\\.mtiles")) {
                logger.debug("Found debug map: {}", mapFile.getAbsolutePath());
                Data data = new Data.Builder()
                        .putString(MapWorker.KEY_ACTION, Intent.ACTION_INSERT)
                        .putString(MapWorker.KEY_FILE_URI, mapFile.getAbsolutePath())
                        .build();
                OneTimeWorkRequest importWorkRequest = new OneTimeWorkRequest.Builder(MapWorker.class)
                        .setInputData(data)
                        .build();
                WorkManager.getInstance(this).enqueue(importWorkRequest);
            }
        }

    }

    @Nullable
    public SafeResultReceiver getResultReceiver() {
        return mResultReceiver;
    }

    public void setResultReceiver(@NonNull SafeResultReceiver resultReceiver) {
        mResultReceiver = resultReceiver;
    }

    // TODO Introduce State subclass for state saving
    public Waypoint getEditedWaypoint() {
        return mEditedWaypoint;
    }

    public void setEditedWaypoint(Waypoint waypoint) {
        mEditedWaypoint = waypoint;
    }

    @Nullable
    public List<MapFile> getBitmapLayerMaps() {
        return mBitmapLayerMaps;
    }

    public void setBitmapLayerMaps(@NonNull List<MapFile> bitmapLayerMaps) {
        mBitmapLayerMaps = bitmapLayerMaps;
    }

    @SuppressWarnings("SameParameterValue")
    private void copyAsset(String asset, File outFile) throws IOException {
        logger.debug("copyAsset({})", asset);
        InputStream in = getAssets().open(asset);
        //noinspection IOStreamConstructor
        OutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    public boolean hasPreviousRunsExceptions() {
        long size = Configuration.getExceptionSize();
        if (mExceptionLog.exists() && mExceptionLog.length() > 0L) {
            if (size != mExceptionLog.length()) {
                Configuration.setExceptionSize(mExceptionLog.length());
                return true;
            }
        } else {
            if (size > 0L) {
                Configuration.setExceptionSize(0L);
            }
        }
        return false;
    }

    public String getUserNotification() {
        String notification = mUserNotification;
        mUserNotification = null;
        return notification;
    }

    synchronized public void removeMapDatabase() {
        // close databases
        if (mHillshadeHelper != null) {
            mHillshadeHelper.close();
            mHillshadeHelper = null;
            mHillshadeDatabase = null;
        }
        if (mDetailedMapHelper != null) {
            mDetailedMapHelper.close();
            mDetailedMapHelper = null;
            mDetailedMapDatabase = null;
        }
        File dbFile = new File(getExternalFilesDir("native"), Index.WORLDMAP_FILENAME);
        logger.info("Detailed map database deleted: {}", dbFile.delete());
        File hsFile = new File(getExternalFilesDir("native"), Index.HILLSHADE_FILENAME);
        logger.info("Hillshade database deleted: {}", hsFile.delete());
    }

    synchronized public void optionallyCloseMapDatabase(UUID id) {
        WorkManager workManager = WorkManager.getInstance(this);
        ListenableFuture<List<WorkInfo>> workInfos = workManager.getWorkInfosByTag(MapWorker.TAG);
        boolean hasWorks = false;
        try {
            for (WorkInfo workInfo : workInfos.get()) {
                WorkInfo.State state = workInfo.getState();
                if (workInfo.getId().equals(id))
                    continue;
                hasWorks = hasWorks || (state == WorkInfo.State.RUNNING | state == WorkInfo.State.ENQUEUED);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        logger.debug("optionallyCloseMapDatabase activityExists: {}, hasWorks: {}", mMainActivityExists, hasWorks);
        if (!mMainActivityExists && !hasWorks) {
            // close databases
            if (mHillshadeHelper != null) {
                mHillshadeHelper.close();
                mHillshadeHelper = null;
                mHillshadeDatabase = null;
            }
            if (mDetailedMapHelper != null) {
                mDetailedMapHelper.close();
                mDetailedMapHelper = null;
                mDetailedMapDatabase = null;
            }
        }
    }

    public void onMainActivityFinished() {
        logger.info("MainActivity finished");
        optionallyCloseMapDatabase(null);
        if (mWaypointDbDataSource != null) {
            mWaypointDbDataSource.close();
            mWaypointDbDataSource = null;
        }
        // free indexes
        if (mExtraMapIndex != null) {
            mExtraMapIndex.clear();
            mExtraMapIndex = null;
        }
        mEditedWaypoint = null;
        mBitmapLayerMaps = null;
        mIndex = null;
    }

    /****************************
     * Map objects management
     */

    public static long getNewUID() {
        return Configuration.getUID();
    }

    public static long addMapObject(MapObject mapObject) {
        mapObject._id = getNewUID();
        logger.debug("addMapObject({})", mapObject._id);
        synchronized (mapObjects) {
            mapObjects.put(mapObject._id, mapObject);
        }
        EventBus.getDefault().post(new MapObject.AddedEvent(mapObject));
        return mapObject._id;
    }

    public static boolean removeMapObject(long id) {
        synchronized (mapObjects) {
            logger.debug("removeMapObject({})", id);
            MapObject mapObject = mapObjects.get(id);
            mapObjects.delete(id);
            if (mapObject != null) {
                mapObject.setBitmap(null);
                EventBus.getDefault().post(new MapObject.RemovedEvent(mapObject));
            }
            return mapObject != null;
        }
    }

    @Nullable
    public static MapObject getMapObject(long id) {
        return mapObjects.get(id);
    }

    @NonNull
    public static Iterator<MapObject> getMapObjects() {
        return LongSparseArrayIterator.iterate(mapObjects);
    }

    /****************************
     * Exception handling
     */

    public File getExceptionLog() {
        return mExceptionLog;
    }

    public void registerException(Throwable ex) {
        mExceptionHandler.caughtException(Thread.currentThread(), ex);
    }

    private class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler defaultHandler;

        DefaultExceptionHandler() {
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        }

        void caughtException(final Thread thread, final Throwable ex) {
            try {
                StringBuilder msg = new StringBuilder();
                msg.append(DateFormat.format("dd.MM.yyyy hh:mm:ss", System.currentTimeMillis()));
                try {
                    PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                    if (info != null) {
                        msg.append("\nVersion : ")
                                .append(info.versionCode).append(" ").append(info.versionName);
                    }
                } catch (Throwable ignore) {
                }
                msg.append("\n")
                        .append("Thread : ")
                        .append(thread.toString())
                        .append("\nException :\n\n");

                File file = mExceptionLog.getParentFile();
                if (file != null && file.canWrite()) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(mExceptionLog, false));
                    writer.write(msg.toString());
                    ex.printStackTrace(new PrintWriter(writer));
                    writer.write("\n\n");
                    writer.close();
                }
            } catch (Exception e) {
                // swallow all exceptions
                logger.error("Exception while handle other exception", e);
            }
        }

        @Override
        public void uncaughtException(@NonNull final Thread thread, @NonNull final Throwable ex) {
            caughtException(thread, ex);
            defaultHandler.uncaughtException(thread, ex);
        }
    }

    class ListeningToActivityCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
            logger.warn("{} is onActivityCreated", activity.getLocalClassName());
            if (activity.getLocalClassName().equals(MainActivity.class.getSimpleName()))
                mMainActivityExists = true;
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            logger.warn("{} is onActivityStarted", activity.getLocalClassName());
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            logger.warn("{} is onActivityResumed", activity.getLocalClassName());
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            logger.warn("{} is onActivityPaused", activity.getLocalClassName());
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            logger.warn("{} is onActivityStopped", activity.getLocalClassName());
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            logger.warn("{} is onActivitySaveInstanceState", activity.getLocalClassName());
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            logger.warn("{} is onActivityDestroyed", activity.getLocalClassName());
            if (activity.isFinishing() && activity.getLocalClassName().equals(MainActivity.class.getSimpleName()))
                mMainActivityExists = false;
        }

        @Override
        public void onActivityPostDestroyed(@NonNull Activity activity) {
            if (!mMainActivityExists && activity.getLocalClassName().equals(MainActivity.class.getSimpleName()))
                onMainActivityFinished();
        }
    }
}
