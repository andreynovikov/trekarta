package mobi.maptrek.layers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.oscim.map.Map;

import mobi.maptrek.data.Track;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.ITrackingListener;
import mobi.maptrek.location.LocationService;

public class CurrentTrackLayer extends TrackLayer {
    private boolean mBound;
    private ILocationService mTrackingService;
    private Context mContext;

    public CurrentTrackLayer(Map map, Context context) {
        super(map, new Track());
        mContext = context;
        mBound = mContext.bindService(new Intent(mContext, LocationService.class), mTrackingConnection, 0);
        setColor(org.oscim.backend.canvas.Color.fade(mLineStyle.color, 0.7));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unbind();
    }

    private void unbind() {
        if (!mBound)
            return;

        if (mTrackingService != null) {
            mTrackingService.unregisterTrackingCallback(mTrackingListener);
        }

        mContext.unbindService(mTrackingConnection);
        mBound = false;
    }

    private ServiceConnection mTrackingConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mTrackingService = (ILocationService) service;
            mTrack.copyFrom(mTrackingService.getTrack());
            mTrackingService.registerTrackingCallback(mTrackingListener);
        }

        public void onServiceDisconnected(ComponentName className) {
            mTrackingService = null;
        }
    };

    //FIXME Ugly hack
    Track.TrackPoint point = null;

    private ITrackingListener mTrackingListener = new ITrackingListener() {
        public void onNewPoint(boolean continuous, double lat, double lon, float elev, float speed, float trk, float accuracy, long time) {
            if (point != null) {
                // We do not need distance in this track
                mTrack.addPointFast(point.continuous, point.latitudeE6, point.longitudeE6, point.elevation, point.speed, point.bearing, point.accuracy, point.time);
                updatePoints();
            }
            point = mTrack.new TrackPoint(continuous, (int) (lat * 1E6), (int) (lon * 1E6), elev, speed, trk, accuracy, time);
        }
    };
}
