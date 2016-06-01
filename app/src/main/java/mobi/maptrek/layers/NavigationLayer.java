/*
 * Copyright 2012 osmdroid authors: Viesturs Zarins, Martin Pearman
 * Copyright 2012 Hannes Janetzek
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

package mobi.maptrek.layers;

import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.theme.styles.LineStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.async.SimpleWorker;
import org.oscim.utils.geom.LineClipper;

import java.util.ArrayList;

/**
 * This class draws a great circle navigation line.
 */
//TODO Extract base class (PathLayer) and combine with TrackLayer
public class NavigationLayer extends Layer {
    private GeoPoint mDestination;

    /**
     * Line style
     */
    LineStyle mLineStyle;
    private double mLatitude;
    private double mLongitude;

    protected boolean mUpdatePoints;

    final Worker mWorker;

    public NavigationLayer(Map map, int lineColor, float lineWidth) {
        super(map);
        mLineStyle = new LineStyle(lineColor, lineWidth, Cap.BUTT);
        mWorker = new Worker(map);
        mRenderer = new RenderPath();
    }


    public void setDestination(GeoPoint destination) {
        synchronized (mWorker) {
            mDestination = destination;
        }
        update();
    }

    public GeoPoint getDestination() {
        return mDestination;
    }

    public void setPosition(double lat, double lon) {
        synchronized (mWorker) {
            mLatitude = lat;
            mLongitude = lon;
        }
        update();
    }

    protected void update() {
        mWorker.submit(10);
        mUpdatePoints = true;
    }

    /***
     * everything below runs on GL- and Worker-Thread
     ***/
    final class RenderPath extends BucketRenderer {

        public RenderPath() {

            buckets.addLineBucket(0, mLineStyle);
        }

        private int mCurX = -1;
        private int mCurY = -1;
        private int mCurZ = -1;

        @Override
        public synchronized void update(GLViewport v) {
            int tz = 1 << v.pos.zoomLevel;
            int tx = (int) (v.pos.x * tz);
            int ty = (int) (v.pos.y * tz);

            // update layers when map moved by at least one tile
            if ((tx != mCurX || ty != mCurY || tz != mCurZ)) {
                mWorker.submit(100);
                mCurX = tx;
                mCurY = ty;
                mCurZ = tz;
            }

            Task t = mWorker.poll();
            if (t == null)
                return;

            // keep position to render relative to current state
            mMapPosition.copy(t.pos);

            // compile new layers
            buckets.set(t.bucket.get());
            compile();
        }
    }

    final static class Task {
        RenderBuckets bucket = new RenderBuckets();
        MapPosition pos = new MapPosition();
    }


    final class Worker extends SimpleWorker<Task> {

        // limit coords
        private final int max = 2048;

        public Worker(Map map) {
            super(map, 0, new Task(), new Task());
            mClipper = new LineClipper(-max, -max, max, max);
            mPPoints = new float[0];
        }

        private static final int MIN_DIST = 3;

        // pre-projected polygonPoints
        private double[] mPreprojected = new double[2];

        // projected polygonPoints
        private float[] mPPoints;
        private final LineClipper mClipper;
        private int mNumPoints;

        @Override
        public boolean doWork(Task task) {

            int size = mNumPoints;

            if (mUpdatePoints) {
                synchronized (mWorker) {
                    mUpdatePoints = false;

                    double step = .2d;
                    double lat = mLatitude, lon = mLongitude;
                    double dstLat = mDestination.getLatitude(), dstLon = mDestination.getLongitude();

                    ArrayList<Point> coordinates = new ArrayList<>();

                    while (Math.abs(lat - dstLat) > step || Math.abs(lon - dstLon) > step) {
                        double bearing = GeoPoint.bearing(lat, lon, dstLat, dstLon);

                        lon += Math.sin(GeoPoint.DEG2RAD * bearing) * step / Math.cos(GeoPoint.DEG2RAD * lat);
                        lat += Math.cos(GeoPoint.DEG2RAD * bearing) * step;

                        if (lon < -180d)
                            lon += 360d;
                        if (lon > 180d)
                            lon -= 360d;

                        coordinates.add(new Point(lon, lat));
                    }

                    mNumPoints = size = coordinates.size() + 2;

                    double[] points = mPreprojected;

                    if (size * 2 >= points.length) {
                        points = mPreprojected = new double[size * 2];
                        mPPoints = new float[size * 2];
                    }

                    MercatorProjection.project(mLatitude, mLongitude, points, 0);

                    for (int i = 0; i < size - 2; i++) {
                        Point point = coordinates.get(i);
                        MercatorProjection.project(point.y, point.x, points, i + 1);
                    }

                    MercatorProjection.project(mDestination.getLatitude(), mDestination.getLongitude(), points, size - 1);
                }
            }
            if (size == 0) {
                if (task.bucket.get() != null) {
                    task.bucket.clear();
                    mMap.render();
                }
                return true;
            }

            RenderBuckets layers = task.bucket;

            LineBucket ll = layers.getLineBucket(0);
            ll.line = mLineStyle;
            ll.scale = ll.line.width;

            mMap.getMapPosition(task.pos);

            int zoomlevel = task.pos.zoomLevel;
            task.pos.scale = 1 << zoomlevel;

            double mx = task.pos.x;
            double my = task.pos.y;
            double scale = Tile.SIZE * task.pos.scale;

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

            mClipper.clipStart(x, y);

            float[] projected = mPPoints;
            int i = addPoint(projected, 0, x, y);

            float prevX = x;
            float prevY = y;

            float[] segment = null;

            for (int j = 2; j < size * 2; j += 2) {
                //noinspection PointlessArithmeticExpression
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

                if (flip != flipDirection) {
                    flip = flipDirection;
                    if (i > 2)
                        ll.addLine(projected, i, false);

                    mClipper.clipStart(x, y);
                    i = addPoint(projected, 0, x, y);
                    continue;
                }

                int clip = mClipper.clipNext(x, y);
                if (clip < 1) {
                    if (i > 2)
                        ll.addLine(projected, i, false);

                    if (clip < 0) {
                        /* add line segment */
                        segment = mClipper.getLine(segment, 0);
                        ll.addLine(segment, 4, false);
                        prevX = mClipper.outX2;
                        prevY = mClipper.outY2;
                    }
                    i = 0;
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
            task.bucket.clear();
        }

        private int addPoint(float[] points, int i, int x, int y) {
            points[i++] = x;
            points[i++] = y;
            return i;
        }
    }

}
