#pragma Name("DatsaMoire")

#include <include/colorspace.fs>

#define TE_NOTRANSLATE

#ifdef SHADER_TOY
#iUniform float iScale = 1. in { 0., 1. }
#iUniform float iQuantity = 1. in { 1., 30. }
#iUniform float iWow1 = 0.0 in { 0., 1. }
#iUniform float iWow2 = 0.0 in { 0., 1. }
#iUniform vec3 iColorRGB = vec3(.964, .144, .519)
#iUniform vec3 iColor2RGB = vec3(.226, .046, .636)

#iUniform float iTranslateX = 0. in {-1., 1.}
#iUniform float iTranslateY = 0. in {-1., 1.}
#endif

#pragma TEControl.SIZE.Range(0.5,0.0,1.0)
#pragma TEControl.XPOS.Range(0.1,0.0,1.0)
#pragma TEControl.YPOS.Range(0.0,0.0,1.0)
#pragma TEControl.WOW1.Range(0.0,0.0,1.0)
#pragma TEControl.WOW2.Range(0.0,0.0,1.0)
#pragma TEControl.QUANTITY.Range(1.0,1.0,30.0)

#pragma TEControl.LEVELREACTIVITY.Disable
#pragma TEControl.FREQREACTIVITY.Disable
#pragma TEControl.ANGLE.Disable
#pragma TEControl.SPIN.Disable
#pragma TEControl.WOWTRIGGER.Disable

#define PI 3.14159265359

const float ARMS  = 20.;  // int number of arms per spiral
const float LINES = 20.4;  // radial frequency
const float RSPEED = 2.2 / ARMS;  // normalized spiral rotation speed
const float BLUR = .125;  // smoothstep range

// // from IQ's oklab shader
// // Takes two RGB colors, performs linear interpolation between them in the the Oklab
// // color space and returns an RGB color that is a mix of the two.
// //
// // Oklab is a perceptually uniform color space, so the interpolation should have
// // a very even appearance with minimal color banding and uniform brightness
// //
// // Original oklab paper by Björn Ottosson: https://bottosson.github.io/posts/oklab
// // LMS (Long, Medium, Short) color space: https://en.wikipedia.org/wiki/LMS_color_space
// vec3 oklab_mix( vec3 colA, vec3 colB, float h ) {
//     const mat3 forwardXform = mat3(
//     0.4121656120,  0.2118591070,  0.0883097947,
//     0.5362752080,  0.6807189584,  0.2818474174,
//     0.0514575653,  0.1074065790,  0.6302613616);
//     const mat3 inverseXform = mat3(
//     4.0767245293, -1.2681437731, -0.0041119885,
//     -3.3072168827,  2.6093323231, -0.7034763098,
//     0.2307590544, -0.3411344290,  1.7068625689);
//
//     // convert input colors from RGB to oklab (actually LMS)
//     vec3 lmsA = pow( forwardXform * colA, vec3(1.0/3.0));
//     vec3 lmsB = pow( forwardXform * colB, vec3(1.0/3.0));
//
//     // interpolate in oklab color space
//     vec3 lms = mix( lmsA, lmsB, h );
//
//     // slight boost to midrange tones, as suggested by iq
//     lms *= (1.0+0.2*h*(1.0-h));
//
//     // now convert result back to RGB
//     return inverseXform*(lms*lms*lms);
// }

// Rotation dir: 1 for clockwise, -1 for ccw
float spiral(in vec2 fragCoord, in vec2 center, in float dir, in float offset) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    vec2 aspect = vec2(iResolution.x / iResolution.y, 1.0);

    // Position relative to the center, aspect‑corrected
    vec2 pos = (uv - center) * aspect;

    float arms = max(1., round(iQuantity));
    float rspeed = 2.2 / arms;

    float r = length(pos);
    float a = atan(pos.y, pos.x);
    float t = iTime * rspeed;
    return cos((LINES + 200. * iScale) * r - dir * arms * (a + dir * t) + dir * offset);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    #ifdef SHADER_TOY
        vec2 iTranslate = vec2(iTranslateX, iTranslateY);
    #endif

    // Set to < -1., > 1. to shift spiral centers off canvas. Annoying with mouse control.
    vec2 shift = vec2(0., 0.);

    // First spiral center
    vec2 c1 = (vec2(1., 1.)) * .5;
    c1 += iTranslate * .5;

    // Shift spiral centers on minor diagonal. Pattern will be symmetrical in X/Y if 0/0.
    vec2 otherShift = iTranslate * .5 * .66666666;

    // Other spirals, position linked to c1
    vec2 c2 = vec2(1. - c1.x, 1. - c1.y);     // diagonal sibling
    vec2 c3 = vec2(c1.x - otherShift.x, 1. - c1.y + otherShift.y);  // vertical sibling
    vec2 c4 = vec2(1. - c1.x + otherShift.x, c1.y - otherShift.y);  // horizontal sibling

    float arms = max(5., round(iQuantity));

    // Spirals
    float s1 = spiral(fragCoord, c1, 1.,  0.);
    float s2 = spiral(fragCoord, c2, 1., PI / arms * 0.5);
    float s3 = spiral(fragCoord, c3, -1., PI / arms * 0.5);
    float s4 = spiral(fragCoord, c4, -1., 0.);

    // Moire interference, each block is a little weirder than the last
    float m12 = s1 * s2;
    float m34 = s3 * s4;
    float mBlend = mix(m12, m34, iWow1);

    float m1234 = mix(s1 + s2, s3 + s4, iWow1); // another weird option
    float mm = mix(mBlend, m1234, iWow2);

    float shade = smoothstep(-BLUR, BLUR, mm);

    // // 1-color version
    // fragColor = vec4(iColorRGB * shade, shade);

    // 2-color version
    float bigblur = smoothstep(-1., 1., mm);
    fragColor = vec4(oklab_mix(iColorRGB, iColor2RGB, bigblur) * shade, bigblur);
}
