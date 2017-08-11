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

    public static final String BROADCAST_DOWNLOAD_PROCESSED = "DownloadProcessed";

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
