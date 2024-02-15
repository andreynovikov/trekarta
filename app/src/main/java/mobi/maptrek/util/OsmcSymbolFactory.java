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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.utils.ColorUtil;
import org.oscim.utils.ColorsCSS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.MapTrek;

/**
 * <p>Generates bitmap representing hiking symbol encoded in
 * <a href="http://wiki.openstreetmap.org/wiki/Key:osmc:symbol">OSM format</a>.</p>
 * <p>Some examples:</p>
 * <pre>
 * yellow::yellow_diamond
 * red:white:red_bar
 * blue:blue:shell_modern
 * white
 * orange:orange
 * yellow:white:yellow_turned_T
 * black:black:A1:white
 * blue:blue::3:white
 * red:white:::T:red
 * blue:black:black_rectangle:A5:white
 * yellow:white:yellow_lower:253:black
 * yellow:yellow:white_lower:red_bar:KGW:black
 * red:green_frame:white_rectangle:hiker:RRa:green
 * </pre>
 */
public class OsmcSymbolFactory {
    private static final Logger logger = LoggerFactory.getLogger(OsmcSymbolFactory.class);

    private static final HashSet<String> VALID_BACKGROUNDS;
    private static final HashSet<String> VALID_FOREGROUNDS;

    static {
        VALID_BACKGROUNDS = new HashSet<>(3);
        VALID_BACKGROUNDS.add("round");
        VALID_BACKGROUNDS.add("circle");
        VALID_BACKGROUNDS.add("frame");

        VALID_FOREGROUNDS = new HashSet<>(36);
        VALID_FOREGROUNDS.add("ammonit");
        VALID_FOREGROUNDS.add("arch");
        VALID_FOREGROUNDS.add("backslash");
        VALID_FOREGROUNDS.add("bar");
        VALID_FOREGROUNDS.add("black_red_diamond");
        VALID_FOREGROUNDS.add("bowl");
        VALID_FOREGROUNDS.add("circle");
        VALID_FOREGROUNDS.add("corner");
        VALID_FOREGROUNDS.add("cross");
        VALID_FOREGROUNDS.add("diamond");
        VALID_FOREGROUNDS.add("diamond_line");
        VALID_FOREGROUNDS.add("dot");
        VALID_FOREGROUNDS.add("drop_line");
        VALID_FOREGROUNDS.add("fork");
        VALID_FOREGROUNDS.add("hexagon");
        VALID_FOREGROUNDS.add("hiker");
        VALID_FOREGROUNDS.add("horse");
        VALID_FOREGROUNDS.add("lower");
        VALID_FOREGROUNDS.add("L");
        VALID_FOREGROUNDS.add("mine");
        VALID_FOREGROUNDS.add("pointer");
        VALID_FOREGROUNDS.add("rectangle");
        VALID_FOREGROUNDS.add("rectangle_line");
        VALID_FOREGROUNDS.add("shell");
        VALID_FOREGROUNDS.add("shell_modern");
        VALID_FOREGROUNDS.add("slash");
        VALID_FOREGROUNDS.add("stripe");
        VALID_FOREGROUNDS.add("tower");
        VALID_FOREGROUNDS.add("triangle");
        VALID_FOREGROUNDS.add("triangle_line");
        VALID_FOREGROUNDS.add("triangle_turned");
        VALID_FOREGROUNDS.add("turned_T");
        VALID_FOREGROUNDS.add("wheel");
        VALID_FOREGROUNDS.add("white_red_diamond");
        VALID_FOREGROUNDS.add("wolfshook");
        VALID_FOREGROUNDS.add("x");
    }

    private final BitmapCache<String, Bitmap> mBitmapCache;

    public OsmcSymbolFactory() {
        mBitmapCache = new BitmapCache<>(4098);
    }

    public @Nullable
    synchronized Bitmap getBitmap(@NonNull String osmcSymbol, int symbolPercent) {
        String key = osmcSymbol + "%%%" + symbolPercent;
        Bitmap bitmap = mBitmapCache.get(key);
        if (bitmap != null)
            return bitmap;

        int size = (int) (symbolPercent * 0.2 * MapTrek.density); // 20dip base
        float hSize = size / 2;
        float pWidth = 3 * MapTrek.density;
        float hWidth = pWidth / 2;

        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // osmc:symbol=waycolor:background[:foreground][[:foreground2]:text:textcolor]
        logger.debug("symbol: {}", osmcSymbol);
        String[] parts = osmcSymbol.trim().split("\\s*:\\s*");

        // draw background
        boolean isRound = false;
        String[] background = parts.length > 1 ?
                parts[1].trim().split("\\s*_\\s*") :
                parts[0].trim().split("\\s*_\\s*");
        logger.debug("  background: {}", Arrays.toString(background));
        Integer backgroundColor = ColorsCSS.get(background[0]);
        if (BuildConfig.DEBUG && backgroundColor == null)
            logger.error("Unknown background color: {}", background[0]);
        if (background.length == 1 || !VALID_BACKGROUNDS.contains(background[1])) {
            if (backgroundColor != null)
                canvas.drawColor(backgroundColor);
            else
                canvas.drawColor(Color.WHITE);
        } else {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(pWidth);
            int fillColor = "white".equals(background[0]) ? Color.BLACK : Color.WHITE;
            int backColor = backgroundColor != null ? backgroundColor : Color.BLACK;
            if ("round".equals(background[1])) {
                isRound = true;
                paint.setColor(backColor);
                canvas.drawCircle(hSize, hSize, hSize - hWidth, paint);
            } else if ("circle".equals(background[1])) {
                isRound = true;
                paint.setColor(fillColor);
                canvas.drawCircle(hSize, hSize, hSize - hWidth, paint);
                paint.setColor(backColor);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(hSize, hSize, hSize - hWidth, paint);
            } else {
                //paint.setStyle(Paint.Style.STROKE);
                paint.setColor(fillColor);
                //noinspection SuspiciousNameCombination
                canvas.drawRect(hWidth, hWidth, size - hWidth, size - hWidth, paint);
                paint.setColor(backColor);
                paint.setStyle(Paint.Style.STROKE);
                //noinspection SuspiciousNameCombination
                canvas.drawRect(hWidth, hWidth, size - hWidth, size - hWidth, paint);
            }
        }

        // draw symbol
        if (parts.length == 3 || parts.length > 4)
            drawSymbol(canvas, parts[2], size);

        // draw second symbol
        if (parts.length > 5)
            drawSymbol(canvas, parts[3], size);

        // draw text
        if (parts.length > 3) {
            String text = parts[parts.length - 2];
            String color = parts[parts.length - 1];
            Integer textColor = ColorsCSS.get(color);
            if ("yellow".equals(color)) // yellow text is unreadable
                textColor = ColorUtil.modHsv(textColor, 1.0, 1.2, 0.8, false);
            if (BuildConfig.DEBUG && textColor == null)
                logger.error("Unknown text color: {}", parts[parts.length - 1]);
            if (textColor != null && text.length() > 0) {
                Paint textPaint = new Paint();
                textPaint.setColor(textColor);
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                float width = size - pWidth;
                if (isRound)
                    width = hSize;
                else if (text.length() == 2)
                    width = width * 0.7f;
                else if (text.length() == 1)
                    width = width * 0.5f;
                setTextSizeForWidth(textPaint, width, text);
                float y = hSize - ((textPaint.descent() + textPaint.ascent()) / 2);
                canvas.drawText(text, hSize, y, textPaint);
            }
        }

        bitmap = new AndroidBitmap(bmp);
        mBitmapCache.put(key, bitmap);
        return bitmap;
    }

    private void drawSymbol(Canvas canvas, String foreground, int size) {
        if (foreground.length() == 0)
            return;

        Integer foregroundColor = null;
        String symbol = null;
        if (VALID_FOREGROUNDS.contains(foreground)) {
            // foreground is encoded as symbol without color
            logger.debug("  foreground: black {}", foreground);
            if ("shell".equals(foreground) || "shell_modern".equals(foreground))
                foregroundColor = Color.YELLOW;
            else
                foregroundColor = Color.BLACK;
            symbol = foreground;

        } else {
            // foreground should contain coloured symbol
            String[] foreground_parts = foreground.trim().split("\\s*_\\s*", 2);
            logger.debug("  foreground: {}", Arrays.toString(foreground_parts));
            if (foreground_parts.length == 2) {
                foregroundColor = ColorsCSS.get(foreground_parts[0]);
                if (VALID_FOREGROUNDS.contains(foreground_parts[1]))
                    symbol = foreground_parts[1];
            }
            if (BuildConfig.DEBUG && foregroundColor == null)
                logger.error("Unknown foreground color: {}", foreground_parts[0]);
            if (BuildConfig.DEBUG && symbol == null)
                logger.error("Unknown foreground symbol: {}", foreground_parts.length == 1 ?
                        foreground_parts[0] : foreground_parts[1]);
        }
        if (foregroundColor != null && symbol != null) {
            try {
                Bitmap symbolBitmap = CanvasAdapter.getBitmapAsset("symbols/osmc", symbol + ".svg", size, size, 100, foregroundColor);
                canvas.drawBitmap(AndroidGraphics.getBitmap(symbolBitmap), 0, 0, null);
            } catch (IOException e) {
                logger.error("Failed to load bitmap for " + symbol, e);
            }
        }
    }

    /**
     * Sets the text size for a Paint object so a given string of text will be a
     * given width.
     *
     * @param paint        the Paint to set the text size for
     * @param desiredWidth the desired width
     * @param text         the text that should be that width
     */
    private static void setTextSizeForWidth(Paint paint, float desiredWidth, String text) {
        // Pick a reasonably large value for the test. Larger values produce
        // more accurate results, but may cause problems with hardware
        // acceleration. But there are workarounds for that, too; refer to
        // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        final float testTextSize = 100;

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Do not oversize too short text
        float width = bounds.width() > bounds.height() ? bounds.width() : bounds.height();

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = testTextSize * desiredWidth / width;

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize);
    }

    public void dispose() {
        mBitmapCache.clear();
    }
}
