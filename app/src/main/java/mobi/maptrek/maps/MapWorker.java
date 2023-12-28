/*
 * Copyright 2023 Andrey Novikov
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

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import mobi.maptrek.MainActivity;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.util.ProgressListener;

public class MapWorker extends Worker {
   private static final Logger logger = LoggerFactory.getLogger(MapWorker.class);

   public static final String TAG = "MapWorker";
   public static final String KEY_ACTION = "ACTION";
   public static final String KEY_FILE_URI = "FILENAME";
   public static final String KEY_X = "x";
   public static final String KEY_Y = "y";

   public static final String BROADCAST_MAP_ADDED = "mobi.maptrek.MapAdded";
   public static final String BROADCAST_MAP_REMOVED = "mobi.maptrek.MapRemoved";

   public static final String EXTRA_X = "x";
   public static final String EXTRA_Y = "y";

   private final NotificationCompat.Builder builder;
   private final int notificationId;

   public MapWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
      super(context, workerParams);
      notificationId = Sequence.nextValue();
      builder = new NotificationCompat.Builder(context, "ongoing")
              .setSmallIcon(R.drawable.ic_import_export)
              .setGroup("maptrek")
              .setCategory(Notification.CATEGORY_PROGRESS)
              .setPriority(Notification.PRIORITY_LOW)
              .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
              .setColor(context.getColor(R.color.colorAccent))
              .setOngoing(true);
      setForegroundAsync(createForegroundInfo());
   }

   @NonNull
   @Override
   public Result doWork() {
      Data inputData = getInputData();
      String action = inputData.getString(KEY_ACTION);
      logger.error(action);

      boolean actionImport = Intent.ACTION_INSERT.equals(action);
      boolean actionRemoval = Intent.ACTION_DELETE.equals(action);

      int titleRes = actionImport ? R.string.title_map_import : R.string.title_map_removal;
      String title = getApplicationContext().getString(titleRes);

      builder.setContentTitle(title).setTicker(title);
      setForegroundAsync(createForegroundInfo());

      try {
         MapTrek application = MapTrek.getApplication();
         Index mapIndex = application.getMapIndex();

         if (actionImport) {
            String fileUri = inputData.getString(KEY_FILE_URI);
            importMap(application, mapIndex, fileUri);
         }
         if (actionRemoval) {
            int x = inputData.getInt(KEY_X, -1);
            int y = inputData.getInt(KEY_Y, -1);
            removeMap(application, mapIndex, x, y);
         }

         application.optionallyCloseMapDatabase(getId());
      } catch (Exception e) {
         logger.error(e.getMessage(), e);
         showErrorNotification();
         return Result.failure();
      }

      return Result.success();
   }

   private void importMap(MapTrek application, Index mapIndex, String fileUri) {
      Uri uri = Uri.parse(fileUri);
      logger.error(uri.toString());

      String filename = uri.getLastPathSegment();
      if (filename == null)
         return;
      boolean hillshade = false;
      final int x, y;
      if (Index.BASEMAP_FILENAME.equals(filename)) {
         x = -1;
         y = -1;
         String title = application.getString(R.string.baseMapTitle);
         builder.setContentTitle(title).setTicker(title);
      } else {
         String[] parts = filename.split("[\\-.]");
         if (parts.length != 3)
            throw new NumberFormatException("unexpected name");
         x = Integer.parseInt(parts[0]);
         y = Integer.parseInt(parts[1]);
         hillshade = "mbtiles".equals(parts[2]);
         if (x > 127 || y > 127)
            throw new NumberFormatException("out of range");
         String title = application.getString(hillshade ? R.string.hillshadeTitle : R.string.mapTitle, x, y);
         builder.setContentTitle(title).setTicker(title);
      }
      setForegroundAsync(createForegroundInfo());

      if (processDownload(mapIndex, x, y, hillshade, uri.getPath(), new OperationProgressListener())) {
         Intent intent = new Intent(BROADCAST_MAP_ADDED).putExtra(EXTRA_X, x).putExtra(EXTRA_Y, y);
         intent.setPackage(application.getPackageName());
         application.sendBroadcast(intent);
         builder.setContentText(application.getString(R.string.complete));
         setForegroundAsync(createForegroundInfo());
      } else {
         showErrorNotification();
      }
   }

   private void removeMap(MapTrek application, Index mapIndex, int x, int y) {
      logger.error(String.format(Locale.getDefault(), "%d %d", x, y));
      mapIndex.removeNativeMap(x, y, new OperationProgressListener());
      Intent intent = new Intent(BROADCAST_MAP_REMOVED).putExtra(EXTRA_X, x).putExtra(EXTRA_Y, y);
      intent.setPackage(application.getPackageName());
      application.sendBroadcast(intent);
   }

   @NonNull
   private ForegroundInfo createForegroundInfo() {
      if (Build.VERSION.SDK_INT < 34)
         return new ForegroundInfo(notificationId, builder.build());
      else
         return new ForegroundInfo(notificationId, builder.build(),FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);
   }

   private void showErrorNotification() {
      Context context = getApplicationContext();
      NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      //TODO Pass-through to map management
      Intent launchIntent = new Intent(Intent.ACTION_MAIN);
      launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
      launchIntent.setComponent(new ComponentName(getApplicationContext(), MainActivity.class));
      launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
      PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      String title = context.getString(R.string.failed);
      builder.setOngoing(false)
              .setContentIntent(pendingIntent)
              .setContentText(title)
              .setTicker(title)
              .setProgress(0, 0, false)
              .setAutoCancel(true)
              .setCategory(Notification.CATEGORY_ERROR)
              .setPriority(Notification.PRIORITY_HIGH);
      if (notificationManager != null)
         notificationManager.notify(Sequence.nextValue(), builder.build());
   }

   private boolean processDownload(Index mapIndex, int x, int y, boolean hillshade, String path, OperationProgressListener progressListener) {
      if (hillshade)
         return mapIndex.processDownloadedHillshade(x, y, path, progressListener);
      else
         return mapIndex.processDownloadedMap(x, y, path, progressListener);
   }

   private class OperationProgressListener implements ProgressListener {
      int progress = 0;
      float step = 0f;

      @Override
      public void onProgressStarted(int length) {
         step = length / 100f;
         if (step == 0f)
            return;
         String title = getApplicationContext().getString(R.string.processed, 0);
         builder.setContentText(title).setTicker(title).setProgress(100, 0, false);
         setForegroundAsync(createForegroundInfo());
      }

      @Override
      public void onProgressChanged(int progress) {
         if (step == 0f)
            return;
         int percent = (int) (progress / step);
         if (percent > this.progress) {
            this.progress = percent;
            String title = getApplicationContext().getString(R.string.processed, this.progress);
            builder.setContentText(title).setTicker(title).setProgress(100, this.progress, false);
            setForegroundAsync(createForegroundInfo());
         }
      }

      @Override
      public void onProgressFinished() {
         if (step == 0f)
            return;
         builder.setProgress(0, 0, false);
         setForegroundAsync(createForegroundInfo());
      }

      @Override
      public void onProgressFinished(Bundle data) {
         onProgressFinished();
      }

      @Override
      public void onProgressAnnotated(String annotation) {
      }
   }

   public static class Sequence {
      private static final AtomicInteger counter = new AtomicInteger(1000);

      public static int nextValue() {
         return counter.getAndIncrement();
      }
   }
}
