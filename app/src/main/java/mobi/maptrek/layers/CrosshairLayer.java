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

import android.os.SystemClock;

import org.oscim.backend.GL;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.MapRenderer;

import static org.oscim.backend.GLAdapter.gl;

public class CrosshairLayer extends Layer implements Map.UpdateListener {
    private static final int DEFAULT_COLOR = 0xff333333;

    public CrosshairLayer(Map map, float scale) {
        super(map);
        mRenderer = new CrosshairRenderer(scale);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled == isEnabled())
            return;
        super.setEnabled(enabled);
        if (enabled)
            ((CrosshairRenderer) mRenderer).show();
    }

    @Override
    public void onMapEvent(Event event, MapPosition mapPosition) {
        if (isEnabled() && (event == Map.MOVE_EVENT || event == Map.POSITION_EVENT)) {
            ((CrosshairRenderer) mRenderer).show();
        }
    }

    public void lock(int color) {
        ((CrosshairRenderer) mRenderer).setColor(color);
        ((CrosshairRenderer) mRenderer).setFading(false);
        ((CrosshairRenderer) mRenderer).show();
    }

    public void unlock() {
        ((CrosshairRenderer) mRenderer).setColor(DEFAULT_COLOR);
        ((CrosshairRenderer) mRenderer).setFading(true);
    }

    private class CrosshairRenderer extends LayerRenderer {
        private float mScale;
        private int mColor;
        private float mAlpha;
        private boolean mFading;
        private int mShaderProgram;
        private int hVertexPosition;
        private int hMatrixPosition;
        private int hScale;
        private int hColor;

        private final static long ANIM_RATE = 50;
        private final static long FADE_DURATION = 1000;
        private final static long FADE_TIMEOUT = 3000;

        private boolean mInitialized;

        private boolean mRunAnim;
        private long mAnimStart;
        private long mLastShown;

        CrosshairRenderer(float scale) {
            super();
            mScale = scale;
            mColor = DEFAULT_COLOR;
            mAlpha = 1f;
            mFading = true;
        }

        void setColor(int color) {
            mColor = color;
            mMap.render();
        }

        void setFading(boolean fading) {
            mFading = fading;
            if (mFading)
                animate(true);
        }

        void show() {
            animate(false);
            mAlpha = 1f;
            mLastShown = SystemClock.elapsedRealtime();
            if (!mFading)
                return;
            mMap.postDelayed(() -> {
                if (mLastShown + FADE_TIMEOUT <= SystemClock.elapsedRealtime())
                    animate(true);
            }, FADE_TIMEOUT + 100);
        }

        private void animate(boolean enable) {
            if (mRunAnim == enable)
                return;

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
                    lastRun = System.currentTimeMillis();
                }
            };

            mAnimStart = SystemClock.elapsedRealtime();
            mMap.postDelayed(action, ANIM_RATE);
        }

        private float animPhase() {
            return (float) ((MapRenderer.frametime - mAnimStart) % FADE_DURATION) / FADE_DURATION;
        }

        @Override
        public void update(GLViewport v) {
            if (!mInitialized) {
                init();
                show();
                mInitialized = true;
            }
            setReady(isEnabled());
        }

        @Override
        public void render(GLViewport v) {
            GLState.useProgram(mShaderProgram);
            GLState.blend(true);
            GLState.test(false, false);

            GLState.enableVertexArrays(hVertexPosition, -1);
            MapRenderer.bindQuadVertexVBO(hVertexPosition);

            if (mRunAnim) {
                float alpha = 1f - animPhase();
                if (alpha > mAlpha || alpha < 0.01f) {
                    mAlpha = 0f;
                    animate(false);
                } else {
                    mAlpha = alpha;
                }
            }

            v.mvp.setTransScale(0f, 0f, 1);
            // use v.viewproj to honor rotation ant tilt, or v.proj otherwise
            v.mvp.multiplyMM(v.proj, v.mvp);
            v.mvp.setAsUniform(hMatrixPosition);

            gl.uniform1f(hScale, mScale);
            GLUtils.setColor(hColor, mColor, mAlpha);

            gl.drawArrays(GL.TRIANGLE_STRIP, 0, 4);
        }

        private boolean init() {
            int shader = GLShader.loadShader("crosshair");
            if (shader == 0)
                return false;

            mShaderProgram = shader;
            hVertexPosition = gl.getAttribLocation(shader, "a_pos");
            hMatrixPosition = gl.getUniformLocation(shader, "u_mvp");
            hScale = gl.getUniformLocation(shader, "u_scale");
            hColor = gl.getUniformLocation(shader, "u_color");

            return true;
        }
    }
}
