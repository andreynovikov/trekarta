package mobi.maptrek;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;

import mobi.maptrek.data.MapObject;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.view.GaugePanel;

public class Configuration {
    private static final String PREF_LATITUDE = "latitude";
    private static final String PREF_LONGITUDE = "longitude";
    private static final String PREF_MAP_SCALE = "map_scale";
    private static final String PREF_MAP_BEARING = "map_bearing";
    private static final String PREF_MAP_TILT = "map_tilt";
    private static final String PREF_MAP_3D_BUILDINGS = "map_3d_buildings";
    private static final String PREF_MAP_GRID = "map_grid";
    private static final String PREF_BITMAP_MAP = "bitmap_map";
    private static final String PREF_POINTS_COUNTER = "wpt_counter";
    private static final String PREF_LOCATION_STATE = "location_state";
    private static final String PREF_PREVIOUS_LOCATION_STATE = "previous_location_state";
    private static final String PREF_TRACKING_STATE = "tracking_state";
    private static final String PREF_ACTION_PANEL_STATE = "action_panel_state";
    private static final String PREF_NAVIGATION_WAYPOINT = "navigation_waypoint";
    private static final String PREF_NAVIGATION_LATITUDE = "navigation_waypoint_latitude";
    private static final String PREF_NAVIGATION_LONGITUDE = "navigation_waypoint_longitude";
    private static final String PREF_NAVIGATION_PROXIMITY = "navigation_waypoint_proximity";
    private static final String PREF_GAUGES = "gauges";
    private static final String PREF_ADVICE_STATES = "advice_states";
    private static final String PREF_NIGHT_MODE_STATE = "night_mode_state";
    private static final String PREF_LANGUAGE = "language";

    public static final long ADVICE_UPDATE_EXTERNAL_SOURCE = 0x0000000000000001;
    public static final long ADVICE_SUNRISE_SUNSET = 0x0000000000000002;

    private static SharedPreferences mSharedPreferences;

    public static void initialize(SharedPreferences sharedPreferences) {
        mSharedPreferences = sharedPreferences;
    }

    public static boolean initialized() {
        return mSharedPreferences != null;
    }

    public static int getPointsCounter() {
        assert mSharedPreferences != null : "Configuration not initialized";
        int counter = mSharedPreferences.getInt(PREF_POINTS_COUNTER, 0);
        counter++;
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREF_POINTS_COUNTER, counter);
        editor.apply();
        return counter;
    }

    public static int getLocationState() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getInt(PREF_LOCATION_STATE, 0);
    }

    public static void setLocationState(int locationState) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREF_LOCATION_STATE, locationState);
        editor.apply();
    }

    public static int getPreviousLocationState() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getInt(PREF_PREVIOUS_LOCATION_STATE, 0);
    }

    public static void setPreviousLocationState(int locationState) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREF_PREVIOUS_LOCATION_STATE, locationState);
        editor.apply();
    }

    public static int getTrackingState() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getInt(PREF_TRACKING_STATE, 0);
    }

    public static void setTrackingState(int trackingState) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREF_TRACKING_STATE, trackingState);
        editor.apply();
    }

    public static boolean getActionPanelState() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getBoolean(PREF_ACTION_PANEL_STATE, true);
    }

    public static void setActionPanelState(boolean panelState) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PREF_ACTION_PANEL_STATE, panelState);
        editor.apply();
    }

    /**
     * Returns saved navigation waypoint and removes it from memory.
     */
    @Nullable
    public static MapObject getNavigationPoint() {
        assert mSharedPreferences != null : "Configuration not initialized";
        MapObject waypoint = null;
        String navWpt = mSharedPreferences.getString(PREF_NAVIGATION_WAYPOINT, null);
        if (navWpt != null) {
            waypoint = new MapObject();
            waypoint.name = navWpt;
            waypoint.latitude = (double) mSharedPreferences.getFloat(PREF_NAVIGATION_LATITUDE, 0);
            waypoint.longitude = (double) mSharedPreferences.getFloat(PREF_NAVIGATION_LONGITUDE, 0);
            waypoint.proximity = mSharedPreferences.getInt(PREF_NAVIGATION_PROXIMITY, 0);
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(PREF_NAVIGATION_WAYPOINT, null);
            editor.apply();
        }
        return waypoint;
    }

    public static void setNavigationPoint(@Nullable MapObject mapObject) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        if (mapObject != null) {
            editor.putString(PREF_NAVIGATION_WAYPOINT, mapObject.name);
            editor.putFloat(PREF_NAVIGATION_LATITUDE, (float) mapObject.latitude);
            editor.putFloat(PREF_NAVIGATION_LONGITUDE, (float) mapObject.longitude);
            editor.putInt(PREF_NAVIGATION_PROXIMITY, mapObject.proximity);
        } else {
            editor.putString(PREF_NAVIGATION_WAYPOINT, null);
        }
        editor.apply();
    }

    public static String getGauges() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getString(PREF_GAUGES, GaugePanel.DEFAULT_GAUGE_SET);
    }

    public static void setGauges(String gauges) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREF_GAUGES, gauges);
        editor.apply();
    }

    @NonNull
    public static MapPosition getPosition() {
        assert mSharedPreferences != null : "Configuration not initialized";
        MapPosition mapPosition = new MapPosition();
        int latitudeE6 = mSharedPreferences.getInt(PREF_LATITUDE, 0);
        int longitudeE6 = mSharedPreferences.getInt(PREF_LONGITUDE, 0);
        float scale = mSharedPreferences.getFloat(PREF_MAP_SCALE, 1);
        float bearing = mSharedPreferences.getFloat(PREF_MAP_BEARING, 0);
        float tilt = mSharedPreferences.getFloat(PREF_MAP_TILT, 0);
        mapPosition.setPosition(latitudeE6 / 1E6, longitudeE6 / 1E6);
        mapPosition.setScale(scale);
        mapPosition.setBearing(bearing);
        mapPosition.setTilt(tilt);
        return mapPosition;
    }

    public static void setPosition(@NonNull MapPosition mapPosition) {
        assert mSharedPreferences != null : "Configuration not initialized";
        GeoPoint geoPoint = mapPosition.getGeoPoint();
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREF_LATITUDE, geoPoint.latitudeE6);
        editor.putInt(PREF_LONGITUDE, geoPoint.longitudeE6);
        editor.putFloat(PREF_MAP_SCALE, (float) mapPosition.scale);
        editor.putFloat(PREF_MAP_BEARING, mapPosition.bearing);
        editor.putFloat(PREF_MAP_TILT, mapPosition.tilt);
        editor.apply();
    }

    public static boolean getBuildingsLayerEnabled() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getBoolean(PREF_MAP_3D_BUILDINGS, true);
    }

    public static void setBuildingsLayerEnabled(boolean buildingsLayerEnabled) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PREF_MAP_3D_BUILDINGS, buildingsLayerEnabled);
        editor.apply();
    }

    public static boolean getGridLayerEnabled() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getBoolean(PREF_MAP_GRID, false);
    }

    public static void setGridLayerEnabled(boolean GRIDLayerEnabled) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PREF_MAP_GRID, GRIDLayerEnabled);
        editor.apply();
    }

    @Nullable
    public static String getBitmapMap() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getString(PREF_BITMAP_MAP, null);
    }

    public static void setBitmapMap(@Nullable MapFile mapFile) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        if (mapFile != null)
            editor.putString(PREF_BITMAP_MAP, mapFile.fileName);
        else
            editor.putString(PREF_BITMAP_MAP, null);
        editor.apply();
    }

    public static boolean getAdviceState(long advice) {
        assert mSharedPreferences != null : "Configuration not initialized";
        return (mSharedPreferences.getLong(PREF_ADVICE_STATES, 0L) & advice) == 0L;
    }

    public static void setAdviceState(long advice) {
        assert mSharedPreferences != null : "Configuration not initialized";
        long state = mSharedPreferences.getLong(PREF_ADVICE_STATES, 0L);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(PREF_ADVICE_STATES, state | advice);
        editor.apply();
    }

    public static void resetAdviceState() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(PREF_ADVICE_STATES, 0L);
        editor.apply();
    }

    public static int getNightModeState() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getInt(PREF_NIGHT_MODE_STATE, 0);
    }

    public static void setNightModeState(int nightModeState) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREF_NIGHT_MODE_STATE, nightModeState);
        editor.apply();
    }

    public static String getLanguage() {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getString(PREF_LANGUAGE, null);
    }

    public static void setLanguage(String language) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREF_LANGUAGE, language);
        editor.apply();
    }
}
