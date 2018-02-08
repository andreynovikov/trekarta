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

package mobi.maptrek.data.style;

public class TrackStyle extends Style<TrackStyle> {
    public static int DEFAULT_COLOR = android.graphics.Color.MAGENTA;
    public static float DEFAULT_WIDTH = 5;

    public int color = DEFAULT_COLOR;
    public float width = DEFAULT_WIDTH;


    @Override
    public boolean isDefault() {
        return color == DEFAULT_COLOR && width == DEFAULT_WIDTH;
    }

    @Override
    public void copy(TrackStyle style) {
        style.color = color;
        style.width = width;
    }
}
