/*
 * Copyright 2023 Andrey Novikov
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

package mobi.maptrek.location;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Icon;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import android.text.format.DateUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.Configuration;
import mobi.maptrek.MainActivity;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.io.Manager;
import mobi.maptrek.util.ProgressListener;
import mobi.maptrek.util.StringFormatter;

public class LocationService extends BaseLocationService implements LocationListener, GpsStatus.Listener, OnSharedPreferenceChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);

    private static final int SKIP_INITIAL_LOCATIONS = 2;
    private static final long TOO_SMALL_PERIOD = DateUtils.MINUTE_IN_MILLIS; // 1 minute
    private static final float TOO_SMALL_DISTANCE = 100f; // 100 meters
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);

    private static final int NOTIFICATION_ID = 25501;
    private static final boolean DEBUG_ERRORS = false;

    // Fake locations used for test purposes
    private static final boolean enableMockLocations = false;
    private final Handler mMockCallback = new Handler(Looper.getMainLooper());
    private int mMockLocationTicker = 0;

    // Real locations
    private LocationManager mLocationManager = null;
    private GnssStatus.Callback mGnssStatusCallback = null;
    private boolean mLocationsEnabled = false;
    private long mLastLocationMillis = 0;

    private int mGpsStatus = GPS_OFF;
    private int mTSats = 0;
    private int mFSats = 0;
    private Location mLastKnownLocation = null;
    private boolean mContinuous = false;
    private boolean mJustStarted = true;
    private float mNmeaGeoidHeight = Float.NaN;
    private float mHDOP = Float.NaN;
    private float mVDOP = Float.NaN;

    // Tracking
    private SQLiteDatabase mTrackDB = null;
    private boolean mTrackingEnabled = false;
    private boolean mForegroundTracking = false;
    private boolean mForegroundLocations = false;
    private String mErrorMsg = "";
    private long mErrorTime = 0;

    private Location mLastWrittenLocation = null;
    private float mDistanceTracked = 0f;
    /**
     * Time in milliseconds when the current track was started
     */
    private long mTrackStarted = 0;
    /**
     * Start time of tracking in milliseconds, for internal use
     */
    private long mTrackingStarted;
    private float mDistanceNotified;
    private Track mLastTrack;

    private long mMinTime = 2000; // 2 seconds (default)
    @SuppressWarnings("FieldCanBeLocal")
    private long mMaxTime = 300000; // 5 minutes
    private int mMinDistance = 3; // 3 meters (default)

    private final Binder mBinder = new LocalBinder();
    private final Set<ILocationListener> mLocationCallbacks = new HashSet<>();
    private final Set<ITrackingListener> mTrackingCallbacks = new HashSet<>();
    private ProgressListener mProgressListener;
    private final RemoteCallbackList<ILocationCallback> mLocationRemoteCallbacks = new RemoteCallbackList<>();

    private static final String PREF_TRACKING_MIN_TIME = "tracking_min_time";
    private static final String PREF_TRACKING_MIN_DISTANCE = "tracking_min_distance";

    @Override
    public void onCreate() {
        mLastKnownLocation = new Location("unknown");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            mGnssStatusCallback = new GnssStatus.Callback() {
                public void onFirstFix(int ttffMillis) {
                    onGpsFirstFix();
                }

                public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                    mTSats = status.getSatelliteCount();
                    mFSats = 0;
                    for (int i = 0; i < mTSats; i++) {
                        if (status.usedInFix(i))
                            mFSats++;
                    }
                    updateGpsStatus();
                }

                public void onStarted() {
                    onGpsStarted();
                }

                public void	onStopped() {
                    onGpsStopped();
                }
            };
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Tracking preferences
        onSharedPreferenceChanged(sharedPreferences, PREF_TRACKING_MIN_TIME);
        onSharedPreferenceChanged(sharedPreferences, PREF_TRACKING_MIN_DISTANCE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        logger.debug("Service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null)
            return START_NOT_STICKY;

        String action = intent.getAction();
        logger.error("Command: {}", action);
        if (action.equals(ENABLE_TRACK)) {
            if (!mTrackingEnabled) { // Command can be sent on activity restart, while service already is running
                mErrorMsg = "";
                mErrorTime = 0;
                mTrackingEnabled = true;
                mContinuous = false;
                mDistanceNotified = 0f;
                openDatabase();
                mTrackingStarted = SystemClock.uptimeMillis();
                mTrackStarted = System.currentTimeMillis();
                mForegroundTracking = true;
                updateDistanceTracked();
                // https://developer.android.com/training/monitoring-device-state/doze-standby#support_for_other_use_cases
                if (!mForegroundLocations)
                    if (Build.VERSION.SDK_INT < 34)
                        startForeground(NOTIFICATION_ID, getNotification());
                    else
                        startForeground(NOTIFICATION_ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            }
            sendBroadcast(new Intent(BROADCAST_TRACK_STATE)
                    .putExtra("state", TRACKING_STATE.TRACKING.ordinal())
                    .setPackage(getPackageName()));
            Configuration.setTrackingState(TRACKING_STATE.TRACKING.ordinal());
        }
        if (action.equals(DISABLE_TRACK) || action.equals(PAUSE_TRACK) && mTrackingEnabled) {
            mTrackingEnabled = false;
            mForegroundTracking = false;
            updateDistanceTracked();
            closeDatabase();
            long trackedTime = (SystemClock.uptimeMillis() - mTrackingStarted) / 60000;
            Configuration.updateTrackingTime(trackedTime);
            if (action.equals(DISABLE_TRACK)) {
                sendBroadcast(new Intent(BROADCAST_TRACK_STATE)
                        .putExtra("state", TRACKING_STATE.DISABLED.ordinal())
                        .setPackage(getPackageName()));
                Configuration.setTrackingState(TRACKING_STATE.DISABLED.ordinal());
                tryToSaveTrack();
            }
            if (action.equals(PAUSE_TRACK)) {
                sendBroadcast(new Intent(BROADCAST_TRACK_STATE)
                        .putExtra("state", TRACKING_STATE.PAUSED.ordinal())
                        .setPackage(getPackageName()));
                Configuration.setTrackingState(TRACKING_STATE.PAUSED.ordinal());
            }
            if (!mForegroundLocations) {
                stopForeground(true);
                stopSelf();
            }
        }
        if (action.equals(ENABLE_BACKGROUND_LOCATIONS)) {
            mForegroundLocations = true;
            if (!mForegroundTracking)
                if (Build.VERSION.SDK_INT < 34)
                    startForeground(NOTIFICATION_ID, getNotification());
                else
                    startForeground(NOTIFICATION_ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        }
        if (action.equals(DISABLE_BACKGROUND_LOCATIONS)) {
            mForegroundLocations = false;
            if (!mForegroundTracking)
                stopForeground(true);
        }
        updateNotification();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        updateDistanceTracked();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        disconnect();
        closeDatabase();
        logger.debug("Service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (MAPTREK_LOCATION_SERVICE.equals(intent.getAction()) || ILocationRemoteService.class.getName().equals(intent.getAction())) {
            return mLocationRemoteBinder;
        } else {
            return mBinder;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_TRACKING_MIN_TIME.equals(key)) {
            mMinTime = 500; //Integer.parseInt(sharedPreferences.getString(key, "500"));
        } else if (PREF_TRACKING_MIN_DISTANCE.equals(key)) {
            mMinDistance = 5; //Integer.parseInt(sharedPreferences.getString(key, "5"));
        }
    }

    public static boolean isGpsProviderEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        return false;
    }

    private void connect() {
        logger.debug("connect()");
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager != null) {
            mLastLocationMillis = -SKIP_INITIAL_LOCATIONS;
            mContinuous = false;
            mJustStarted = true;
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
                } else {
                    mLocationManager.addGpsStatusListener(this);
                }
                try {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_DELAY, 0, this);
                    //mLocationManager.addNmeaListener(this);
                    mLocationsEnabled = true;
                    logger.debug("Gps provider set");
                } catch (IllegalArgumentException e) {
                    logger.warn("Cannot set gps provider, likely no gps on device");
                }
            } else {
                logger.error("Missing ACCESS_FINE_LOCATION permission");
            }
        }
        if (enableMockLocations && BuildConfig.DEBUG) {
            mMockLocationTicker = 0;
            mMockCallback.post(mSendMockLocation);
            mLocationsEnabled = true;
        }
    }

    private void disconnect() {
        logger.debug("disconnect()");
        if (mLocationManager != null) {
            mLocationsEnabled = false;
            //mLocationManager.removeNmeaListener(this);
            try {
                mLocationManager.removeUpdates(this);
            } catch (SecurityException e) {
                logger.error("Failed to remove updates", e);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
            } else {
                mLocationManager.removeGpsStatusListener(this);
            }
            mLocationManager = null;
        }
        if (enableMockLocations && BuildConfig.DEBUG) {
            mLocationsEnabled = false;
            mMockCallback.removeCallbacks(mSendMockLocation);
        }
    }

    private Notification getNotification() {
        int titleId = R.string.notifLocating;
        int ntfId = R.mipmap.ic_stat_locating;
        if (mForegroundTracking) {
            titleId = R.string.notifTracking;
            ntfId = R.mipmap.ic_stat_tracking;
        }
        if (mGpsStatus != LocationService.GPS_OK) {
            titleId = R.string.notifLocationWaiting;
            ntfId = R.mipmap.ic_stat_waiting;
        }
        if (mGpsStatus == LocationService.GPS_OFF) {
            ntfId = R.mipmap.ic_stat_off;
        }
        if (mErrorTime > 0) {
            titleId = R.string.notifTrackingFailure;
            ntfId = R.mipmap.ic_stat_failure;
        }

        Notification.Builder builder = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT > 25)
            builder.setChannelId("ongoing");

        String message = null;
        if (mForegroundTracking) {
            String timeTracked = (String) DateUtils.getRelativeTimeSpanString(getApplicationContext(), mTrackStarted);
            String distanceTracked = StringFormatter.distanceH(mDistanceTracked);

            StringBuilder sb = new StringBuilder(40);
            sb.append(getString(R.string.msgTracked, distanceTracked, timeTracked));
            message = sb.toString();
            sb.insert(0, ". ");
            sb.insert(0, getString(R.string.msgTracking));
            sb.append(". ");
            sb.append(getString(R.string.msgTrackingActions));
            sb.append(".");
            String bigText = sb.toString();

            builder.setStyle(new Notification.BigTextStyle().setBigContentTitle(getText(titleId)).bigText(bigText));

            Intent iStop = new Intent(DISABLE_TRACK, null, getApplicationContext(), LocationService.class);
            PendingIntent piStop = PendingIntent.getService(this, 0, iStop, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon stopIcon = Icon.createWithResource(this, R.drawable.ic_stop);

            Intent iPause = new Intent(PAUSE_TRACK, null, getApplicationContext(), LocationService.class);
            PendingIntent piPause = PendingIntent.getService(this, 0, iPause, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon pauseIcon = Icon.createWithResource(this, R.drawable.ic_pause);

            Notification.Action actionStop = new Notification.Action.Builder(stopIcon, getString(R.string.actionStop), piStop).build();
            Notification.Action actionPause = new Notification.Action.Builder(pauseIcon, getString(R.string.actionPause), piPause).build();

            builder.addAction(actionPause);
            builder.addAction(actionStop);
        } else {
            message = getString(R.string.msgLocating);
        }

        Intent iLaunch = new Intent(Intent.ACTION_MAIN);
        iLaunch.addCategory(Intent.CATEGORY_LAUNCHER);
        iLaunch.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
        iLaunch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent piResult = PendingIntent.getActivity(this, 0, iLaunch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.setWhen(mErrorTime);
        builder.setSmallIcon(ntfId);
        builder.setContentIntent(piResult);
        builder.setContentTitle(getText(titleId));
        builder.setGroup("maptrek");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            builder.setCategory(Notification.CATEGORY_NAVIGATION);
        else
            builder.setCategory(Notification.CATEGORY_PROGRESS);
        builder.setPriority(Notification.PRIORITY_LOW);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setColor(getResources().getColor(R.color.colorAccent, getTheme()));
        if (mErrorTime > 0 && DEBUG_ERRORS)
            builder.setContentText(mErrorMsg);
        else
            builder.setContentText(message);
        builder.setOngoing(true);
        return builder.build();
    }

    private void updateNotification() {
        if (mForegroundTracking || mForegroundLocations) {
            logger.debug("updateNotification()");
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null)
                notificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    private void openDatabase() {
        //noinspection SpellCheckingInspection
        File path = new File(getExternalFilesDir("databases"), "track.sqlitedb");
        try {
            mTrackDB = SQLiteDatabase.openDatabase(path.getAbsolutePath(), null, SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = mTrackDB.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = 'track'", null);
            if (cursor.getCount() == 0) {
                mTrackDB.execSQL("CREATE TABLE track (_id INTEGER PRIMARY KEY, latitude INTEGER, longitude INTEGER, code INTEGER, elevation REAL, speed REAL, track REAL, accuracy REAL, datetime INTEGER)");
            }
            cursor.close();
            mDistanceTracked = 0f;
            cursor = mTrackDB.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = 'track_properties'", null);
            if (cursor.getCount() == 0) {
                mTrackDB.execSQL("CREATE TABLE track_properties (_id INTEGER PRIMARY KEY, distance REAL)");
            } else {
                Cursor propertiesCursor = mTrackDB.rawQuery("SELECT * FROM track_properties ORDER BY _id DESC LIMIT 1", null);
                if (propertiesCursor.moveToFirst()) {
                    mDistanceTracked = propertiesCursor.getFloat(propertiesCursor.getColumnIndexOrThrow("distance"));
                }
                propertiesCursor.close();
            }
            cursor.close();
            mTrackStarted = getTrackStartTime();
        } catch (SQLiteException e) {
            mTrackDB = null;
            logger.error("openDatabase", e);
            mErrorMsg = "Failed to open DB";
            mErrorTime = System.currentTimeMillis();
            updateNotification();
        }
    }

    private void closeDatabase() {
        if (mTrackDB != null) {
            mTrackDB.close();
            mTrackDB = null;
        }
    }

    public Track getTrack() {
        Track track = getTrack(0);
        mDistanceTracked = track.getDistance();
        return track;
    }

    public Track getTrack(long limit) {
        if (mTrackDB == null)
            openDatabase();
        Track track = new Track(getString(R.string.currentTrack), true);
        if (mTrackDB == null)
            return track;
        String limitStr = limit > 0 ? " LIMIT " + limit : "";
        Cursor cursor = mTrackDB.rawQuery("SELECT * FROM track ORDER BY _id DESC" + limitStr, null);
        for (boolean hasItem = cursor.moveToLast(); hasItem; hasItem = cursor.moveToPrevious()) {
            int latitudeE6 = cursor.getInt(cursor.getColumnIndexOrThrow("latitude"));
            int longitudeE6 = cursor.getInt(cursor.getColumnIndexOrThrow("longitude"));
            float elevation = cursor.getFloat(cursor.getColumnIndexOrThrow("elevation"));
            float speed = cursor.getFloat(cursor.getColumnIndexOrThrow("speed"));
            float bearing = cursor.getFloat(cursor.getColumnIndexOrThrow("track"));
            float accuracy = cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy"));
            int code = cursor.getInt(cursor.getColumnIndexOrThrow("code"));
            long time = cursor.getLong(cursor.getColumnIndexOrThrow("datetime"));
            track.addPoint(code == 0, latitudeE6, longitudeE6, elevation, speed, bearing, accuracy, time);
        }
        cursor.close();
        return track;
    }

    public Track getTrack(long start, long end) {
        if (mTrackDB == null)
            openDatabase();
        Track track = new Track();
        if (mTrackDB == null)
            return track;
        Cursor cursor = mTrackDB.rawQuery("SELECT * FROM track WHERE datetime >= ? AND datetime <= ? ORDER BY _id DESC", new String[]{String.valueOf(start), String.valueOf(end)});
        for (boolean hasItem = cursor.moveToLast(); hasItem; hasItem = cursor.moveToPrevious()) {
            int latitudeE6 = cursor.getInt(cursor.getColumnIndexOrThrow("latitude"));
            int longitudeE6 = cursor.getInt(cursor.getColumnIndexOrThrow("longitude"));
            float elevation = cursor.getFloat(cursor.getColumnIndexOrThrow("elevation"));
            float speed = cursor.getFloat(cursor.getColumnIndexOrThrow("speed"));
            float bearing = cursor.getFloat(cursor.getColumnIndexOrThrow("track"));
            float accuracy = cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy"));
            int code = cursor.getInt(cursor.getColumnIndexOrThrow("code"));
            long time = cursor.getLong(cursor.getColumnIndexOrThrow("datetime"));
            track.addPoint(code == 0, latitudeE6, longitudeE6, elevation, speed, bearing, accuracy, time);
        }
        cursor.close();
        return track;
    }

    public long getTrackStartTime() {
        long res = Long.MIN_VALUE;
        if (mTrackDB == null)
            openDatabase();
        if (mTrackDB == null)
            return res;
        Cursor cursor = mTrackDB.rawQuery("SELECT MIN(datetime) FROM track WHERE datetime > 0", null);
        if (cursor.moveToFirst())
            res = cursor.getLong(0);
        cursor.close();
        return res;
    }

    public long getTrackEndTime() {
        long res = Long.MAX_VALUE;
        if (mTrackDB == null)
            openDatabase();
        if (mTrackDB == null)
            return res;
        Cursor cursor = mTrackDB.rawQuery("SELECT MAX(datetime) FROM track", null);
        if (cursor.moveToFirst())
            res = cursor.getLong(0);
        cursor.close();
        return res;
    }

    public void clearTrack() {
        mDistanceTracked = 0f;
        if (mTrackDB == null)
            openDatabase();
        if (mTrackDB != null) {
            mTrackDB.execSQL("DELETE FROM track");
            mTrackDB.execSQL("DELETE FROM track_properties");
        }
    }

    public void tryToSaveTrack() {
        mLastTrack = getTrack();
        if (mLastTrack.points.size() == 0)
            return;

        long startTime = mLastTrack.points.get(0).time;
        long stopTime = mLastTrack.getLastPoint().time;
        long period = stopTime - startTime;
        int flags = DateUtils.FORMAT_NO_NOON | DateUtils.FORMAT_NO_MIDNIGHT;

        if (period < DateUtils.WEEK_IN_MILLIS)
            flags |= DateUtils.FORMAT_SHOW_TIME;

        if (period < DateUtils.WEEK_IN_MILLIS * 4)
            flags |= DateUtils.FORMAT_SHOW_WEEKDAY;

        //TODO Try to 'guess' starting and ending location name
        mLastTrack.description = DateUtils.formatDateRange(this, startTime, stopTime, flags) +
                " — " + StringFormatter.distanceH(mLastTrack.getDistance());
        flags |= DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        mLastTrack.name = DateUtils.formatDateRange(this, startTime, stopTime, flags);

        if (period < TOO_SMALL_PERIOD) {
            sendBroadcast(new Intent(BROADCAST_TRACK_SAVE)
                    .putExtra("saved", false)
                    .putExtra("reason", "period")
                    .setPackage(getPackageName()));
            clearTrack();
            return;
        }

        if (mLastTrack.getDistance() < TOO_SMALL_DISTANCE) {
            sendBroadcast(new Intent(BROADCAST_TRACK_SAVE)
                    .putExtra("saved", false)
                    .putExtra("reason", "distance")
                    .setPackage(getPackageName()));
            clearTrack();
            return;
        }
        saveTrack();
    }

    private void saveTrack() {
        if (mLastTrack == null || mLastTrack.points.isEmpty()) {
            sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", false)
                    .putExtra("reason", "missing")
                    .setPackage(getPackageName()));
            return;
        }
        File dataDir = getExternalFilesDir("data");
        if (dataDir == null) {
            logger.error("Can not save track: application data folder missing");
            sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", false)
                    .putExtra("reason", "error")
                    .putExtra("exception", new RuntimeException("Application data folder missing"))
                    .setPackage(getPackageName()));
            return;
        }
        FileDataSource source = new FileDataSource();
        //FIXME Not UTC time!
        source.name = TIME_FORMAT.format(new Date(mLastTrack.points.get(0).time));
        source.tracks.add(mLastTrack);
        Manager.save(source, new Manager.OnSaveListener() {
            @Override
            public void onSaved(FileDataSource source) {
                sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", true)
                        .putExtra("path", source.path)
                        .setPackage(getPackageName()));
                clearTrack();
                mLastTrack = null;
            }

            @Override
            public void onError(FileDataSource source, Exception e) {
                sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", false)
                        .putExtra("reason", "error")
                        .putExtra("exception", e)
                        .setPackage(getPackageName()));
            }
        }, mProgressListener);
    }

    private void updateDistanceTracked() {
        if (mTrackDB != null) {
            mTrackDB.delete("track_properties", null, null);
            ContentValues values = new ContentValues();
            values.put("distance", mDistanceTracked);
            mTrackDB.insert("track_properties", null, values);
        }
    }

    public void addPoint(boolean continuous, double latitude, double longitude, float elevation, float speed, float bearing, float accuracy, long time) {
        if (mTrackDB == null) {
            openDatabase();
            if (mTrackDB == null)
                return;
        }

        ContentValues values = new ContentValues();
        values.put("latitude", (int) (latitude * 1E6));
        values.put("longitude", (int) (longitude * 1E6));
        values.put("code", continuous ? 0 : 1);
        values.put("elevation", elevation);
        values.put("speed", speed);
        values.put("track", bearing);
        values.put("accuracy", accuracy);
        values.put("datetime", time);

        try {
            mTrackDB.insertOrThrow("track", null, values);
        } catch (SQLException e) {
            logger.error("addPoint", e);
            mErrorMsg = e.getMessage();
            mErrorTime = System.currentTimeMillis();
            updateNotification();
            closeDatabase();
        }
    }

    private void writeTrackPoint(final Location loc, final float distance, final boolean continuous) {
        addPoint(continuous, loc.getLatitude(), loc.getLongitude(), (float) loc.getAltitude(), loc.getSpeed(), loc.getBearing(), loc.getAccuracy(), loc.getTime());
        mDistanceTracked += distance;
        mDistanceNotified += distance;
        if (mDistanceNotified > mDistanceTracked / 100) {
            updateNotification();
            mDistanceNotified = 0f;
        }
        mLastWrittenLocation = loc;

        for (ITrackingListener callback : mTrackingCallbacks) {
            callback.onNewPoint(continuous, loc.getLatitude(), loc.getLongitude(), (float) loc.getAltitude(), loc.getSpeed(), loc.getBearing(), loc.getAccuracy(), loc.getTime());
        }
    }

    private void writeTrack(Location loc, boolean continuous) {
        float distance = 0;
        long time = 0;
        if (mLastWrittenLocation != null) {
            distance = loc.distanceTo(mLastWrittenLocation);
            time = loc.getTime() - mLastWrittenLocation.getTime();
        }
        if (mLastWrittenLocation == null || !continuous || time > mMaxTime || distance > mMinDistance && time > mMinTime) {
            writeTrackPoint(loc, distance, continuous);
        }
    }

    private void tearTrack() {
        if (mLastKnownLocation != mLastWrittenLocation && !"unknown".equals(mLastKnownLocation.getProvider())) {
            float distance = mLastWrittenLocation != null ? mLastKnownLocation.distanceTo(mLastWrittenLocation) : 0f;
            writeTrackPoint(mLastKnownLocation, distance, mContinuous);
        }
        mContinuous = false;
    }

    private void updateLocation() {
        final Location location = mLastKnownLocation;
        final boolean continuous = mContinuous;

        final Handler handler = new Handler(Looper.getMainLooper());

        if (mTrackingEnabled) {
            handler.post(() -> writeTrack(location, continuous));
        }
        for (final ILocationListener callback : mLocationCallbacks) {
            handler.post(callback::onLocationChanged);
        }
        final int n = mLocationRemoteCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            final ILocationCallback callback = mLocationRemoteCallbacks.getBroadcastItem(i);
            try {
                callback.onLocationChanged();
            } catch (RemoteException e) {
                logger.error("Location broadcast error", e);
            }
        }
        mLocationRemoteCallbacks.finishBroadcast();
    }

    private void updateGpsStatus() {
        if (mLastLocationMillis >= 0) {
            if (SystemClock.elapsedRealtime() - mLastLocationMillis < 3000) {
                mGpsStatus = GPS_OK;
            } else {
                if (mContinuous)
                    tearTrack();
                mGpsStatus = GPS_SEARCHING;
            }
        }
        if (mGpsStatus == GPS_SEARCHING)
            logger.debug("Searching: {}/{}", mFSats, mTSats);
        updateNotification();
        final Handler handler = new Handler(Looper.getMainLooper());
        for (final ILocationListener callback : mLocationCallbacks) {
            handler.post(callback::onGpsStatusChanged);
        }
        final int n = mLocationRemoteCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            final ILocationCallback callback = mLocationRemoteCallbacks.getBroadcastItem(i);
            try {
                callback.onGpsStatusChanged();
            } catch (RemoteException e) {
                logger.error("Location broadcast error", e);
            }
        }
        mLocationRemoteCallbacks.finishBroadcast();
    }

    @Override
    public void onLocationChanged(@NonNull final Location location) {
        if (enableMockLocations && BuildConfig.DEBUG)
            return;

        // skip initial locations
        if (mLastLocationMillis < 0) {
            mLastLocationMillis++;
            return;
        }

        long time = SystemClock.elapsedRealtime();

        long prevLocationMillis = mLastLocationMillis;
        float prevSpeed = mLastKnownLocation.getSpeed();
        float prevTrack = mLastKnownLocation.getBearing();

        mLastKnownLocation = location;

        if (mLastKnownLocation.getSpeed() == 0 && prevTrack != 0) {
            mLastKnownLocation.setBearing(prevTrack);
        }

        mLastLocationMillis = time;

        //if (!Float.isNaN(mNmeaGeoidHeight))
        //    mLastKnownLocation.setAltitude(mLastKnownLocation.getAltitude() + mNmeaGeoidHeight);

        if (mJustStarted) {
            mJustStarted = prevSpeed == 0;
        } else if (mLastKnownLocation.getSpeed() > 0) {
            // filter speed outrages
            double a = 2 * 9.8 * (mLastLocationMillis - prevLocationMillis) / 1000;
            if (Math.abs(mLastKnownLocation.getSpeed() - prevSpeed) > a)
                mLastKnownLocation.setSpeed(prevSpeed);
        }

        updateLocation();

        mContinuous = true;
    }

    /*
    @Override
    public void onNmeaReceived(long timestamp, String nmea) {
        if (nmea.indexOf('\n') == 0)
            return;
        if (nmea.indexOf('\n') > 0) {
            nmea = nmea.substring(0, nmea.indexOf('\n') - 1);
        }
        int len = nmea.length();
        if (len < 9) {
            return;
        }
        if (nmea.charAt(len - 3) == '*') {
            nmea = nmea.substring(0, len - 3);
        }
        String[] tokens = nmea.split(",");
        String sentenceId = tokens[0].length() > 5 ? tokens[0].substring(3, 6) : "";

        try {
            if (sentenceId.equals("GGA") && tokens.length > 11) {
                // String time = tokens[1];
                // String latitude = tokens[2];
                // String latitudeHemi = tokens[3];
                // String longitude = tokens[4];
                // String longitudeHemi = tokens[5];
                // String fixQuality = tokens[6];
                // String numSatellites = tokens[7];
                // String horizontalDilutionOfPrecision = tokens[8];
                // String altitude = tokens[9];
                // String altitudeUnits = tokens[10];
                String heightOfGeoid = tokens[11];
                if (!"".equals(heightOfGeoid))
                    mNmeaGeoidHeight = Float.parseFloat(heightOfGeoid);
                // String heightOfGeoidUnits = tokens[12];
                // String timeSinceLastDgpsUpdate = tokens[13];
            } else if (sentenceId.equals("GSA") && tokens.length > 17) {
                // String selectionMode = tokens[1]; // m=manual, a=auto 2d/3d
                // String mode = tokens[2]; // 1=no fix, 2=2d, 3=3d
                @SuppressWarnings("unused")
                String pdop = tokens[15];
                String hdop = tokens[16];
                String vdop = tokens[17];
                if (!"".equals(hdop))
                    mHDOP = Float.parseFloat(hdop);
                if (!"".equals(vdop))
                    mVDOP = Float.parseFloat(vdop);
            }
        } catch (NumberFormatException e) {
            logger.error("NFE", e);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("AIOOBE", e);
        }
    }
    */

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (enableMockLocations && BuildConfig.DEBUG)
            return;

        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            switch (status) {
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                case LocationProvider.OUT_OF_SERVICE:
                    tearTrack();
                    updateNotification();
                    break;
            }
        }
    }

    @Override
    public void onGpsStatusChanged(int event) {
        if (enableMockLocations && BuildConfig.DEBUG)
            return;

        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                onGpsStarted();
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                onGpsFirstFix();
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                onGpsStopped();
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                if (mLocationManager == null)
                    return;
                try {
                    GpsStatus status = mLocationManager.getGpsStatus(null);
                    if (status == null)
                        return;
                    Iterator<GpsSatellite> it = status.getSatellites().iterator();
                    mTSats = 0;
                    mFSats = 0;
                    while (it.hasNext()) {
                        mTSats++;
                        GpsSatellite sat = it.next();
                        if (sat.usedInFix())
                            mFSats++;
                    }
                    updateGpsStatus();
                } catch (SecurityException e) {
                    logger.error("Failed to update gps status", e);
                }
                break;
        }
    }

    private void onGpsFirstFix() {
        mContinuous = false;
    }

    private void onGpsStarted() {
        mGpsStatus = GPS_SEARCHING;
        mTSats = 0;
        mFSats = 0;
        updateGpsStatus();
    }

    private void onGpsStopped() {
        tearTrack();
        mGpsStatus = GPS_OFF;
        mTSats = 0;
        mFSats = 0;
        updateGpsStatus();
    }

    private final ILocationRemoteService.Stub mLocationRemoteBinder = new ILocationRemoteService.Stub() {
        public void registerCallback(ILocationCallback callback) {
            logger.debug("Register callback");
            if (callback == null)
                return;
            if (!"unknown".equals(mLastKnownLocation.getProvider())) {
                try {
                    callback.onLocationChanged();
                    callback.onGpsStatusChanged();
                } catch (RemoteException e) {
                    logger.error("Location broadcast error", e);
                }
            }
            mLocationRemoteCallbacks.register(callback);
        }

        public void unregisterCallback(ILocationCallback callback) {
            if (callback != null)
                mLocationRemoteCallbacks.unregister(callback);
        }

        public boolean isLocating() {
            return mLocationsEnabled;
        }

        @Override
        public Location getLocation() {
            return mLastKnownLocation;
        }

        @Override
        public int getStatus() {
            return mGpsStatus;
        }
    };

    public class LocalBinder extends Binder implements ILocationService {
        @Override
        public void registerLocationCallback(ILocationListener callback) {
            if (!mLocationsEnabled)
                connect();
            if (!"unknown".equals(mLastKnownLocation.getProvider())) {
                callback.onLocationChanged();
                callback.onGpsStatusChanged();
            }
            mLocationCallbacks.add(callback);
        }

        @Override
        public void unregisterLocationCallback(ILocationListener callback) {
            mLocationCallbacks.remove(callback);
        }

        @Override
        public void registerTrackingCallback(ITrackingListener callback) {
            mTrackingCallbacks.add(callback);
        }

        @Override
        public void unregisterTrackingCallback(ITrackingListener callback) {
            mTrackingCallbacks.remove(callback);
        }

        @Override
        public void setProgressListener(ProgressListener listener) {
            mProgressListener = listener;
        }

        @Override
        public boolean isLocating() {
            return mLocationsEnabled;
        }

        @Override
        public boolean isTracking() {
            return mTrackingEnabled;
        }

        @Override
        public Location getLocation() {
            return mLastKnownLocation;
        }

        @Override
        public int getStatus() {
            return mGpsStatus;
        }

        @Override
        public int getSatellites() {
            return (mFSats << 7) + mTSats;
        }

        @Override
        public float getHDOP() {
            return mHDOP;
        }

        @Override
        public float getVDOP() {
            return mVDOP;
        }

        @Override
        public Track getTrack() {
            return LocationService.this.getTrack();
        }

        @Override
        public Track getTrack(long start, long end) {
            return LocationService.this.getTrack(start, end);
        }

        @Override
        public void saveTrack() {
            LocationService.this.saveTrack();
        }

        @Override
        public void clearTrack() {
            LocationService.this.clearTrack();
        }

        @Override
        public long getTrackStartTime() {
            return LocationService.this.getTrackStartTime();
        }

        @Override
        public long getTrackEndTime() {
            return LocationService.this.getTrackEndTime();
        }
    }

    /**
     * Mock location generator used for application testing. Locations are generated
     * by logic required for particular test.
     */
    final private Runnable mSendMockLocation = new Runnable() {
        public void run() {
            mMockCallback.postDelayed(this, LOCATION_DELAY);
            mMockLocationTicker++;

            // 200 ticks - 60 seconds
            int ddd = mMockLocationTicker % 200;

            // Search for satellites for first 3 seconds and each 1 minute
            if (ddd >= 0 && ddd < 10) {
                mGpsStatus = GPS_SEARCHING;
                mFSats = mMockLocationTicker % 10;
                mTSats = 25;
                mContinuous = false;
                updateGpsStatus();
                return;
            }

            if (mGpsStatus == GPS_SEARCHING) {
                mGpsStatus = GPS_OK;
                updateGpsStatus();
            }

            mLastKnownLocation = new Location(LocationManager.GPS_PROVIDER);
            mLastKnownLocation.setTime(System.currentTimeMillis());
            mLastKnownLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            mLastKnownLocation.setAccuracy(3 + mMockLocationTicker % 100);
            mLastKnownLocation.setSpeed(20);
            mLastKnownLocation.setAltitude(20 + mMockLocationTicker);
            //mLastKnownLocation.setLatitude(34.865792);
            //mLastKnownLocation.setLongitude(32.351646);
            //mLastKnownLocation.setBearing((System.currentTimeMillis() / 166) % 360);
            //mLastKnownLocation.setAltitude(169);
            /*
            double lat = 55.813557;
            double lon = 37.645524;
            if (ddd < 50) {
                lat += ddd * 0.0001;
                mLastKnownLocation.setBearing(0);
            } else if (ddd < 100) {
                lat += 0.005;
                lon += (ddd - 50) * 0.0001;
                mLastKnownLocation.setBearing(90);
            } else if (ddd < 150) {
                lat += (150 - ddd) * 0.0001;
                lon += 0.005;
                mLastKnownLocation.setBearing(180);
            } else {
                lon += (200 - ddd) * 0.0001;
                mLastKnownLocation.setBearing(270);
            }
            */
            double lat = 60.0 - mMockLocationTicker * 0.0001;
            double lon = 30.3;
            if (ddd < 10) {
                mLastKnownLocation.setBearing(180 + ddd);
            }
            if (ddd < 90) {
                mLastKnownLocation.setBearing(180 + 10);
            } else if (ddd < 110) {
                mLastKnownLocation.setBearing(180 + 100 - ddd);
            } else if (ddd < 190) {
                mLastKnownLocation.setBearing(180 - 10);
            } else {
                mLastKnownLocation.setBearing(180 - 200 + ddd);
            }
            mLastKnownLocation.setLatitude(lat);
            mLastKnownLocation.setLongitude(lon);
            mNmeaGeoidHeight = 0;

            updateLocation();
            mContinuous = true;
        }
    };
}
