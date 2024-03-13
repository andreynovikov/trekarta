/*
 * Copyright 2024 Andrey Novikov
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
 *
 * Copyright 2012 osmdroid authors: Viesturs Zarins, Martin Pearman
 * Copyright 2012 Hannes Janetzek
 * Copyright 2016-2018 devemux86
 * Copyright 2016 Bezzu
 * Copyright 2016 Pedinel
 * Copyright 2017 Andrey Novikov
 * Copyright 2018 Gustl22
 *
 * Parts of this file taken from the OpenScienceMap project (http://www.opensciencemap.org).
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

package mobi.maptrek.layers;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.theme.styles.LineStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.async.SimpleWorker;
import org.oscim.utils.geom.GeometryUtils;
import org.oscim.utils.geom.LineClipper;

import java.util.ArrayList;
import java.util.Collection;

import mobi.maptrek.data.Route;
import mobi.maptrek.data.Track;
import mobi.maptrek.layers.marker.MarkerSymbol;

public class RouteLayer extends Layer implements Route.UpdateListener {
    private static final int STROKE_MIN_ZOOM = 12;
    protected int LINE_ALPHA = 0x80;

    private final Route mRoute;
    private Track mTrack;

    private final MarkerSymbol mPointSymbol;
    private final MarkerSymbol mStartSymbol;
    private final MarkerSymbol mEndSymbol;
    private boolean mEnableSymbols = true;

    protected final ArrayList<GeoPoint> mPoints;
    private boolean mUpdatePoints;

    /**
     * Line style
     */
    protected LineStyle mLineStyle;

    final Worker mWorker;

    public RouteLayer(Map map, Route route, MarkerSymbol symbol) {
        this(map, route, symbol, symbol, symbol);
    }

    public RouteLayer(Map map, Route route, MarkerSymbol pointSymbol, MarkerSymbol startSymbol, MarkerSymbol endSymbol) {
        this(map, route, route.style.color, route.style.width, pointSymbol, startSymbol, endSymbol);
    }

    public RouteLayer(Map mMap, Route route, Track track, MarkerSymbol symbol) {
        this(mMap, route, symbol);
        mTrack = track;
        setPoints(track.points);
    }

    public RouteLayer(Map map, Route route, int lineColor, float lineWidth, MarkerSymbol symbol) {
        this(map, route, lineColor, lineWidth, symbol, symbol, symbol);
    }

    public RouteLayer(Map map, Route route, int lineColor, float lineWidth, MarkerSymbol pointSymbol, MarkerSymbol startSymbol, MarkerSymbol endSymbol) {
        super(map);

        mLineStyle = new LineStyle(Color.setA(lineColor, LINE_ALPHA), lineWidth, Paint.Cap.ROUND);
        mPointSymbol = pointSymbol;
        mStartSymbol = startSymbol;
        mEndSymbol = endSymbol;

        mPoints = new ArrayList<>();
        mRenderer = new RouteRenderer();
        mWorker = new Worker(map);

        mRoute = route;
        mRoute.setUpdateListener(this);
        onRouteChanged();
    }

    public void setWidth(int width) {
        setLineStyle(mLineStyle.color, width);
    }

    public void setLineStyle(int lineColor, float lineWidth) {
        mLineStyle = new LineStyle(Color.setA(lineColor, LINE_ALPHA), lineWidth, mLineStyle.cap);
        update();
    }

    public void setPoints(Collection<? extends GeoPoint> pts) {
        synchronized (mPoints) {
            mPoints.clear();
            mPoints.addAll(pts);
        }
        updatePoints();
    }

    protected void updatePoints() {
        mWorker.submit(10);
        mUpdatePoints = true;
    }

    public void update() {
        mWorker.submit(10);
    }

    @Override
    public void onDetach() {
        mRoute.removeUpdateListener();
    }

    @Override
    public void onRouteChanged() {
        setPoints(mRoute.getCoordinates());
    }

    public Route getRoute() {
        return mRoute;
    }

    public Track getTrack() {
        return mTrack;
    }

    public void enableSymbols(boolean enable) {
        mEnableSymbols = enable;
    }

    /***
     * everything below runs on GL- and Worker-Thread
     ***/
    final class RouteRenderer extends BucketRenderer {
        private int mCurX = -1;
        private int mCurY = -1;
        private int mCurZ = -1;

        @Override
        public synchronized void update(GLViewport v) {
            int tz = 1 << v.pos.zoomLevel;
            int tx = (int) (v.pos.x * tz);
            int ty = (int) (v.pos.y * tz);

            /* update layers when map moved by at least one tile */
            if ((tx != mCurX || ty != mCurY || tz != mCurZ)) {
                mWorker.submit(100);
                mCurX = tx;
                mCurY = ty;
                mCurZ = tz;
            }

            Task t = mWorker.poll();
            if (t == null)
                return;

            /* keep position to render relative to current state */
            mMapPosition.copy(t.position);

            /* compile new layers */
            buckets.set(t.buckets.get());
            compile();
        }
    }

    static final class Task {
        final SymbolBucket symbolBucket = new SymbolBucket();
        final RenderBuckets buckets = new RenderBuckets();
        final MapPosition position = new MapPosition();
        final float[] box = new float[8];
    }

    final class Worker extends SimpleWorker<Task> {
        public Worker(Map map) {
            super(map, 0, new Task(), new Task());
            // limit coordinates to maximum resolution of GL.Short
            int MAX_CLIP = (int) (Short.MAX_VALUE / MapRenderer.COORD_SCALE);
            mClipper = new LineClipper(-MAX_CLIP, -MAX_CLIP, MAX_CLIP, MAX_CLIP);
            mPPoints = new float[0];
        }

        private static final int MIN_DIST = 3;

        /**
         * increase view to show items that are partially visible
         */
        private static final int mExtents = 100;

        // pre-projected points
        private double[] mPreprojected = new double[2];

        // projected points
        private float[] mPPoints;
        private final LineClipper mClipper;
        private int mNumPoints;

        @Override
        public boolean doWork(Task task) {
            mMap.viewport().getMapExtents(task.box, mExtents);
            int size = mNumPoints;

            if (mUpdatePoints) {
                synchronized (mPoints) {
                    mUpdatePoints = false;
                    mNumPoints = size = mPoints.size();

                    double[] points = mPreprojected;

                    if (size * 2 >= points.length) {
                        points = mPreprojected = new double[size * 2];
                        mPPoints = new float[size * 2];
                    }

                    for (int i = 0; i < size; i++)
                        MercatorProjection.project(mPoints.get(i), points, i);
                }
            }

            if (size == 0) {
                if (task.buckets.get() != null) {
                    task.buckets.clear();
                    mMap.render();
                }
                return true;
            }

            task.symbolBucket.setLevel(1);
            task.buckets.set(task.symbolBucket);
            LineBucket ll;

            if (mLineStyle.stipple == 0 && mLineStyle.texture == null)
                ll = task.buckets.getLineBucket(0);
            else
                ll = task.buckets.getLineTexBucket(0);

            ll.line = mLineStyle;

            if (!mLineStyle.fixed && mLineStyle.strokeIncrease > 1)
                ll.scale = (float) Math.pow(mLineStyle.strokeIncrease, Math.max(task.position.getZoom() - STROKE_MIN_ZOOM, 0));

            mMap.getMapPosition(task.position);

            int zoomlevel = task.position.zoomLevel;
            task.position.scale = 1 << zoomlevel;

            double mx = task.position.x;
            double my = task.position.y;
            double scale = Tile.SIZE * task.position.scale;

            // flip around dateline
            int flip = 0;
            int maxx = Tile.SIZE << (zoomlevel - 1);

            int x = (int) ((mPreprojected[0] - mx) * scale);
            int y = (int) ((mPreprojected[1] - my) * scale);

            if (x > maxx) {
                x -= (maxx * 2);
                flip = -1;
            } else if (x < -maxx) {
                x += (maxx * 2);
                flip = 1;
            }

            MarkerSymbol symbol = size == 1 ? mEndSymbol : mStartSymbol;
            if (mEnableSymbols && symbol != null && GeometryUtils.pointInPoly(x, y, task.box, 8, 0))
                addPointSymbol(task.symbolBucket, x, y, symbol);

            mClipper.clipStart(x, y);

            float[] projected = mPPoints;
            int i = addPoint(projected, 0, x, y);

            float prevX = x;
            float prevY = y;

            float[] segment = null;

            for (int j = 2; j < size * 2; j += 2) {
                x = (int) ((mPreprojected[j + 0] - mx) * scale);
                y = (int) ((mPreprojected[j + 1] - my) * scale);

                int flipDirection = 0;
                if (x > maxx) {
                    x -= maxx * 2;
                    flipDirection = -1;
                } else if (x < -maxx) {
                    x += maxx * 2;
                    flipDirection = 1;
                }

                symbol = j == size * 2 - 2 ? mEndSymbol : mPointSymbol;
                if (mEnableSymbols && symbol != null && GeometryUtils.pointInPoly(x, y, task.box, 8, 0))
                    addPointSymbol(task.symbolBucket, x, y, symbol);

                if (flip != flipDirection) {
                    flip = flipDirection;
                    if (i > 2)
                        ll.addLine(projected, i, false);

                    mClipper.clipStart(x, y);
                    i = addPoint(projected, 0, x, y);

                    continue;
                }

                int clip = mClipper.clipNext(x, y);
                if (clip != LineClipper.INSIDE) {
                    if (i > 2)
                        ll.addLine(projected, i, false);

                    if (clip == LineClipper.INTERSECTION) {
                        /* add line segment */
                        segment = mClipper.getLine(segment, 0);
                        ll.addLine(segment, 4, false);
                        // the prev point is the real point not the clipped point
                        //prevX = mClipper.outX2;
                        //prevY = mClipper.outY2;
                        prevX = x;
                        prevY = y;
                    }
                    i = 0;
                    // if the end point is inside, add it
                    if (mClipper.getPrevOutcode() == LineClipper.INSIDE) {
                        projected[i++] = prevX;
                        projected[i++] = prevY;
                    }
                    continue;
                }

                float dx = x - prevX;
                float dy = y - prevY;
                if ((i == 0) || FastMath.absMaxCmp(dx, dy, MIN_DIST)) {
                    projected[i++] = prevX = x;
                    projected[i++] = prevY = y;
                }
            }
            if (i > 2)
                ll.addLine(projected, i, false);

            // trigger redraw to let renderer fetch the result.
            mMap.render();

            return true;
        }

        @Override
        public void cleanup(Task task) {
            task.buckets.clear();
        }

        private int addPoint(float[] points, int i, int x, int y) {
            points[i++] = x;
            points[i++] = y;
            return i;
        }

        private void addPointSymbol(SymbolBucket symbolBucket, int x, int y, MarkerSymbol symbol) {
            Bitmap bitmap = symbol.getBitmap();
            SymbolItem s = SymbolItem.pool.get();
            s.set(x, y, bitmap, true);
            s.offset = symbol.getHotspot();
            s.billboard = symbol.isBillboard();
            symbolBucket.pushSymbol(s);
        }
    }
}
