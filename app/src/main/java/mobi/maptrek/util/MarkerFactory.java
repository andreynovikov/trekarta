package mobi.maptrek.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.VectorDrawable;
import android.support.annotation.DrawableRes;

import org.oscim.backend.CanvasAdapter;

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
