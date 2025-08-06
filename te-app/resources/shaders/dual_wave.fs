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
    // shape the motion a little.  The 0.22 offset helps reposition the
    // wave overlap at the end of the car, which can look like a stall
    // when the effect is initially triggered.
    float t1 = mod(0.22 + iTime,0.995);
    t1 = t1 * t1;

    // calculate the position of the leading edge of each wave
    float pct1 = x - t1;
    float pct2 = -x - t1;

    // get the brightness of the wave, using a sawtooth generator so that it is
    // bright at the leading edge, and then falls accoring to the tailPct
    float bri1 = max(0.,(tailPct - 1.0 + sawtooth(pct1,1.0)) / tailPct);
    float bri2 = max(0.,(tailPct - 1.0 + sawtooth(pct2,1.0)) / tailPct);

    // Keep a large strip at the leading edge at full brightness. This helps preserve
    // the illusion of smooth motion on the car when the wave is moving quickly.
    bri1 = min(1.0, (bri1 * bri1 * bri1) * 1.3);
    bri2 = min(1.0, (bri2 * bri2 * bri2) * 1.3);

    // Calculate the wave colors and the final pixel color.
    vec4 col1 = bri1 * vec4(iColorRGB,1.0);
    vec4 col2 = bri2 * vec4(oklab_mix(iColorRGB, iColor2RGB,iWow2),1.0);
    fragColor = col1 + col2;
}