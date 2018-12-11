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

package mobi.maptrek.location;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.oscim.core.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.Configuration;
import mobi.maptrek.MainActivity;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.Route;
import mobi.maptrek.util.Geo;
import mobi.maptrek.util.StringFormatter;

public class NavigationService extends BaseNavigationService implements OnSharedPreferenceChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(NavigationService.class);

    private static final int NOTIFICATION_ID = 25502;

    private ILocationService mLocationService = null;
    private Location mLastKnownLocation;
    private boolean mForeground = false;

    private int mRouteProximity = 200;
    private boolean mUseTraverse = true;

    /**
     * Active route waypoint
     */
    public MapObject navWaypoint = null;
    /**
     * Previous route waypoint
     */
    public MapObject prevWaypoint = null;
    /**
     * Active route
     */
    public Route navRoute = null;

    public int navDirection = 0;
    /**
     * Active route waypoint index
     */
    public int navCurrentRoutePoint = -1;
    private double navRouteDistance = -1;

    /**
     * Distance to active waypoint
     */
    public int navProximity = 0;
    public double navDistance = 0d;
    public double navBearing = 0d;
    public long navTurn = 0;
    public double navVMG = 0d;
    public int navETE = Integer.MAX_VALUE;
    public double navCourse = 0d;
    public double navXTK = Double.NEGATIVE_INFINITY;

    private long tics = 0;
    private double[] vmgav = null;
    private double avvmg = 0.0;

    private final Binder mBinder = new LocalBinder();

    private static final String PREF_NAVIGATION_PROXIMITY = "navigation_proximity";
    private static final String PREF_NAVIGATION_TRAVERSE = "navigation_traverse";
    public static final String PREF_NAVIGATION_BACKGROUND = "navigation_background";

    @Override
    public void onCreate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        onSharedPreferenceChanged(sharedPreferences, PREF_NAVIGATION_PROXIMITY);
        onSharedPreferenceChanged(sharedPreferences, PREF_NAVIGATION_TRAVERSE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        logger.debug("Service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null)
            return START_NOT_STICKY;

        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        logger.debug("Command: {}", action);
        if (action.equals(NAVIGATE_TO_POINT)) {
            MapObject mo = new MapObject(extras.getDouble(EXTRA_LATITUDE), extras.getDouble(EXTRA_LONGITUDE));
            mo.name = extras.getString(EXTRA_NAME);
            mo.proximity = extras.getInt(EXTRA_PROXIMITY);
            navigateTo(mo);
        }
        if (action.equals(NAVIGATE_TO_OBJECT)) {
            long id = extras.getLong(EXTRA_ID);
            MapObject mo = MapTrek.getMapObject(id);
            if (mo == null)
                return START_NOT_STICKY;
            navigateTo(mo);
        }
        if (action.equals(NAVIGATE_ROUTE)) {
            int index = extras.getInt(EXTRA_ROUTE_INDEX);
            int dir = extras.getInt(EXTRA_ROUTE_DIRECTION, DIRECTION_FORWARD);
            int start = extras.getInt(EXTRA_ROUTE_START, -1);
            Route route = null; //application.getRoute(index);
            //TODO Reimplement route navigation
            //noinspection ConstantConditions
            if (route == null)
                return START_NOT_STICKY;
            navigateTo(route, dir);
            if (start != -1)
                setRouteWaypoint(start);
        }
        if (action.equals(STOP_NAVIGATION) || action.equals(PAUSE_NAVIGATION)) {
            mForeground = false;
            stopForeground(true);
            if (action.equals(STOP_NAVIGATION))
                stopNavigation();
            Configuration.setNavigationPoint(navWaypoint);
            stopSelf();
        }
        if (action.equals(ENABLE_BACKGROUND_NAVIGATION)) {
            mForeground = true;
            startForeground(NOTIFICATION_ID, getNotification());
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putBoolean(PREF_NAVIGATION_BACKGROUND, true);
            editor.apply();
        }
        if (action.equals(DISABLE_BACKGROUND_NAVIGATION)) {
            mForeground = false;
            stopForeground(true);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putBoolean(PREF_NAVIGATION_BACKGROUND, false);
            editor.apply();
        }
        updateNotification();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        disconnect();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        logger.debug("Service stopped");
    }

    public class LocalBinder extends Binder implements INavigationService {
        @Override
        public boolean isNavigating() {
            return navWaypoint != null;
        }

        @Override
        public boolean isNavigatingViaRoute() {
            return navRoute != null;
        }

        @Override
        public MapObject getWaypoint() {
            return navWaypoint;
        }

        @Override
        public float getDistance() {
            return (float) navDistance;
        }

        @Override
        public float getBearing() {
            return (float) navBearing;
        }

        @Override
        public float getTurn() {
            return navTurn;
        }

        @Override
        public float getVmg() {
            return (float) navVMG;
        }

        @Override
        public float getXtk() {
            return (float) navXTK;
        }

        @Override
        public int getEte() {
            return navETE;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_NAVIGATION_PROXIMITY.equals(key)) {
            mRouteProximity = 200; //Integer.parseInt(sharedPreferences.getString(key, getString(R.string.def_navigation_proximity)));
        } else if (PREF_NAVIGATION_TRAVERSE.equals(key)) {
            mUseTraverse = true; //sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.def_navigation_traverse));
        }
    }

    private void connect() {
        EventBus.getDefault().register(this);
        bindService(new Intent(this, LocationService.class), locationConnection, BIND_AUTO_CREATE);
    }

    private void disconnect() {
        EventBus.getDefault().unregister(this);
        if (mLocationService != null) {
            mLocationService.unregisterLocationCallback(locationListener);
            unbindService(locationConnection);
            mLocationService = null;
        }
    }

    private Notification getNotification() {
        String title = getString(R.string.msgNavigating, navWaypoint.name);
        String bearing = StringFormatter.angleH(navBearing);
        String distance = StringFormatter.distanceH(navDistance);

        StringBuilder sb = new StringBuilder(40);
        sb.append(getString(R.string.msgNavigationProgress, distance, bearing));
        String message = sb.toString();
        sb.append(". ");
        sb.append(getString(R.string.msgNavigationActions));
        sb.append(".");
        String bigText = sb.toString();

        Intent iLaunch = new Intent(Intent.ACTION_MAIN);
        iLaunch.addCategory(Intent.CATEGORY_LAUNCHER);
        iLaunch.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
        iLaunch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent piResult = PendingIntent.getActivity(this, 0, iLaunch, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent iStop = new Intent(STOP_NAVIGATION, null, getApplicationContext(), NavigationService.class);
        PendingIntent piStop = PendingIntent.getService(this, 0, iStop, PendingIntent.FLAG_CANCEL_CURRENT);
        Icon stopIcon = Icon.createWithResource(this, R.drawable.ic_cancel_black);

        Intent iPause = new Intent(PAUSE_NAVIGATION, null, getApplicationContext(), NavigationService.class);
        PendingIntent piPause = PendingIntent.getService(this, 0, iPause, PendingIntent.FLAG_CANCEL_CURRENT);
        Icon pauseIcon = Icon.createWithResource(this, R.drawable.ic_pause);

        Notification.Action actionStop = new Notification.Action.Builder(stopIcon, getString(R.string.actionStop), piStop).build();
        Notification.Action actionPause = new Notification.Action.Builder(pauseIcon, getString(R.string.actionPause), piPause).build();

        Notification.Builder builder = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT > 25)
            builder.setChannelId("ongoing");
        builder.setSmallIcon(R.mipmap.ic_stat_navigation);
        builder.setContentIntent(piResult);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setWhen(System.currentTimeMillis());
        builder.setStyle(new Notification.BigTextStyle().setBigContentTitle(title).bigText(bigText));
        builder.addAction(actionPause);
        builder.addAction(actionStop);
        builder.setGroup("maptrek");
        builder.setCategory(Notification.CATEGORY_SERVICE);
        builder.setPriority(Notification.PRIORITY_LOW);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setColor(getResources().getColor(R.color.colorAccent, getTheme()));
        builder.setOngoing(true);

        return builder.build();
    }

    private void updateNotification() {
        if (mForeground) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    public void stopNavigation() {
        logger.debug("Stop navigation");
        updateNavigationState(STATE_STOPPED);
        stopForeground(true);
        clearNavigation();
        disconnect();
    }

    private void clearNavigation() {
        navWaypoint = null;
        prevWaypoint = null;
        navRoute = null;

        navDirection = 0;
        navCurrentRoutePoint = -1;

        navProximity = mRouteProximity;
        navDistance = 0f;
        navBearing = 0f;
        navTurn = 0;
        navVMG = 0f;
        navETE = Integer.MAX_VALUE;
        navCourse = 0f;
        navXTK = Double.NEGATIVE_INFINITY;

        vmgav = null;
        avvmg = 0.0;
    }

    private void navigateTo(final MapObject waypoint) {
        if (navWaypoint != null)
            stopNavigation();

        connect();

        vmgav = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        navWaypoint = waypoint;
        navProximity = navWaypoint.proximity > 0 ? navWaypoint.proximity : mRouteProximity;
        updateNavigationState(STATE_STARTED);
        if (mLastKnownLocation != null)
            calculateNavigationStatus(mLastKnownLocation, 0, 0);
    }

    private void navigateTo(final Route route, final int direction) {
        if (navWaypoint != null)
            stopNavigation();

        connect();

        vmgav = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        navRoute = route;
        navDirection = direction;
        navCurrentRoutePoint = navDirection == 1 ? 1 : navRoute.length() - 2;

        navWaypoint = navRoute.getWaypoint(navCurrentRoutePoint);
        prevWaypoint = navRoute.getWaypoint(navCurrentRoutePoint - navDirection);
        navProximity = navWaypoint.proximity > 0 ? navWaypoint.proximity : mRouteProximity;
        navRouteDistance = -1;
        navCourse = prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
        updateNavigationState(STATE_STARTED);
        if (mLastKnownLocation != null)
            calculateNavigationStatus(mLastKnownLocation, 0, 0);
    }

    public void setRouteWaypoint(int waypoint) {
        navCurrentRoutePoint = waypoint;
        navWaypoint = navRoute.getWaypoint(navCurrentRoutePoint);
        int prev = navCurrentRoutePoint - navDirection;
        if (prev >= 0 && prev < navRoute.length())
            prevWaypoint = navRoute.getWaypoint(prev);
        else
            prevWaypoint = null;
        navProximity = navWaypoint.proximity > 0 ? navWaypoint.proximity : mRouteProximity;
        navRouteDistance = -1;
        navCourse = prevWaypoint == null ? 0d : prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
        updateNavigationState(STATE_NEXT_WPT);
    }

    public MapObject getNextRouteWaypoint() {
        try {
            return navRoute.getWaypoint(navCurrentRoutePoint + navDirection);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public void nextRouteWaypoint() throws IndexOutOfBoundsException {
        navCurrentRoutePoint += navDirection;
        navWaypoint = navRoute.getWaypoint(navCurrentRoutePoint);
        prevWaypoint = navRoute.getWaypoint(navCurrentRoutePoint - navDirection);
        navProximity = navWaypoint.proximity > 0 ? navWaypoint.proximity : mRouteProximity;
        navRouteDistance = -1;
        navCourse = prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
        updateNavigationState(STATE_NEXT_WPT);
    }

    public void prevRouteWaypoint() throws IndexOutOfBoundsException {
        navCurrentRoutePoint -= navDirection;
        navWaypoint = navRoute.getWaypoint(navCurrentRoutePoint);
        int prev = navCurrentRoutePoint - navDirection;
        if (prev >= 0 && prev < navRoute.length())
            prevWaypoint = navRoute.getWaypoint(prev);
        else
            prevWaypoint = null;
        navProximity = navWaypoint.proximity > 0 ? navWaypoint.proximity : mRouteProximity;
        navRouteDistance = -1;
        navCourse = prevWaypoint == null ? 0d : prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
        updateNavigationState(STATE_NEXT_WPT);
    }

    public boolean hasNextRouteWaypoint() {
        if (navRoute == null)
            return false;
        boolean hasNext = false;
        if (navDirection == DIRECTION_FORWARD)
            hasNext = (navCurrentRoutePoint + navDirection) < navRoute.length();
        if (navDirection == DIRECTION_REVERSE)
            hasNext = (navCurrentRoutePoint + navDirection) >= 0;
        return hasNext;
    }

    public boolean hasPrevRouteWaypoint() {
        if (navRoute == null)
            return false;
        boolean hasPrev = false;
        if (navDirection == DIRECTION_FORWARD)
            hasPrev = (navCurrentRoutePoint - navDirection) >= 0;
        if (navDirection == DIRECTION_REVERSE)
            hasPrev = (navCurrentRoutePoint - navDirection) < navRoute.length();
        return hasPrev;
    }

    public int navRouteCurrentIndex() {
        return navDirection == DIRECTION_FORWARD ? navCurrentRoutePoint : navRoute.length() - navCurrentRoutePoint - 1;
    }

    /**
     * Calculates distance between current route waypoint and last route waypoint.
     *
     * @return distance left
     */
    public double navRouteDistanceLeft() {
        if (navRouteDistance < 0) {
            navRouteDistance = navRouteDistanceLeftTo(navRoute.length() - 1);
        }
        return navRouteDistance;
    }

    /**
     * Calculates distance between current route waypoint and route waypoint with specified index.
     * Method honors navigation direction.
     *
     * @param index
     * @return distance left
     */
    public double navRouteDistanceLeftTo(int index) {
        int current = navRouteCurrentIndex();
        int progress = index - current;

        if (progress <= 0)
            return 0.0;

        double distance = 0.0;
        if (navDirection == DIRECTION_FORWARD)
            distance = navRoute.distanceBetween(navCurrentRoutePoint, index);
        if (navDirection == DIRECTION_REVERSE)
            distance = navRoute.distanceBetween(navRoute.length() - index - 1, navCurrentRoutePoint);

        return distance;
    }

    public int navRouteWaypointETE(int index) {
        if (index == 0)
            return 0;
        int ete = Integer.MAX_VALUE;
        if (avvmg > 0) {
            int i = navDirection == DIRECTION_FORWARD ? index : navRoute.length() - index - 1;
            int j = i - navDirection;
            MapObject w1 = navRoute.getWaypoint(i);
            MapObject w2 = navRoute.getWaypoint(j);
            double distance = w1.coordinates.vincentyDistance(w2.coordinates);
            ete = (int) Math.round(distance / avvmg / 60);
        }
        return ete;
    }

    /**
     * Calculates route ETE.
     *
     * @param distance route distance
     * @return route ETE
     */
    public int navRouteETE(double distance) {
        int eta = Integer.MAX_VALUE;
        if (avvmg > 0) {
            eta = (int) Math.round(distance / avvmg / 60);
        }
        return eta;
    }

    public int navRouteETETo(int index) {
        double distance = navRouteDistanceLeftTo(index);
        if (distance <= 0.0)
            return 0;

        return navRouteETE(distance);
    }

    private void calculateNavigationStatus(Location loc, double smoothspeed, double avgspeed) {
        GeoPoint point = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        double distance = point.vincentyDistance(navWaypoint.coordinates);
        double bearing = point.bearingTo(navWaypoint.coordinates);
        double track = loc.getBearing();

        // turn
        long turn = Math.round(bearing - track);
        if (Math.abs(turn) > 180) {
            turn = turn - (long) (Math.signum(turn)) * 360;
        }

        // vmg
        double vmg = Geo.vmg(smoothspeed, Math.abs(turn));

        // ete
        double curavvmg = Geo.vmg(avgspeed, Math.abs(turn));
        if (avvmg == 0.0 || tics % 10 == 0) {
            for (int i = vmgav.length - 1; i > 0; i--) {
                avvmg += vmgav[i];
                vmgav[i] = vmgav[i - 1];
            }
            avvmg += curavvmg;
            vmgav[0] = curavvmg;
            avvmg = avvmg / vmgav.length;
        }

        int ete = Integer.MAX_VALUE;
        if (avvmg > 0)
            ete = (int) Math.round(distance / avvmg / 60);

        double xtk = Double.NEGATIVE_INFINITY;

        if (navRoute != null) {
            boolean hasNext = hasNextRouteWaypoint();
            if (distance < navProximity) {
                if (hasNext) {
                    nextRouteWaypoint();
                    return;
                } else {
                    updateNavigationState(STATE_REACHED);
                    stopNavigation();
                    return;
                }
            }

            if (prevWaypoint != null) {
                double dtk = prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
                xtk = Geo.xtk(distance, dtk, bearing);

                if (xtk == Double.NEGATIVE_INFINITY) {
                    if (mUseTraverse && hasNext) {
                        double cxtk2 = Double.NEGATIVE_INFINITY;
                        MapObject nextWpt = getNextRouteWaypoint();
                        if (nextWpt != null) {
                            double dtk2 = nextWpt.coordinates.bearingTo(navWaypoint.coordinates);
                            cxtk2 = Geo.xtk(0, dtk2, bearing);
                        }

                        if (cxtk2 != Double.NEGATIVE_INFINITY) {
                            nextRouteWaypoint();
                            return;
                        }
                    }
                }
            }
        }

        tics++;

        if (distance != navDistance || bearing != navBearing || turn != navTurn || vmg != navVMG || ete != navETE || xtk != navXTK) {
            navDistance = distance;
            navBearing = bearing;
            navTurn = turn;
            navVMG = vmg;
            navETE = ete;
            navXTK = xtk;
            updateNavigationStatus();
        }
    }

    private void updateNavigationState(final int state) {
        if (state != STATE_STOPPED && state != STATE_REACHED)
            updateNotification();
        sendBroadcast(new Intent(BROADCAST_NAVIGATION_STATE).putExtra("state", state));
        logger.trace("State dispatched");
    }

    private void updateNavigationStatus() {
        updateNotification();
        sendBroadcast(new Intent(BROADCAST_NAVIGATION_STATUS));
        logger.trace("Status dispatched");
    }

    @Subscribe
    public void onMapObjectUpdated(MapObject.UpdatedEvent event) {
        logger.error("onMapObjectUpdated({})", (event.mapObject.equals(navWaypoint)));
        if (event.mapObject.equals(navWaypoint))
            calculateNavigationStatus(mLastKnownLocation, 0, 0);
    }

    private ServiceConnection locationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mLocationService = (ILocationService) service;
            mLocationService.registerLocationCallback(locationListener);
            logger.debug("Location service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            mLocationService = null;
            logger.debug("Location service disconnected");
        }
    };

    private ILocationListener locationListener = new ILocationListener() {
        @Override
        public void onLocationChanged() {
            if (mLocationService == null)
                return;

            mLastKnownLocation = mLocationService.getLocation();

            if (navWaypoint != null)
                //TODO Redesign VMG, ETE calculation
                calculateNavigationStatus(mLastKnownLocation, 0, 0);
        }

        @Override
        public void onGpsStatusChanged() {
        }
    };
}
