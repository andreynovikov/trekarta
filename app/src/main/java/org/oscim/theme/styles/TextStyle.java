/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2017 devemux86
 * Copyright 2016 Andrey Novikov
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
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.backend.canvas.Paint.FontFamily;
import org.oscim.backend.canvas.Paint.FontStyle;
import org.oscim.renderer.atlas.TextureRegion;

public final class TextStyle extends RenderStyle<TextStyle> {

    public static int MAX_TEXT_WIDTH = 600;
    public static String K = "name";

    public static class TextBuilder<T extends TextBuilder<T>> extends StyleBuilder<T> {

        public float fontSize;

        public String textKey;
        public boolean caption;
        public float dy;
        public int priority;
        public float areaSize;
        public Bitmap bitmap;
        public TextureRegion texture;
        public FontFamily fontFamily;
        public FontStyle fontStyle;

        public boolean mandatory;

        public int symbolWidth;
        public int symbolHeight;
        public int symbolPercent;
        public int symbolColor;

        public T reset() {
            cat = null;
            fontFamily = FontFamily.DEFAULT;
            fontStyle = FontStyle.NORMAL;
            style = null;
            textKey = K;
            fontSize = 0;
            caption = false;
            priority = 0;
            areaSize = 0f;
            bitmap = null;
            texture = null;
            fillColor = Color.BLACK;
            strokeColor = Color.BLACK;
            strokeWidth = 0;
            dy = 0;

            mandatory = false;

            symbolWidth = 0;
            symbolHeight = 0;
            symbolPercent = 100;
            symbolColor = 0;

            return self();
        }

        public TextBuilder() {
            reset();
        }

        @Override
        public TextStyle build() {
            TextStyle t = new TextStyle(this);
            t.fontHeight = t.paint.getFontHeight();
            t.fontDescent = t.paint.getFontDescent();
            return t;
        }

        public TextStyle buildInternal() {
            return new TextStyle(this);
        }

        public T fontSize(float fontSize) {
            this.fontSize = fontSize;
            return self();
        }

        public T textKey(String textKey) {
            this.textKey = textKey;
            return self();
        }

        public T isCaption(boolean caption) {
            this.caption = caption;
            return self();
        }

        public T offsetY(float dy) {
            this.dy = dy;
            return self();
        }

        public T priority(int priority) {
            this.priority = priority;
            return self();
        }

        public T areaSize(float areaSize) {
            this.areaSize = areaSize;
            return self();
        }

        public T bitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
            return self();
        }

        public T texture(TextureRegion texture) {
            this.texture = texture;
            return self();
        }

        public T fontFamily(FontFamily fontFamily) {
            this.fontFamily = fontFamily;
            return self();
        }

        public T fontStyle(FontStyle fontStyle) {
            this.fontStyle = fontStyle;
            return self();
        }

        public T mandatory(boolean mandatory) {
            this.mandatory = mandatory;
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

        public T symbolColor(int symbolPercent) {
            this.symbolColor = symbolPercent;
            return self();
        }

        public T from(TextBuilder<?> other) {
            cat = other.cat;
            fontFamily = other.fontFamily;
            fontStyle = other.fontStyle;
            style = other.style;
            textKey = other.textKey;
            fontSize = other.fontSize;
            caption = other.caption;
            priority = other.priority;
            areaSize = other.areaSize;
            bitmap = other.bitmap;
            texture = other.texture;
            fillColor = other.fillColor;
            strokeColor = other.strokeColor;
            strokeWidth = other.strokeWidth;
            dy = other.dy;

            mandatory = other.mandatory;

            symbolWidth = other.symbolWidth;
            symbolHeight = other.symbolHeight;
            symbolPercent = other.symbolPercent;
            symbolColor = other.symbolColor;

            return self();
        }

        public TextBuilder<?> set(TextStyle text) {
            if (text == null)
                return reset();

            this.cat = text.cat;
            this.style = text.style;
            this.textKey = text.textKey;
            this.caption = text.caption;
            this.dy = text.dy;
            this.priority = text.priority;
            this.areaSize = text.areaSize;
            this.bitmap = text.bitmap;
            this.texture = text.texture;
            this.fillColor = themeCallback != null ? themeCallback.getColor(text.paint.getColor()) : text.paint.getColor();
            this.fontFamily = text.fontFamily;
            this.fontStyle = text.fontStyle;
            if (text.stroke != null) {
                this.strokeColor = themeCallback != null ? themeCallback.getColor(text.stroke.getColor()) : text.stroke.getColor();
                this.strokeWidth = text.stroke.getStrokeWidth();
            }
            this.fontSize = text.fontSize;

            this.mandatory = text.mandatory;

            this.symbolWidth = text.symbolWidth;
            this.symbolHeight = text.symbolHeight;
            this.symbolPercent = text.symbolPercent;
            this.symbolColor = text.symbolColor;

            return self();
        }
    }

    TextStyle(TextBuilder<?> b) {
        this.cat = b.cat;
        this.style = b.style;
        this.textKey = b.textKey;
        this.caption = b.caption;
        this.dy = b.dy;
        this.priority = b.priority;
        this.areaSize = b.areaSize;
        this.bitmap = b.bitmap;
        this.texture = b.texture;

        paint = CanvasAdapter.newPaint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(b.fontFamily, b.fontStyle);

        paint.setColor(b.themeCallback != null ? b.themeCallback.getColor(b.fillColor) : b.fillColor);
        paint.setTextSize(b.fontSize);

        if (b.strokeWidth > 0) {
            stroke = CanvasAdapter.newPaint();
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setTextAlign(Paint.Align.CENTER);
            stroke.setTypeface(b.fontFamily, b.fontStyle);
            stroke.setColor(b.themeCallback != null ? b.themeCallback.getColor(b.strokeColor) : b.strokeColor);
            stroke.setStrokeWidth(b.strokeWidth);
            stroke.setTextSize(b.fontSize);
            strokeWidth = b.strokeWidth;
        } else {
            stroke = null;
            strokeWidth = 0;
        }

        this.fontFamily = b.fontFamily;
        this.fontStyle = b.fontStyle;
        this.fontSize = b.fontSize;

        this.mandatory = b.mandatory;

        this.symbolWidth = b.symbolWidth;
        this.symbolHeight = b.symbolHeight;
        this.symbolPercent = b.symbolPercent;
        this.symbolColor = b.symbolColor;
    }

    public final String style;

    public final FontFamily fontFamily;
    public final FontStyle fontStyle;
    public float fontSize;
    public final Paint paint;
    public final Paint stroke;
    public final String textKey;

    public final boolean caption;
    public final float dy;
    public final int priority;
    public final float areaSize;

    public float fontHeight;
    public float fontDescent;
    public float strokeWidth;

    public final Bitmap bitmap;
    public final TextureRegion texture;

    public final boolean mandatory;

    public final int symbolWidth;
    public final int symbolHeight;
    public final int symbolPercent;
    public final int symbolColor;

    @Override
    public void dispose() {
        if (bitmap != null)
            bitmap.recycle();
    }

    @Override
    public void renderNode(Callback cb) {
        cb.renderText(this);
    }

    @Override
    public void renderWay(Callback cb) {
        cb.renderText(this);
    }

    @Override
    public TextStyle current() {
        return (TextStyle) mCurrent;
    }

    @Override
    public void scaleTextSize(float scaleFactor) {
        fontSize *= scaleFactor;
        paint.setTextSize(fontSize);
        if (stroke != null)
            stroke.setTextSize(fontSize);

        fontHeight = paint.getFontHeight();
        fontDescent = paint.getFontDescent();
    }

    @SuppressWarnings("rawtypes")
    public static TextBuilder<?> builder() {
        return new TextBuilder();
    }
}
