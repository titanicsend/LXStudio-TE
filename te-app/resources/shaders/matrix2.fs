/**
 * Matrix2:
 *
 * Migrated from MatrixScroller: https://github.com/titanicsend/LXStudio-TE/pull/40
 *
 * Related ShaderToy: https://www.shadertoy.com/view/ldjBW1
 *
 */

#pragma name "Matrix2"
// #iUniform color3 iColorRGB = vec3(0.964, 0.144, 0.519)
// #iUniform color3 iColor2RGB = vec3(0.226, 0.046, 0.636)
#iUniform float iRotationAngle = 0.0 in {0.0, 6.28}
#iUniform float iSpeed = 0.5 in {-4.0, 4.0}
#iUniform float iScale = 45.0 in {20.0, 100.0}
#iUniform float iQuantity = 0.6 in {0.01, 1.0}
#iUniform float iWow1 = 0.5 in {0.0, 1.0}
#iUniform float iWow2 = 0.2 in {0.0, 1.0}

#pragma TEControl.YPOS.Value(-0.1)
#pragma TEControl.LEVELREACTIVITY.Range(0.5,0.0,1.0)

#pragma TEControl.WOWTRIGGER.Disable

#ifdef GL_ES
precision mediump float;
#endif

#define PI 3.141592653589793
#define HALF_PI 1.5707963267948966
#define TWO_PI 6.28318530718
#define TAU 6.28318530718

#define TEXTURE_SIZE 512.0
#define CHANNEL_COUNT 16.0
#define pixPerBin (TEXTURE_SIZE / CHANNEL_COUNT)
#define halfBin (pixPerBin / 2.0)

vec3 col1 = vec3(0.964, 0.144, 0.519);
vec3 col2 = vec3(0.226, 0.046, 0.636);

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

float random (in vec2 st) {
    return fract(sin(dot(st.xy,
                         vec2(12.9898,78.233)))
                 * 43758.5453123);
}

// 2D Noise based on Morgan McGuire @morgan3d
// https://www.shadertoy.com/view/4dS3Wd
float noise (in vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    // Four corners in 2D of a tile
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    // Smooth Interpolation

    // Cubic Hermine Curve.  Same as SmoothStep()
    vec2 u = f*f*(3.0-2.0*f);
    // u = smoothstep(0.,1.,f);

    // Mix 4 coorners percentages
    return mix(a, b, u.x) +
            (c - a)* u.y * (1.0 - u.x) +
            (d - b) * u.x * u.y;
}

#include <include/colorspace.fs>

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec3 color = vec3(0.);
    vec2 st = fragCoord.xy/iResolution.xy;
    st.x *= iResolution.x/iResolution.y;
    st = rotate(st, iRotationAngle);
    st -= vec2(0.5);

    vec3 v = vec3(st, 1.0);

    // Calculate the distance to each axis boundary. For each component of v,
    // this computes how far you can travel in that direction before hitting
    // the edge of the [-0.5, 0.5] cube.
    vec3 s = 0.3 / abs(v);

    // Set s.z to the minimum of the x and y distances. This represents the
    // closest boundary in the XY plane.
    s.z = min(s.y, s.x);

    // axis swapping based on which boundary is closer. If the y-boundary is closer than x-boundary, it uses
    // (v.x, v.z, v.z), otherwise (v.z, v.y, v.z). This effectively projects the 3D point onto the appropriate
    // face of the cube.
    vec3 t = s.y < s.x ? v.xzz : v.yzz;

    // Scales the projected coordinates by the distance to boundary (s.z) times 800, then takes the ceiling.
    // This creates a grid pattern that gets denser as you approach the boundaries, creating the tunnel effect.
    vec3 h = s.z * t;
    vec3 i = ceil(8e2 * h) / iScale;

    // Picking an FFT bin: h.x will be the "row" of squares we see moving towards us on the screen,
    // after the side of cube has been computed but before it gets scaled by 8e2 / iScale
    // (to produce perspective effect).

    // h.x will be equivalent to the 3-D perspective aware column,
    // either along X for the bottom/top sides of the cube, or Y for right/left sides.
    float row = (1. + clamp(h.x, -1., 1.)) / 2.;
    // to invert the row number for opposite sides of the cube: (t.x > 0. ? row : 1. - row)
    float mirroredRow = row;
    float binNum = mirroredRow * CHANNEL_COUNT;
    float fftBin = (2. * (1.0+texelFetch(iChannel0, ivec2(halfBin+pixPerBin * binNum, 0), 0).x)) - 1.;

    float szFFT = fftBin * levelReact;

    i *= szFFT;

    // Takes the fractional part of each component, creating repeating patterns in the [0,1] range.
    vec3 j = fract(i);
    i -= j;

    float b = (9. + 8. * sin(i).x);
    int tb = int(iTime * b);
    vec3 p = vec3(9, tb, 0) + i;

    // pseudo-random value based on a 2D point p
    float R = fract(1e2 * sin(p.x * 5. + p.y ));
    float k = R / s.z;



    // It's too busy to have the whole perspective z-axis turn into an equalizer all at the same time.
    // "s.z" controls how far back in the "rows" of squares we're modifying.
    //
    // pow(sin(s.z),4) ensures it's sparse but intense. I wanted to offset phase a bit between
    // the top-bottom and left-right sides of the cube, so there's always *some* equalizer in-frame without
    // it ever totally visually dominating the scene.
    //
    // using 2*s.z so it's more frequent and narrower bands.
    //
    // Comparing to volumeRatio so that there's some built-in effect that the equalizer goes away when things
    // are quiet.
    float szBand = pow(
      sin(
        (s.y < s.x ? 1. : 0.) // offset phases a bit
        + 2.*s.z // more frequent along Z
        + iTime*3.
      ),
      4
    ); // > (1.0 - iWow1) ? s.z : 0.;

    // get a number we can multiply to other values:
        // -> if we're outside the szBand, just 1.0
        // -> if we're inside the szBand, (value of fftBin * levelReact) + 1.0
//         float szFFT = fftBin * levelReact; //mix(1.0, szBand * (1.0 + (fftBin * levelReact)), frequencyReact);

    // To debug how much of the z-axis we want to activate at a time:
    //     float szBand = (s.z > iWow1 && s.z <= iWow1 * 1.5) ? s.z : 0.;

    fragColor = vec4(vec3(k * getGradientColor(mix(0.0, 1.0 - szFFT, iWow2))), 1.);

    // Also appealing:
    //     float mask = (R > 0.5 && j.x < 0.6 * szFFT && j.y < 0.8 * szFFT) ? 1.0 : 0.0;

    float mask = R > (1.0 - iQuantity) ? 1.0 + 0.25*szFFT : 0.0; // NOTE: R > (1. / iQuantity) ??

    float equalizerBounce = 2.*szFFT*(1.0 + iWow1);
    mask *= 1. - smoothstep(equalizerBounce, equalizerBounce + 0.05, j.x);
    mask *= 1. - smoothstep(equalizerBounce, equalizerBounce + 0.05, j.y);
    //mask *= 1. - smoothstep(0.6*equalizerBounce, 0.6*equalizerBounce + 0.05, j.x);
    //mask *= 1. - smoothstep(0.8*equalizerBounce, 0.8*equalizerBounce + 0.05, j.y);
//     mask *= j.x < 0.6 * szFFT
//     mask *= j.y < 0.8 * szFFT) ;

    fragColor *= vec4(vec3(mask), R > 0.5 * szFFT ? 1.0 : clamp(szFFT, 0.5, 1.3));
}