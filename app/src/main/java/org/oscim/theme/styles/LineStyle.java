/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2019 devemux86
 * Copyright 2017 Longri
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
 */
package org.oscim.theme.styles;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.renderer.bucket.TextureItem;

import static org.oscim.backend.canvas.Color.parseColor;

public final class LineStyle extends RenderStyle<LineStyle> {

    public enum Half {
        LEFT, RIGHT
    }

    public static final float REPEAT_START_DEFAULT = 0f;
    public static final float REPEAT_GAP_DEFAULT = 0f;

    private final int level;
    public final String style;
    public final float width;
    public final int color;
    public final Cap cap;
    public final boolean outline;
    public final boolean fixed;
    public final Half half;
    public final double strokeIncrease;
    public final int fadeScale;
    public final float blur;

    public final int stipple;
    public final int stippleColor;
    public final float stippleWidth;
    public final float stippleRatio;
    public final TextureItem texture;

    public final float heightOffset;
    public final boolean randomOffset;

    public final int symbolWidth;
    public final int symbolHeight;
    public final int symbolPercent;
    public final int symbolColor;

    public final float[] dashArray;
    public final boolean repeat;
    public final float repeatStart;
    public final float repeatGap;

    public LineStyle(int stroke, float width) {
        this(0, "", stroke, width, Cap.BUTT, true, null, 1, 0, 0, 0, 0.5f, -1, 0, false, null, true, null, REPEAT_START_DEFAULT * CanvasAdapter.getScale(), REPEAT_GAP_DEFAULT * CanvasAdapter.getScale());
    }

    public LineStyle(int level, int stroke, float width) {
        this(level, "", stroke, width, Cap.BUTT, true, null, 1, 0, 0, 0, 0.5f, -1, 0, false, null, true, null, REPEAT_START_DEFAULT * CanvasAdapter.getScale(), REPEAT_GAP_DEFAULT * CanvasAdapter.getScale());
    }

    public LineStyle(int stroke, float width, Cap cap) {
        this(0, "", stroke, width, cap, true, null, 1, 0, 0, 0, 0.5f, -1, 0, false, null, true, null, REPEAT_START_DEFAULT * CanvasAdapter.getScale(), REPEAT_GAP_DEFAULT * CanvasAdapter.getScale());
    }

    public LineStyle(int level, String style, int color, float width,
                     Cap cap, boolean fixed, Half half, double strokeIncrease,
                     int stipple, int stippleColor, float stippleWidth, float stippleRatio,
                     int fadeScale, float blur, boolean isOutline, TextureItem texture,
                     boolean randomOffset, float[] dashArray, float repeatStart, float repeatGap) {

        this.level = level;
        this.style = style;
        this.outline = isOutline;

        this.cap = cap;
        this.color = color;
        this.width = width;
        this.fixed = fixed;
        this.half = half;
        this.strokeIncrease = strokeIncrease;

        this.stipple = stipple;
        this.stippleColor = stippleColor;
        this.stippleWidth = stippleWidth;
        this.stippleRatio = stippleRatio;
        this.texture = texture;

        this.blur = blur;
        this.fadeScale = fadeScale;

        this.heightOffset = 0;
        this.randomOffset = randomOffset;

        this.symbolWidth = 0;
        this.symbolHeight = 0;
        this.symbolPercent = 100;
        this.symbolColor = 0;

        this.dashArray = dashArray;
        this.repeat = false;
        this.repeatStart = repeatStart;
        this.repeatGap = repeatGap;
    }

    private LineStyle(LineBuilder<?> b) {
        this.cat = b.cat;
        this.level = b.level;
        this.style = b.style;
        this.width = b.strokeWidth;
        this.color = b.themeCallback != null ? b.themeCallback.getColor(this, b.fillColor) : b.fillColor;
        this.cap = b.cap;
        this.outline = b.outline;
        this.fixed = b.fixed;
        this.half = b.half;
        this.strokeIncrease = b.strokeIncrease;
        this.fadeScale = b.fadeScale;
        this.blur = b.blur;
        this.stipple = b.stipple;
        this.stippleColor = b.themeCallback != null ? b.themeCallback.getColor(this, b.stippleColor) : b.stippleColor;
        this.stippleWidth = b.stippleWidth;
        this.stippleRatio = b.stippleRatio;
        this.texture = b.texture;

        this.heightOffset = b.heightOffset;
        this.randomOffset = b.randomOffset;

        this.symbolWidth = b.symbolWidth;
        this.symbolHeight = b.symbolHeight;
        this.symbolPercent = b.symbolPercent;
        this.symbolColor = b.symbolColor;

        this.dashArray = b.dashArray;
        this.repeat = b.repeat;
        this.repeatStart = b.repeatStart;
        this.repeatGap = b.repeatGap;
    }

    @Override
    public LineStyle current() {
        return (LineStyle) mCurrent;
    }

    @Override
    public void renderWay(Callback cb) {
        cb.renderWay(this, level);
    }

    public static class LineBuilder<T extends LineBuilder<T>> extends StyleBuilder<T> {

        public Cap cap;
        public boolean outline;
        public boolean fixed;
        public Half half;
        public double strokeIncrease;
        public int fadeScale;
        public float blur;

        public int stipple;
        public int stippleColor;
        public float stippleWidth;
        public float stippleRatio;
        public TextureItem texture;

        public float heightOffset;
        public boolean randomOffset;

        public int symbolWidth;
        public int symbolHeight;
        public int symbolPercent;
        public int symbolColor;

        public float[] dashArray;
        public boolean repeat;
        public float repeatStart;
        public float repeatGap;

        public LineBuilder() {
        }

        public T set(LineStyle line) {
            if (line == null)
                return reset();

            this.cat = line.cat;
            this.level = line.level;
            this.style = line.style;
            this.strokeWidth = line.width;
            this.fillColor = themeCallback != null ? themeCallback.getColor(line, line.color) : line.color;
            this.cap = line.cap;
            this.outline = line.outline;
            this.fixed = line.fixed;
            this.half = line.half;
            this.strokeIncrease = line.strokeIncrease;
            this.fadeScale = line.fadeScale;
            this.blur = line.blur;
            this.stipple = line.stipple;
            this.stippleColor = themeCallback != null ? themeCallback.getColor(line, line.stippleColor) : line.stippleColor;
            this.stippleWidth = line.stippleWidth;
            this.stippleRatio = line.stippleRatio;
            this.texture = line.texture;

            this.heightOffset = line.heightOffset;
            this.randomOffset = line.randomOffset;

            this.symbolWidth = line.symbolWidth;
            this.symbolHeight = line.symbolHeight;
            this.symbolPercent = line.symbolPercent;
            this.symbolColor = line.symbolColor;

            this.dashArray = line.dashArray;
            this.repeat = line.repeat;
            this.repeatStart = line.repeatStart;
            this.repeatGap = line.repeatGap;

            return self();
        }

        public T blur(float blur) {
            this.blur = blur;
            return self();
        }

        public T fadeScale(int zoom) {
            this.fadeScale = zoom;
            return self();
        }

        public T stipple(int width) {
            this.stipple = width;
            return self();
        }

        public T stippleColor(int color) {
            this.stippleColor = color;
            return self();
        }

        public T stippleColor(String color) {
            this.stippleColor = parseColor(color);
            return self();
        }

        public T stippleWidth(float width) {
            this.stippleWidth = width;
            return self();
        }

        public T stippleRatio(float ratio) {
            this.stippleRatio = ratio;
            return self();
        }

        public T isOutline(boolean outline) {
            this.outline = outline;
            return self();
        }

        public T cap(Cap cap) {
            this.cap = cap;
            return self();
        }

        public T fixed(boolean b) {
            this.fixed = b;
            return self();
        }

        public T half(Half half) {
            this.half = half;
            return self();
        }

        public T strokeIncrease(double strokeIncrease) {
            this.strokeIncrease = strokeIncrease;
            return self();
        }

        public T texture(TextureItem texture) {
            this.texture = texture;
            return self();
        }

        public T heightOffset(float heightOffset) {
            this.heightOffset = heightOffset;
            return self();
        }

        public T randomOffset(boolean randomOffset) {
            this.randomOffset = randomOffset;
            return self();
        }

        public T symbolWidth(int symbolWidth) {
            this.symbolWidth = symbolWidth;
            return self();
        }

        public T symbolHeight(int symbolHeight) {
            this.symbolHeight = symbolHeight;
            return self();
        }

        public T symbolPercent(int symbolPercent) {
            this.symbolPercent = symbolPercent;
            return self();
        }

        public T symbolColor(int symbolColor) {
            this.symbolColor = symbolColor;
            return self();
        }

        public T dashArray(float[] dashArray) {
            this.dashArray = dashArray;
            return self();
        }

        public T repeat(boolean repeat) {
            this.repeat = repeat;
            return self();
        }

        public T repeatStart(float repeatStart) {
            this.repeatStart = repeatStart;
            return self();
        }

        public T repeatGap(float repeatGap) {
            this.repeatGap = repeatGap;
            return self();
        }

        public T reset() {
            cat = null;
            level = -1;
            style = null;
            fillColor = Color.BLACK;
            cap = Cap.ROUND;
            outline = false;
            strokeWidth = 1;
            fixed = false;
            half = null;
            strokeIncrease = 1;

            fadeScale = -1;
            blur = 0;

            stipple = 0;
            stippleWidth = 1;
            stippleRatio = 0.5f;
            stippleColor = Color.BLACK;
            texture = null;

            heightOffset = 0;
            randomOffset = true;

            symbolWidth = 0;
            symbolHeight = 0;
            symbolPercent = 100;
            symbolColor = 0;

            dashArray = null;
            repeatStart = REPEAT_START_DEFAULT * CanvasAdapter.getScale();
            repeatGap = REPEAT_GAP_DEFAULT * CanvasAdapter.getScale();

            return self();
        }

        @Override
        public LineStyle build() {
            return new LineStyle(this);
        }
    }

    @SuppressWarnings("rawtypes")
    public static LineBuilder<?> builder() {
        return new LineBuilder();
    }
}
