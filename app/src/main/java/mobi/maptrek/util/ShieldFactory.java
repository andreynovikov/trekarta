/*
 * Copyright 2021 Andrey Novikov
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.Tag;
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
        String ref = tags.getValue(Tag.KEY_REF);
        if (ref == null)
            return null;

        String color = tags.getValue(Tag.KEY_ROUTE_COLOR);

        String cache_key = src + ":" + color == null ? ref : ref + "@" + color;
        Bitmap bitmap = mBitmapCache.get(cache_key);
        if (bitmap != null)
            return bitmap;

        String[] parts = src.replace("/shield/", "").trim().split("/");
        if (parts.length < 3)
            return null;

        String[] lines = ref.split(";");

        float textSize = Float.parseFloat(parts[0]) * mFontSize;
        int backColor = Color.parseColor(parts[1], Color.WHITE);
        int textColor = Color.parseColor(parts[2], Color.BLACK);

        boolean round = parts.length > 3 && "round".equals(parts[3]);

        float gap = 4f * MapTrek.density * mFontSize;
        float border = 1.6f * MapTrek.density * mFontSize;

        float size = percent * 0.01f * textSize * MapTrek.density;
        Rect bounds = new Rect();
        float textHeight = 0;

        Paint textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setAntiAlias(true);
        if (round) {
            float testTextSize = size * 4;
            textPaint.setTextSize(testTextSize);
            textPaint.getTextBounds(lines[0], 0, lines[0].length(), bounds);
            float fontSize = testTextSize * size / bounds.width();
            textPaint.setTextSize(Math.min(fontSize, size - gap));
        } else {
            textPaint.setTextSize(size);
            for (String line : lines) {
                Rect rect = new Rect();
                textPaint.getTextBounds(line, 0, line.length(), rect);
                bounds.union(rect); // update width
                bounds.bottom += textHeight;
                if (textHeight == 0f)
                    textHeight = textPaint.descent() - textPaint.ascent();
            }
        }

        float r = 2f * border;
        float cw = !round && color != null ? bounds.width() : 0f;
        float ch = !round && color != null ? 2f * gap: 0f;
        if (cw > 0) {
            if (cw < 6f * gap)
                cw = 6f * gap;
            if (cw > 12f * gap)
                cw = 12f * gap;
        }
        if (bounds.width() < cw)
            bounds.right = (int) cw;
        if (ch != 0f)
            bounds.bottom += gap + ch;

        int width = (int) ((round ? size : bounds.width()) + 2f * (gap + border));
        int height = round ? width : (int) (bounds.height() + 2f * (gap + border));

        float w2 = width / 2f;

        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // draw background
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(border);
        paint.setColor(backColor);
        if (round)
            canvas.drawCircle(w2, w2, w2 - border, paint);
        else
            canvas.drawRoundRect(border, border, width - border, height - border, r, r, paint);
        paint.setColor(textColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        if (round)
            canvas.drawCircle(w2, w2, w2 - border, paint);
        else
            canvas.drawRoundRect(border, border, width - border, height - border, r, r, paint);

        float y = (height - textPaint.descent() - textPaint.ascent() - textHeight * (lines.length - 1)) / 2f;
        if (ch != 0f)
            y -= (ch + gap) / 2f;
        for (String line : lines) {
            canvas.drawText(line, w2, y, textPaint);
            y += textHeight;
        }

        if (color != null) { // TODO: emphasize white route color
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Integer.parseInt(color)); // route color is without alpha
            paint.setAlpha(0xFF);
            float hg = (bounds.width() - cw) / 2f + gap;
            float cr = r / 2f;
            canvas.drawRoundRect(border + hg, height - border - gap - ch, width - border - hg, height - border - gap, cr, cr, paint);
        }

        bitmap = new AndroidBitmap(bmp);
        mBitmapCache.put(cache_key, bitmap);
        return bitmap;
    }

    public void setFontSize(float fontSize) {
        mFontSize = fontSize;
    }

    public void dispose() {
        mBitmapCache.clear();
    }
}
