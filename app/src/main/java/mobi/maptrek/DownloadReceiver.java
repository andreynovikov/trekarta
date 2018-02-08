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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.maps.MapService;

public class DownloadReceiver extends BroadcastReceiver
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadReceiver.class);

    @Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
			long ref = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(ref);
			Cursor cursor = downloadManager.query(query);
			if (cursor.moveToFirst()) {
				int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
				if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    String fileName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    Uri uri = Uri.parse(fileName);
                    logger.debug("Downloaded: {}", fileName);
					Intent importIntent = new Intent(Intent.ACTION_INSERT, uri, context, MapService.class);
					context.startService(importIntent);
				}
			}
		}
	}
}
