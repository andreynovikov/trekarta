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

package mobi.maptrek.layers;

import org.oscim.backend.canvas.Paint.Cap;
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
import org.oscim.theme.styles.LineStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.async.SimpleWorker;
import org.oscim.utils.geom.LineClipper;

import mobi.maptrek.data.Track;

/**
 * This class draws a path line in given color.
 */
public class TrackLayer extends Layer {

    /**
     * Stores points, converted to the map projection.
     */
    final Track mTrack;
    private boolean mUpdatePoints;

    /**
     * Line style
     */
    LineStyle mLineStyle;

    private final Worker mWorker;

    public TrackLayer(Map map, Track track) {
        super(map);
        mWorker = new Worker(map);
        mLineStyle = new LineStyle(track.style.color, track.style.width, Cap.BUTT);
        mRenderer = new PathRenderer();
        mTrack = track;
        updatePoints();
    }

    void updatePoints() {
        mWorker.submit(10);
        mUpdatePoints = true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mWorker.submit(10);
    }

    public Track getTrack() {
        return mTrack;
    }

    public void setColor(int color) {
        mLineStyle = new LineStyle(color, mLineStyle.width, mLineStyle.cap);
        mWorker.submit(10);
    }

    public void setWidth(int width) {
        mLineStyle = new LineStyle(mLineStyle.color, width, mLineStyle.cap);
        mWorker.submit(10);
    }

    /***
     * everything below runs on GL- and Worker-Thread
     ***/
    private final class PathRenderer extends BucketRenderer {

        PathRenderer() {
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
            mMapPosition.copy(t.position);

            // compile new layers
            buckets.set(t.buckets.get());
            compile();
        }
    }

    final static class Task {
        final RenderBuckets buckets = new RenderBuckets();
        final MapPosition position = new MapPosition();
    }

    private final class Worker extends SimpleWorker<Task> {
        private static final int GROW_INDICES = 32;
        // limit coords to maximum resolution of GL.Short
        private final int MAX_CLIP = (int) (Short.MAX_VALUE / MapRenderer.COORD_SCALE);

        Worker(Map map) {
            super(map, 0, new Task(), new Task());
            mClipper = new LineClipper(-MAX_CLIP, -MAX_CLIP, MAX_CLIP, MAX_CLIP);
            mPPoints = new float[0];
        }

        private static final int MIN_DIST = 3;

        // pre-projected points
        private double[] mPreprojected = new double[2];

        // projected points
        private float[] mPPoints;
        private final LineClipper mClipper;
        private int mNumPoints;

        // tear index
        private int[] index = new int[1];

        @Override
        public boolean doWork(Task task) {

            int size = mNumPoints;

            if (mUpdatePoints) {
                synchronized (mTrack) {
                    mUpdatePoints = false;
                    int indexPos = 0;
                    index[indexPos] = -1;
                    mNumPoints = size = mTrack.points.size();

                    double[] points = mPreprojected;

                    if (size * 2 >= points.length) {
                        points = mPreprojected = new double[size * 2];
                        mPPoints = new float[size * 2];
                    }

                    for (int i = 0; i < size; i++) {
                        Track.TrackPoint point = mTrack.points.get(i);
                        MercatorProjection.project(point, points, i);

                        if (!point.continuous && i > 0) {
                            if (indexPos + 1 >= index.length)
                                ensureIndexSize(indexPos + 1, true);
                            index[indexPos] = i;
                            indexPos++;
                            if (index.length > indexPos + 1)
                                index[indexPos] = -1;
                        }
                    }
                }
            }

            if (size == 0 || !isEnabled()) {
                if (task.buckets.get() != null) {
                    task.buckets.clear();
                    mMap.render();
                }
                return true;
            }

            RenderBuckets layers = task.buckets;

            LineBucket ll = layers.getLineBucket(0);
            ll.line = mLineStyle;
            ll.scale = ll.line.width;

            //if (!mLineStyle.fixed && mLineStyle.strokeIncrease > 1)
            //    ll.scale = (float) Math.pow(mLineStyle.strokeIncrease, Math.max(task.position.getZoom() - STROKE_MIN_ZOOM, 0));

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

            mClipper.clipStart(x, y);

            float[] projected = mPPoints;
            int i = addPoint(projected, 0, x, y);

            float prevX = x;
            float prevY = y;

            float[] segment = null;

            int indexPos = 0;

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

                if (index[indexPos] == (j >> 1)) {
                    if (i > 2)
                        ll.addLine(projected, i, false);

                    mClipper.clipStart(x, y);
                    i = addPoint(projected, 0, x, y);
                    indexPos++;
                    continue;
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

        /**
         * Ensure index size.
         *
         * @param size the size
         * @param copy the copy
         * @return the short[] array holding current index
         */
        int[] ensureIndexSize(int size, boolean copy) {
            if (size < index.length)
                return index;

            int[] newIndex = new int[size + GROW_INDICES];
            if (copy)
                System.arraycopy(index, 0, newIndex, 0, index.length);

            index = newIndex;

            return index;
        }
    }
}
