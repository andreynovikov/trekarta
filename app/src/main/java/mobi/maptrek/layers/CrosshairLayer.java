package mobi.maptrek.layers;

import android.os.SystemClock;

import org.oscim.backend.GL;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.MapRenderer;

import static org.oscim.backend.GLAdapter.gl;

public class CrosshairLayer extends Layer implements Map.UpdateListener {
    public CrosshairLayer(Map map) {
        super(map);
        mRenderer = new CrosshairRenderer();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled == isEnabled())
            return;
        super.setEnabled(enabled);
        if (enabled)
            ((CrosshairRenderer)mRenderer).fade();
    }

    @Override
    public void onMapEvent(Event event, MapPosition mapPosition) {
        if (event == Map.MOVE_EVENT && isEnabled()) {
            ((CrosshairRenderer)mRenderer).fade();
        }
    }

    private class CrosshairRenderer extends LayerRenderer {
        private float mAlpha;
        private int mShaderProgram;
        private int hVertexPosition;
        private int hMatrixPosition;
        private int hPhase;

        private final static long ANIM_RATE = 50;
        private final static long FADE_DURATION = 1000;
        private final static long FADE_TIMEOUT = 3000;

        private boolean mInitialized;

        private boolean mRunAnim;
        private long mAnimStart;

        private long mLastShown;

        CrosshairRenderer() {
            super();
            mAlpha = 1f;
        }

        void fade() {
            animate(false);
            mAlpha = 1f;
            mLastShown = SystemClock.elapsedRealtime();
            mMap.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mLastShown + FADE_TIMEOUT <= SystemClock.elapsedRealtime())
                        animate(true);
                }
            }, FADE_TIMEOUT);
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
                fade();
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
            v.mvp.multiplyMM(v.viewproj, v.mvp);
            v.mvp.setAsUniform(hMatrixPosition);

            gl.uniform1f(hPhase, mAlpha);

            gl.drawArrays(GL.TRIANGLE_STRIP, 0, 4);
        }

        private boolean init() {
            int shader = GLShader.loadShader("crosshair");
            if (shader == 0)
                return false;

            mShaderProgram = shader;
            hVertexPosition = gl.getAttribLocation(shader, "a_pos");
            hMatrixPosition = gl.getUniformLocation(shader, "u_mvp");
            hPhase = gl.getUniformLocation(shader, "u_phase");

            return true;
        }
    }
}
