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

    public final int repeatGap;
    public final int mergeGap;
    public final String mergeGroup;
    public final int mergeGroupGap;
    public final boolean textOverlap;

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

        this.repeatGap = 0;
        this.mergeGap = -1;
        this.mergeGroup = null;
        this.mergeGroupGap = -1;
        this.textOverlap = true;
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

        this.repeatGap = b.repeatGap;
        this.mergeGap = b.mergeGap;
        this.mergeGroup = b.mergeGroup;
        this.mergeGroupGap = b.mergeGroupGap;
        this.textOverlap = b.textOverlap;
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
        private String src; // used by custom symbol generators

        public int symbolWidth;
        public int symbolHeight;
        public int symbolPercent;

        public int repeatGap;
        public int mergeGap;
        public String mergeGroup;
        public int mergeGroupGap;
        public boolean textOverlap = true;

        public SymbolBuilder() {
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

            this.repeatGap = symbol.repeatGap;
            this.mergeGap = symbol.mergeGap;
            this.mergeGroup = symbol.mergeGroup;
            this.mergeGroupGap = symbol.mergeGroupGap;
            this.textOverlap = symbol.textOverlap;

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

        public T reset() {
            cat = null;

            bitmap = null;
            texture = null;
            hash = 0;
            src = null;

            symbolWidth = 0;
            symbolHeight = 0;
            symbolPercent = 100;

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
