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

package mobi.maptrek.maps.maptrek;

import org.oscim.core.Tag;

public class ExtendedTag extends Tag {
    public ExtendedTag next = null;

    public ExtendedTag(String key, String value) {
        super(key, value);
    }

    public ExtendedTag addTag(String key, String value) {
        ExtendedTag tag = new ExtendedTag(key, value);
        tag.next = this;
        return tag;
    }
}
