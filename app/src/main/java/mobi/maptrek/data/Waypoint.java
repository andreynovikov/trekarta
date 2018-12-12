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

import org.oscim.core.GeoPoint;

import java.util.Date;

import mobi.maptrek.data.source.DataSource;

//TODO Refactor
public class Waypoint extends MapObject
{
    /**
     * Date and time in GMT
     */
	public Date date;
	public DataSource source;
	public boolean locked;

    public Waypoint(double latitude, double longitude)
	{
		super(latitude, longitude);
	}

	public Waypoint(int latitudeE6, int longitudeE6)
	{
		super(latitudeE6, longitudeE6);
	}

	public Waypoint(GeoPoint coordinates) {
		super(coordinates);
	}

	public Waypoint(String name, double lat, double lon)
	{
		super(lat, lon);
		this.name = name;
	}
}