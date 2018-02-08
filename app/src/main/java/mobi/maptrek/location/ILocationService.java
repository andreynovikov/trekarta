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

import android.location.Location;

import mobi.maptrek.data.Track;
import mobi.maptrek.util.ProgressListener;

public interface ILocationService
{
	void registerLocationCallback(ILocationListener callback);
	void unregisterLocationCallback(ILocationListener callback);
	void registerTrackingCallback(ITrackingListener callback);
	void unregisterTrackingCallback(ITrackingListener callback);
	void setProgressListener(ProgressListener listener);
	boolean isLocating();
	boolean isTracking();
	Location getLocation();
    int getStatus();

	/**
	 * Returns used and visible satellite counts. Used sats are shifted in 7 bits.
	 *
	 * @return Packed satellite counts
	 */
	int getSatellites();
	float getHDOP();
	float getVDOP();
	Track getTrack();
	Track getTrack(long start, long end);

    void saveTrack();

    void clearTrack();
	long getTrackStartTime();
	long getTrackEndTime();
}
