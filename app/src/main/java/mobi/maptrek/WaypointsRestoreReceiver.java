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
        File fromFile = new File(context.getExternalFilesDir("databases"), "waypoints.sqlitedb.restore");
        File toFile = new File(context.getExternalFilesDir("databases"), "waypoints.sqlitedb");
        if (fromFile.exists() && toFile.delete() && fromFile.renameTo(toFile)) {
            logger.info("Waypoints restored");
        } else {
            Toast.makeText(context, R.string.msgRestorePlacesFailed, Toast.LENGTH_LONG).show();
        }
        context.sendBroadcast(new Intent(WaypointDbDataSource.BROADCAST_WAYPOINTS_REWRITTEN));
    }
}
