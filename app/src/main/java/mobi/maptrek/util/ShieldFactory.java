/*
 * Copyright 2017 Andrey Novikov
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.TagSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mobi.maptrek.MapTrek;

/**
 * <p>Generates bitmap representing a road shield</p>
 */
public class ShieldFactory {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(ShieldFactory.class);

    private final BitmapCache<String, Bitmap> mBitmapCache;
    private float mFontSize;

    public ShieldFactory() {
        mBitmapCache = new BitmapCache<>(512);
        mFontSize = 1f;
    }

    public @Nullable
    synchronized Bitmap getBitmap(@NonNull TagSet tags, String src, int percent) {
        String ref = tags.getValue("ref");
        if (ref == null)
            return null;

        Bitmap bitmap = mBitmapCache.get(ref);
        if (bitmap != null)
            return bitmap;

        String[] parts = src.replace("/shield/", "").trim().split("/");
        if (parts.length < 3)
            return null;

        String[] lines = ref.split(";");

        float textSize = Float.parseFloat(parts[0]) * mFontSize;
        int backColor = Color.parseColor(parts[1], Color.WHITE);
        int textColor = Color.parseColor(parts[2], Color.BLACK);

        float size = percent * 0.01f * textSize * MapTrek.density;

        Paint textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(size);
        float textHeight = 0;
        Rect bounds = new Rect();
        for (String line : lines) {
            Rect rect = new Rect();
            textPaint.getTextBounds(line, 0, line.length(), rect);
            bounds.union(rect); // update width
            bounds.bottom += textHeight;
            if (textHeight == 0f)
                textHeight = textPaint.descent() - textPaint.ascent();
        }

        float gap = 4f * MapTrek.density * mFontSize;
        float border = 1.6f * MapTrek.density * mFontSize;
        float r = 2f * border;
        float hb = border / 2f;
        int width = (int) (bounds.width() + 2 * (gap + border));
        int height = (int) (bounds.height() + 2 * (gap + border));

        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // draw background
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(border);
        paint.setColor(backColor);
        canvas.drawRoundRect(hb, hb, width - hb, height - hb, r, r, paint);
        paint.setColor(textColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        canvas.drawRoundRect(hb, hb, width - hb, height - hb, r, r, paint);

        float x = width / 2f;
        float y = height / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f) - textHeight * (lines.length - 1) / 2f;
        for (String line : lines) {
            canvas.drawText(line, x, y, textPaint);
            y += textHeight;
        }

        bitmap = new AndroidBitmap(bmp);
        mBitmapCache.put(ref, bitmap);
        return bitmap;
    }

    public void setFontSize(float fontSize) {
        mFontSize = fontSize;
    }

    public void dispose() {
        mBitmapCache.clear();
    }
}
