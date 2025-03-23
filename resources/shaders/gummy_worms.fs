#pragma name "GummyWorms"
#iUniform color3 iColorRGB = vec3(0.964, 0.144, 0.519)
#iUniform color3 iColor2RGB = vec3(0.226, 0.046, 0.636)
#iUniform float iRotationAngle = 0.0 in {0.0, 6.28}
#iUniform float iSpeed = 0.5 in {0.25, 3.0}
#iUniform float iScale = 0.05 in {0.01, 1.0}
#iUniform float iQuantity = 3.0 in {1.0, 9.0}
#iUniform float iWow2 = 2.5 in {1.0, 3.0}
#iUniform float iWow1 = 0.9 in {0.5, 2.0}

#ifdef GL_ES
precision mediump float;
#endif

#define PI 3.141592653589793
#define HALF_PI 1.5707963267948966
#define TWO_PI 6.28318530718
#define TAU 6.28318530718

/****************************************************
 * Palettes                                         *
 ****************************************************/

vec3 palette( float t )
{
    vec3 a = vec3(0.5, 0.5, 0.5);
    vec3 b = vec3(0.5, 0.5, 0.5);
    vec3 c = vec3(1.0, 1.0, 1.0);
    vec3 d = vec3(0.263, 0.416, 0.557);
    
    return a + b*cos( 6.28318*(c*t*d) );
}

vec3 mixPalette( vec3 c1, vec3 c2, float t )
{
    float mixFactor = 0.5 + sin(t);
    return mix(c1, c2, mixFactor);
}

vec3 rgb2hsb( in vec3 c ){
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz),
                 vec4(c.gb, K.xy),
                 step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r),
                 vec4(c.r, p.yzx),
                 step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)),
                d / (q.x + e),
                q.x);
}

vec3 hsb2rgb( in vec3 c ){
    vec3 rgb = clamp(abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),
                             6.0)-3.0)-1.0,
                     0.0,
                     1.0 );
    rgb = rgb*rgb*(3.0-2.0*rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

vec3 getPaletteColor(float t) {
    vec3 c1hsb = rgb2hsb(iColorRGB);
    vec3 c2hsb = rgb2hsb(iColor2RGB);
    float mixFactor = 0.5 + sin(t);
    return hsb2rgb(mixPalette(c1hsb, c2hsb, mixFactor));

    //// for inside the loop:
    // float mixFactor = fract((i + N*0.5*iTime) / N);
}

/****************************************************
 * SDFs                                             *
 ****************************************************/

float sdCircle(vec2 st) {
    // NOTE: modified to reference zero
    return length(st-.0)*2.;
}

float sdRect(vec2 st, vec2 s) {
    // st = st*2.-1.;
    //// NOTE: modified to reference zero (commented line above)
    return max(abs(st.x/s.x), abs(st.y/s.y));
}

/****************************************************
 * Drawing                                          *
 ****************************************************/

float stroke(float x, float s, float w) {
    float d = step(s, x + w * .5) - step(s, x - w * .5);
    return clamp(d, 0., 1.);
}

float fill(float x, float size) {
    return 1. - step(size, x);
}

void debugGrid(in vec2 pt, out vec3 color) {
    color.r += 0.2*(1.0 - smoothstep(0.0, 0.02, abs(fract(pt.x))));
    color.g += 0.2*(1.0 - smoothstep(0.0, 0.02, abs(fract(pt.y))));
    // color.r += 1.0 - smoothstep(0.0, 0.02, abs(pt.x));
    // color.g += 1.0 - smoothstep(0.0, 0.02, abs(pt.y));
}

void debugRect(in float rect, out vec3 color) {
    if (rect < 1.) {
        color.b += .4*(rect);
    }
    if (rect < 2.) {
        color.g += .4*(rect / 2.);
    } 
    color.b += 0.3*stroke(rect, 1., 0.01);
    color.g += 0.2*stroke(rect, 2., 0.01);
}

/****************************************************
 * Utils                                            *
 ****************************************************/

float flip(float v, float pct) {
    return mix(v, 1.-v, pct);
}

vec2 rotate(vec2 st, float a) {
    st = mat2(cos(a), -sin(a), sin(a), cos(a)) * (st-.5);
    return st + .5;
}

vec2 random2( vec2 p ) {
    return fract(sin(vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))))*43758.5453);
}

#define N 200.
#define ALPHA 1. / N

vec3 drawSquiggle(
    in vec2 st,
    in float rand,
    in vec2 dims,
    in vec2 offset,
    in float max_n,
    in vec4 wave,
    in float time
) {
    vec3 color = vec3(0.);
    float radius = 0.1;
    radius = iScale;

    // vec2 x_wave = wave.xy;
    float x_freq = wave.x;
    float x_speed = wave.y;

    // vec2 y_wave = wave.zw;
    float y_freq = wave.z;
    float y_speed = wave.w;

    vec2 crop = dims.xy - vec2(.5 * radius);
    vec3 pal = getPaletteColor(time);

    vec2 step = vec2(0.);  // x and y step size (for next circle)    
    vec2 acc = vec2(0.);   // accumulated offset, relative to (st), incl all past steps
    vec2 dir = vec2(1.);   // direction of the squiggle

    for (float i = 0.; i < N; i++) {
        if (i > max_n) {
            break;
        }

        vec2 limit = dir * crop;
        step = dir * offset;

        if (y_freq > 0.) {
            step.y *= sin(y_freq*i + y_speed*iTime);
            step.y *= clamp(i, 0., 10.) * 0.05; // fix the base
        } else {
            if (dir.y > 0. ? acc.y > limit.y : acc.y < limit.y) {
                offset.x += dir.y * 0.02;
                dir.y *= -1.;
            }
        }
         
        if (x_freq > 0.) {
            step.x *= sin(x_freq*i + x_speed*iTime);
            step.x *= clamp(i, 0., 10.) * 0.05; // fix the base
        } else {
            if (dir.x > 0. ? acc.x > limit.x : acc.x < limit.x) {
                offset.y += dir.x * 0.02;
                dir.x *= -1.;
            }
        }

        acc += step;

        color += fill(sdCircle(st + acc), radius) * (N-i) * pal * ALPHA;
    }

    return color;
}

vec3 tiledSquiggles(in vec2 st, float time) {
    vec3 color = vec3(0.);

    float w = .4;
    float h = .4;
    vec2 bounds = vec2(w, h);

    vec2 n = floor(st);
    vec2 f = fract(st);
    f = rotate(f, -2.*iRotationAngle);
    vec2 pt = f - vec2(0.5);

    

    vec2 rand2 = random2(n);
    float r = rand2.x + rand2.y;
    // r = 1.;

    float freq = .1 + .1 * r;
    float speed = 1.5 + r;

    float len = N*(0.5 + .5*sin(time + 2.*r));
    
    vec2 step_size = vec2(
        0.02 + 0.02 * r,
        2. / N
    );
    vec4 wave = vec4(freq, speed, 0., 0.);

    return drawSquiggle(pt, r, bounds, step_size, len, wave, time);

    // // alternate axes
    // //
    // vec2 step_size = vec2(
    //     2. / N,
    //     0.02 + 0.02 * r
    // );    
    // vec4 wave = vec4(0., 0., freq, speed);
    // drawSquiggle(pt, r, bounds, step_size, len, wave);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec3 color = vec3(0.);
    vec2 st = fragCoord.xy/iResolution.xy;
    st.x *= iResolution.x/iResolution.y;
    st = rotate(st, iRotationAngle);
    st -= vec2(0.5);
    
    st *= iQuantity;

    float time = iTime;
    time *= iSpeed;

    // // overlapping...
    color += 1.0 - tiledSquiggles(st, time);
    color *= 1.0 - tiledSquiggles(st*(iWow2 + 0.2*sin(time)), time);

    // color += tiledSquiggles(st);

    fragColor = vec4(color,1.0);

    // float rect = sdRect(pt, dims);
    // debugRect(rect, color);
    // debugGrid(pt, color);
}