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
    public static final String ENABLE_BACKGROUND_LOCATIONS = "mobi.maptrek.location.enableBackgroundLocations";
    /**
     * Service command to stop background mode. Service is switched back from <em>Foreground</em>
     * state.
     */
    public static final String DISABLE_BACKGROUND_LOCATIONS = "mobi.maptrek.location.disableBackgroundLocations";
    /**
     * Broadcast sent when track recording state changes
     */
    public static final String BROADCAST_TRACK_STATE = "mobi.maptrek.location.TrackState";
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

    public enum TRACKING_STATE {
        DISABLED,
        PENDING,
        TRACKING,
        PAUSED
    }
}
