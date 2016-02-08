/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2012  Andrey Novikov <http://andreynovikov.info/>
 *
 * This file is part of Androzic application.
 *
 * Androzic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Androzic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Androzic.  If not, see <http://www.gnu.org/licenses/>.
 */

package mobi.maptrek.data;

import android.graphics.Bitmap;

//TODO Refactor
public class MapObject
{
	public long _id = 0;
	public String name = "";
	public String description = "";
	public String marker = "";
	public boolean drawImage = false;
	public String style;
	public double latitude = 0;
	public double longitude = 0;
	public int altitude = Integer.MIN_VALUE;
	public int proximity = 0;
	public Bitmap bitmap;
	public int anchorX;
	public int anchorY;
	public int textcolor = Integer.MIN_VALUE;
	public int backcolor = Integer.MIN_VALUE;

	public MapObject()
	{
	}

	public MapObject(double lat, double lon)
	{
		latitude = lat;
		longitude = lon;
	}
}
