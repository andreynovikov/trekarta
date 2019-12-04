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

package mobi.maptrek.data.style;

public class RouteStyle extends Style<RouteStyle> {
    public static int DEFAULT_COLOR = android.graphics.Color.DKGRAY;
    public static float DEFAULT_WIDTH = 8;

    public int color = DEFAULT_COLOR;
    public float width = DEFAULT_WIDTH;


    @Override
    public boolean isDefault() {
        return color == DEFAULT_COLOR && width == DEFAULT_WIDTH;
    }

    @Override
    public void copy(RouteStyle style) {
        style.color = color;
        style.width = width;
    }
}