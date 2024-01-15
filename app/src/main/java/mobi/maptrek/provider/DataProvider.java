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

package mobi.maptrek.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.MapTrek;
import mobi.maptrek.data.MapObject;

public class DataProvider extends ContentProvider {
    private static final Logger logger = LoggerFactory.getLogger(DataProvider.class);

    private static final int MAP_OBJECTS = 1;
    private static final int MAP_OBJECTS_ID = 2;
    private static final int MARKERS_ID = 3;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(DataContract.AUTHORITY, DataContract.MAPOBJECTS_PATH, MAP_OBJECTS);
        uriMatcher.addURI(DataContract.AUTHORITY, DataContract.MAPOBJECTS_PATH + "/#", MAP_OBJECTS_ID);
        uriMatcher.addURI(DataContract.AUTHORITY, DataContract.MARKERS_PATH + "/*", MARKERS_ID);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case MAP_OBJECTS:
                return "vnd.android.cursor.dir/vnd.mobi.maptrek.provider.mapobject";
            case MAP_OBJECTS_ID:
                return "vnd.android.cursor.item/vnd.mobi.maptrek.provider.mapobject";
            case MARKERS_ID:
                return "vnd.android.cursor.item/vnd.mobi.maptrek.provider.marker";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        logger.debug("query({})", uri);
        if (uriMatcher.match(uri) != MARKERS_ID) {
            throw new UnsupportedOperationException("Querying objects is not supported");
        }

        String id = uri.getLastPathSegment();
        MatrixCursor cursor = new MatrixCursor(projection);

        /*
        String path = application.markerPath;
        Bitmap bitmap = BitmapFactory.decodeFile(path + File.separator + id);
        if (bitmap != null)
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bytes = stream.toByteArray();
            MatrixCursor.RowBuilder row = cursor.newRow();
            row.add(bytes);
        }
        */

        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        logger.debug("insert({})", uri);
        if (uriMatcher.match(uri) != MAP_OBJECTS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (values == null) {
            throw new IllegalArgumentException("Values can not be null");
        }

        MapObject mo = new MapObject(0, 0);
        populateFields(mo, values);

        long id = MapTrek.addMapObject(mo);
        Uri objectUri = ContentUris.withAppendedId(DataContract.MAPOBJECTS_URI, id);
        Context context = getContext();
        if (context != null)
            context.getContentResolver().notifyChange(objectUri, null);
        return objectUri;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        logger.debug("update({})", uri);
        if (uriMatcher.match(uri) != MAP_OBJECTS_ID) {
            if (uriMatcher.match(uri) == MAP_OBJECTS)
                throw new UnsupportedOperationException("Currently only updating one object by ID is supported");
            else
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (values == null) {
            throw new IllegalArgumentException("Values can not be null");
        }
        long id = ContentUris.parseId(uri);

        MapObject mo = MapTrek.getMapObject(id);
        if (mo == null)
            return 0;

        populateFields(mo, values);
        EventBus.getDefault().post(new MapObject.UpdatedEvent(mo));

        Context context = getContext();
        if (context != null)
            context.getContentResolver().notifyChange(uri, null);
        return 1;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        logger.debug("delete({})", uri);
        long[] ids = null;
        if (uriMatcher.match(uri) == MAP_OBJECTS) {
            if (!DataContract.MAPOBJECT_ID_SELECTION.equals(selection))
                throw new IllegalArgumentException("Deleting is supported only by ID");
            ids = new long[selectionArgs.length];
            for (int i = 0; i < ids.length; i++)
                ids[i] = Long.parseLong(selectionArgs[i], 10);
        }
        if (uriMatcher.match(uri) == MAP_OBJECTS_ID) {
            ids = new long[]{ContentUris.parseId(uri)};
        }
        if (ids == null)
            throw new IllegalArgumentException("Unknown URI: " + uri);

        int result = 0;
        for (long id : ids) {
            if (MapTrek.removeMapObject(id))
                result++;
        }
        return result;
    }

    private void populateFields(MapObject mapObject, ContentValues values) {
        double latitude = 0.0, longitude = 0.0;

        String key = DataContract.MAPOBJECT_COLUMNS[DataContract.MAPOBJECT_NAME_COLUMN];
        if (values.containsKey(key))
            mapObject.name = values.getAsString(key);

        key = DataContract.MAPOBJECT_COLUMNS[DataContract.MAPOBJECT_DESCRIPTION_COLUMN];
        if (values.containsKey(key))
            mapObject.description = values.getAsString(key);

        key = DataContract.MAPOBJECT_COLUMNS[DataContract.MAPOBJECT_LATITUDE_COLUMN];
        if (values.containsKey(key))
            latitude = values.getAsDouble(key);

        key = DataContract.MAPOBJECT_COLUMNS[DataContract.MAPOBJECT_LONGITUDE_COLUMN];
        if (values.containsKey(key))
            longitude = values.getAsDouble(key);

        key = DataContract.MAPOBJECT_COLUMNS[DataContract.MAPOBJECT_MARKER_COLUMN];
        if (values.containsKey(key))
            mapObject.marker = values.getAsString(key);

        key = DataContract.MAPOBJECT_COLUMNS[DataContract.MAPOBJECT_COLOR_COLUMN];
        if (values.containsKey(key)) {
            mapObject.textColor = values.getAsInteger(key);
            mapObject.style.color = mapObject.textColor;
        }

        key = DataContract.MAPOBJECT_COLUMNS[DataContract.MAPOBJECT_BITMAP_COLUMN];
        if (values.containsKey(key)) {
            byte[] bytes = values.getAsByteArray(key);
            if (bytes != null)
                mapObject.setBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        }
        if (mapObject.coordinates.getLatitude() != latitude || mapObject.coordinates.getLongitude() != longitude) {
            mapObject.setCoordinates(latitude, longitude);
            mapObject.moving = true;
        } else {
            mapObject.moving = false;
        }
    }
}
