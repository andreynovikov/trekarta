/*
 * Copyright 2016 devemux86
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
package org.oscim.layers.tile.vector.labeling;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.layers.tile.TileSet;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.geom.OBB2D;
import org.oscim.utils.pool.Inlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.oscim.layers.tile.MapTile.State.NEW_DATA;
import static org.oscim.layers.tile.MapTile.State.READY;

public class LabelPlacement {
    static final boolean dbg = false;
    static final Logger log = LoggerFactory.getLogger(LabelPlacement.class);

    public final static LabelTileData getLabels(MapTile tile) {
        return (LabelTileData) tile.getData(LabelLayer.LABEL_DATA);
    }

    private final static float MIN_CAPTION_DIST = 5;
    private final static float MIN_WAY_DIST = 3;

    /**
     * thread local pool of for unused label items
     */
    private final LabelPool mLabelPool = new LabelPool();
    /**
     * thread local pool of for unused symbol items
     */
    private final SymbolPool mSymbolPool = new SymbolPool();

    private final TileSet mTileSet = new TileSet();
    private final TileRenderer mTileRenderer;
    private final Map mMap;

    /**
     * list of current labels
     */
    private Label mLabels;

    /**
     * list of current symbols
     */
    private Symbol mSymbols;

    private float mSquareRadius;

    /**
     * incremented each update, to prioritize labels
     * that became visible earlier.
     */
    private int mRelabelCnt;

    public LabelPlacement(Map map, TileRenderer tileRenderer) {
        mMap = map;
        mTileRenderer = tileRenderer;
    }

    /**
     * remove Label l from mLabels and return l.next
     */
    private Label removeLabel(Label l) {
        Label ret = (Label) l.next;
        mLabels = (Label) mLabelPool.release(mLabels, l);
        return ret;
    }

    public void addLabel(Label l) {
        l.next = mLabels;
        mLabels = l;
    }

    public void addSymbol(Symbol s) {
        s.next = mSymbols;
        mSymbols = s;
    }

    private byte checkOverlap(Label l) {

        for (Label o = mLabels; o != null; ) {
            //check bounding box
            if (!Label.bboxOverlaps(l, o, 100)) {
                o = (Label) o.next;
                continue;
            }

            if (Label.shareText(l, o)) {
                // keep the label that was active earlier
                if (o.active <= l.active)
                    return 1;

                // keep the label with longer segment
                if (o.length < l.length) {
                    o = removeLabel(o);
                    continue;
                }
                // keep other
                return 2;
            }
            if (l.bbox.overlaps(o.bbox)) {
                if (o.active <= l.active)
                    return 1;

                if (!o.text.caption
                        && (o.text.priority > l.text.priority
                        || o.length < l.length)) {

                    o = removeLabel(o);
                    continue;
                }
                // keep other
                return 1;
            }
            o = (Label) o.next;
        }
        return 0;
    }

    private byte checkOverlap(Symbol s) {
        // if symbol is marked to not overlap text check overlaps with text labels
        if (!s.textOverlap) {
            for (Label o = mLabels; o != null; ) {
                if (s.bbox.overlaps(o.bbox)) {
                    // drop symbol as it overlaps text
                    return 1;
                }
                o = (Label) o.next;
            }
        }
        // if symbol is marked for merge check overlaps with other symbols
        if (s.mergeGap >= 0) {
            for (Symbol o = mSymbols; o != null; ) {
                int gap = s.mergeGap;
                if (s.mergeGroup == null) {
                    // if bitmaps differ skip merging
                    if (s.bitmap != null && s.bitmap != o.bitmap) {
                        o = (Symbol) o.next;
                        continue;
                    }
                    // if texture regions differ skip merging
                    if (s.texRegion != null && o.texRegion != null
                            && s.texRegion.texture.id != o.texRegion.texture.id) {
                        o = (Symbol) o.next;
                        continue;
                    }
                } else if (!s.mergeGroup.equals(o.mergeGroup)) {
                    // if merge groups differ skip merging
                    o = (Symbol) o.next;
                    continue;
                } else {
                    // select group gap if bitmaps are not the same
                    if ((s.bitmap != null && s.bitmap != o.bitmap) ||
                            (s.texRegion != null && o.texRegion != null
                            && s.texRegion.texture.id != o.texRegion.texture.id)) {
                        gap = s.mergeGroupGap;
                    }
                }

                // check distance
                if (gap > 0) {
                    // calculate Euclidian distance
                    float vx = s.x - o.x;
                    float vy = s.y - o.y;
                    float a = (float) Math.sqrt(vx * vx + vy * vy);
                    if (a < gap)
                        return 1;
                }

                // check bounding box
                if (!s.bbox.overlaps(o.bbox)) {
                    o = (Symbol) o.next;
                    continue;
                }

                if (o.active <= s.active)
                    return 1;

                // TODO Add priorities?
                /*
                if (l.text.priority < o.text.priority) {
                    o = removeLabel(o);
                    continue;
                }
                */
                /*
                if (!o.text.caption && (o.text.priority > l.text.priority || o.length < l.length)) {
                    o = removeLabel(o);
                    continue;
                }
                */
                // keep other
                return 2;
            }
        }
        return 0;
    }

    private boolean isVisible(float x, float y) {
        // rough filter
        float dist = x * x + y * y;
        if (dist > mSquareRadius)
            return false;

        return true;
    }

    private boolean wayIsVisible(Label ti) {
        // rough filter
        float dist = ti.x * ti.x + ti.y * ti.y;
        if (dist < mSquareRadius)
            return true;

        dist = ti.x1 * ti.x1 + ti.y1 * ti.y1;
        if (dist < mSquareRadius)
            return true;

        dist = ti.x2 * ti.x2 + ti.y2 * ti.y2;
        if (dist < mSquareRadius)
            return true;

        return false;
    }

    private Label getLabel() {
        Label l = (Label) mLabelPool.get();
        l.active = Integer.MAX_VALUE;

        return l;
    }

    private Symbol getSymbol() {
        Symbol s = (Symbol) mSymbolPool.get();
        s.active = Integer.MAX_VALUE;

        return s;
    }

    private static float flipLongitude(float dx, int max) {
        // flip around date-line
        if (dx > max)
            dx = dx - max * 2;
        else if (dx < -max)
            dx = dx + max * 2;

        return dx;
    }

    private void placeLabelFrom(Label l, TextItem ti) {
        // set line endpoints relative to view to be able to
        // check intersections with label from other tiles
        float w = (ti.x2 - ti.x1) / 2f;
        float h = (ti.y2 - ti.y1) / 2f;

        l.x1 = l.x - w;
        l.y1 = l.y - h;
        l.x2 = l.x + w;
        l.y2 = l.y + h;
    }

    private Label addWayLabels(MapTile t, Label l, float dx, float dy,
                               double scale) {

        LabelTileData ld = getLabels(t);
        if (ld == null)
            return l;

        for (TextItem ti : ld.labels) {
            if (ti.text.caption)
                continue;

            /* acquire a TextItem to add to TextLayer */
            if (l == null)
                l = getLabel();

            /* check if path at current scale is long enough */
            if (!dbg && ti.width > ti.length * scale)
                continue;

            l.clone(ti);
            l.x = (float) ((dx + ti.x) * scale);
            l.y = (float) ((dy + ti.y) * scale);
            placeLabelFrom(l, ti);

            if (!wayIsVisible(l))
                continue;

            byte overlaps = -1;

            if (l.bbox == null)
                l.bbox = new OBB2D(l.x, l.y, l.x1, l.y1,
                        l.width + MIN_WAY_DIST,
                        l.text.fontHeight + MIN_WAY_DIST);
            else
                l.bbox.set(l.x, l.y, l.x1, l.y1,
                        l.width + MIN_WAY_DIST,
                        l.text.fontHeight + MIN_WAY_DIST);

            if (dbg || ti.width < ti.length * scale)
                overlaps = checkOverlap(l);

            if (dbg)
                Debug.addDebugBox(l, ti, overlaps, false, (float) scale);

            if (overlaps == 0) {
                addLabel(l);
                l.item = TextItem.copy(ti);
                l.tileX = t.tileX;
                l.tileY = t.tileY;
                l.tileZ = t.zoomLevel;
                l.active = mRelabelCnt;
                l = null;
            }
        }
        return l;
    }

    private Label addNodeLabels(MapTile t, Label l, float dx, float dy,
                                double scale, float cos, float sin) {

        LabelTileData ld = getLabels(t);
        if (ld == null)
            return l;

        O:
        for (TextItem ti : ld.labels) {
            if (!ti.text.caption)
                continue;

            // acquire a TextItem to add to TextLayer
            if (l == null)
                l = getLabel();

            l.clone(ti);
            l.x = (float) ((dx + ti.x) * scale);
            l.y = (float) ((dy + ti.y) * scale);
            if (!isVisible(l.x, l.y))
                continue;

            if (l.bbox == null)
                l.bbox = new OBB2D();

            l.bbox.setNormalized(l.x, l.y, cos, -sin,
                    l.width + MIN_CAPTION_DIST,
                    l.text.fontHeight + MIN_CAPTION_DIST,
                    l.text.dy);

            for (Label o = mLabels; o != null; ) {
                if (l.bbox.overlaps(o.bbox)) {
                    if (l.text.priority < o.text.priority) {
                        o = removeLabel(o);
                        continue;
                    }
                    continue O;
                }
                o = (Label) o.next;
            }

            addLabel(l);
            l.item = TextItem.copy(ti);
            l.tileX = t.tileX;
            l.tileY = t.tileY;
            l.tileZ = t.zoomLevel;
            l.active = mRelabelCnt;
            l = null;
        }
        return l;
    }

    boolean updateLabels(LabelTask work) {

        /* get current tiles */
        boolean changedTiles = mTileRenderer.getVisibleTiles(mTileSet);

        if (mTileSet.cnt == 0) {
            return false;
        }

        MapPosition pos = work.pos;
        boolean changedPos = mMap.viewport().getMapPosition(pos);

        /* do not loop! */
        if (!changedTiles && !changedPos)
            return false;

        mRelabelCnt++;

        MapTile[] tiles = mTileSet.tiles;
        int zoom = tiles[0].zoomLevel;

        /* estimation for visible area to be labeled */
        int mw = (mMap.getWidth() + Tile.SIZE) / 2;
        int mh = (mMap.getHeight() + Tile.SIZE) / 2;
        mSquareRadius = mw * mw + mh * mh;

        /* scale of tiles zoom-level relative to current position */
        double scale = pos.scale / (1 << zoom);

        double angle = Math.toRadians(pos.bearing);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        int maxx = Tile.SIZE << (zoom - 1);

        // FIXME ???
        SymbolBucket sl = work.symbolLayer;
        sl.clearItems();

        double tileX = (pos.x * (Tile.SIZE << zoom));
        double tileY = (pos.y * (Tile.SIZE << zoom));

        /* put current label to previous label */
        Label prevLabels = mLabels;

        /* new labels */
        mLabels = null;
        Label l = null;

        /* add currently active labels first */
        for (l = prevLabels; l != null; ) {

            if (l.text.caption) {
                // TODO!!!
                l = mLabelPool.releaseAndGetNext(l);
                continue;
            }

            int diff = l.tileZ - zoom;
            if (diff > 1 || diff < -1) {
                l = mLabelPool.releaseAndGetNext(l);
                continue;
            }

            float div = FastMath.pow(diff);
            float sscale = (float) (pos.scale / (1 << l.tileZ));

            // plus 10 to rather keep label and avoid flickering
            if (l.width > (l.length + 10) * sscale) {
                l = mLabelPool.releaseAndGetNext(l);
                continue;
            }

            float dx = (float) (l.tileX * Tile.SIZE - tileX * div);
            float dy = (float) (l.tileY * Tile.SIZE - tileY * div);

            dx = flipLongitude(dx, maxx);
            l.x = (float) ((dx + l.item.x) * sscale);
            l.y = (float) ((dy + l.item.y) * sscale);
            placeLabelFrom(l, l.item);

            if (!wayIsVisible(l)) {
                l = mLabelPool.releaseAndGetNext(l);
                continue;
            }

            l.bbox.set(l.x, l.y, l.x1, l.y1,
                    l.width + MIN_WAY_DIST,
                    l.text.fontHeight + MIN_WAY_DIST);

            byte overlaps = checkOverlap(l);

            if (dbg)
                Debug.addDebugBox(l, l.item, overlaps, true, sscale);

            if (overlaps == 0) {
                Label ll = l;
                l = (Label) l.next;

                ll.next = null;
                addLabel(ll);
                continue;
            }
            l = mLabelPool.releaseAndGetNext(l);
        }

        /* add way labels */
        for (int i = 0, n = mTileSet.cnt; i < n; i++) {
            MapTile t = tiles[i];
            if (!t.state(READY | NEW_DATA))
                continue;

            float dx = (float) (t.tileX * Tile.SIZE - tileX);
            float dy = (float) (t.tileY * Tile.SIZE - tileY);
            dx = flipLongitude(dx, maxx);

            l = addWayLabels(t, l, dx, dy, scale);
        }

        /* add caption */
        for (int i = 0, n = mTileSet.cnt; i < n; i++) {
            MapTile t = tiles[i];
            if (!t.state(READY | NEW_DATA))
                continue;

            float dx = (float) (t.tileX * Tile.SIZE - tileX);
            float dy = (float) (t.tileY * Tile.SIZE - tileY);
            dx = flipLongitude(dx, maxx);

            l = addNodeLabels(t, l, dx, dy, scale, cos, sin);
        }

        for (Label ti = mLabels; ti != null; ti = (Label) ti.next) {
            /* add caption symbols */
            if (ti.text.caption) {
                if (ti.text.bitmap != null || ti.text.texture != null) {
                    SymbolItem s = SymbolItem.pool.get();
                    if (ti.text.bitmap != null)
                        s.bitmap = ti.text.bitmap;
                    else
                        s.texRegion = ti.text.texture;
                    s.x = ti.x;
                    s.y = ti.y;
                    s.billboard = true;
                    // TODO Discover what these symbols are
                    sl.addSymbol(s);
                }
                continue;
            }

            /* flip way label orientation */
            if (cos * (ti.x2 - ti.x1) - sin * (ti.y2 - ti.y1) < 0) {
                float tmp = ti.x1;
                ti.x1 = ti.x2;
                ti.x2 = tmp;

                tmp = ti.y1;
                ti.y1 = ti.y2;
                ti.y2 = tmp;
            }
        }

        Symbol prevSymbols = mSymbols;
        mSymbols = null;
        Symbol s = null;

        for (s = prevSymbols; s != null; ) {
            if (s.tileZ != zoom) {
                s = mSymbolPool.releaseAndGetNext(s);
                continue;
            }

            float sscale = (float) (pos.scale / (1 << s.tileZ));

            float dx = (float) (s.tileX * Tile.SIZE - tileX);
            float dy = (float) (s.tileY * Tile.SIZE - tileY);

            dx = flipLongitude(dx, maxx);
            s.x = (dx + s.item.x) * sscale;
            s.y = (dy + s.item.y) * sscale;

            if (!isVisible(s.x, s.y)) {
                s = mSymbolPool.releaseAndGetNext(s);
                continue;
            }

            s.bbox.set(s.x, s.y, s.x - s.w / 2, s.y - s.h / 2, s.w * 1.2f, s.h * 1.2f);

            if (checkOverlap(s) == 0) {
                Symbol ss = s;
                ss.item = SymbolItem.copy(s.item);
                s = (Symbol) s.next;

                ss.next = null;
                addSymbol(ss);
                continue;
            }

            s = mSymbolPool.releaseAndGetNext(s);
        }

        /* add symbol items */
        for (int i = 0, n = mTileSet.cnt; i < n; i++) {
            MapTile t = tiles[i];
            if (!t.state(READY | NEW_DATA))
                continue;

            float dx = (float) (t.tileX * Tile.SIZE - tileX);
            float dy = (float) (t.tileY * Tile.SIZE - tileY);
            dx = flipLongitude(dx, maxx);

            LabelTileData ld = getLabels(t);
            if (ld == null)
                continue;

            O:
            for (SymbolItem si : ld.symbols) {
                if (si.bitmap == null && si.texRegion == null)
                    continue;

                float x = (dx + si.x) * (float) scale;
                float y = (dy + si.y) * (float) scale;

                if (!isVisible(x, y))
                    continue;

                for (Symbol o = mSymbols; o != null; ) {
                    if (t.tileX == o.tileX && t.tileY == o.tileY && t.zoomLevel == o.tileZ
                            && si.x == o.item.x && si.y == o.item.y) {
                        if (si.bitmap != null && si.bitmap == o.bitmap) {
                            continue O;
                        }
                        if (si.texRegion != null && o.texRegion != null
                                && si.texRegion.texture.id == o.texRegion.texture.id) {
                            continue O;
                        }
                    }
                    o = (Symbol) o.next;
                }

                // acquire a SymbolItem to add to SymbolLayer
                if (s == null)
                    s = getSymbol();
                s.clone(si);
                s.x = x;
                s.y = y;
                s.w = si.bitmap != null ? si.bitmap.getWidth() : si.texRegion.rect.w;
                s.h = si.bitmap != null ? si.bitmap.getHeight() : si.texRegion.rect.h;
                if (s.bbox == null)
                    s.bbox = new OBB2D(s.x, s.y, s.x - s.w / 2, s.y - s.h / 2, s.w * 1.2f, s.h * 1.2f);
                else
                    s.bbox.set(s.x, s.y, s.x - s.w / 2, s.y - s.h / 2, s.w * 1.2f, s.h * 1.2f);

                if (checkOverlap(s) != 0) {
                    continue;
                }

                s.item = SymbolItem.copy(si);
                s.tileX = t.tileX;
                s.tileY = t.tileY;
                s.tileZ = t.zoomLevel;
                s.active = mRelabelCnt;
                addSymbol(s);
                s = null;
            }
        }

        // reverse list to keep order constant on each update
        mSymbols = Inlist.reverse(mSymbols);

        for (s = mSymbols; s != null; s = (Symbol) s.next) {
            SymbolItem item = SymbolItem.copy(s.item);
            item.x = s.x;
            item.y = s.y;
            sl.addSymbol(item);
        }

        /* temporary used Label */
        l = (Label) mLabelPool.release(l);

        /* draw text to bitmaps and create vertices */
        work.textLayer.labels = groupLabels(mLabels);
        work.textLayer.prepare();
        work.textLayer.labels = null;

        /* remove tile locks */
        mTileRenderer.releaseTiles(mTileSet);

        return true;
    }

    public void cleanup() {
        mLabels = (Label) mLabelPool.releaseAll(mLabels);
        mSymbols = (Symbol) mSymbolPool.releaseAll(mSymbols);
        mTileSet.releaseTiles();
    }

    /**
     * group labels by string and type
     */
    protected Label groupLabels(Label labels) {
        for (Label cur = labels; cur != null; cur = (Label) cur.next) {
            /* keep pointer to previous for removal */
            Label p = cur;
            TextStyle t = cur.text;
            float w = cur.width;

            /* iterate through following */
            for (Label l = (Label) cur.next; l != null; l = (Label) l.next) {

                if (w != l.width || t != l.text || !cur.label.equals(l.label)) {
                    p = l;
                    continue;
                } else if (cur.next == l) {
                    l.label = cur.label;
                    p = l;
                    continue;
                }
                l.label = cur.label;

                /* insert l after cur */
                Label tmp = (Label) cur.next;
                cur.next = l;

                /* continue outer loop at l */
                cur = l;

                /* remove l from previous place */
                p.next = l.next;
                l.next = tmp;

                /* continue from previous */
                l = p;
            }
        }
        return labels;
    }
}
