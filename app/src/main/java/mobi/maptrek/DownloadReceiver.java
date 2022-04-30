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

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.maps.MapService;
import mobi.maptrek.maps.MapWorker;

public class DownloadReceiver extends BroadcastReceiver
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadReceiver.class);

    @Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			long ref = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(ref);
            assert downloadManager != null;
            Cursor cursor = downloadManager.query(query);
			if (cursor.moveToFirst()) {
				int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
				if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    String fileUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                    Uri uri = Uri.parse(fileUri);
                    logger.debug("Downloaded: {}", fileUri);
					Intent importIntent = new Intent(Intent.ACTION_INSERT, uri, context, MapService.class);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
						Data data = new Data.Builder()
								.putString(MapWorker.KEY_ACTION, Intent.ACTION_INSERT)
								.putString(MapWorker.KEY_FILE_URI, fileUri)
								.build();
						OneTimeWorkRequest importWorkRequest = new OneTimeWorkRequest.Builder(MapWorker.class)
								.addTag(MapWorker.TAG)
								.setInputData(data)
								.build();
						WorkManager.getInstance(context).enqueue(importWorkRequest);
					} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(importIntent);
                    } else {
                        context.startService(importIntent);
                    }
				}
			}
		}
	}
}
