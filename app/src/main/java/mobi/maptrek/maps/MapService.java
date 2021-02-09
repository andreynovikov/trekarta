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

package mobi.maptrek.maps;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.MainActivity;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.util.ProgressListener;

public class MapService extends IntentService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);

    public static final String BROADCAST_MAP_ADDED = "mobi.maptrek.MapAdded";
    public static final String BROADCAST_MAP_REMOVED = "mobi.maptrek.MapRemoved";

    public static final String EXTRA_X = "x";
    public static final String EXTRA_Y = "y";

    static final int JOB_ID = 1000;

    public MapService() {
        super("MapService");
    }

    /*
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, MapService.class, JOB_ID, work);
    }
    */

    @Override
    //protected void onHandleWork(@NonNull Intent intent) {
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null)
            return;

        boolean actionImport = Intent.ACTION_INSERT.equals(intent.getAction());
        boolean actionRemoval = Intent.ACTION_DELETE.equals(intent.getAction());

        //TODO Pass-through to map management
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final Notification.Builder builder = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT > 25)
            builder.setChannelId("ongoing");
        int titleRes = actionImport ? R.string.title_map_import : R.string.title_map_removal;
        builder.setContentTitle(getString(titleRes))
                .setSmallIcon(R.drawable.ic_import_export)
                .setGroup("maptrek")
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(Notification.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(getResources().getColor(R.color.colorAccent, getTheme()));

        startForeground(JOB_ID, builder.build());

        MapTrek application = MapTrek.getApplication();
        Index mapIndex = application.getMapIndex();

        if (actionImport) {
            Uri uri = intent.getData();
            if (uri == null)
                return;
            String filename = uri.getLastPathSegment();
            if (filename == null)
                return;
            boolean hillshade = false;
            final int x, y;
            if (Index.BASEMAP_FILENAME.equals(filename)) {
                x = -1;
                y = -1;
                builder.setContentTitle(getString(R.string.baseMapTitle));
            } else {
                String[] parts = filename.split("[\\-.]");
                try {
                    if (parts.length != 3)
                        throw new NumberFormatException("unexpected name");
                    x = Integer.parseInt(parts[0]);
                    y = Integer.parseInt(parts[1]);
                    hillshade = "mbtiles".equals(parts[2]);
                    if (x > 127 || y > 127)
                        throw new NumberFormatException("out of range");
                } catch (NumberFormatException e) {
                    logger.error(e.getMessage());
                    builder.setContentIntent(pendingIntent);
                    builder.setContentText(getString(R.string.failed)).setProgress(0, 0, false);
                    if (notificationManager != null)
                        notificationManager.notify(JOB_ID, builder.build());
                    return;
                }
                builder.setContentTitle(getString(hillshade ? R.string.hillshadeTitle : R.string.mapTitle, x, y));
            }
            if (notificationManager != null)
                notificationManager.notify(JOB_ID, builder.build());

            if (processDownload(mapIndex, x, y, hillshade, uri.getPath(),
                    new OperationProgressListener(notificationManager, builder))) {
                application.sendBroadcast(new Intent(BROADCAST_MAP_ADDED).putExtra(EXTRA_X, x).putExtra(EXTRA_Y, y));
                builder.setContentText(getString(R.string.complete));
            } else {
                builder.setContentIntent(pendingIntent);
                builder.setContentText(getString(R.string.failed)).setProgress(0, 0, false);
            }
            if (notificationManager != null)
                notificationManager.notify(JOB_ID, builder.build());
        }

        if (actionRemoval) {
            int x = intent.getIntExtra(EXTRA_X, -1);
            int y = intent.getIntExtra(EXTRA_Y, -1);
            mapIndex.removeNativeMap(x, y, new OperationProgressListener(notificationManager, builder));
            application.sendBroadcast(new Intent(BROADCAST_MAP_REMOVED).putExtras(intent));
            if (notificationManager != null)
                notificationManager.cancel(0);
        }

        stopForeground(true);
    }

    private boolean processDownload(Index mapIndex, int x, int y, boolean hillshade, String path,
                                    OperationProgressListener progressListener) {
        if (hillshade)
            return mapIndex.processDownloadedHillshade(x, y, path, progressListener);
        else
            return mapIndex.processDownloadedMap(x, y, path, progressListener);
    }

    private class OperationProgressListener implements ProgressListener {
        private final NotificationManager notificationManager;
        private final Notification.Builder builder;
        int progress = 0;
        float step = 0f;

        OperationProgressListener(NotificationManager notificationManager, Notification.Builder builder) {
            this.notificationManager = notificationManager;
            this.builder = builder;
        }

        @Override
        public void onProgressStarted(int length) {
            step = length / 100f;
            if (step == 0f)
                return;
            builder.setContentText(getString(R.string.processed, 0)).setProgress(100, 0, false);
            notificationManager.notify(JOB_ID, builder.build());
        }

        @Override
        public void onProgressChanged(int progress) {
            if (step == 0f)
                return;
            int percent = (int) (progress / step);
            if (percent > this.progress) {
                this.progress = percent;
                builder.setContentText(getString(R.string.processed, this.progress)).setProgress(100, this.progress, false);
                notificationManager.notify(JOB_ID, builder.build());
            }
        }

        @Override
        public void onProgressFinished() {
            if (step == 0f)
                return;
            builder.setProgress(0, 0, false);
            notificationManager.notify(JOB_ID, builder.build());
        }

        @Override
        public void onProgressFinished(Bundle data) {
            onProgressFinished();
        }

        @Override
        public void onProgressAnnotated(String annotation) {
        }
    }
}
