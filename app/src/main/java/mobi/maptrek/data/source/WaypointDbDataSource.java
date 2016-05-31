package mobi.maptrek.data.source;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;

public class WaypointDbDataSource extends DataSource implements WaypointDataSource {
    private SQLiteDatabase mDatabase;
    private WaypointDbHelper mDbHelper;

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
            WaypointDbHelper.COLUMN_ICON
    };

    public WaypointDbDataSource(Context context, File file) {
        mDbHelper = new WaypointDbHelper(context, file);
        name = context.getString(R.string.waypointStoreName);
    }

    public void open() throws SQLException {
        mDatabase = mDbHelper.getWritableDatabase();
        setLoaded();
    }

    public void close() {
        mDbHelper.close();
    }

    public boolean isOpen() {
        return mDatabase.isOpen();
    }

    public void saveWaypoint(Waypoint waypoint) {
        ContentValues values = new ContentValues();
        if (waypoint._id > 0)
            values.put(WaypointDbHelper.COLUMN_ID, waypoint._id);
        values.put(WaypointDbHelper.COLUMN_NAME, waypoint.name);
        values.put(WaypointDbHelper.COLUMN_LATE6, waypoint.latitude * 1E6);
        values.put(WaypointDbHelper.COLUMN_LONE6, waypoint.longitude * 1E6);
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
        return cursor.getCount();
    }

    @Override
    public Cursor getCursor() {
        return mDatabase.query(WaypointDbHelper.TABLE_NAME, mAllColumns, null, null, null, null, WaypointDbHelper.COLUMN_NAME);
    }

    @Override
    public int getDataType(int position) {
        return TYPE_WAYPOINT;
    }

    @Override
    public Waypoint cursorToWaypoint(Cursor cursor) {
        Waypoint waypoint = new Waypoint();
        waypoint._id = cursor.getLong(cursor.getColumnIndex(WaypointDbHelper.COLUMN_ID));
        waypoint.name = cursor.getString(cursor.getColumnIndex(WaypointDbHelper.COLUMN_NAME));
        waypoint.latitude = cursor.getLong(cursor.getColumnIndex(WaypointDbHelper.COLUMN_LATE6)) / 1E6;
        waypoint.longitude = cursor.getLong(cursor.getColumnIndex(WaypointDbHelper.COLUMN_LONE6)) / 1E6;
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
        waypoint.source = this;
        return waypoint;
    }

    @Override
    public boolean isNativeTrack() {
        return false;
    }
}
