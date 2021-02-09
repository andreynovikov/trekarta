/*
 * Copyright 2019 Andrey Novikov
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

import mobi.maptrek.maps.maptrek.Tags;

public class Amenity extends Waypoint {
    public enum Fee {
        YES, NO
    }

    public enum Wheelchair {
        YES, LIMITED, NO
    }

    public int kindNumber;
    public String kind;
    public int type;
    public String openingHours;
    public String phone;
    public String wikipedia;
    public String website;
    public Fee fee;
    public Wheelchair wheelchair;

    public Amenity(long id, int kind, int type, double lat, double lon) {
        super(lat, lon);
        this.kindNumber = kind;
        this.kind = Tags.getKindName(kindNumber);
        this.type = Tags.getTypeName(type);
        this._id = id;
    }
}
