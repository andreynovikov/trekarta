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

package mobi.maptrek.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.VectorDrawable;
import android.support.annotation.DrawableRes;

import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.style.MarkerStyle;

public class MarkerFactory {
    public static Bitmap getMarkerSymbol(Context context) {
        return getMarkerSymbol(context, MarkerStyle.DEFAULT_COLOR);
    }

    public static Bitmap getMarkerSymbol(Context context, int color) {
        return getMarkerSymbol(context, R.drawable.marker, color);
    }

    public static Bitmap getMarkerSymbol(Context context, @DrawableRes int drawableRes, int color) {
        VectorDrawable vectorDrawable = (VectorDrawable) context.getDrawable(drawableRes);
        assert vectorDrawable != null;
        int size = (int) (25 * MapTrek.density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, size, size);
        //if (color != DEFAULT_COLOR) - tint is cached for some reason o_O
        vectorDrawable.setTint(color);
        vectorDrawable.draw(canvas);
        return bitmap;
    }
}
