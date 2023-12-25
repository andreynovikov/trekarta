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
import android.os.SystemClock;

import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.utils.math.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;

import mobi.maptrek.Configuration;
import mobi.maptrek.MainActivity;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.Route;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.RouteManager;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.Geo;
import mobi.maptrek.util.StringFormatter;

public class NavigationService extends BaseNavigationService implements OnSharedPreferenceChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(NavigationService.class);

    private static final int NOTIFICATION_ID = 25502;

    private static final int DEFAULT_WAYPOINT_PROXIMITY = 3;
    private static final int DEFAULT_ROUTE_PROXIMITY = 20; // TODO: implement dynamic proximity

    private ILocationService mLocationService = null;
    private Location mLastKnownLocation;
    private boolean mForeground = false;

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

    private int navSecs = 0;
    private int prevSecs = 0;
    // 10 min, 6 min, 3 min average
    private final double[] avgVMG = new double[] {0.0, 0.0, 0.0};

    private String ntTitle = null;
    private String ntBearing = null;
    private String ntDistance = null;

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
            if (extras == null)
                return START_NOT_STICKY;
            MapObject mo = new MapObject(extras.getDouble(EXTRA_LATITUDE), extras.getDouble(EXTRA_LONGITUDE));
            mo.name = extras.getString(EXTRA_NAME);
            mo.proximity = extras.getInt(EXTRA_PROXIMITY);
            navigateTo(mo);
        }
        if (action.equals(NAVIGATE_TO_OBJECT)) {
            if (extras == null)
                return START_NOT_STICKY;
            long id = extras.getLong(EXTRA_ID);
            MapObject mo = MapTrek.getMapObject(id);
            if (mo == null)
                return START_NOT_STICKY;
            navigateTo(mo);
        }
        if (action.equals(NAVIGATE_VIA_ROUTE)) {
            if (extras == null)
                return START_NOT_STICKY;
            Route route = extras.getParcelable(EXTRA_ROUTE);
            int dir = extras.getInt(EXTRA_ROUTE_DIRECTION, DIRECTION_FORWARD);
            int start = extras.getInt(EXTRA_ROUTE_START, -1);
            navigateTo(route, dir);
            if (start != -1)
                setRouteWaypoint(start);
        }
        if (action.equals(RESUME_NAVIGATION)) {
            navCurrentRoutePoint = Configuration.getNavigationRoutePoint();
            navDirection = Configuration.getNavigationRouteDirection();
            navWaypoint = Configuration.getNavigationPoint();
            if (navWaypoint == null)
                return START_NOT_STICKY;
            if (Configuration.getNavigationViaRoute()) {
                loadRoute();
                if (navRoute != null) {
                    resumeRoute();
                } else {
                    logger.error("No route to resume");
                    stopNavigation();
                }
            } else {
                resumeWaypoint();
            }
        }
        if (action.equals(STOP_NAVIGATION) || action.equals(PAUSE_NAVIGATION)) {
            mForeground = false;
            stopForeground(true);
            if (action.equals(STOP_NAVIGATION))
                stopNavigation();
            Configuration.setNavigationViaRoute(navRoute != null);
            Configuration.setNavigationRoutePoint(navCurrentRoutePoint);
            Configuration.setNavigationRouteDirection(navDirection);
            Configuration.setNavigationPoint(navWaypoint);
            stopSelf();
        }
        if (action.equals(ENABLE_BACKGROUND_NAVIGATION)) {
            mForeground = true;
            startForeground(NOTIFICATION_ID, getNotification(true));
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
        public boolean hasNextRouteWaypoint() {
            return NavigationService.this.hasNextRouteWaypoint();
        }

        @Override
        public boolean hasPrevRouteWaypoint() {
            return NavigationService.this.hasPrevRouteWaypoint();
        }

        @Override
        public void nextRouteWaypoint() {
            if (!hasNextRouteWaypoint())
                return;

            NavigationService.this.nextRouteWaypoint();
        }

        @Override
        public void prevRouteWaypoint() {
            if (!hasPrevRouteWaypoint())
                return;

            NavigationService.this.prevRouteWaypoint();
        }

        @Override
        public BoundingBox getRouteBoundingBox() {
            BoundingBox box;
            if (isNavigatingViaRoute()) {
                box = navRoute.getBoundingBox();
            } else {
                box = new BoundingBox();
                box.extend(navWaypoint.coordinates.getLatitude(), navWaypoint.coordinates.getLongitude());
            }
            if (mLastKnownLocation != null)
                box.extend(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
            return box;
        }

        @Override
        public String getInstructionText() {
            if (isNavigatingViaRoute()) {
                return navRoute.getInstructionText(navRouteCurrentIndex());
            } else {
                return navWaypoint.name;
            }
        }

        @Override
        public int getSign() {
            //noinspection StatementWithEmptyBody
            if (isNavigatingViaRoute()) {
                return navRoute.getSign(navRouteCurrentIndex());
            } else {
                // TODO: add sign for waypoint navigation?
            }
            return -1;
        }

        @Override
        public float getDistance() {
            if (isNavigatingViaRoute())
                return (float) (navRouteDistanceLeft() + navDistance);
            else
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
            if (isNavigatingViaRoute() && navRouteCurrentIndex() < navRoute.length() - 1)
                return navRouteETE(navRouteDistanceLeft() + navDistance);
            else
                return navETE;
        }

        @Override
        public float getWptDistance() {
            return (float) navDistance;
        }

        @Override
        public int getWptEte() {
            return navETE;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_NAVIGATION_TRAVERSE.equals(key)) {
            mUseTraverse = true; //sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.def_navigation_traverse));
        }
    }

    private void connect() {
        if (!EventBus.getDefault().isRegistered(this))
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
        mLastKnownLocation = null;
    }

    private Notification getNotification(boolean force) {
        String name = navWaypoint.name != null ? navWaypoint.name : getString(R.string.msgNavigatingPoint);
        String title = getString(R.string.msgNavigating, name);
        String bearing = StringFormatter.angleH(navBearing);
        String distance = StringFormatter.distanceH(navDistance);

        if (!force && title.equals(ntTitle) && bearing.equals(ntBearing) && distance.equals(ntDistance))
            return null; // not changed

        ntTitle = title;
        ntBearing = bearing;
        ntDistance = distance;

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
        PendingIntent piResult = PendingIntent.getActivity(this, 0, iLaunch, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent iStop = new Intent(STOP_NAVIGATION, null, getApplicationContext(), NavigationService.class);
        PendingIntent piStop = PendingIntent.getService(this, 0, iStop, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Icon stopIcon = Icon.createWithResource(this, R.drawable.ic_cancel_black);

        Intent iPause = new Intent(PAUSE_NAVIGATION, null, getApplicationContext(), NavigationService.class);
        PendingIntent piPause = PendingIntent.getService(this, 0, iPause, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            builder.setCategory(Notification.CATEGORY_NAVIGATION);
        else
            builder.setCategory(Notification.CATEGORY_PROGRESS);
        builder.setPriority(Notification.PRIORITY_LOW);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setColor(getResources().getColor(R.color.colorAccent, getTheme()));
        builder.setOngoing(true);
        return builder.build();
    }

    private void updateNotification() {
        if (mForeground) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification notification = getNotification(false);
            if (notification != null)
                notificationManager.notify(NOTIFICATION_ID, notification);
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

        navProximity = DEFAULT_WAYPOINT_PROXIMITY;
        navDistance = 0f;
        navBearing = 0f;
        navTurn = 0;
        navVMG = 0f;
        navETE = Integer.MAX_VALUE;
        navCourse = 0f;
        navXTK = Double.NEGATIVE_INFINITY;

        navSecs = 0;
        prevSecs = 0;
        avgVMG[0] = 0.0;
        avgVMG[1] = 0.0;
        avgVMG[2] = 0.0;

        Configuration.setNavigationPoint(null);
    }

    private void navigateTo(final MapObject waypoint) {
        if (navWaypoint != null)
            stopNavigation();
        navWaypoint = waypoint;

        resumeWaypoint();
    }

    private void resumeWaypoint() {
        connect();

        navProximity = navWaypoint.proximity > 0 ? navWaypoint.proximity : DEFAULT_WAYPOINT_PROXIMITY;
        updateNavigationState(STATE_STARTED);
        if (mLastKnownLocation != null)
            calculateNavigationStatus();
    }

    private void navigateTo(final Route route, final int direction) {
        if (navWaypoint != null)
            stopNavigation();
        navRoute = route;
        navDirection = direction;
        navCurrentRoutePoint = navDirection == 1 ? 1 : navRoute.length() - 2;

        saveRoute();
        resumeRoute();
    }

    private void resumeRoute() {
        connect();

        navWaypoint = new MapObject(navRoute.get(navCurrentRoutePoint).getCoordinates());
        prevWaypoint = new MapObject(navRoute.get(navCurrentRoutePoint - navDirection).getCoordinates());
        navProximity = DEFAULT_ROUTE_PROXIMITY;
        navRouteDistance = -1;
        navCourse = prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
        updateNavigationState(STATE_STARTED);
        updateNavigationState(STATE_NEXT_WPT);
        if (mLastKnownLocation != null)
            calculateNavigationStatus();
    }

    public void setRouteWaypoint(int waypoint) {
        navCurrentRoutePoint = waypoint;
        navWaypoint = new MapObject(navRoute.get(navCurrentRoutePoint).getCoordinates());
        int prev = navCurrentRoutePoint - navDirection;
        if (prev >= 0 && prev < navRoute.length())
            prevWaypoint = new MapObject(navRoute.get(prev).getCoordinates());
        else
            prevWaypoint = null;
        navRouteDistance = -1;
        navCourse = prevWaypoint == null ? 0d : prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
        navETE = Integer.MAX_VALUE;
        calculateNavigationStatus();
        updateNavigationState(STATE_NEXT_WPT);
    }

    public MapObject getNextRouteWaypoint() {
        int next = navCurrentRoutePoint + navDirection;
        if (next >= 0 && next < navRoute.length())
            return new MapObject(navRoute.get(next).getCoordinates());
        return null;
    }

    public void nextRouteWaypoint() throws IndexOutOfBoundsException {
        navCurrentRoutePoint += navDirection;
        navWaypoint = new MapObject(navRoute.get(navCurrentRoutePoint).getCoordinates());
        prevWaypoint = new MapObject(navRoute.get(navCurrentRoutePoint - navDirection).getCoordinates());
        navRouteDistance = -1;
        navCourse = prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
        if (avgVMG[0] < 0) avgVMG[0] = 0.0;
        if (avgVMG[1] < 0) avgVMG[1] = 0.0;
        if (avgVMG[2] < 0) avgVMG[2] = 0.0;
        navETE = Integer.MAX_VALUE;
        calculateNavigationStatus();
        updateNavigationState(STATE_NEXT_WPT);
    }

    public void prevRouteWaypoint() throws IndexOutOfBoundsException {
        navCurrentRoutePoint -= navDirection;
        navWaypoint = new MapObject(navRoute.get(navCurrentRoutePoint).getCoordinates());
        int prev = navCurrentRoutePoint - navDirection;
        if (prev >= 0 && prev < navRoute.length())
            prevWaypoint = new MapObject(navRoute.get(prev).getCoordinates());
        else
            prevWaypoint = null;
        navRouteDistance = -1;
        navCourse = prevWaypoint == null ? 0d : prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
        if (avgVMG[0] < 0) avgVMG[0] = 0.0;
        if (avgVMG[1] < 0) avgVMG[1] = 0.0;
        if (avgVMG[2] < 0) avgVMG[2] = 0.0;
        navETE = Integer.MAX_VALUE;
        calculateNavigationStatus();
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
     * @param index waypoint index
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

    /**
     * Calculates ETE for route segment.
     *
     * @param index waypoint index
     * @return segment ETE
     */
    public int navRouteWaypointETE(int index) {
        if (index == 0)
            return 0;
        int ete = Integer.MAX_VALUE;
        if (avgVMG[0] > 0) {
            int i = navDirection == DIRECTION_FORWARD ? index : navRoute.length() - index - 1;
            int j = i - navDirection;
            MapObject w1 = new MapObject(navRoute.get(i).getCoordinates());
            MapObject w2 = new MapObject(navRoute.get(j).getCoordinates());
            double distance = w1.coordinates.vincentyDistance(w2.coordinates);
            ete = (int) Math.round(distance / avgVMG[0] / 60);
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
        if (avgVMG[0] > 0) {
            eta = (int) Math.round(distance / avgVMG[0] / 60);
        }
        return eta;
    }

    public int navRouteETETo(int index) {
        double distance = navRouteDistanceLeftTo(index);
        if (distance <= 0.0)
            return 0;

        return navRouteETE(distance);
    }

    private void calculateNavigationStatus() {
        int secs = (int) (mLastKnownLocation.getElapsedRealtimeNanos() * 1e-9);
        int diff = secs - prevSecs;
        if (diff < 1)
            return;

        //if (diff < 600)
            navSecs += diff;
        prevSecs = secs;

        GeoPoint point = new GeoPoint(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
        double distance = point.vincentyDistance(navWaypoint.coordinates);
        double bearing = point.bearingTo(navWaypoint.coordinates);

        // turn
        long turn = Math.round(bearing - mLastKnownLocation.getBearing());
        if (Math.abs(turn) > 180) {
            turn = turn - (long) (Math.signum(turn)) * 360;
        }

        // vmg
        double vmg = Geo.vmg(mLastKnownLocation.getSpeed(), Math.abs(turn));
        avgVMG[0] = movingAverage(vmg, avgVMG[0], MathUtils.clamp(600 - diff, 1, navSecs)); // 10 minutes average
        avgVMG[1] = movingAverage(vmg, avgVMG[1], MathUtils.clamp(360 - diff, 1, navSecs)); // 6 minutes average
        avgVMG[2] = movingAverage(vmg, avgVMG[2], MathUtils.clamp(180 - diff, 1, navSecs)); // 3 minutes average

        // ete
        int ete = Integer.MAX_VALUE;
        if (navETE <= 3 && avgVMG[2] > 0 && avgVMG[2] > avgVMG[1]) // otherwise ete can jump back and forth
            ete = (int) Math.round(distance / avgVMG[2] / 60);
        else if (navETE <= 6 && avgVMG[1] > 0 && avgVMG[1] > avgVMG[0])
            ete = (int) Math.round(distance / avgVMG[1] / 60);
        else if (avgVMG[0] > 0)
            ete = (int) Math.round(distance / avgVMG[0] / 60);

        double xtk = Double.NEGATIVE_INFINITY;

        boolean hasNext = hasNextRouteWaypoint();
        if (distance < navProximity) {
            if (hasNext) {
                nextRouteWaypoint();
            } else {
                updateNavigationState(STATE_REACHED);
                stopNavigation();
            }
            return;
        }

        if (prevWaypoint != null) {
            double dtk = prevWaypoint.coordinates.bearingTo(navWaypoint.coordinates);
            xtk = Geo.xtk(distance, dtk, bearing);

            if (xtk == Double.NEGATIVE_INFINITY) {
                if (mUseTraverse && hasNext) {
                    MapObject nextWpt = getNextRouteWaypoint();
                    if (nextWpt != null) {
                        double dtk2 = nextWpt.coordinates.bearingTo(navWaypoint.coordinates);
                        double xtk2 = Geo.xtk(0, dtk2, bearing);
                        if (xtk2 != Double.NEGATIVE_INFINITY) {
                            nextRouteWaypoint();
                            return;
                        }
                    }
                }
            }
        }

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

    /** @noinspection unused*/
    @Subscribe
    public void onMapObjectUpdated(MapObject.UpdatedEvent event) {
        logger.error("onMapObjectUpdated({})", (event.mapObject.equals(navWaypoint)));
        if (event.mapObject.equals(navWaypoint))
            calculateNavigationStatus();
    }

    private void saveRoute() {
        File dataDir = getExternalFilesDir("data");
        if (dataDir == null) {
            logger.error("Can not save route: application data folder missing");
            return; // TODO: what to do?
        }
        FileDataSource source = new FileDataSource();
        source.name = "CurrentRoute";
        File file = new File(dataDir, FileUtils.sanitizeFilename(source.name) + RouteManager.EXTENSION);
        source.path = file.getAbsolutePath();
        source.routes.add(navRoute);
        Manager.save(source, new Manager.OnSaveListener() {
            @Override
            public void onSaved(FileDataSource source) {
                // TODO: what to do?
            }

            @Override
            public void onError(FileDataSource source, Exception e) {
                // TODO: what to do?
            }
        });
    }

    private void loadRoute() {
        File dataDir = getExternalFilesDir("data");
        if (dataDir == null) {
            logger.error("Can not load route: application data folder missing");
            return; // TODO: what to do?
        }
        File file = new File(dataDir, FileUtils.sanitizeFilename("CurrentRoute") + RouteManager.EXTENSION);
        Manager manager = Manager.getDataManager(file.getName());
        if (manager != null) {
            try {
                FileDataSource source = manager.loadData(new FileInputStream(file), file.getAbsolutePath());
                source.path = file.getAbsolutePath();
                source.setLoaded();
                navRoute = source.routes.get(0);
            } catch (Exception e) {
                logger.error("Saved route not found");
            }
        }
    }

    private final ServiceConnection locationConnection = new ServiceConnection() {
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

    private final ILocationListener locationListener = new ILocationListener() {
        @Override
        public void onLocationChanged() {
            if (mLocationService == null)
                return;
            Location location = mLocationService.getLocation();
            if ((SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) > 1e+9 * 30) // do not trust location which is more then 30 seconds old
                return;
            mLastKnownLocation = location;
            if (prevSecs == 0)
                prevSecs = (int) (mLastKnownLocation.getElapsedRealtimeNanos() * 1e-9);

            if (navWaypoint != null) {
                if (prevWaypoint == null) // set to current location to correctly calculate XTK
                    prevWaypoint = new MapObject(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                calculateNavigationStatus();
            }
        }

        @Override
        public void onGpsStatusChanged() {
        }
    };

    private double movingAverage(double current, double previous, int ratio) {
        // return (1.0 - ratio) * previous + ratio * current;
        // https://stackoverflow.com/a/50854247
        return previous + (current - previous) / ratio;
    }
}
