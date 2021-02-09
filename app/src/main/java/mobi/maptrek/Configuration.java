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

package mobi.maptrek;

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;

import java.util.Collection;

import mobi.maptrek.data.MapObject;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.view.GaugePanel;

@SuppressWarnings("WeakerAccess")
public class Configuration {
    private static final String PREF_LATITUDE = "latitude";
    private static final String PREF_LONGITUDE = "longitude";
    private static final String PREF_MAP_SCALE = "map_scale";
    private static final String PREF_MAP_BEARING = "map_bearing";
    private static final String PREF_MAP_TILT = "map_tilt";
    private static final String PREF_MAP_3D_BUILDINGS = "map_3d_buildings";
    private static final String PREF_MAP_CONTOURS = "map_contours";
    public static final String PREF_MAP_HILLSHADES = "map_hillshades";
    public static final String PREF_HILLSHADES_TRANSPARENCY = "hillshades_transparency";
    private static final String PREF_MAP_GRID = "map_grid";
    private static final String PREF_BITMAP_MAP = "bitmap_map";
    private static final String PREF_POINTS_COUNTER = "wpt_counter";
    private static final String PREF_UID = "uid";
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
    private static final String PREF_MAP_STYLE = "map_style";
    private static final String PREF_ACTIVITY = "activity";
    private static final String PREF_MAP_FONT_SIZE = "map_font_size";
    private static final String PREF_MAP_USER_SCALE = "map_user_scale";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_HIDE_MAP_OBJECTS = "hide_map_objects";
    private static final String PREF_BITMAP_MAP_TRANSPARENCY = "bitmap_map_transparency";
    private static final String PREF_EXCEPTION_SIZE = "exception_size";
    public static final String PREF_ZOOM_BUTTONS_VISIBLE = "zoom_buttons_visible";
    public static final String PREF_ACCESSIBILITY_BADGES = "accessibility_badges";
    public static final String PREF_SPEED_UNIT = "speed_unit";
    public static final String PREF_DISTANCE_UNIT = "distance_unit";
    public static final String PREF_ELEVATION_UNIT = "elevation_unit";
    public static final String PREF_ANGLE_UNIT = "angle_unit";
    public static final String PREF_UNIT_PRECISION = "unit_precision";
    private static final String PREF_COORDINATES_FORMAT = "coordinates_format";
    private static final String PREF_REMEMBERED_SCALE = "remembered_scale";
    private static final String PREF_AUTO_TILT = "auto_tilt";
    private static final String PREF_HIDE_SYSTEM_UI = "hide_system_ui";
    private static final String PREF_ACTION_RATING = "action_rating";
    private static final String LAST_SEEN_INTRODUCTION = "last_seen_introduction";
    private static final String LAST_SEEN_CHANGELOG = "last_seen_changelog";
    private static final String PREF_RUNNING_TIME = "running_time";
    private static final String PREF_TRACKING_TIME = "tracking_time";
    private static final String PREF_FULLSCREEN_TIMES = "fullscreen_times";
    private static final String PREF_EXTERNAL_STORAGE = "external_storage";
    private static final String PREF_NEW_EXTERNAL_STORAGE = "new_external_storage";
    private static final String PREF_HIKING_TIMES = "hiking_times";
    private static final String PREF_CYCLING_TIMES = "cycling_times";
    private static final String PREF_SKIING_TIMES = "skiing_times";
    private static final String PREF_HIGHLIGHTED_TYPE = "highlighted_type";

    public static final long ADVICE_IMPORTANCE_MASK = 0x0000000000003400L;
    public static final long ADVICE_UPDATE_EXTERNAL_SOURCE = 0x0000000000000001L;
    public static final long ADVICE_SUNRISE_SUNSET = 0x0000000000000002L;
    public static final long ADVICE_MORE_GAUGES = 0x0000000000000004L;
    public static final long ADVICE_REMEMBER_SCALE = 0x0000000000000008L;
    public static final long ADVICE_ENABLE_LOCATIONS = 0x0000000000000010L;
    public static final long ADVICE_MAP_SETTINGS = 0x0000000000000020L;
    public static final long ADVICE_ADDING_PLACE = 0x0000000000000040L;
    public static final long ADVICE_RECORD_TRACK = 0x0000000000000080L;
    public static final long ADVICE_RECORDED_TRACKS = 0x0000000000000100L;
    public static final long ADVICE_VIEW_DATA_ITEM = 0x0000000000000200L;
    public static final long ADVICE_SWITCH_COORDINATES_FORMAT = 0x0000000000000400L;
    public static final long ADVICE_LOCKED_COORDINATES = 0x0000000000000800L;
    public static final long ADVICE_HIDE_MAP_OBJECTS = 0x0000000000001000L;
    public static final long ADVICE_LOCK_MAP_POSITION = 0x0000000000002000L;
    public static final long ADVICE_TEXT_SEARCH = 0x0000000000004000L;
    public static final long ADVICE_ACTIVE_MAPS_SIZE = 0x0000000000008000L;
    public static final long ADVICE_AMENITY_SETUP = 0x0000000000010000L;
    public static final long ADVICE_MAP_LEGEND = 0x0000000000020000L;
    public static final long ADVICE_NIGHT_MODE = 0x0000000000040000L;
    public static final long ADVICE_SELECT_MULTIPLE_MAPS = 0x0000000000080000L;

    private static long mAdviceMask;
    private static SharedPreferences mSharedPreferences;

    public static void initialize(SharedPreferences sharedPreferences) {
        mAdviceMask = 0L;
        mSharedPreferences = sharedPreferences;
    }

    public static boolean isInitialized() {
        return mSharedPreferences != null;
    }

    public static int getPointsCounter() {
        int counter = loadInt(PREF_POINTS_COUNTER, 0) + 1;
        saveInt(PREF_POINTS_COUNTER, counter);
        return counter;
    }

    public static long getUID() {
        long uid = loadLong(PREF_UID, 0L) + 1L;
        saveLong(PREF_UID, uid);
        return uid;
    }

    public static int getLocationState() {
        return loadInt(PREF_LOCATION_STATE, LocationState.DISABLED.ordinal());
    }

    public static void setLocationState(int locationState) {
        saveInt(PREF_LOCATION_STATE, locationState);
    }

    public static int getPreviousLocationState() {
        return loadInt(PREF_PREVIOUS_LOCATION_STATE, LocationState.NORTH.ordinal());
    }

    public static void setPreviousLocationState(int locationState) {
        saveInt(PREF_PREVIOUS_LOCATION_STATE, locationState);
    }

    public static int getTrackingState() {
        return loadInt(PREF_TRACKING_STATE, 0);
    }

    public static void setTrackingState(int trackingState) {
        saveInt(PREF_TRACKING_STATE, trackingState);
    }

    public static boolean getActionPanelState() {
        return loadBoolean(PREF_ACTION_PANEL_STATE, true);
    }

    public static void setActionPanelState(boolean panelState) {
        saveBoolean(PREF_ACTION_PANEL_STATE, panelState);
    }

    /**
     * Returns saved navigation waypoint and removes it from memory.
     */
    @Nullable
    public static MapObject getNavigationPoint() {
        MapObject waypoint = null;
        String navWpt = loadString(PREF_NAVIGATION_WAYPOINT, null);
        if (navWpt != null) {
            waypoint = new MapObject(mSharedPreferences.getFloat(PREF_NAVIGATION_LATITUDE, 0), mSharedPreferences.getFloat(PREF_NAVIGATION_LONGITUDE, 0));
            waypoint.name = navWpt;
            waypoint.proximity = loadInt(PREF_NAVIGATION_PROXIMITY, 0);
            saveString(PREF_NAVIGATION_WAYPOINT, null);
        }
        return waypoint;
    }

    public static void setNavigationPoint(@Nullable MapObject mapObject) {
        if (mapObject != null) {
            saveString(PREF_NAVIGATION_WAYPOINT, mapObject.name);
            saveFloat(PREF_NAVIGATION_LATITUDE, (float) mapObject.coordinates.getLatitude());
            saveFloat(PREF_NAVIGATION_LONGITUDE, (float) mapObject.coordinates.getLongitude());
            saveInt(PREF_NAVIGATION_PROXIMITY, mapObject.proximity);
        } else {
            saveString(PREF_NAVIGATION_WAYPOINT, null);
        }
    }

    public static String getGauges() {
        return loadString(PREF_GAUGES, GaugePanel.DEFAULT_GAUGE_SET);
    }

    public static void setGauges(String gauges) {
        saveString(PREF_GAUGES, gauges);
    }

    @NonNull
    public static MapPosition getPosition() {
        MapPosition mapPosition = new MapPosition();
        int latitudeE6 = loadInt(PREF_LATITUDE, 0);
        int longitudeE6 = loadInt(PREF_LONGITUDE, 0);
        float scale = loadFloat(PREF_MAP_SCALE, 1);
        float bearing = loadFloat(PREF_MAP_BEARING, 0);
        float tilt = loadFloat(PREF_MAP_TILT, 0);
        mapPosition.setPosition(latitudeE6 / 1E6, longitudeE6 / 1E6);
        mapPosition.setScale(scale);
        mapPosition.setBearing(bearing);
        mapPosition.setTilt(tilt);
        return mapPosition;
    }

    public static void setPosition(@NonNull MapPosition mapPosition) {
        GeoPoint geoPoint = mapPosition.getGeoPoint();
        saveInt(PREF_LATITUDE, geoPoint.latitudeE6);
        saveInt(PREF_LONGITUDE, geoPoint.longitudeE6);
        saveFloat(PREF_MAP_SCALE, (float) mapPosition.scale);
        saveFloat(PREF_MAP_BEARING, mapPosition.bearing);
        saveFloat(PREF_MAP_TILT, mapPosition.tilt);
    }

    public static boolean getBuildingsLayerEnabled() {
        return loadBoolean(PREF_MAP_3D_BUILDINGS, true);
    }

    public static void setBuildingsLayerEnabled(boolean buildingsLayerEnabled) {
        saveBoolean(PREF_MAP_3D_BUILDINGS, buildingsLayerEnabled);
    }

    public static boolean getContoursEnabled() {
        return loadBoolean(PREF_MAP_CONTOURS, true);
    }

    public static void setContoursEnabled(boolean contoursEnabled) {
        saveBoolean(PREF_MAP_CONTOURS, contoursEnabled);
    }

    public static boolean getHillshadesEnabled() {
        return loadBoolean(PREF_MAP_HILLSHADES, false); // disabled until first time downloaded
    }

    public static void setHillshadesEnabled(boolean hillshadesEnabled) {
        saveBoolean(PREF_MAP_HILLSHADES, hillshadesEnabled);
    }

    public static int getHillshadesTransparency() {
        return loadInt(PREF_HILLSHADES_TRANSPARENCY, 50);
    }

    public static boolean getGridLayerEnabled() {
        return loadBoolean(PREF_MAP_GRID, false);
    }

    public static void setGridLayerEnabled(boolean GRIDLayerEnabled) {
        saveBoolean(PREF_MAP_GRID, GRIDLayerEnabled);
    }

    @Nullable
    public static String[] getBitmapMaps() {
        String filenames = loadString(PREF_BITMAP_MAP, null);
        if (filenames == null)
            return null;
        return filenames.split(";");
    }

    public static void setBitmapMaps(@NonNull Collection<MapFile> mapFiles) {
        if (mapFiles.isEmpty()) {
            saveString(PREF_BITMAP_MAP, null);
        } else {
            String[] filenames = new String[mapFiles.size()];
            int i = 0;
            for (MapFile mapFile : mapFiles) {
                filenames[i] = mapFile.tileSource.getOption("path");
                i++;
            }
            saveString(PREF_BITMAP_MAP, TextUtils.join(";", filenames));
        }
    }

    public static boolean getAdviceState(long advice) {
        return ((loadLong(PREF_ADVICE_STATES, 0L) & advice) | (advice & mAdviceMask)) == 0L;
    }

    public static void setAdviceState(long advice) {
        long state = loadLong(PREF_ADVICE_STATES, 0L);
        saveLong(PREF_ADVICE_STATES, state | advice);
        mAdviceMask = ADVICE_IMPORTANCE_MASK;
    }

    public static void clearAdviceState(long advice) {
        long state = loadLong(PREF_ADVICE_STATES, 0L);
        saveLong(PREF_ADVICE_STATES, state ^ advice);
    }

    public static void resetAdviceState() {
        saveLong(PREF_ADVICE_STATES, 0L);
        mAdviceMask = 0L;
    }

    public static int getNightModeState() {
        return loadInt(PREF_NIGHT_MODE_STATE, 0);
    }

    public static void setNightModeState(int nightModeState) {
        saveInt(PREF_NIGHT_MODE_STATE, nightModeState);
    }

    public static int getMapStyle() {
        return loadInt(PREF_MAP_STYLE, 2);
    }

    public static void setMapStyle(int style) {
        saveInt(PREF_MAP_STYLE, style);
    }

    public static int getActivity() {
        return loadInt(PREF_ACTIVITY, 0);
    }

    public static void setActivity(int activity) {
        saveInt(PREF_ACTIVITY, activity);
    }

    public static int getMapFontSize() {
        return loadInt(PREF_MAP_FONT_SIZE, 2);
    }

    public static void setMapFontSize(int mapFontSize) {
        saveInt(PREF_MAP_FONT_SIZE, mapFontSize);
    }

    public static int getMapUserScale() {
        return loadInt(PREF_MAP_USER_SCALE, 2);
    }

    public static void setMapUserScale(int mapScale) {
        saveInt(PREF_MAP_USER_SCALE, mapScale);
    }

    public static String getLanguage() {
        return loadString(PREF_LANGUAGE, null);
    }

    public static void setLanguage(String language) {
        saveString(PREF_LANGUAGE, language);
    }

    public static boolean getHideMapObjects() {
        return loadBoolean(PREF_HIDE_MAP_OBJECTS, true);
    }

    public static void setHideMapObjects(boolean hideMapObjects) {
        saveBoolean(PREF_HIDE_MAP_OBJECTS, hideMapObjects);
    }

    public static int getBitmapMapTransparency() {
        return loadInt(PREF_BITMAP_MAP_TRANSPARENCY, 0);
    }

    public static void setBitmapMapTransparency(int transparency) {
        saveInt(PREF_BITMAP_MAP_TRANSPARENCY, transparency);
    }

    public static long getExceptionSize() {
        return loadLong(PREF_EXCEPTION_SIZE, 0L);
    }

    public static void setExceptionSize(long size) {
        saveLong(PREF_EXCEPTION_SIZE, size);
    }

    public static int getSpeedUnit() {
        return Integer.parseInt(loadString(PREF_SPEED_UNIT, "0"));
    }

    public static int getDistanceUnit() {
        return Integer.parseInt(loadString(PREF_DISTANCE_UNIT, "0"));
    }

    public static int getElevationUnit() {
        return Integer.parseInt(loadString(PREF_ELEVATION_UNIT, "0"));
    }

    public static int getAngleUnit() {
        return Integer.parseInt(loadString(PREF_ANGLE_UNIT, "0"));
    }

    public static boolean getUnitPrecision() {
        return loadBoolean(PREF_UNIT_PRECISION, false);
    }

    public static boolean getZoomButtonsVisible() {
        return loadBoolean(PREF_ZOOM_BUTTONS_VISIBLE, false);
    }

    public static boolean getAccessibilityBadgesEnabled() {
        return loadBoolean(PREF_ACCESSIBILITY_BADGES, true);
    }

    public static int getCoordinatesFormat() {
        return loadInt(PREF_COORDINATES_FORMAT, 0);
    }

    public static void setCoordinatesFormat(int format) {
        saveInt(PREF_COORDINATES_FORMAT, format);
    }

    public static float getRememberedScale() {
        return loadFloat(PREF_REMEMBERED_SCALE, (1 << 15) - 5f);
    }

    public static void setRememberedScale(float scale) {
        saveFloat(PREF_REMEMBERED_SCALE, scale);
    }

    public static float getAutoTilt() {
        return loadFloat(PREF_AUTO_TILT, 65f);
    }

    public static void setAutoTilt(float tilt) {
        saveFloat(PREF_AUTO_TILT, tilt);
    }

    public static boolean getHideSystemUI() {
        return loadBoolean(PREF_HIDE_SYSTEM_UI, false);
    }

    public static void setHideSystemUI(boolean hide) {
        saveBoolean(PREF_HIDE_SYSTEM_UI, hide);
    }

    public static boolean ratingActionPerformed() {
        return loadBoolean(PREF_ACTION_RATING, false);
    }

    public static void setRatingActionPerformed() {
        saveBoolean(PREF_ACTION_RATING, true);
    }

    public static int getLastSeenIntroduction() {
        return loadInt(LAST_SEEN_INTRODUCTION, 0);
    }

    public static void setLastSeenIntroduction(int last) {
        saveInt(LAST_SEEN_INTRODUCTION, last);
    }

    public static int getLastSeenChangelog() {
        return loadInt(LAST_SEEN_CHANGELOG, 0);
    }

    public static void setLastSeenChangelog(int code) {
        saveInt(LAST_SEEN_CHANGELOG, code);
    }

    public static String getExternalStorage() {
        return loadString(PREF_EXTERNAL_STORAGE, null);
    }

    public static void setExternalStorage(String storage) {
        saveString(PREF_EXTERNAL_STORAGE, storage);
    }

    public static String getNewExternalStorage() {
        return loadString(PREF_NEW_EXTERNAL_STORAGE, null);
    }

    public static void setNewExternalStorage(String storage) {
        saveString(PREF_NEW_EXTERNAL_STORAGE, storage);
    }

    public static int getHighlightedType() {
        return loadInt(PREF_HIGHLIGHTED_TYPE, -1);
    }

    public static void setHighlightedType(int type) {
        saveInt(PREF_HIGHLIGHTED_TYPE, type);
    }

    public static long getRunningTime() {
        return loadLong(PREF_RUNNING_TIME, 0L);
    }

    public static void updateRunningTime(long time) {
        saveLong(PREF_RUNNING_TIME, Configuration.getRunningTime() + time);
    }

    public static long getTrackingTime() {
        return loadLong(PREF_TRACKING_TIME, 0L);
    }

    public static void updateTrackingTime(long time) {
        saveLong(PREF_TRACKING_TIME, Configuration.getTrackingTime() + time);
    }

    public static int getFullScreenTimes() {
        return loadInt(PREF_FULLSCREEN_TIMES, 0);
    }

    public static void accountFullScreen() {
        saveInt(PREF_FULLSCREEN_TIMES, Configuration.getFullScreenTimes() + 1);
    }

    public static int getHikingTimes() {
        return loadInt(PREF_HIKING_TIMES, 0);
    }

    public static void accountHiking() {
        saveInt(PREF_HIKING_TIMES, Configuration.getHikingTimes() + 1);
    }

    public static int getCyclingTimes() {
        return loadInt(PREF_CYCLING_TIMES, 0);
    }

    public static void accountCycling() {
        saveInt(PREF_CYCLING_TIMES, Configuration.getCyclingTimes() + 1);
    }

    public static int getSkiingTimes() {
        return loadInt(PREF_SKIING_TIMES, 0);
    }

    public static void accountSkiing() {
        saveInt(PREF_SKIING_TIMES, Configuration.getSkiingTimes() + 1);
    }

    private static int loadInt(String key, int defValue) {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getInt(key, defValue);
    }
    
    private static void saveInt(String key, int value) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
        EventBus.getDefault().post(new ChangedEvent(key));
    }

    private static long loadLong(String key, long defValue) {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getLong(key, defValue);
    }

    private static void saveLong(String key, long value) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
        EventBus.getDefault().post(new ChangedEvent(key));
    }

    private static float loadFloat(String key, float defValue) {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getFloat(key, defValue);
    }

    private static void saveFloat(String key, float value) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putFloat(key, value);
        editor.apply();
        EventBus.getDefault().post(new ChangedEvent(key));
    }

    private static boolean loadBoolean(String key, boolean defValue) {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getBoolean(key, defValue);
    }

    private static void saveBoolean(String key, boolean value) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
        EventBus.getDefault().post(new ChangedEvent(key));
    }

    private static String loadString(String key, String defValue) {
        assert mSharedPreferences != null : "Configuration not initialized";
        return mSharedPreferences.getString(key, defValue);
    }

    private static void saveString(String key, String value) {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
        EventBus.getDefault().post(new ChangedEvent(key));
    }

    public static boolean commit() {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        return editor.commit();
    }

    public static void loadKindZoomState() {
        assert mSharedPreferences != null : "Configuration not initialized";
        for (int i = 0; i < Tags.kinds.length; i++) {
            Tags.kindZooms[i] = mSharedPreferences.getInt(Tags.kinds[i], Tags.kindZooms[i]);
        }
    }

    public static void saveKindZoomState() {
        assert mSharedPreferences != null : "Configuration not initialized";
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        for (int i = 0; i < Tags.kinds.length; i++) {
            editor.putInt(Tags.kinds[i], Tags.kindZooms[i]);
            EventBus.getDefault().post(new ChangedEvent(Tags.kinds[i]));
        }
        editor.apply();
    }

    /**
     * Event bus
     */
    public static class ChangedEvent {
        public String key;

        public ChangedEvent(String key) {
            this.key = key;
        }
    }
}
