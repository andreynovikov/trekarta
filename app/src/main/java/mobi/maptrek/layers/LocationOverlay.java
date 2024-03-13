/*
 * Copyright 2013 Ahmad Saleem
 * Copyright 2013 Hannes Janetzek
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
 */

package mobi.maptrek.layers;

import android.os.SystemClock;

import org.oscim.backend.GL;
import org.oscim.backend.canvas.Color;
import org.oscim.core.Box;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.MapRenderer;
import org.oscim.utils.FastMath;
import org.oscim.utils.math.Interpolation;

import static org.oscim.backend.GLAdapter.gl;

public class LocationOverlay extends Layer {
    private final Point mLocation = new Point();
    private float mBearing;

    public LocationOverlay(Map map, int type, int size, int color, float scale) {
        super(map);
        mRenderer = new LocationIndicator(scale);
        setEnabled(false);
        setType(type);
        setSize(size);
        setColor(color);
    }

    public void setType(int type) {
        ((LocationIndicator) mRenderer).setType(type);
    }

    public void setSize(int size) {
        ((LocationIndicator) mRenderer).setSize(size);
    }

    public void setColor(int color) {
        ((LocationIndicator) mRenderer).setColor(color);
    }

    public void setPosition(double latitude, double longitude, float bearing) {
        mLocation.x = MercatorProjection.longitudeToX(longitude);
        mLocation.y = MercatorProjection.latitudeToY(latitude);
        mBearing = bearing;
        ((LocationIndicator) mRenderer).animate(true);
    }

    public Point getPosition() {
        return new Point(mLocation.x, mLocation.y);
    }

    public double getX() {
        return mLocation.x;
    }

    public double getY() {
        return mLocation.y;
    }

    public boolean isVisible() {
        return ((LocationIndicator) mRenderer).isVisible();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled == isEnabled())
            return;

        super.setEnabled(enabled);

        //if (!enabled)
        ((LocationIndicator) mRenderer).animate(enabled);
    }

    private class LocationIndicator extends LayerRenderer {
        private final float mScale;
        private int mShaderProgram;
        private int hVertexPosition;
        private int hMatrixPosition;
        private int hScale;
        private int hPhase;
        private int hDirection;
        private int hType;
        private int hColor;

        private static final int COLOR = 0xffff5722;
        private final static long ANIM_RATE = 50;
        private final static long INTERVAL = 8000;

        private int mType = 0;
        private int mSize = 20;
        private final float[] mColors = new float[4];
        private final Point mIndicatorPosition = new Point();

        private final Box mBBox = new Box();

        private boolean mInitialized;

        private boolean mLocationIsVisible;

        private boolean mRunAnim;
        private long mAnimStart;

        private boolean mReanimated = false;

        LocationIndicator(float scale) {
            super();
            mScale = scale;
            setColor(COLOR);
        }

        public void setType(int type) {
            mType = type;
        }

        public void setSize(int size) {
            mSize = size;
        }

        public void setColor(int color) {
            float a = Color.aToFloat(color);
            mColors[0] = a * Color.rToFloat(color);
            mColors[1] = a * Color.gToFloat(color);
            mColors[2] = a * Color.bToFloat(color);
            mColors[3] = a;
        }

        public boolean isVisible() {
            return mLocationIsVisible;
        }

        private void animate(boolean enable) {
            if (mRunAnim == enable)
                return;

            mReanimated = true;
            mRunAnim = enable;
            if (!enable)
                return;

            final Runnable action = new Runnable() {
                private long lastRun;

                @Override
                public void run() {
                    if (!mRunAnim)
                        return;

                    long diff = SystemClock.elapsedRealtime() - lastRun;
                    mMap.postDelayed(this, Math.min(ANIM_RATE, diff));
                    mMap.render();
                    lastRun = SystemClock.elapsedRealtime();
                }
            };

            mAnimStart = SystemClock.elapsedRealtime();
            mMap.postDelayed(action, ANIM_RATE);
        }

        private float animPhase() {
            return (float) ((MapRenderer.frametime - mAnimStart) % INTERVAL) / INTERVAL;
        }

        @Override
        public void update(GLViewport v) {
            if (!mInitialized) {
                init();
                mInitialized = true;
            }

            if (!isEnabled()) {
                setReady(false);
                return;
            }

            if (!v.changed() && !mReanimated && !isReady())
                return;

            setReady(true);

            // clamp location to a position that can be
            // safely translated to screen coordinates
            v.getBBox(mBBox, 0);

            mLocationIsVisible = mBBox.contains(mLocation);
            mIndicatorPosition.x = FastMath.clamp(mLocation.x, mBBox.xmin, mBBox.xmax);
            mIndicatorPosition.y = FastMath.clamp(mLocation.y, mBBox.ymin, mBBox.ymax);
        }

        @Override
        public void render(GLViewport v) {
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

            gl.uniform1f(hScale, mSize * mScale * (mType == 0 ? 1.5f : 1f));

            if (mLocationIsVisible) {
                animate(false);
                gl.uniform1f(hPhase, 1);
                float rotation = mBearing - 90;
                if (rotation > 180)
                    rotation -= 360;
                else if (rotation < -180)
                    rotation += 360;

                gl.uniform2f(hDirection,
                        (float) Math.cos(Math.toRadians(rotation)),
                        (float) Math.sin(Math.toRadians(rotation)));
            } else {
                animate(true);
                float phase = Math.abs(animPhase() - 0.5f) * 2;
                phase = Interpolation.swing.apply(phase);
                gl.uniform1f(hPhase, 0.8f + phase * 0.2f);
                gl.uniform2f(hDirection, 0, 0);
            }

            // Pointer type
            gl.uniform1f(hType, mType);

            // Pointer color
            GLUtils.glUniform4fv(hColor, 1, mColors);

            gl.drawArrays(GL.TRIANGLE_STRIP, 0, 4);
        }

        /** @noinspection UnusedReturnValue*/
        private boolean init() {
            int shader = GLShader.loadShader("location_pointer");
            if (shader == 0)
                return false;

            mShaderProgram = shader;
            hVertexPosition = gl.getAttribLocation(shader, "a_pos");
            hMatrixPosition = gl.getUniformLocation(shader, "u_mvp");
            hPhase = gl.getUniformLocation(shader, "u_phase");
            hScale = gl.getUniformLocation(shader, "u_scale");
            hDirection = gl.getUniformLocation(shader, "u_dir");
            hType = gl.getUniformLocation(shader, "u_type");
            hColor = gl.getUniformLocation(shader, "u_color");

            return true;
        }
    }
}
