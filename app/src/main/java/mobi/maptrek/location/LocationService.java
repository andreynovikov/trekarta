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
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Icon;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import mobi.maptrek.MainActivity;
import mobi.maptrek.R;
import mobi.maptrek.data.DataSource;
import mobi.maptrek.data.Track;
import mobi.maptrek.io.Manager;
import mobi.maptrek.util.StringFormatter;

public class LocationService extends BaseLocationService implements LocationListener, NmeaListener, GpsStatus.Listener, OnSharedPreferenceChangeListener {
    private static final String TAG = "Location";

    private static final long TOO_SMALL_PERIOD = DateUtils.MINUTE_IN_MILLIS; // 1 minute
    private static final float TOO_SMALL_DISTANCE = 100f; // 100 meters
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    private static final int NOTIFICATION_ID = 25501;
    private static final boolean DEBUG_ERRORS = false;

    // Fake locations used for test purposes
    private static final boolean enableMockLocations = false;
    private Handler mMockCallback = new Handler();
    private int mMockLocationTicker = 0;

    // Real locations
    private LocationManager mLocationManager = null;
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
    private boolean mForeground = false;
    private String mErrorMsg = "";
    private long mErrorTime = 0;

    private Location mLastWrittenLocation = null;
    private float mDistanceTracked = 0f;
    private long mTrackingStarted = 0;
    private float mDistanceNotified;
    private Track mLastTrack;

    private long mMinTime = 2000; // 2 seconds (default)
    @SuppressWarnings("FieldCanBeLocal")
    private long mMaxTime = 300000; // 5 minutes
    private int mMinDistance = 3; // 3 meters (default)

    private final Binder mBinder = new LocalBinder();
    private final Set<ILocationListener> mLocationCallbacks = new HashSet<>();
    private final Set<ITrackingListener> mTrackingCallbacks = new HashSet<>();
    private Manager.ProgressListener mProgressListener;

    private static final String PREF_TRACKING_MIN_TIME = "tracking_min_time";
    private static final String PREF_TRACKING_MIN_DISTANCE = "tracking_min_distance";

    @Override
    public void onCreate() {
        mLastKnownLocation = new Location("unknown");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Tracking preferences
        onSharedPreferenceChanged(sharedPreferences, PREF_TRACKING_MIN_TIME);
        onSharedPreferenceChanged(sharedPreferences, PREF_TRACKING_MIN_DISTANCE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        Log.i(TAG, "Service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null)
            return 0;

        String action = intent.getAction();
        Log.i(TAG, "Command: " + action);
        if (action.equals(ENABLE_TRACK) || action.equals(ENABLE_BACKGROUND_TRACK) && !mTrackingEnabled) {
            mErrorMsg = "";
            mErrorTime = 0;
            mTrackingEnabled = true;
            mContinuous = false;
            Track leftTrack = getTrack();
            mDistanceTracked = leftTrack.distance;
            Track.TrackPoint firstPoint = leftTrack.getPointCount() > 0 ? leftTrack.getPoint(0) : null;
            mTrackingStarted = firstPoint != null ? firstPoint.time : System.currentTimeMillis();
            mDistanceNotified = 0f;
            openDatabase();
        }
        if (action.equals(DISABLE_TRACK) || action.equals(PAUSE_TRACK) && mTrackingEnabled) {
            mTrackingEnabled = false;
            mForeground = false;
            closeDatabase();
            stopForeground(true);
            if (action.equals(DISABLE_TRACK)) {
                if (intent.getBooleanExtra("self", false)) { // Preference is normally updated by Activity but not in this case
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    editor.putInt(MainActivity.PREF_TRACKING_STATE, MainActivity.TRACKING_STATE.DISABLED.ordinal());
                    editor.apply();
                }
                tryToSaveTrack();
                clearTrack();
            }
            stopSelf();
        }
        if (action.equals(ENABLE_BACKGROUND_TRACK)) {
            mForeground = true;
            startForeground(NOTIFICATION_ID, getNotification());
        }
        if (action.equals(DISABLE_BACKGROUND_TRACK)) {
            mForeground = false;
            stopForeground(true);
        }
        updateNotification();

        return START_REDELIVER_INTENT | START_STICKY;
    }

    @Override
    public void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        disconnect();
        closeDatabase();
        Log.i(TAG, "Service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_TRACKING_MIN_TIME.equals(key)) {
            mMinTime = 500; //Integer.parseInt(sharedPreferences.getString(key, "500"));
        } else if (PREF_TRACKING_MIN_DISTANCE.equals(key)) {
            mMinDistance = 5; //Integer.parseInt(sharedPreferences.getString(key, "5"));
        }
    }

    private void connect() {
        Log.e(TAG, "connect()");
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager != null) {
            mLastLocationMillis = 0;
            mContinuous = false;
            mJustStarted = true;
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationManager.addGpsStatusListener(this);
                try {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_DELAY, 0, this);
                    mLocationManager.addNmeaListener(this);
                    mLocationsEnabled = true;
                    Log.d(TAG, "Gps provider set");
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "Cannot set gps provider, likely no gps on device");
                }
            } else {
                Log.e(TAG, "Missing ACCESS_FINE_LOCATION permission");
            }
        }
        if (enableMockLocations) {
            mMockLocationTicker = 0;
            mMockCallback.post(mSendMockLocation);
            mLocationsEnabled = true;
        }
    }

    private void disconnect() {
        Log.e(TAG, "disconnect()");
        if (mLocationManager != null) {
            mLocationsEnabled = false;
            mLocationManager.removeNmeaListener(this);
            try {
                mLocationManager.removeUpdates(this);
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to remove updates", e);
            }
            mLocationManager.removeGpsStatusListener(this);
            mLocationManager = null;
        }
        if (enableMockLocations) {
            mLocationsEnabled = false;
            mMockCallback.removeCallbacks(mSendMockLocation);
        }
    }

    private Notification getNotification() {
        int titleId = R.string.notif_tracking;
        int ntfId = R.mipmap.ic_stat_tracking;
        if (mGpsStatus != LocationService.GPS_OK) {
            titleId = R.string.notif_loc_waiting;
            ntfId = R.mipmap.ic_stat_waiting;
        }
        if (mGpsStatus == LocationService.GPS_OFF) {
            titleId = R.string.notif_loc_waiting;
            ntfId = R.mipmap.ic_stat_off;
        }
        if (mErrorTime > 0) {
            titleId = R.string.notif_tracking_failure;
            ntfId = R.mipmap.ic_stat_failure;
        }

        String timeTracked = (String) DateUtils.getRelativeTimeSpanString(getApplicationContext(), mTrackingStarted);
        String distanceTracked = StringFormatter.distanceH(mDistanceTracked);

        StringBuilder sb = new StringBuilder(40);
        sb.append(getString(R.string.msg_tracked, distanceTracked, timeTracked));
        String message = sb.toString();
        sb.insert(0, ". ");
        sb.insert(0, getString(R.string.msg_tracking));
        sb.append(". ");
        sb.append(getString(R.string.msg_tracking_actions));
        sb.append(".");
        String bigText = sb.toString();

        Intent iLaunch = new Intent(Intent.ACTION_MAIN);
        iLaunch.addCategory(Intent.CATEGORY_LAUNCHER);
        iLaunch.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
        iLaunch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent piResult = PendingIntent.getActivity(this, 0, iLaunch, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent iStop = new Intent(this, LocationService.class);
        iStop.setAction(DISABLE_TRACK);
        iStop.putExtra("self", true);
        PendingIntent piStop = PendingIntent.getService(this, 0, iStop, PendingIntent.FLAG_CANCEL_CURRENT);
        Icon stopIcon = Icon.createWithResource(this, R.drawable.ic_stop);

        Intent iPause = new Intent(this, LocationService.class);
        iPause.setAction(PAUSE_TRACK);
        PendingIntent piPause = PendingIntent.getService(this, 0, iPause, PendingIntent.FLAG_CANCEL_CURRENT);
        Icon pauseIcon = Icon.createWithResource(this, R.drawable.ic_pause);

        Notification.Action actionStop = new Notification.Action.Builder(stopIcon, getString(R.string.action_stop), piStop).build();
        Notification.Action actionPause = new Notification.Action.Builder(pauseIcon, getString(R.string.action_pause), piPause).build();

        Notification.Builder builder = new Notification.Builder(this);
        builder.setWhen(mErrorTime);
        builder.setSmallIcon(ntfId);
        builder.setContentIntent(piResult);
        builder.setContentTitle(getText(titleId));
        builder.setStyle(new Notification.BigTextStyle().setBigContentTitle(getText(titleId)).bigText(bigText));
        builder.addAction(actionPause);
        builder.addAction(actionStop);
        builder.setGroup("maptrek");
        builder.setCategory(Notification.CATEGORY_SERVICE);
        builder.setPriority(Notification.PRIORITY_LOW);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setColor(getResources().getColor(R.color.colorAccent, getTheme()));
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (mErrorTime > 0 && DEBUG_ERRORS)
            builder.setContentText(mErrorMsg);
        else
            builder.setContentText(message);
        builder.setOngoing(true);

        return builder.build();
    }

    private void updateNotification() {
        if (mForeground) {
            Log.e(TAG, "updateNotification()");
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    private void openDatabase() {
        //noinspection SpellCheckingInspection
        File path = new File(getExternalFilesDir("databases"), "track.sqlitedb");
        try {
            mTrackDB = SQLiteDatabase.openDatabase(path.getAbsolutePath(), null, SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            //noinspection SpellCheckingInspection
            Cursor cursor = mTrackDB.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = 'track'", null);
            if (cursor.getCount() == 0) {
                mTrackDB.execSQL("CREATE TABLE track (_id INTEGER PRIMARY KEY, latitude REAL, longitude REAL, code INTEGER, elevation REAL, speed REAL, track REAL, accuracy REAL, datetime INTEGER)");
            }
            cursor.close();
        } catch (SQLiteException e) {
            mTrackDB = null;
            Log.e(TAG, "openDatabase", e);
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
        return getTrack(0);
    }

    public Track getTrack(long limit) {
        if (mTrackDB == null)
            openDatabase();
        Track track = new Track();
        if (mTrackDB == null)
            return track;
        String limitStr = limit > 0 ? " LIMIT " + limit : "";
        Cursor cursor = mTrackDB.rawQuery("SELECT * FROM track ORDER BY _id DESC" + limitStr, null);
        for (boolean hasItem = cursor.moveToLast(); hasItem; hasItem = cursor.moveToPrevious()) {
            double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
            double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
            double elevation = cursor.getDouble(cursor.getColumnIndex("elevation"));
            double speed = cursor.getDouble(cursor.getColumnIndex("speed"));
            double bearing = cursor.getDouble(cursor.getColumnIndex("track"));
            double accuracy = cursor.getDouble(cursor.getColumnIndex("accuracy"));
            int code = cursor.getInt(cursor.getColumnIndex("code"));
            long time = cursor.getLong(cursor.getColumnIndex("datetime"));
            track.addPoint(code == 0, latitude, longitude, elevation, speed, bearing, accuracy, time);
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
            double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
            double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
            double elevation = cursor.getDouble(cursor.getColumnIndex("elevation"));
            double speed = cursor.getDouble(cursor.getColumnIndex("speed"));
            double bearing = cursor.getDouble(cursor.getColumnIndex("track"));
            double accuracy = cursor.getDouble(cursor.getColumnIndex("accuracy"));
            int code = cursor.getInt(cursor.getColumnIndex("code"));
            long time = cursor.getLong(cursor.getColumnIndex("datetime"));
            track.addPoint(code == 0, latitude, longitude, elevation, speed, bearing, accuracy, time);
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
        if (mTrackDB == null)
            openDatabase();
        if (mTrackDB != null)
            mTrackDB.execSQL("DELETE FROM track");
    }

    public void tryToSaveTrack() {
        mLastTrack = getTrack();
        if (mLastTrack.getPointCount() == 0)
            return;

        mLastTrack.color = getColor(R.color.trackColor);
        mLastTrack.width = getResources().getInteger(R.integer.trackWidth);

        long startTime = mLastTrack.getPoint(0).time;
        long stopTime = mLastTrack.getLastPoint().time;
        long period = stopTime - startTime;
        int flags = DateUtils.FORMAT_NO_NOON | DateUtils.FORMAT_NO_MIDNIGHT;

        if (period < DateUtils.WEEK_IN_MILLIS)
            flags |= DateUtils.FORMAT_SHOW_TIME;

        if (period < DateUtils.WEEK_IN_MILLIS * 4)
            flags |= DateUtils.FORMAT_SHOW_WEEKDAY;

        //TODO Try to 'guess' starting and ending location name
        mLastTrack.description = DateUtils.formatDateRange(this, startTime, stopTime, flags) +
                " \u2014 " + StringFormatter.distanceH(mLastTrack.distance);
        flags |= DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        mLastTrack.name = DateUtils.formatDateRange(this, startTime, stopTime, flags);

        if (period < TOO_SMALL_PERIOD) {
            sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "period"));
            return;
        }

        if (mLastTrack.distance < TOO_SMALL_DISTANCE) {
            sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "distance"));
            return;
        }
        saveTrack();
    }

    private void saveTrack() {
        if (mLastTrack == null) {
            sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "missing"));
            return;
        }
        File dataDir = getExternalFilesDir("data");
        if (dataDir == null) {
            Log.e(TAG, "Can not save track: application data folder missing");
            sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "error"));
            return;
        }
        //FIXME Not UTC time!
        DataSource source = new DataSource(TIME_FORMAT.format(new Date(mLastTrack.getPoint(0).time)));
        source.tracks.add(mLastTrack);
        Manager.save(this, source, new Manager.OnSaveListener() {
            @Override
            public void onSaved(DataSource source) {
                sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", true).putExtra("path", source.path));
                mLastTrack = null;
            }

            @Override
            public void onError(DataSource source, Exception e) {
                sendBroadcast(new Intent(BROADCAST_TRACK_SAVE).putExtra("saved", false).putExtra("reason", "error"));
            }
        }, mProgressListener);
    }

    public void addPoint(boolean continuous, double latitude, double longitude, double elevation, float speed, float bearing, float accuracy, long time) {
        if (mTrackDB == null) {
            openDatabase();
            if (mTrackDB == null)
                return;
        }

        ContentValues values = new ContentValues();
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        values.put("code", continuous ? 0 : 1);
        values.put("elevation", elevation);
        values.put("speed", speed);
        values.put("track", bearing);
        values.put("accuracy", accuracy);
        values.put("datetime", time);

        try {
            mTrackDB.insertOrThrow("track", null, values);
        } catch (SQLException e) {
            Log.e(TAG, "addPoint", e);
            mErrorMsg = e.getMessage();
            mErrorTime = System.currentTimeMillis();
            updateNotification();
            closeDatabase();
        }
    }

    private void writeTrackPoint(final Location loc, final float distance, final boolean continuous) {
        Log.d(TAG, "Fix needs writing");
        addPoint(continuous, loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), loc.getSpeed(), loc.getBearing(), loc.getAccuracy(), loc.getTime());
        mDistanceTracked += distance;
        mDistanceNotified += distance;
        if (mDistanceNotified > mDistanceTracked / 100) {
            updateNotification();
            mDistanceNotified = 0f;
        }
        mLastWrittenLocation = loc;

        for (ITrackingListener callback : mTrackingCallbacks) {
            callback.onNewPoint(continuous, loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), loc.getSpeed(), loc.getBearing(), loc.getAccuracy(), loc.getTime());
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
        //final boolean geoId = !Float.isNaN(mNmeaGeoidHeight);

        final Handler handler = new Handler();

        if (mTrackingEnabled) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    writeTrack(location, continuous);
                }
            });
        }
        for (final ILocationListener callback : mLocationCallbacks) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onLocationChanged();
                }
            });
        }
        Log.d(TAG, "Location dispatched: " + mLocationCallbacks.size());
    }

    private void updateLocation(final ILocationListener callback) {
        if (!"unknown".equals(mLastKnownLocation.getProvider()))
            callback.onLocationChanged();
    }

    private void updateGpsStatus() {
        if (mGpsStatus == GPS_SEARCHING)
            Log.d(TAG, "Searching: " + mFSats + "/" + mTSats);
        updateNotification();
        final Handler handler = new Handler();
        for (final ILocationListener callback : mLocationCallbacks) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onGpsStatusChanged();
                }
            });
        }
        Log.d(TAG, "GPS status dispatched: " + mLocationCallbacks.size());
    }

    private void updateGpsStatus(final ILocationListener callback) {
        if (!"unknown".equals(mLastKnownLocation.getProvider()))
            callback.onLocationChanged();
    }

    @Override
    public void onLocationChanged(final Location location) {
        if (enableMockLocations)
            return;

        long time = SystemClock.elapsedRealtime();

        // Log.i(TAG, "Location arrived: "+location.toString());

        Log.d(TAG, "Fix arrived");

        long prevLocationMillis = mLastLocationMillis;
        float prevSpeed = mLastKnownLocation.getSpeed();
        float prevTrack = mLastKnownLocation.getBearing();

        mLastKnownLocation = location;

        if (mLastKnownLocation.getSpeed() == 0 && prevTrack != 0) {
            mLastKnownLocation.setBearing(prevTrack);
        }

        mLastLocationMillis = time;

        if (!Float.isNaN(mNmeaGeoidHeight)) {
            mLastKnownLocation.setAltitude(mLastKnownLocation.getAltitude() + mNmeaGeoidHeight);
        }

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
            Log.e(TAG, "NFE", e);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "AIOOBE", e);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (enableMockLocations)
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
        if (enableMockLocations)
            return;

        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                mGpsStatus = GPS_SEARCHING;
                mTSats = 0;
                mFSats = 0;
                updateGpsStatus();
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                mContinuous = false;
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                tearTrack();
                mGpsStatus = GPS_OFF;
                mTSats = 0;
                mFSats = 0;
                updateGpsStatus();
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                if (mLocationManager == null)
                    return;
                GpsStatus status = mLocationManager.getGpsStatus(null);
                Iterator<GpsSatellite> it = status.getSatellites().iterator();
                mTSats = 0;
                mFSats = 0;
                while (it.hasNext()) {
                    mTSats++;
                    GpsSatellite sat = it.next();
                    if (sat.usedInFix())
                        mFSats++;
                }
                if (SystemClock.elapsedRealtime() - mLastLocationMillis < 3000) {
                    mGpsStatus = GPS_OK;
                    updateGpsStatus();
                } else {
                    if (mContinuous)
                        tearTrack();
                    mGpsStatus = GPS_SEARCHING;
                    updateGpsStatus();
                }
                break;
        }
    }

    public class LocalBinder extends Binder implements ILocationService {
        @Override
        public void registerLocationCallback(ILocationListener callback) {
            if (!mLocationsEnabled)
                connect();
            updateLocation(callback);
            updateGpsStatus(callback);
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
        public void setProgressListener(Manager.ProgressListener listener) {
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

            //TODO Why 200?
            int ddd = mMockLocationTicker % 200;

            // Search for satellites for first 3 seconds and each 1 minute
            if (ddd >= 0 && ddd < 3) {
                mGpsStatus = GPS_SEARCHING;
                mFSats = mMockLocationTicker % 10;
                mTSats = 25;
                updateGpsStatus();
                return;
            }

            if (mGpsStatus == GPS_SEARCHING) {
                mGpsStatus = GPS_OK;
                updateGpsStatus();
            }

            mLastKnownLocation = new Location(LocationManager.GPS_PROVIDER);
            mLastKnownLocation.setTime(System.currentTimeMillis());
            mLastKnownLocation.setAccuracy(3 + mMockLocationTicker % 100);
            mLastKnownLocation.setSpeed(20);
            mLastKnownLocation.setBearing(323);
            mLastKnownLocation.setAltitude(39);
            //mLastKnownLocation.setLatitude(34.865792);
            //mLastKnownLocation.setLongitude(32.351646);
            //mLastKnownLocation.setBearing((System.currentTimeMillis() / 166) % 360);
            //mLastKnownLocation.setAltitude(169);
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
            mLastKnownLocation.setLatitude(lat);
            mLastKnownLocation.setLongitude(lon);
            mNmeaGeoidHeight = 0;

            updateLocation();
            mContinuous = true;
        }
    };
}
