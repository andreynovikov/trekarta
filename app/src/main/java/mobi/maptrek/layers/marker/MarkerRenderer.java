/*
 * Copyright 2013 Hannes Janetzek
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

package mobi.maptrek.layers.marker;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextBucket;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.TimSort;
import org.oscim.utils.geom.GeometryUtils;

import java.util.Comparator;
import java.util.HashMap;

import static org.oscim.backend.GLAdapter.gl;

import androidx.annotation.NonNull;

class MarkerRenderer extends BucketRenderer {
    private static final float FOCUS_CIRCLE_SIZE = 8;

    MarkerSymbol mDefaultMarker;

    private final SymbolBucket mSymbolBucket;
    private final TextBucket mTextBucket;
    private final float[] mBox = new float[8];
    private final MarkerLayer<MarkerItem> mMarkerLayer;
    private final Point mMapPoint = new Point();
    private final float mScale;
    private boolean mTitlesEnabled;

    private int mShaderProgram;
    private int hVertexPosition;
    private int hMatrixPosition;
    private int hScale;
    private int hColor;

    private final Point mIndicatorPosition = new Point();
    private boolean mInitialized;

    /**
     * increase view to show items that are partially visible
     */
    private static final int mExtents = 100;

    /**
     * flag to force update of markers
     */
    private boolean mUpdate;

    private InternalItem[] mItems;
    private int mOutlineColor;

    private static class InternalItem {
        MarkerItem item;
        boolean visible;
        boolean changes;
        float x, y;
        double px, py;
        float dy;

        @NonNull
        @Override
        public String toString() {
            return "\n" + x + ":" + y + " / " + dy + " " + visible;
        }
    }

    MarkerRenderer(MarkerLayer<MarkerItem> markerLayer, MarkerSymbol defaultSymbol, float scale, int outlineColor) {
        mSymbolBucket = new SymbolBucket();
        mTextBucket = new TextBucket();
        mSymbolBucket.next = mTextBucket;
        mMarkerLayer = markerLayer;
        mDefaultMarker = defaultSymbol;
        mScale = scale;
        mTitlesEnabled = true;
        mOutlineColor = outlineColor;
    }

    public void setTitlesEnabled(boolean titlesEnabled) {
        mTitlesEnabled = titlesEnabled;
    }

    public void setOutlineColor(int color) {
        mOutlineColor = color;
        update();
    }

    public void setDefaultMarker(MarkerSymbol symbol) {
        mDefaultMarker = symbol;
        update();
    }

    @Override
    public synchronized void update(GLViewport v) {
        if (!mInitialized) {
            mInitialized = init();
        }

        if (!v.changed() && !mUpdate)
            return;

        mUpdate = false;

        double mx = v.pos.x;
        double my = v.pos.y;
        double scale = Tile.SIZE * v.pos.scale;

        //int changesInvisible = 0;
        //int changedVisible = 0;
        int numVisible = 0;

        mMarkerLayer.map().viewport().getMapExtents(mBox, mExtents);

        long flip = (long) (Tile.SIZE * v.pos.scale) >> 1;

        if (mItems == null || !mMarkerLayer.isEnabled()) {
            if (buckets.get() != null) {
                buckets.clear();
                compile();
            }
            return;
        }

        double angle = Math.toRadians(v.pos.bearing);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        mIndicatorPosition.x = Double.MAX_VALUE;

		/* check visibility */
        for (InternalItem it : mItems) {
            it.changes = false;
            it.x = (float) ((it.px - mx) * scale);
            it.y = (float) ((it.py - my) * scale);

            if (it.x > flip)
                it.x -= (flip << 1);
            else if (it.x < -flip)
                it.x += (flip << 1);

            if (!GeometryUtils.pointInPoly(it.x, it.y, mBox, 8, 0)) {
                if (it.visible) {
                    it.changes = true;
                    //changesInvisible++;
                }
                continue;
            }

            it.dy = sin * it.x + cos * it.y;

            if (!it.visible) {
                it.visible = true;
                //changedVisible++;
            }
            numVisible++;

            if (it.item == mMarkerLayer.mFocusedItem) {
                mIndicatorPosition.x = it.px;
                mIndicatorPosition.y = it.py;
            }
        }

        //log.debug(numVisible + " " + changedVisible + " " + changesInvisible);

		/* only update when zoomlevel changed, new items are visible
         * or more than 10 of the current items became invisible */
        //if ((numVisible == 0) && (changedVisible == 0 && changesInvisible < 10))
        //	return;
        buckets.clear();

        if (numVisible == 0) {
            compile();
            return;
        }
        /* keep position for current state */
        mMapPosition.copy(v.pos);
        mMapPosition.bearing = -mMapPosition.bearing;

        sort(mItems, 0, mItems.length);
        HashMap<Long, TextStyle> textStyles = new HashMap<>();
        for (InternalItem it : mItems) {
            if (!it.visible)
                continue;

            if (it.changes) {
                it.visible = false;
                continue;
            }

            MarkerSymbol marker = it.item.getMarker();
            if (marker == null)
                marker = mDefaultMarker;

            Bitmap bitmap = marker.getBitmap();

            SymbolItem s = SymbolItem.pool.get();
            s.set(it.x, it.y, bitmap, true);
            s.offset = marker.getHotspot();
            s.billboard = marker.isBillboard();
            mSymbolBucket.pushSymbol(s);

            if (mTitlesEnabled && it.item.title != null) {
                float dy = -s.offset.y * bitmap.getHeight() - 10 * mScale * CanvasAdapter.textScale;
                long k = (((long) dy) << 32) | (it.item.color & 0xffffffffL);
                TextStyle textStyle = textStyles.get(k);
                if (textStyle == null) {
                    textStyle = TextStyle.builder()
                            .fontSize(20 * mScale * CanvasAdapter.textScale)
                            .color(it.item.color)
                            .outline(mOutlineColor, 3f * mScale * CanvasAdapter.textScale)
                            .isCaption(true)
                            .offsetY(dy)
                            .build();
                    textStyles.put(k, textStyle);
                }
                TextItem t = TextItem.pool.get();
                t.set(it.x, it.y, it.item.title, textStyle);
                mTextBucket.addText(t);
            }
        }

        buckets.set(mSymbolBucket);
        buckets.prepare();

        compile();
    }

    @Override
    public void render(GLViewport v) {
        if (mIndicatorPosition.x != Double.MAX_VALUE) {
            GLState.useProgram(mShaderProgram);
            GLState.blend(true);
            GLState.test(false, false);

            GLState.enableVertexArrays(hVertexPosition, GLState.DISABLED);
            MapRenderer.bindQuadVertexVBO(hVertexPosition);

            double x = mIndicatorPosition.x - v.pos.x;
            double y = mIndicatorPosition.y - v.pos.y;
            double tileScale = Tile.SIZE * v.pos.scale;

            v.mvp.setTransScale((float) (x * tileScale), (float) (y * tileScale), 1);
            v.mvp.multiplyMM(v.viewproj, v.mvp);
            v.mvp.setAsUniform(hMatrixPosition);

            gl.uniform1f(hScale, FOCUS_CIRCLE_SIZE * mScale);

            float r = ((mMarkerLayer.mFocusColor >> 16) & 0xFF) * 1f / 0xFF;
            float g = ((mMarkerLayer.mFocusColor >> 8) & 0xFF) * 1f / 0xFF;
            float b = (mMarkerLayer.mFocusColor & 0xFF) * 1f / 0xFF;
            gl.uniform4f(hColor, r, g, b, 1f);

            gl.drawArrays(GL.TRIANGLE_STRIP, 0, 4);
        }
        super.render(v);
    }

    void populate(int size) {

        InternalItem[] tmp = new InternalItem[size];

        for (int i = 0; i < size; i++) {
            InternalItem it = new InternalItem();
            tmp[i] = it;
            it.item = mMarkerLayer.createItem(i);

			/* pre-project polygonPoints */
            MercatorProjection.project(it.item.getPoint(), mMapPoint);
            it.px = mMapPoint.x;
            it.py = mMapPoint.y;
        }
        synchronized (this) {
            mUpdate = true;
            mItems = tmp;
        }
    }

    public void update() {
        mUpdate = true;
    }

    private static final TimSort<InternalItem> ZSORT = new TimSort<>();

    private static void sort(InternalItem[] a, int lo, int hi) {
        int nRemaining = hi - lo;
        if (nRemaining < 2) {
            return;
        }

        ZSORT.doSort(a, zComparator, lo, hi);
    }

    private static final Comparator<InternalItem> zComparator = new Comparator<InternalItem>() {
        @Override
        public int compare(InternalItem a, InternalItem b) {
            if (a.visible && b.visible) {
                if (a.dy > b.dy) {
                    return -1;
                }
                if (a.dy < b.dy) {
                    return 1;
                }
            } else if (a.visible) {
                return -1;
            } else if (b.visible) {
                return 1;
            }

            return 0;
        }
    };

    private boolean init() {
        int shader = GLShader.createProgram(vShaderStr, fShaderStr);
        if (shader == 0)
            return false;

        mShaderProgram = shader;
        hVertexPosition = gl.getAttribLocation(shader, "a_pos");
        hMatrixPosition = gl.getUniformLocation(shader, "u_mvp");
        hScale = gl.getUniformLocation(shader, "u_scale");
        hColor = gl.getUniformLocation(shader, "u_color");

        return true;
    }

    private static final String vShaderStr = ""
            + "precision mediump float;"
            + "uniform mat4 u_mvp;"
            + "uniform float u_scale;"
            + "attribute vec2 a_pos;"
            + "varying vec2 v_tex;"
            + "void main() {"
            + "  gl_Position = u_mvp * vec4(a_pos * u_scale, 0.0, 1.0);"
            + "  v_tex = a_pos;"
            + "}";

    private static final String fShaderStr = ""
            + "precision mediump float;"
            + "varying vec2 v_tex;"
            + "uniform float u_scale;"
            + "uniform vec4 u_color;"

            + "void main() {"
            + "  float len = 1.0 - length(v_tex);"
            + "  gl_FragColor = u_color * 0.5 * smoothstep(0.0, 1.0 / u_scale, len);"
            + "}";
}
