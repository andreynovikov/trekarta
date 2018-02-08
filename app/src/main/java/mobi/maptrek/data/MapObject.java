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

package mobi.maptrek.data;

import android.graphics.Bitmap;

import org.oscim.core.GeoPoint;

import mobi.maptrek.data.style.MarkerStyle;

//TODO Refactor
public class MapObject {
    public long _id = 0;
    public String name;
    public String description;
    public GeoPoint coordinates;
    /**
     * Object altitude, if set to Integer.MIN_VALUE then it is undefined
     */
    public int altitude = Integer.MIN_VALUE;
    public int proximity = 0;
    public MarkerStyle style = new MarkerStyle();
    private Bitmap bitmap;
    public String marker;
    public int textColor;

    public MapObject(double latitude, double longitude) {
        coordinates = new GeoPoint(latitude, longitude);
    }

    public MapObject(int latitudeE6, int longitudeE6) {
        coordinates = new GeoPoint(latitudeE6, longitudeE6);
    }

    public MapObject(String name, GeoPoint coordinates) {
        this.name = name;
        this.coordinates = coordinates;
    }

    public void setCoordinates(GeoPoint coordinates) {
        this.coordinates = coordinates;
    }

    public void setCoordinates(double latitude, double longitude) {
        setCoordinates(new GeoPoint(latitude, longitude));
    }

    public synchronized Bitmap getBitmap() {
        return bitmap;
    }

    public synchronized Bitmap getBitmapCopy() {
        return bitmap == null ? null : bitmap.copy(bitmap.getConfig(), false);
    }

    public synchronized void setBitmap(Bitmap bitmap) {
        if (this.bitmap != null)
            this.bitmap.recycle();
        this.bitmap = bitmap;
    }

    @Override
    public boolean equals(Object o) {
        return this._id != 0 && o instanceof MapObject && this._id == ((MapObject) o)._id || super.equals(o);
    }


    public static class AddedEvent {
        public MapObject mapObject;

        public AddedEvent(MapObject mapObject) {
            this.mapObject = mapObject;
        }
    }

    public static class RemovedEvent {
        public MapObject mapObject;

        public RemovedEvent(MapObject mapObject) {
            this.mapObject = mapObject;
        }
    }

    public static class UpdatedEvent {
        public MapObject mapObject;

        public UpdatedEvent(MapObject mapObject) {
            this.mapObject = mapObject;
        }
    }
}
