/*
 * Copyright 2012 osmdroid authors:
 * Copyright 2012 Nicolas Gramlich
 * Copyright 2012 Theodore Hong
 * Copyright 2012 Fred Eisele
 * 
 * Copyright 2014 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
 */

package mobi.maptrek.layers.marker;

import android.graphics.Color;

import org.oscim.core.GeoPoint;

/**
 * Class describing a GeoPoint with a Title and a Description.
 */
public class MarkerItem {

	public static final int ITEM_STATE_FOCUSED_MASK = 4;
	public static final int ITEM_STATE_PRESSED_MASK = 1;
	public static final int ITEM_STATE_SELECTED_MASK = 2;

	/**
	 * Indicates a hotspot for an area. This is where the origin (0,0) of a
	 * point will be located relative to the area. In other words this acts as an
	 * offset. NONE indicates that no adjustment should be made.
	 */
	public enum HotspotPlace {
		NONE, CENTER, BOTTOM_CENTER,
		TOP_CENTER, RIGHT_CENTER, LEFT_CENTER,
		UPPER_RIGHT_CORNER, LOWER_RIGHT_CORNER,
		UPPER_LEFT_CORNER, LOWER_LEFT_CORNER
	}

	public Object uid;
	public String title;
	public String description;
	public GeoPoint geoPoint;
	public int color;
	protected MarkerSymbol mMarker;

	/**
	 * @param title
	 *            this should be <b>singleLine</b> (no <code>'\n'</code> )
	 * @param description
	 *            a <b>multiLine</b> description ( <code>'\n'</code> possible)
	 */
	public MarkerItem(String title, String description, GeoPoint geoPoint) {
		this(null, title, description, geoPoint);
	}

	public MarkerItem(Object uid, String title, String description, GeoPoint geoPoint) {
		this(uid, title, description, geoPoint, Color.BLACK);
	}

	public MarkerItem(Object uid, String title, String description, GeoPoint geoPoint, int color) {
		this.title = title;
		this.description = description;
		this.geoPoint = geoPoint;
		this.uid = uid;
		this.color = color;
	}

	public Object getUid() {
		return uid;
	}

	public String getTitle() {
		return title;
	}

	public String getSnippet() {
		return description;
	}

	public GeoPoint getPoint() {
		return geoPoint;
	}

	public void setPoint(GeoPoint geoPoint) {
		this.geoPoint = geoPoint;
	}

	public MarkerSymbol getMarker() {
		return mMarker;
	}

	public void setMarker(MarkerSymbol marker) {
		mMarker = marker;
	}
}
