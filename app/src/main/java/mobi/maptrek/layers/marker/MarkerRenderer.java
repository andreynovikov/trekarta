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

import org.oscim.backend.GL;
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
import org.oscim.utils.TimSort;
import org.oscim.utils.geom.GeometryUtils;

import java.util.Comparator;

import static org.oscim.backend.GLAdapter.gl;

public class MarkerRenderer extends BucketRenderer {
    private static final float FOCUS_CIRCLE_SIZE = 25;

    protected final MarkerSymbol mDefaultMarker;

    private final SymbolBucket mSymbolLayer;
    private final float[] mBox = new float[8];
    private final MarkerLayer<MarkerItem> mMarkerLayer;
    private final Point mMapPoint = new Point();

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
    protected int mExtents = 100;

    /**
     * flag to force update of markers
     */
    private boolean mUpdate;

    private InternalItem[] mItems;

    static class InternalItem {
        MarkerItem item;
        boolean visible;
        boolean changes;
        float x, y;
        double px, py;
        float dy;

        @Override
        public String toString() {
            return "\n" + x + ":" + y + " / " + dy + " " + visible;
        }
    }

    public MarkerRenderer(MarkerLayer<MarkerItem> markerLayer, MarkerSymbol defaultSymbol) {
        mSymbolLayer = new SymbolBucket();
        mMarkerLayer = markerLayer;
        mDefaultMarker = defaultSymbol;
    }

    @Override
    public synchronized void update(GLViewport v) {
        if (!mInitialized) {
            init();
            mInitialized = true;
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

        if (mItems == null) {
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
        //log.debug(Arrays.toString(mItems));
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

            SymbolItem s = SymbolItem.pool.get();
            s.set(it.x, it.y, marker.getBitmap(), true);
            s.offset = marker.getHotspot();
            s.billboard = marker.isBillboard();
            mSymbolLayer.pushSymbol(s);
        }

        buckets.set(mSymbolLayer);
        buckets.prepare();

        compile();
    }

    @Override
    public void render(GLViewport v) {
        if (mIndicatorPosition.x != Double.MAX_VALUE) {
            GLState.useProgram(mShaderProgram);
            GLState.blend(true);
            GLState.test(false, false);

            GLState.enableVertexArrays(hVertexPosition, -1);
            MapRenderer.bindQuadVertexVBO(hVertexPosition);

            double x = mIndicatorPosition.x - v.pos.x;
            double y = mIndicatorPosition.y - v.pos.y;
            double tileScale = Tile.SIZE * v.pos.scale;

            v.mvp.setTransScale((float) (x * tileScale), (float) (y * tileScale), 1);
            v.mvp.multiplyMM(v.viewproj, v.mvp);
            v.mvp.setAsUniform(hMatrixPosition);

            gl.uniform1f(hScale, FOCUS_CIRCLE_SIZE);

            float r = ((mMarkerLayer.mFocusColor >> 16) & 0xFF) * 1f / 0xFF;
            float g = ((mMarkerLayer.mFocusColor >> 8) & 0xFF) * 1f / 0xFF;
            float b = (mMarkerLayer.mFocusColor & 0xFF) * 1f / 0xFF;
            gl.uniform4f(hColor, r, g, b, 1f);

            gl.drawArrays(GL.TRIANGLE_STRIP, 0, 4);
        }
        super.render(v);
    }

    protected void populate(int size) {

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

    static TimSort<InternalItem> ZSORT = new TimSort<>();

    public static void sort(InternalItem[] a, int lo, int hi) {
        int nRemaining = hi - lo;
        if (nRemaining < 2) {
            return;
        }

        ZSORT.doSort(a, zComparator, lo, hi);
    }

    final static Comparator<InternalItem> zComparator = new Comparator<InternalItem>() {
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

    private final static String vShaderStr = ""
            + "precision mediump float;"
            + "uniform mat4 u_mvp;"
            + "uniform float u_scale;"
            + "attribute vec2 a_pos;"
            + "varying vec2 v_tex;"
            + "void main() {"
            + "  gl_Position = u_mvp * vec4(a_pos * u_scale, 0.0, 1.0);"
            + "  v_tex = a_pos;"
            + "}";

    private final static String fShaderStr = ""
            + "precision mediump float;"
            + "varying vec2 v_tex;"
            + "uniform float u_scale;"
            + "uniform vec4 u_color;"

            + "void main() {"
            + "  float len = 1.0 - length(v_tex);"
            + "  gl_FragColor = u_color * 0.5 * smoothstep(0.0, 1.0 / u_scale, len);"
            + "}";

    //	/**
    //	 * Returns the Item at the given index.
    //	 *
    //	 * @param position
    //	 *            the position of the item to return
    //	 * @return the Item of the given index.
    //	 */
    //	public final Item getItem(int position) {
    //
    //		synchronized (lock) {
    //			InternalItem item = mItems;
    //			for (int i = mSize - position - 1; i > 0 && item != null; i--)
    //				item = item.next;
    //
    //			if (item != null)
    //				return item.item;
    //
    //			return null;
    //		}
    //	}
}
