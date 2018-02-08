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

public class MarkerStyle extends Style<MarkerStyle> {
    public static final int[] DEFAULT_COLORS = {
            android.graphics.Color.DKGRAY,
            0xff2062af,
            0xff58AEB7,
            0xffF4B528,
            0xffDD3E48,
            0xffBF89AE,
            0xff5C88BE,
            0xff59BC10,
            0xffE87034,
            0xfff84c44,
            0xff8c47fb,
            0xff51C1EE,
            0xff8cc453,
            0xffC2987D,
            0xffCE7777,
            0xff9086BA
    };

    public static int DEFAULT_COLOR = android.graphics.Color.DKGRAY;

    public int color = DEFAULT_COLOR;
    //TODO Add processing for this style field
    public String icon;

    @Override
    public boolean isDefault() {
        return color == DEFAULT_COLOR && icon == null;
    }

    @Override
    public void copy(MarkerStyle style) {
        style.color = color;
        style.icon = icon;
    }
}
