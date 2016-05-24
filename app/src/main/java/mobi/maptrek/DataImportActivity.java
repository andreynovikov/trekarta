package mobi.maptrek;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import mobi.maptrek.io.GPXManager;
import mobi.maptrek.io.KMLManager;
import mobi.maptrek.util.FileUtils;

public class DataImportActivity extends Activity {
    private static final String TAG = "DataImportActivity";
    private static final DateFormat SUFFIX_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Log.e(TAG, "Action: " + action);
        Log.e(TAG, "Type: " + type);

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            handleSendImage(intent);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            handleSendMultipleImages(intent);
        }
        finish();
    }

    private void handleSendImage(Intent intent) {
        Uri streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (streamUri != null) {
            readFile(streamUri);
        } else {
            readFile(intent.getData());
        }
    }

    private void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared
        }
    }

    private void readFile(Uri uri) {
        Log.e(TAG, uri.toString());
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            String path = uri.getPath();
            File src = new File(path);
            String name = uri.getLastPathSegment();
            long size = src.length();
            Log.e(TAG, "file: " + name + " [" + size + "]");
            //TODO Check file override
            File dst = new File(getExternalFilesDir("data"), name);
            try {
                FileUtils.copyFile(src, dst);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
            String name = null;
            long length = -1;
            if (cursor != null) {
                Log.e(TAG, "   from cursor");
                cursor.moveToFirst();
                name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                length = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                cursor.close();
            } else {
                try {
                    AssetFileDescriptor afd = resolver.openAssetFileDescriptor(uri, "r");
                    if (afd != null)
                        length = afd.getLength();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Log.e(TAG, "Length: " + length);
            }
            //TODO This is just to not crash, should be correctly implemented
            if (name == null)
                name = uri.getLastPathSegment();

            Log.e(TAG, "Import: [" + name + "][" + length + "]");

            if (!name.endsWith(KMLManager.EXTENSION) && !name.endsWith(GPXManager.EXTENSION)) {
                Log.e(TAG, "Unsupported file format");
                return;
            }
            try {
                File dir = getExternalFilesDir("data");
                if (dir == null) {
                    Log.e(TAG, "Data path unavailable");
                    return;
                }
                File dst = new File(dir, name);
                if (dst.exists()) {
                    String ext = name.substring(name.lastIndexOf("."));
                    name = name.replace(ext, "-" + SUFFIX_FORMAT.format(new Date()) + ext);
                    dst = new File(dir, name);
                }
                InputStream is = resolver.openInputStream(uri);
                FileUtils.copyStreamToFile(is, dst);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Unsupported transfer method");
        }
    }
}
