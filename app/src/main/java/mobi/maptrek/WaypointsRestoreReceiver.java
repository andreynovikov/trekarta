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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import mobi.maptrek.data.source.WaypointDbDataSource;

public class WaypointsRestoreReceiver extends BroadcastReceiver {
    private static final Logger logger = LoggerFactory.getLogger(WaypointsRestoreReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        logger.debug(intent.getAction());
        MapTrek application = MapTrek.getApplication();
        File fromFile = new File(application.getExternalDir("databases"), "waypoints.sqlitedb.restore");
        File toFile = new File(application.getExternalDir("databases"), "waypoints.sqlitedb");
        if (fromFile.exists() && toFile.delete() && fromFile.renameTo(toFile)) {
            logger.info("Waypoints restored");
        } else {
            Toast.makeText(context, R.string.msgRestorePlacesFailed, Toast.LENGTH_LONG).show();
        }
        context.sendBroadcast(new Intent(WaypointDbDataSource.BROADCAST_WAYPOINTS_REWRITTEN));
    }
}
