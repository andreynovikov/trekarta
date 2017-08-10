package mobi.maptrek.maps;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.MainActivity;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.util.ProgressListener;

import static mobi.maptrek.DownloadReceiver.BROADCAST_DOWNLOAD_PROCESSED;

public class ImportService extends IntentService {
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

    public ImportService() {
        super("ImportService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null)
            return;

        //TODO Pass-through to map management
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Map import")
                .setSmallIcon(R.drawable.ic_import_export)
                .setGroup("maptrek")
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(Notification.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(getResources().getColor(R.color.colorAccent, getTheme()));
        notificationManager.notify(0, builder.build());

        MapTrek application = MapTrek.getApplication();
        Uri uri = intent.getData();

        String[] parts = uri.getLastPathSegment().split("[\\-\\.]");
        final int x, y;
        try {
            if (parts.length != 3)
                throw new NumberFormatException("unexpected name");
            x = Integer.valueOf(parts[0]);
            y = Integer.valueOf(parts[1]);
            if (x > 127 || y > 127)
                throw new NumberFormatException("out of range");
        } catch (NumberFormatException e) {
            logger.error(e.getMessage());
            builder.setContentIntent(pendingIntent);
            builder.setContentText(getString(R.string.failed)).setProgress(0, 0, false);
            notificationManager.notify(0, builder.build());
            return;
        }

        builder.setContentTitle(getString(R.string.mapTitle, x, y));
        notificationManager.notify(0, builder.build());

        Index mapIndex = application.getMapIndex();
        if (mapIndex.processDownloadedMap(x, y, uri.getPath(), new ImportProgressListener(notificationManager, builder))) {
            application.sendBroadcast(new Intent(BROADCAST_DOWNLOAD_PROCESSED));
            builder.setContentText(getString(R.string.complete));
            notificationManager.notify(0, builder.build());
            notificationManager.cancel(0);
        } else {
            builder.setContentIntent(pendingIntent);
            builder.setContentText(getString(R.string.failed)).setProgress(0, 0, false);
            notificationManager.notify(0, builder.build());
        }
    }

    private class ImportProgressListener implements ProgressListener {
        private final NotificationManager notificationManager;
        private final Notification.Builder builder;
        int progress = 0;
        int step = 0;

        ImportProgressListener(NotificationManager notificationManager, Notification.Builder builder) {
            this.notificationManager = notificationManager;
            this.builder = builder;
        }

        @Override
        public void onProgressStarted(int length) {
            step = length / 100;
            if (step == 0)
                return;
            builder.setContentText(getString(R.string.processed, 0)).setProgress(100, 0, false);
            notificationManager.notify(0, builder.build());
        }

        @Override
        public void onProgressChanged(int progress) {
            if (step == 0)
                return;
            int percent = progress / step;
            if (percent > this.progress) {
                this.progress = percent;
                builder.setContentText(getString(R.string.processed, this.progress)).setProgress(100, this.progress, false);
                notificationManager.notify(0, builder.build());
            }
        }

        @Override
        public void onProgressFinished() {
            if (step == 0)
                return;
            builder.setProgress(0, 0, false);
            notificationManager.notify(0, builder.build());
        }

        @Override
        public void onProgressAnnotated(String annotation) {
        }
    }
}
