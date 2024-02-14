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
import mobi.maptrek.data.Waypoint;

public class WaypointDbDataSource extends DataSource implements WaypointDataSource {
    @SuppressWarnings("WeakerAccess")
    public static final String BROADCAST_WAYPOINTS_MODIFIED = "mobi.maptrek.event.WaypointsModified";
    public static final String BROADCAST_WAYPOINTS_RESTORED = "mobi.maptrek.event.WaypointsRestored";
    public static final String BROADCAST_WAYPOINTS_REWRITTEN = "mobi.maptrek.event.WaypointsRewritten";

    private Context mContext;
    private SQLiteDatabase mDatabase;
    private WaypointDbHelper mDbHelper;
    private static final Intent mBroadcastIntent = new Intent()
            .setAction(BROADCAST_WAYPOINTS_MODIFIED)
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

    private String[] mAllColumns = {
            WaypointDbHelper.COLUMN_ID,
            WaypointDbHelper.COLUMN_NAME,
            WaypointDbHelper.COLUMN_LATE6,
            WaypointDbHelper.COLUMN_LONE6,
            WaypointDbHelper.COLUMN_ALTITUDE,
            WaypointDbHelper.COLUMN_PROXIMITY,
            WaypointDbHelper.COLUMN_DESCRIPTION,
            WaypointDbHelper.COLUMN_DATE,
            WaypointDbHelper.COLUMN_COLOR,
            WaypointDbHelper.COLUMN_ICON,
            WaypointDbHelper.COLUMN_LOCKED
    };

    public WaypointDbDataSource(Context context, File file) {
        mContext = context;
        mDbHelper = new WaypointDbHelper(context, file);
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

    public void saveWaypoint(Waypoint waypoint) {
        ContentValues values = new ContentValues();
        if (waypoint._id > 0)
            values.put(WaypointDbHelper.COLUMN_ID, waypoint._id);
        values.put(WaypointDbHelper.COLUMN_NAME, waypoint.name);
        values.put(WaypointDbHelper.COLUMN_LATE6, waypoint.coordinates.latitudeE6);
        values.put(WaypointDbHelper.COLUMN_LONE6, waypoint.coordinates.longitudeE6);
        if (waypoint.altitude != Integer.MIN_VALUE)
            values.put(WaypointDbHelper.COLUMN_ALTITUDE, waypoint.altitude);
        if (waypoint.proximity != 0)
            values.put(WaypointDbHelper.COLUMN_PROXIMITY, waypoint.proximity);
        if (waypoint.description != null)
            values.put(WaypointDbHelper.COLUMN_DESCRIPTION, waypoint.description);
        if (waypoint.date != null)
            values.put(WaypointDbHelper.COLUMN_DATE, waypoint.date.getTime());
        values.put(WaypointDbHelper.COLUMN_COLOR, waypoint.style.color);
        if (waypoint.style.icon != null)
            values.put(WaypointDbHelper.COLUMN_ICON, waypoint.style.icon);
        values.put(WaypointDbHelper.COLUMN_LOCKED, waypoint.locked ? 1 : 0);

        int id = (int) mDatabase.insertWithOnConflict(WaypointDbHelper.TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            mDatabase.update(WaypointDbHelper.TABLE_NAME, values, WaypointDbHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(waypoint._id)});
        } else {
            waypoint._id = id;
            waypoint.source = this;
        }
        notifyListeners();
    }

    public void deleteWaypoint(Waypoint waypoint) {
        long id = waypoint._id;
        mDatabase.delete(WaypointDbHelper.TABLE_NAME, WaypointDbHelper.COLUMN_ID + " = " + id, null);
        notifyListeners();
    }

    @Override
    public List<Waypoint> getWaypoints() {
        List<Waypoint> waypoints = new ArrayList<>();
        Cursor cursor = getCursor();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Waypoint waypoint = cursorToWaypoint(cursor);
            waypoints.add(waypoint);
            cursor.moveToNext();
        }
        cursor.close();
        return waypoints;
    }

    @Override
    public int getWaypointsCount() {
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
            orderBy = "((" + WaypointDbHelper.COLUMN_LATE6 + "-(" + Integer.toString(mLatitudeE6) +
                    "))*(" + WaypointDbHelper.COLUMN_LATE6 + "-(" + Integer.toString(mLatitudeE6) +
                    "))+(" +Double.toString(cos2) + ")*(" + WaypointDbHelper.COLUMN_LONE6 + "-(" +
                    Integer.toString(mLongitudeE6)+ "))*(" + WaypointDbHelper.COLUMN_LONE6 + "-(" +
                    Integer.toString(mLongitudeE6) + "))) ASC";
        } else {
            orderBy = WaypointDbHelper.COLUMN_NAME;
        }

        return mDatabase.query(WaypointDbHelper.TABLE_NAME, mAllColumns, null, null, null, null, orderBy);
    }

    @Override
    public int getDataType(int position) {
        return TYPE_WAYPOINT;
    }

    @Override
    public void notifyListeners() {
        super.notifyListeners();
        mContext.sendBroadcast(mBroadcastIntent);
    }

    @Override
    public Waypoint cursorToWaypoint(Cursor cursor) {
        Waypoint waypoint = new Waypoint(cursor.getInt(cursor.getColumnIndex(WaypointDbHelper.COLUMN_LATE6)), cursor.getInt(cursor.getColumnIndex(WaypointDbHelper.COLUMN_LONE6)));
        waypoint._id = cursor.getLong(cursor.getColumnIndex(WaypointDbHelper.COLUMN_ID));
        waypoint.name = cursor.getString(cursor.getColumnIndex(WaypointDbHelper.COLUMN_NAME));
        if (!cursor.isNull(cursor.getColumnIndex(WaypointDbHelper.COLUMN_ALTITUDE)))
            waypoint.altitude = cursor.getInt(cursor.getColumnIndex(WaypointDbHelper.COLUMN_ALTITUDE));
        if (!cursor.isNull(cursor.getColumnIndex(WaypointDbHelper.COLUMN_PROXIMITY)))
            waypoint.proximity = cursor.getInt(cursor.getColumnIndex(WaypointDbHelper.COLUMN_PROXIMITY));
        if (!cursor.isNull(cursor.getColumnIndex(WaypointDbHelper.COLUMN_DESCRIPTION)))
            waypoint.description = cursor.getString(cursor.getColumnIndex(WaypointDbHelper.COLUMN_DESCRIPTION));
        if (!cursor.isNull(cursor.getColumnIndex(WaypointDbHelper.COLUMN_DATE)))
            waypoint.date = new Date(cursor.getLong(cursor.getColumnIndex(WaypointDbHelper.COLUMN_DATE)));
        waypoint.style.color = cursor.getInt(cursor.getColumnIndex(WaypointDbHelper.COLUMN_COLOR));
        if (!cursor.isNull(cursor.getColumnIndex(WaypointDbHelper.COLUMN_ICON)))
            waypoint.style.icon = cursor.getString(cursor.getColumnIndex(WaypointDbHelper.COLUMN_ICON));
        if (!cursor.isNull(cursor.getColumnIndex(WaypointDbHelper.COLUMN_LOCKED)))
            waypoint.locked = cursor.getInt(cursor.getColumnIndex(WaypointDbHelper.COLUMN_LOCKED)) > 0;
        waypoint.source = this;
        return waypoint;
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
        return false; // we want to show data source even when there is only one waypoint
    }
}
