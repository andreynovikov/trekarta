/*
 * Copyright 2024 Andrey Novikov
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

package mobi.maptrek.layers;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import org.oscim.map.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.data.Track;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.ITrackingListener;
import mobi.maptrek.location.LocationService;
import mobi.maptrek.viewmodels.TrackViewModel;

public class CurrentTrackLayer extends TrackLayer {
    private static final Logger logger = LoggerFactory.getLogger(CurrentTrackLayer.class);

    private boolean isBound;
    private ILocationService trackingService;
    private final Context context;
    private TrackViewModel trackViewModel;

    public CurrentTrackLayer(Map map, Context context, ViewModelStoreOwner owner) {
        super(map, new Track());
        logger.error("CurrentTrackLayer created");
        this.context = context;
        trackViewModel = new ViewModelProvider(owner).get(TrackViewModel.class);
        isBound = context.bindService(new Intent(context, LocationService.class), trackingConnection, BIND_AUTO_CREATE);
        setColor(org.oscim.backend.canvas.Color.fade(mLineStyle.color, 0.7));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unbind();
    }

    private void unbind() {
        if (!isBound)
            return;

        if (trackingService != null) {
            trackingService.unregisterTrackingCallback(trackingListener);
        }

        context.unbindService(trackingConnection);
        isBound = false;
    }

    private final ServiceConnection trackingConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            logger.error("tracking service created");
            trackingService = (ILocationService) service;
            AsyncTask.execute(() -> {
                mTrack.copyFrom(trackingService.getTrack());
                logger.error("track loaded");
                if (mTrack.points.size() > 1)
                    updatePoints();
                trackViewModel.currentTrack.postValue(mTrack);
                trackingService.registerTrackingCallback(trackingListener);
            });
        }

        public void onServiceDisconnected(ComponentName className) {
            trackingService = null;
        }
    };

    private final ITrackingListener trackingListener = (continuous, lat, lon, elev, speed, trk, accuracy, time) -> {
            mTrack.addPoint(continuous, (int) (lat * 1E6), (int) (lon * 1E6), elev, speed, trk, accuracy, time);
            if (mTrack.points.size() > 1)
                updatePoints();
            trackViewModel.currentTrack.postValue(mTrack);
    };
}
