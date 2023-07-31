//fork of https://www.shadertoy.com/view/3sXSD2

const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;

float shape(in vec2 st, in int sides) {
    // Number of sides of your shape
    int N = sides;
    // // Remap the space to -1. to 1.
    //
    // // st = st *2.-1.;

    // Angle and radius from the current pixel
    float a = atan(st.x,st.y)+PI;
    float r = TWO_PI/float(N);

    // Shaping function that modulate the distance
    float d = cos(floor(.5+a/r)*r-a)*length(st);

    return d;
}

float box(in vec2 _st, in vec2 _size){
    _size = vec2(0.5) - _size*0.5;
    vec2 uv = smoothstep(_size,
                        _size+vec2(0.001),
                        _st);
    uv *= smoothstep(_size,
                    _size+vec2(0.001),
                    vec2(1.0)-_st);
    return uv.x*uv.y;
}

float xcross(in vec2 st, float xsize, float ysize, float outer, float inner){
    float pct = 0.;
    pct += box(st, vec2(outer*xsize, outer*xsize/4.));
    pct += box(st, vec2(outer*ysize/4., outer*ysize));

    pct -= box(st, vec2(inner*xsize, inner*xsize/4.));
    pct -= box(st, vec2(inner*ysize/4., inner*ysize));

    pct += box(st, vec2(ysize/4., ysize));
    pct += box(st, vec2(xsize, xsize/4.));

    return pct;
}

float nest_xcross(float prev_pct, in vec2 st, float xsize, float ysize, float outer, float inner, float alpha, float base, float inc){
    float f1 = base + inc;
    float f2 = base + 2. * inc;
    float f3 = base + 3. * inc;
    float f4 = base + 4. * inc;

    float pct = prev_pct;
    pct = alpha*xcross(st, xsize*base, ysize*base, outer, inner) - pct;
    pct = alpha*xcross(st, xsize*f1, ysize*f1, outer, inner) - pct;
    pct = alpha*xcross(st, xsize*f2, ysize*f2, outer, inner) - pct;
    pct = alpha*xcross(st, xsize*f3, ysize*f3, outer, inner) - pct;
    pct = alpha*xcross(st, xsize*f3, ysize*f3, outer, inner) - pct;
    return pct;
}

float noise(in float x, in float ts) {
  float amplitude = 0.2 * pow(x, 3.);
  float frequency = 2.;
  float y = sin(x * frequency);
  // float t = 0.01*(-iTime*130.0);
  float t = 0.01*(-ts*130.0);
  y += sin(x*frequency*2.1 + t)*4.5;
  y += sin(x*frequency*1.72 + t*1.121)*4.0;
  y += sin(x*frequency*2.221 + t*0.437)*5.0;
  y += sin(x*frequency*3.1122+ t*4.269)*2.5;
  y *= amplitude*0.06;
  return y;
}

vec2 random2(vec2 st){
    st = vec2( dot(st,vec2(127.1,311.7)),
              dot(st,vec2(269.5,183.3)) );
    return -1.0 + 2.0*fract(sin(st)*43758.5453123);
}

// Gradient Noise by Inigo Quilez - iq/2013
// https://www.shadertoy.com/view/XdXGW8
float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    vec2 u = f*f*(3.0-2.0*f);

    return mix( mix( dot( random2(i + vec2(0.0,0.0) ), f - vec2(0.0,0.0) ),
                     dot( random2(i + vec2(1.0,0.0) ), f - vec2(1.0,0.0) ), u.x),
                mix( dot( random2(i + vec2(0.0,1.0) ), f - vec2(0.0,1.0) ),
                     dot( random2(i + vec2(1.0,1.0) ), f - vec2(1.0,1.0) ), u.x), u.y);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord){
    vec2 st = fragCoord.xy/iResolution.xy;
    vec3 color = vec3(0.0);

    st.x *= iResolution.x/iResolution.y;

    //st = st * 2. - 1.;
    st = st * 2. * iScale - iScale;

    st -= iTranslate;

    // // To move the cross we move the space
    // vec2 translate = vec2(cos(iTime),sin(iTime));
    // st += translate*0.35;

    // Show the coordinates of the space on the background
    // color = vec3(st.x,st.y,0.0);

    float pct = 0.;

    float xsize = 0.1;
    float ysize = 0.15;
    // float xsize = noise(0.1, iTime);
    // float ysize = noise(0.15, iTime);

    // float outer = 1.2;
    // float inner = 1.1;
    float outer = 1.2 + 0.2 * sin(iTime);
    float inner = 1.1 + 0.3 * cos(iTime);

    float yoffset = 0.7;

    st += vec2(0.5, yoffset);

    // pct += 0.5*xcross(st, xsize, ysize, outer, inner);
    // pct = 0.5*xcross(st, xsize*1.5, ysize*1.5, outer, inner) - pct;
    // pct = 0.5*xcross(st, xsize*2.0, ysize*2.0, outer, inner) - pct;
    // pct = 0.5*xcross(st, xsize*2.5, ysize*2.5, outer, inner) - pct;
    // pct = 0.5*xcross(st, xsize*3.0, ysize*3.0, outer, inner) - pct;
    // pct = 0.5*xcross(st, xsize*3.5, ysize*3.5, outer, inner) - pct;
    // pct = 0.5*xcross(st, xsize*4.0, ysize*4.0, outer, inner) - pct;
    // pct = 0.5*xcross(st, xsize*4.5, ysize*4.5, outer, inner) - pct;
    // pct = 0.5*xcross(st, xsize*5.0, ysize*5.0, outer, inner) - pct;

    pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 1.0, 0.5);
    pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 2.5, 0.5);
    pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 5.0, 0.5);
    pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 7.5, 0.5);
    pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 9.0, 0.5);
    pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 10.5, 0.5);
    pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 12.0, 0.5);
    pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 13.5, 0.5);
    pct = nest_xcross(pct, st, xsize, ysize, outer, inner, 0.5, 15.0, 0.5);

    st -= vec2(0.5, yoffset);

    float orig_triangle_dist = shape(st, 3);

    //vec2 st2 = vec2(0.5)-st;
    float r = iScale;
    //    float r = length(st)*2.0;
    float a = atan(st.y,st.x);
    float m = abs(mod(a+iTime*2.,3.14*2.)-3.14)/3.6;
    float f = orig_triangle_dist;
    m += noise(st+iTime*0.1)*.5;
    // a *= 1.+abs(atan(iTime*0.2))*.1;
    // a *= 1.+noise(st+iTime*0.1)*0.1;
    f += sin(a*50.)*noise(st+iTime*.2)*.1;
    f += (sin(a*20.)*.1*pow(m,2.));

    float triangle_dist = f / (iWow1 * 10.0 + 0.01);


    //float triangle_dist = shape(st, 3);
    // pct = step(0.15, triangle_dist);

    float tri_inner_mask = iScale + noise(0.35, 3.*iTime);
    // float tri_inner_mask = 0.35;
    float tri_inner_margin = 0.04;
    float tri_inner_start = tri_inner_mask + tri_inner_margin;
    float tri_inner_thickness = 0.1;

    float tri_outer_margin = 0.01;
    float tri_outer_start = tri_inner_start + tri_inner_thickness + tri_outer_margin;
    float tri_outer_thickness = 0.01;

    float inner_mask = step(tri_inner_mask, triangle_dist);
    float outer_triangle_edge = step(tri_outer_start+tri_outer_thickness+0.07, triangle_dist);

    // pct = 0.;
    pct *= 1. - inner_mask;
    // float exclusion_mask = inner_mask - outer_triangle_edge;
    // pct *= 1. - exclusion_mask;

    pct += step(tri_inner_start, triangle_dist) - step(tri_inner_start+tri_inner_thickness, triangle_dist);
    // pct += 0.4*step(tri_inner_start, triangle_dist) - step(tri_inner_start+tri_inner_thickness, triangle_dist);

    // st += 0.15*vec2(noise(1.1, iTime), noise(1.2, iTime));
    // triangle_dist = shape(st, 3);
    pct += 0.4*step(tri_outer_start, triangle_dist) - step(tri_outer_start+tri_outer_thickness, triangle_dist);
    // st -= 0.15*vec2(noise(1.1, iTime), noise(1.2, iTime));

    // st += 0.15*vec2(noise(1.4, iTime), noise(1.5, iTime));
    // triangle_dist = shape(st, 3);
    pct += step(tri_outer_start+2.*tri_outer_thickness, triangle_dist) - step(tri_outer_start+3.*tri_outer_thickness, triangle_dist);
    // st -= 0.15*vec2(noise(1.4, iTime), noise(1.5, iTime));

    //color += vec3(pct);
    color += pct * iColorRGB;
    fragColor = vec4(color,1.0);
}