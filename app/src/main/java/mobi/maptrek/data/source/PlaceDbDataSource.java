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

package mobi.maptrek.data.source;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import org.oscim.core.GeoPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mobi.maptrek.R;
import mobi.maptrek.data.Place;

public class PlaceDbDataSource extends DataSource implements PlaceDataSource {
    @SuppressWarnings("WeakerAccess")
    public static final String BROADCAST_PLACES_MODIFIED = "mobi.maptrek.event.PlacesModified";
    public static final String BROADCAST_PLACES_RESTORED = "mobi.maptrek.event.PlacesRestored";
    public static final String BROADCAST_PLACES_REWRITTEN = "mobi.maptrek.event.PlacesRewritten";

    private Context mContext;
    private SQLiteDatabase mDatabase;
    private PlaceDbHelper mDbHelper;
    private static final Intent mBroadcastIntent = new Intent()
            .setAction(BROADCAST_PLACES_MODIFIED)
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

    private String[] mAllColumns = {
            PlaceDbHelper.COLUMN_ID,
            PlaceDbHelper.COLUMN_NAME,
            PlaceDbHelper.COLUMN_LATE6,
            PlaceDbHelper.COLUMN_LONE6,
            PlaceDbHelper.COLUMN_ALTITUDE,
            PlaceDbHelper.COLUMN_PROXIMITY,
            PlaceDbHelper.COLUMN_DESCRIPTION,
            PlaceDbHelper.COLUMN_DATE,
            PlaceDbHelper.COLUMN_COLOR,
            PlaceDbHelper.COLUMN_ICON,
            PlaceDbHelper.COLUMN_LOCKED
    };

    public PlaceDbDataSource(Context context, File file) {
        mContext = context;
        mDbHelper = new PlaceDbHelper(context, file);
        name = context.getString(R.string.placeStoreName);
    }

    public void open() throws SQLException {
        mDatabase = mDbHelper.getWritableDatabase();
        setLoaded();
    }

    public void close() {
        mDbHelper.close();
    }

    public boolean isOpen() {
        return mDatabase != null && mDatabase.isOpen();
    }

    public void savePlace(Place place) {
        ContentValues values = new ContentValues();
        if (place._id > 0)
            values.put(PlaceDbHelper.COLUMN_ID, place._id);
        values.put(PlaceDbHelper.COLUMN_NAME, place.name);
        values.put(PlaceDbHelper.COLUMN_LATE6, place.coordinates.latitudeE6);
        values.put(PlaceDbHelper.COLUMN_LONE6, place.coordinates.longitudeE6);
        if (place.altitude != Integer.MIN_VALUE)
            values.put(PlaceDbHelper.COLUMN_ALTITUDE, place.altitude);
        if (place.proximity != 0)
            values.put(PlaceDbHelper.COLUMN_PROXIMITY, place.proximity);
        if (place.description != null)
            values.put(PlaceDbHelper.COLUMN_DESCRIPTION, place.description);
        if (place.date != null)
            values.put(PlaceDbHelper.COLUMN_DATE, place.date.getTime());
        values.put(PlaceDbHelper.COLUMN_COLOR, place.style.color);
        if (place.style.icon != null)
            values.put(PlaceDbHelper.COLUMN_ICON, place.style.icon);
        values.put(PlaceDbHelper.COLUMN_LOCKED, place.locked ? 1 : 0);

        int id = (int) mDatabase.insertWithOnConflict(PlaceDbHelper.TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            mDatabase.update(PlaceDbHelper.TABLE_NAME, values, PlaceDbHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(place._id)});
        } else {
            place._id = id;
            place.source = this;
        }
        notifyListeners();
    }

    public void deletePlace(Place place) {
        long id = place._id;
        mDatabase.delete(PlaceDbHelper.TABLE_NAME, PlaceDbHelper.COLUMN_ID + " = " + id, null);
        notifyListeners();
    }

    @Override
    public List<Place> getPlaces() {
        List<Place> places = new ArrayList<>();
        Cursor cursor = getCursor();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Place place = cursorToPlace(cursor);
            places.add(place);
            cursor.moveToNext();
        }
        cursor.close();
        return places;
    }

    @Override
    public int getPlacesCount() {
        Cursor cursor = getCursor();
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    @Override
    public Cursor getCursor() {
        String orderBy;
        if (mLatitudeE6 != 0 || mLongitudeE6 != 0) {
            double cos2 = Math.pow(Math.cos(Math.toRadians(GeoPoint.e6ToDegree(mLatitudeE6))), 2d);
            orderBy = "((" + PlaceDbHelper.COLUMN_LATE6 + "-(" + Integer.toString(mLatitudeE6) +
                    "))*(" + PlaceDbHelper.COLUMN_LATE6 + "-(" + Integer.toString(mLatitudeE6) +
                    "))+(" +Double.toString(cos2) + ")*(" + PlaceDbHelper.COLUMN_LONE6 + "-(" +
                    Integer.toString(mLongitudeE6)+ "))*(" + PlaceDbHelper.COLUMN_LONE6 + "-(" +
                    Integer.toString(mLongitudeE6) + "))) ASC";
        } else {
            orderBy = PlaceDbHelper.COLUMN_NAME;
        }

        return mDatabase.query(PlaceDbHelper.TABLE_NAME, mAllColumns, null, null, null, null, orderBy);
    }

    @Override
    public int getDataType(int position) {
        return TYPE_PLACE;
    }

    @Override
    public void notifyListeners() {
        super.notifyListeners();
        mContext.sendBroadcast(mBroadcastIntent);
    }

    @Override
    public Place cursorToPlace(Cursor cursor) {
        Place place = new Place(cursor.getInt(cursor.getColumnIndex(PlaceDbHelper.COLUMN_LATE6)), cursor.getInt(cursor.getColumnIndex(PlaceDbHelper.COLUMN_LONE6)));
        place._id = cursor.getLong(cursor.getColumnIndex(PlaceDbHelper.COLUMN_ID));
        place.name = cursor.getString(cursor.getColumnIndex(PlaceDbHelper.COLUMN_NAME));
        if (!cursor.isNull(cursor.getColumnIndex(PlaceDbHelper.COLUMN_ALTITUDE)))
            place.altitude = cursor.getInt(cursor.getColumnIndex(PlaceDbHelper.COLUMN_ALTITUDE));
        if (!cursor.isNull(cursor.getColumnIndex(PlaceDbHelper.COLUMN_PROXIMITY)))
            place.proximity = cursor.getInt(cursor.getColumnIndex(PlaceDbHelper.COLUMN_PROXIMITY));
        if (!cursor.isNull(cursor.getColumnIndex(PlaceDbHelper.COLUMN_DESCRIPTION)))
            place.description = cursor.getString(cursor.getColumnIndex(PlaceDbHelper.COLUMN_DESCRIPTION));
        if (!cursor.isNull(cursor.getColumnIndex(PlaceDbHelper.COLUMN_DATE)))
            place.date = new Date(cursor.getLong(cursor.getColumnIndex(PlaceDbHelper.COLUMN_DATE)));
        place.style.color = cursor.getInt(cursor.getColumnIndex(PlaceDbHelper.COLUMN_COLOR));
        if (!cursor.isNull(cursor.getColumnIndex(PlaceDbHelper.COLUMN_ICON)))
            place.style.icon = cursor.getString(cursor.getColumnIndex(PlaceDbHelper.COLUMN_ICON));
        if (!cursor.isNull(cursor.getColumnIndex(PlaceDbHelper.COLUMN_LOCKED)))
            place.locked = cursor.getInt(cursor.getColumnIndex(PlaceDbHelper.COLUMN_LOCKED)) > 0;
        place.source = this;
        return place;
    }

    @Override
    public int getFormat() {
        return FORMAT_NONE;
    }

    @Override
    public boolean isNativeTrack() {
        return false;
    }

    @Override
    public boolean isNativeRoute() {
        return false;
    }

    @Override
    public boolean isIndividual() {
        return false; // we want to show data source even when there is only one place
    }
}
