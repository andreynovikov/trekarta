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

package mobi.maptrek.maps.maptrek;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.oscim.tiling.source.sqlite.MBTilesDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class HillshadeDatabaseHelper extends MBTilesDatabase.MBTilesDatabaseHelper {
    private static final Logger logger = LoggerFactory.getLogger(HillshadeDatabaseHelper.class);

    public HillshadeDatabaseHelper(Context context, File file) {
        super(context, file);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        logger.info("Creating hillshades database");
        super.onCreate(db);
        db.execSQL(MapTrekDatabaseHelper.PRAGMA_ENABLE_VACUUM);
        db.execSQL(MapTrekDatabaseHelper.PRAGMA_PAGE_SIZE);
        db.execSQL("INSERT INTO metadata VALUES ('name', 'Hillshades')");
        db.execSQL("INSERT INTO metadata VALUES ('type', 'overlay')");
        db.execSQL("INSERT INTO metadata VALUES ('description', 'MapTrek hillshade layer')");
        db.execSQL("INSERT INTO metadata VALUES ('format', 'png')");
        db.execSQL("INSERT INTO metadata VALUES ('minzoom', '8')");
        db.execSQL("INSERT INTO metadata VALUES ('maxzoom', '12')");
        db.execSQL("INSERT INTO metadata VALUES ('tile_row_type', 'xyz')");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        logger.info("Vacuuming hillshades database");
        Cursor cursor = db.rawQuery(MapTrekDatabaseHelper.PRAGMA_VACUUM, null);
        if (cursor.moveToFirst())
            logger.debug("  removed {} pages", cursor.getCount());
        cursor.close();
    }
}
