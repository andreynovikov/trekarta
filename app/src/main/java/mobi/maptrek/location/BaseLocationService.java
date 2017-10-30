package mobi.maptrek.location;

import android.app.Service;

public abstract class BaseLocationService extends Service {
    public static final String MAPTREK_LOCATION_SERVICE = "mobi.maptrek.location";

    /**
     * Service command to enable track recording.
     */
    public static final String ENABLE_TRACK = "mobi.maptrek.location.enableTrack";
    /**
     * Service command to pause track recording. Is used only in background mode when service
     * alone.
     */
    public static final String PAUSE_TRACK = "mobi.maptrek.location.pauseTrack";
    /**
     * Service command to stop track recording. If track is long enough it is automatically saved.
     * Anyway <code>BROADCAST_TRACK_SAVE</code> is sent to notify listeners.
     */
    public static final String DISABLE_TRACK = "mobi.maptrek.location.disableTrack";
    /**
     * Service command to start background mode. Service is switched to <em>Foreground</em>
     * state to ensure it will not be killed by OS.
     */
    public static final String ENABLE_BACKGROUND_TRACK = "mobi.maptrek.location.enableBackgroundTrack";
    /**
     * Service command to stop background mode. Service is switched back from <em>Foreground</em>
     * state. Track is continued to be recorded.
     */
    public static final String DISABLE_BACKGROUND_TRACK = "mobi.maptrek.location.disableBackgroundTrack";
    /**
     * Broadcast sent when track is about to be saved (or not)
     */
    public static final String BROADCAST_TRACK_SAVE = "mobi.maptrek.location.TrackSave";
    /**
     * GPS status code
     */
    public static final int GPS_OFF = 1;
    /**
     * GPS status code
     */
    public static final int GPS_SEARCHING = 2;
    /**
     * GPS status code
     */
    public static final int GPS_OK = 3;

    public static final int LOCATION_DELAY = 300;
}
