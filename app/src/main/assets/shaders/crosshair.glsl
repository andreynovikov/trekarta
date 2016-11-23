#ifdef GLES
precision mediump float;
#endif
uniform mat4 u_mvp;
uniform float u_scale;
attribute vec2 a_pos;
varying vec2 v_tex;
void main() {
  gl_Position = u_mvp * vec4(40.0 * u_scale * a_pos, 0.0, 1.0);
  v_tex = a_pos;
}

$$

#ifdef GLES
precision mediump float;
#endif
varying vec2 v_tex;
uniform float u_phase;

float soft_near(float x, float y1, float y2, float d) {
    return min(smoothstep(y1-d, y1, x), 1.0 - smoothstep(y2, y2+d, x));
}

float crosshair(vec2 texcoord) {
    return
        soft_near(texcoord.x, 0.0, 0.0, 0.02) *
        soft_near(texcoord.y, -1.0, -0.2, 0.02) +
        soft_near(texcoord.x, 0.0, 0.0, 0.02) *
        soft_near(texcoord.y, 0.2, 1.0, 0.02) +
        soft_near(texcoord.y, 0.0, 0.0, 0.02) *
        soft_near(texcoord.x, -1.0, -0.2, 0.02) +
        soft_near(texcoord.y, 0.0, 0.0, 0.02) *
        soft_near(texcoord.x, 0.2, 1.0, 0.02);
}

void main() {
	float circle = 0.5 * (1.0 - smoothstep(0.05, 0.07, abs(length(v_tex))));
	float crosshair = crosshair(v_tex);
    gl_FragColor = vec4(0.2, 0.2, 0.2, 1.0) * u_phase * (circle + crosshair);
}
