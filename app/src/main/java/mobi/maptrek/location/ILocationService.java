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
