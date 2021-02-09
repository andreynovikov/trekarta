/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2017 devemux86
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

import org.oscim.backend.canvas.Bitmap;
import org.oscim.renderer.atlas.TextureRegion;

/**
 * Represents an icon on the map.
 */
public final class SymbolStyle extends RenderStyle<SymbolStyle> {

    public final Bitmap bitmap;
    public final TextureRegion texture;
    public final int hash;
    public final String src;

    public final int symbolWidth;
    public final int symbolHeight;
    public final int symbolPercent;
    public final int symbolColor;

    public final boolean billboard;
    public final boolean rotate;
    public final boolean inverse;
    public final boolean mandatory;

    public final boolean repeat;
    public final int repeatStart;
    public final int repeatGap;
    public final int mergeGap;
    public final String mergeGroup;
    public final int mergeGroupGap;
    public final boolean textOverlap;
    public final int zIndex;

    public SymbolStyle(Bitmap bitmap) {
        this(bitmap, null, 0);
    }

    public SymbolStyle(TextureRegion texture) {
        this(null, texture, 0);
    }

    public SymbolStyle(int hash) {
        this(null, null, hash);
    }

    private SymbolStyle(Bitmap bitmap, TextureRegion texture, int hash) {
        this.bitmap = bitmap;
        this.texture = texture;
        this.hash = hash;
        this.src = null;

        this.symbolWidth = 0;
        this.symbolHeight = 0;
        this.symbolPercent = 100;
        this.symbolColor = 0;

        this.billboard = false;
        this.rotate = false;
        this.inverse = false;
        this.mandatory = false;

        this.repeat = false;
        this.repeatStart = 0;
        this.repeatGap = 0;
        this.mergeGap = -1;
        this.mergeGroup = null;
        this.mergeGroupGap = -1;
        this.textOverlap = true;
        this.zIndex = 0;
    }

    public SymbolStyle(SymbolBuilder<?> b) {
        this.cat = b.cat;

        this.bitmap = b.bitmap;
        this.texture = b.texture;
        this.hash = b.hash;
        this.src = b.src;

        this.symbolWidth = b.symbolWidth;
        this.symbolHeight = b.symbolHeight;
        this.symbolPercent = b.symbolPercent;
        this.symbolColor = b.symbolColor;

        this.billboard = b.billboard;
        this.rotate = b.rotate;
        this.inverse = b.inverse;
        this.mandatory = b.mandatory;

        this.repeat = b.repeat;
        this.repeatStart = b.repeatStart;
        this.repeatGap = b.repeatGap;
        this.mergeGap = b.mergeGap;
        this.mergeGroup = b.mergeGroup;
        this.mergeGroupGap = b.mergeGroupGap;
        this.textOverlap = b.textOverlap;
        this.zIndex = b.zIndex;
    }

    @Override
    public SymbolStyle current() {
        return (SymbolStyle) mCurrent;
    }

    @Override
    public void dispose() {
        if (bitmap != null)
            bitmap.recycle();
    }

    @Override
    public void renderNode(Callback cb) {
        cb.renderSymbol(this);
    }

    @Override
    public void renderWay(Callback cb) {
        cb.renderSymbol(this);
    }

    public static class SymbolBuilder<T extends SymbolBuilder<T>> extends StyleBuilder<T> {

        public Bitmap bitmap;
        public TextureRegion texture;
        public int hash;
        public String src;

        public int symbolWidth;
        public int symbolHeight;
        public int symbolPercent;
        public int symbolColor;

        public boolean billboard;
        public boolean rotate;
        public boolean inverse;
        public boolean mandatory;

        public boolean repeat;
        public int repeatStart;
        public int repeatGap;
        public int mergeGap;
        public String mergeGroup;
        public int mergeGroupGap;
        public boolean textOverlap = true;
        public int zIndex;

        public SymbolBuilder() {
        }

        public T from(SymbolBuilder<?> other) {
            this.cat = other.cat;

            this.bitmap = other.bitmap;
            this.texture = other.texture;
            this.hash = other.hash;
            this.src = other.src;

            this.symbolWidth = other.symbolWidth;
            this.symbolHeight = other.symbolHeight;
            this.symbolPercent = other.symbolPercent;
            this.symbolColor = other.symbolColor;

            this.billboard = other.billboard;
            this.rotate = other.rotate;
            this.inverse = other.inverse;
            this.mandatory = other.mandatory;

            this.repeat = other.repeat;
            this.repeatStart = other.repeatStart;
            this.repeatGap = other.repeatGap;
            this.mergeGap = other.mergeGap;
            this.mergeGroup = other.mergeGroup;
            this.mergeGroupGap = other.mergeGroupGap;
            this.textOverlap = other.textOverlap;
            this.zIndex = other.zIndex;

            return self();
        }

        public T set(SymbolStyle symbol) {
            if (symbol == null)
                return reset();

            this.cat = symbol.cat;

            this.bitmap = symbol.bitmap;
            this.texture = symbol.texture;
            this.hash = symbol.hash;
            this.src = symbol.src;

            this.symbolWidth = symbol.symbolWidth;
            this.symbolHeight = symbol.symbolHeight;
            this.symbolPercent = symbol.symbolPercent;
            this.symbolColor = symbol.symbolColor;

            this.billboard = symbol.billboard;
            this.rotate = symbol.rotate;
            this.inverse = symbol.inverse;
            this.mandatory = symbol.mandatory;

            this.repeat = symbol.repeat;
            this.repeatStart = symbol.repeatStart;
            this.repeatGap = symbol.repeatGap;
            this.mergeGap = symbol.mergeGap;
            this.mergeGroup = symbol.mergeGroup;
            this.mergeGroupGap = symbol.mergeGroupGap;
            this.textOverlap = symbol.textOverlap;
            this.zIndex = symbol.zIndex;

            return self();
        }

        public T bitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.hash = bitmap.hashCode();
            return self();
        }

        public T texture(TextureRegion texture) {
            this.texture = texture;
            this.hash = texture.hashCode();
            return self();
        }

        public T hash(int hash) {
            this.hash = hash;
            return self();
        }


        public T src(String src) {
            this.src = src;
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

        public T billboard(boolean billboard) {
            this.billboard = billboard;
            return self();
        }

        public T rotate(boolean rotate) {
            this.rotate = rotate;
            return self();
        }

        public T inverse(boolean inverse) {
            this.inverse = inverse;
            return self();
        }

        public T mandatory(boolean mandatory) {
            this.mandatory = mandatory;
            return self();
        }

        public T repeat(boolean repeat) {
            this.repeat = repeat;
            return self();
        }

        public T repeatStart(int repeatStart) {
            this.repeatStart = repeatStart;
            return self();
        }

        public T repeatGap(int repeatGap) {
            this.repeatGap = repeatGap;
            return self();
        }

        public T mergeGap(int mergeGap) {
            this.mergeGap = mergeGap;
            return self();
        }

        public T mergeGroup(String mergeGroup) {
            this.mergeGroup = mergeGroup;
            if (this.mergeGap == -1)
                this.mergeGap = 0;
            if (this.mergeGroupGap == -1)
                this.mergeGroupGap = 0;
            return self();
        }

        public T mergeGroupGap(int mergeGroupGap) {
            this.mergeGroupGap = mergeGroupGap;
            return self();
        }

        public T textOverlap(boolean textOverlap) {
            this.textOverlap = textOverlap;
            return self();
        }

        public T zIndex(int zIndex) {
            this.zIndex = zIndex;
            return self();
        }

        public T reset() {
            cat = null;

            bitmap = null;
            texture = null;
            hash = 0;
            src = null;

            symbolWidth = 0;
            symbolHeight = 0;
            symbolPercent = 100;
            symbolColor = 0;

            billboard = false;
            rotate = false;
            inverse = false;
            mandatory = false;

            repeat = false;
            repeatStart = 0;
            repeatGap = 0;
            mergeGap = -1;
            mergeGroup = null;
            mergeGroupGap = -1;
            textOverlap = true;

            return self();
        }

        public SymbolStyle build() {
            return new SymbolStyle(this);
        }
    }

    @SuppressWarnings("rawtypes")
    public static SymbolBuilder<?> builder() {
        return new SymbolBuilder();
    }
}
