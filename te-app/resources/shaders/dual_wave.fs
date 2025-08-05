// Basic dual-wave algorithm developed from a discussion of
// frame buffer-less, multidimensional KITT patterns on the Pixelblaze forum.
// https://forum.electromage.com/t/kitt-without-arrays/1219

#include <include/constants.fs>
#include <include/colorspace.fs>

uniform bool runEffect = false;

// Pixelblaze-flavored helper functions for shaders
float sawtooth(float time,float period) {
    return mod(time,period) / period;
}

float triangle(float n) {
    return  2. * (0.5 - abs(fract(n) - 0.5));
}

float wave(float n) {
    return 0.5+(sin(TAU * abs(fract(n))) * 0.5);
}

float square(float n,float dutyCycle) {
    return (abs(fract(n)) <= dutyCycle) ? 1.0 : 0.0;
}

// random number generators
float rand(vec2 p) {
    return fract(sin(dot(p, vec2(12.543, 514.123)))*4732.12);
}

vec2 random2(vec2 p) {
    return vec2(rand(p), rand(p*vec2(12.9898, 78.233)));
}

// 2D rotation matrix
mat2 rotate2D(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

// KITT!
void mainImage( out vec4 fragColor, in vec2 fragCoord )  {
    if (!runEffect) {
        fragColor = vec4(0.0);
        return;
    }

    float tailPct = iScale;         // length of the tail in 0..1

    // Normalize incoming pixel coords
    vec2 uv = fragCoord / iResolution.xy;
    uv -= 0.5;                       // center
    uv *= rotate2D(iRotationAngle);  // rotate
    uv += 0.5;                       // back to original position

    // and after all that, the x coordinate is the only one we need
    float x =  floor(iQuantity) * uv.x;

    // get time-based position offset for the waves, and square it to
    // shape the motion a little.
    float t1 = fract(iTime);
    t1 = t1 * t1;

    // build the two moving waves
    float pct1 = x - t1;
    float pct2 = -x - t1;

    float bri = max(max(0.,(tailPct - 1.0 + sawtooth(pct1,1.0)) / tailPct),
        max(0.,(tailPct - 1.0 + sawtooth(pct2,1.0)) / tailPct));
    // use smoothstep to keep the front edge very bright
    bri = smoothstep(0.0, 0.6, bri);

    fragColor = vec4(iColorRGB,bri * bri * bri);
}