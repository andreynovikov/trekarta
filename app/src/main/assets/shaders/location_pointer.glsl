#ifdef GLES
precision mediump float;
#endif
uniform mat4 u_mvp;
uniform float u_phase;
uniform float u_scale;
attribute vec2 a_pos;
varying vec2 v_tex;
void main() {
  gl_Position = u_mvp * vec4(a_pos * u_scale * u_phase, 0.0, 1.0);
  v_tex = a_pos;
}

$$

#ifdef GLES
precision mediump float;
#endif
varying vec2 v_tex;
uniform float u_scale;
uniform float u_phase;
uniform float u_type;
uniform vec2 u_dir;

// Computes the signed distance from a line
float line_distance(vec2 p, vec2 p1, vec2 p2) {
    vec2 center = (p1 + p2) * 0.5;
    float len = length(p2 - p1);
    vec2 dir = (p2 - p1) / len;
    vec2 rel_p = p - center;
    return dot(rel_p, vec2(dir.y, -dir.x));
}

float arrow(vec2 texcoord)
{
    float width = 0.35;
    float size = 0.8;
    float d1 = line_distance(texcoord, -size*vec2(+1.0,-width), vec2(0.0, 0.0));
    float d2 = line_distance(texcoord, -size*vec2(+1.0,-width), -vec2(0.85*size,0.0));
    float d3 = line_distance(texcoord, -size*vec2(+1.0,+width), vec2(0.0, 0.0));
    float d4 = line_distance(texcoord, -size*vec2(+1.0,+width), -vec2(0.85*size,0.0));
    return max( max(-d1, d3), - max(-d2,d4));
}

float stroke(float distance, float linewidth, float antialias)
{
    float t = linewidth/2.0 - antialias;
    float signed_distance = distance;
    float border_distance = abs(signed_distance) - t;
    float alpha = border_distance/antialias;
    alpha = exp(-alpha*alpha);
    float r;

    if( border_distance > (linewidth/2.0 + antialias) )
        r = 0.0;
    else if( border_distance < 0.0 )
        r = 1.0;
    else
        r = alpha;

    return r;
}

void main() {
  float len = 1.0 - length(v_tex);
  if (u_dir.x == 0.0 && u_dir.y == 0.0){
    gl_FragColor = vec4(1.0, 0.34, 0.13, 1.0) * len;
  } else if (u_type == 1.0) {
    ///  outer ring
	float a = smoothstep(0.0, 2.0 / u_scale, len);
	///  inner ring
	float b = 0.5 * smoothstep(4.0 / u_scale, 5.0 / u_scale, len);
	///  center point
	float c = 0.5 * (1.0 - smoothstep(14.0 / u_scale, 16.0 / u_scale, 1.0 - len));
	vec2 dir = normalize(v_tex);
	float d = 1.0 - dot(dir, u_dir);
	///  0.5 width of viewshed
	d = clamp(step(0.5, d), 0.4, 0.7);
	///  - subtract inner from outer to create the outline
	///  - multiply by viewshed
	///  - add center point
	a = d * (a - (b + c)) + c;
    gl_FragColor = vec4(1.0, 0.34, 0.13, 1.0) * a;
  } else {
    vec2 coord = vec2(u_dir.x*v_tex.x + u_dir.y*v_tex.y,
                      u_dir.y*v_tex.x - u_dir.x*v_tex.y);

    float a = stroke(arrow(coord), 0.1, 0.02);

    gl_FragColor = vec4(1.0, 0.34, 0.13, 1.0) * a;
  }
}
