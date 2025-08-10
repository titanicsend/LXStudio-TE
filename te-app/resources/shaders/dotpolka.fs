// Inspired by https://www.shadertoy.com/view/XldXRN
#define TE_NOTRANSLATE

#include <include/colorspace.fs>

#ifdef SHADER_TOY
// Control uniforms provided by TE
#iUniform vec3 iColorRGB = vec3(.964, .144, .519)
#iUniform vec3 iColor2RGB = vec3(.226, .046, .636)
#iUniform float iScale = 12. in {1., 20.}
#iUniform float iSpin = 0. in {-1., 1.}
#iUniform float iSpeed = .3 in {0., 1.}
#iUniform float iQuantity = 1. in {1., 4.}
#iUniform float levelReact = 0. in {0, 1}
#iUniform float frequencyReact = 0. in {0, 1}
#iUniform float iWow1 = 0. in {0, 1}
#iUniform float iWow2 = 0. in {0, 1}
const bool iWowTrigger = false;

#pragma teignore
// Other TE uniforms
#iUniform float bassLevel = .5 in {0., 1.}
#iUniform float volumeRatio = 1. in {0., 5.}

#iUniform float iTranslateX = 0. in {-1., 1.}
#iUniform float iTranslateY = 0. in {-1., 1.}

// Pattern-specific uniforms, not provided by TE
#iUniform float iSpeedDiscrete = .25 in {0., 1.}
#iUniform float iAnimationSpeed = .25 in {0., 1.}
#iUniform float iWowTriggerValue = 0. in {0, 1}

// Inline for Shadertoy
vec3 oklab_mix( vec3 colA, vec3 colB, float h ) {
    const mat3 forwardXform = mat3(
    0.4121656120,  0.2118591070,  0.0883097947,
    0.5362752080,  0.6807189584,  0.2818474174,
    0.0514575653,  0.1074065790,  0.6302613616);
    const mat3 inverseXform = mat3(
    4.0767245293, -1.2681437731, -0.0041119885,
    -3.3072168827,  2.6093323231, -0.7034763098,
    0.2307590544, -0.3411344290,  1.7068625689);

    // convert input colors from RGB to oklab (actually LMS)
    vec3 lmsA = pow( forwardXform * colA, vec3(1.0/3.0));
    vec3 lmsB = pow( forwardXform * colB, vec3(1.0/3.0));

    // interpolate in oklab color space
    vec3 lms = mix( lmsA, lmsB, h );

    // slight boost to midrange tones, as suggested by iq
    lms *= (1.0+0.2*h*(1.0-h));

    // now convert result back to RGB
    return inverseXform*(lms*lms*lms);
}
#pragma endteignore

#else  // no SHADER_TOY
// Pattern-specific uniforms
uniform float iBeatTime = 0.;
uniform float iSpeedDiscrete = 0.;
uniform float iAnimationSpeed = .4;
uniform float iWowTriggerValue = 0.;
#endif

const float PI = 3.14159265;
const float SQRT3 = 1.73205080;
const float SQRT3_INV = .57735027;

const float DOT_SIZE = 1.; // Dot size % of max
 // sqrt(3) / 2 is the radius of a unit hexagon inscribed in a circle, the
 // biggest circle we can display without clipping the hex edges.
const float DOT_RADIUS = DOT_SIZE * SQRT3 * .5;

// 1 works here too, but kind of ruins the illusion of movement
const float HEX_RADIUS = .85;

const float SMOOTH_MARGIN = .16;
const float MIN_MARGIN = .1;

// Ring animation constants
const float LIGHT_ELEVATION = 1.;
// Where the inner shape ends and outer shape begins, % of distance from center
const float SPLIT_RADIUS = .6;
const float OUTER_RING_HIGH = 1.;
const float INNER_RING_LOW = 0.;

// Number of beats per ring animation cycle
const float BEAT_SAW_SPEED = 6.;
const float WOW_SPEED_MULTIPLE = 2.;
// Offset between shape animation cycles
const float OFFSET_OFFSET = .5;

// Volume mapping constants
const float CV_DEFAULT = .9;  // output volume at input = 1
const float VOL_CEILING = 4.;  // max volume input
const float CV_OVERDRIVE = 1.4;  // max volume output

// Global ring thickness scale factor
const float DEFAULT_RINGFAT = 1.;
// Ring thickness range for wow trigger animation
const float WOWTRIGGER_RINGFAT_MIN = .4;
const float WOWTRIGGER_RINGFAT_MAX = .8;

// -- Utility and convenience functions ----------------------------------------

float cosEase (float x) {
  return .5 - .5 * cos(x * PI);
}

// Ring animation edge helpers
// Leading edge: ease up to 1 and hold
float getLeadingEdge(float saw, float width, float lo, float hi) {
  return mix(lo, hi, cosEase(clamp(saw / width, 0., 1.)));
}

// Trailing edge: hold at 0 then follow leading edge with ring width delay
float getTrailingEdge(float saw, float width, float lo, float hi) {
  const float eps = .000001;
  float shiftedSaw = fract(max(saw, eps) + width);
  return mix(lo, hi,
      step(shiftedSaw, width) * cosEase(clamp(shiftedSaw / width, 0., 1.)));
}

// -- Easing functions to control shape movement -------------------------------

// Squished cosine: f(x, l) is in [0, 1] for x in [0, l], = 1 in [l, 1].
float cosquish(float x, float l) {
  return max(.5 - cos(fract(x) * PI / l) * .5, step(l, fract(x)));
}

// https://github.com/Michaelangel007/easing?tab=readme-ov-file#cleanup---out-elastic
float easeOutElastic(float t) {
  return 1. + exp2(-10. * fract(t)) * sin((-40. * fract(t) - 3.) * PI / 6.);
}

// Hard bouncey impulse function
float impulseHard(float x) {
  return sin(8. * fract(x) * PI) * (8. * fract(x) * exp(1. - 8. * fract(x))) + 1. - exp2(-64. * fract(x));
}

// -- Shape drawing functions --------------------------------------------------

float ring(vec2 uv, float size, float r1, float r2, float m, float rot, float depth, float lightAngle)
{
    // Ring mask
    float mfixed = max(min((r2 - r1) * .5, m), MIN_MARGIN);
    r1 = clamp(r1, 0., 1.);
    r2 = clamp(r2, 0., 1.);

    float inner = min(r1, r2) * size;
    float outer = max(r1, r2) * size;
    float d = length(uv);

    float ringflat = smoothstep(outer, outer - m, d) -
                     smoothstep(inner, inner - m, d);
    if (depth <= 0.) return ringflat;

    float mask = smoothstep(outer, outer - mfixed, d) -
                 smoothstep(inner, inner - mfixed, d);

    // 3D-ish ring with pseudo‑torus normal
    float majorRad = .5 * (inner + outer);
    float minorRad = .5 * (outer - inner);

    vec2 dir = uv / d;  // in‑plane direction
    vec2 cross = uv - dir * majorRad;  // radial offset
    cross *= mat2(cos(rot), -sin(rot), sin(rot), cos(rot));
    float z = sqrt(max(minorRad * minorRad - dot(cross, cross), 0.));

    vec3 surfaceNormal = normalize(vec3(cross, z));

    // Diffuse shading
    vec3 lightDir = normalize(vec3(
        cos(lightAngle) * cos(LIGHT_ELEVATION),
        sin(lightAngle) * cos(LIGHT_ELEVATION),
        sin(LIGHT_ELEVATION)
    ));
    float diff = max(dot(surfaceNormal, lightDir), 0.);
    float ring3d = diff * mask;

    return mix(ringflat, ring3d, depth);
}

// Signed distance to regular hexagon centered at 0
float sdHex(vec2 p, float radius) {
    const vec3 k = vec3(-SQRT3 / 2., .5, SQRT3_INV);
    p = abs(p);
    p -= 2. * min(dot(k.xy, p), 0.) * k.xy;
    p -= vec2(clamp(p.x, -k.z * radius, k.z * radius), radius);
    return length(p) * sign(p.y);
}

// Hexagon ring with fake depth shading
float hexring(vec2 uv, float size,
                  float r1, float r2,
                  float m, float rot,
                  float depth, float lightAngle) {
    float inner = clamp(min(r1, r2), 0., 1.) * size;
    float outer = clamp(max(r1, r2), 0., 1.) * size;
    float tubeRadius = .5 * (outer - inner);

    float mfixed = max(min(tubeRadius, m), MIN_MARGIN);

    float outerDist = sdHex(uv, outer);
    float innerDist = sdHex(uv, inner);

    // Flat mask
    float ringFlat = (1. - smoothstep(-m, 0., outerDist)) -
                     (1. - smoothstep(-m, 0., innerDist));
    if (depth <= 0.) return ringFlat;

    // Sharper mask for the 3D pass
    float mask3D = (1. - smoothstep(-mfixed, 0., outerDist)) - 
                   (1. - smoothstep(-mfixed, 0., innerDist));

    // Use the hexagon distance field to maintain hexagon shape
    float midlineR = .5 * (inner + outer);
    float dMidline = sdHex(uv, midlineR);

    // Distance from point to the ring's medial axis (hexagon-shaped midline)
    float crossDist = min(abs(dMidline), tubeRadius);

    // Compute normal using a small offset in the hexagon distance field
    const float eps = 0.001;
    vec2 grad = vec2(
        sdHex(uv + vec2(eps, 0.), midlineR) - sdHex(uv - vec2(eps, 0.), midlineR),
        sdHex(uv + vec2(0., eps), midlineR) - sdHex(uv - vec2(0., eps), midlineR)
    );
    vec2 radialDir = normalize(grad + 1e-6);

    vec2 cross = radialDir * crossDist;
    cross *= mat2(cos(rot), -sin(rot), sin(rot), cos(rot));
    float z = sqrt(max(tubeRadius * tubeRadius - crossDist * crossDist, 0.));

    vec3 surfaceNormal = normalize(vec3(cross, z));  // fake 3‑D normal

    // Simple diffuse lighting
    vec3 lightDir = normalize(vec3(
        cos(lightAngle) * cos(LIGHT_ELEVATION),
        sin(lightAngle) * cos(LIGHT_ELEVATION),
        sin(LIGHT_ELEVATION)
    ));
    float diff = max(dot(surfaceNormal, lightDir), 0.);
    float ring3D = diff * mask3D;

    return mix(ringFlat, ring3D, depth);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {

  #ifdef SHADER_TOY
    vec2 iTranslate = vec2(iTranslateX, iTranslateY);

    // iBeatTime is a custom uniform, doesn't exist in Shadertoy. Simulate a 100
    // BPM beat with iTime insted.
    float slowdown = 1.;
    float iBeatTime = iTime * 1.66666667 * slowdown;

    float iRotationAngle = iSpin * iBeatTime / 4.;
  #endif

  // Normalised coordinate: keep vertical FOV fixed.
  vec2 uv = (fragCoord * 2. - iResolution.xy) / iResolution.y;
  vec2 uvPreTransform = uv;

  uv /= iScale;

  float beatMultiple = iSpeedDiscrete;
  float beat = fract(iBeatTime * beatMultiple);
  float beatCount = iBeatTime * beatMultiple;

  // Periodic 60 degree rotation
  float stepIndex = mod(ceil(beatCount), 6.);
  float a = PI * .33333333 * stepIndex;

  // Add iRotationAngle spin
  float spinAngle = iRotationAngle;
  float aplus = a + spinAngle;

  uv *= mat2(cos(aplus), -sin(aplus), sin(aplus), cos(aplus));

  // Hexagonal axial stretching
  uv.y /= SQRT3 * .5;
  uv -= .5;

  // Stagger rows by .5 for a hexagonal grid. Keep this even if you
  // change/disable/whatever the row offset below.
  float row = ceil(uv.y);
  uv.x += (.5 * row);

  float speed = iAnimationSpeed;

  // Time-based row offset, controls how the shapes move. Should be a smooth
  // fxn on [0, N] for fract(t), where N is the number of positions the dots
  // move per step (usually 1).
  float xOffset1 = cosquish(fract(beat), speed);
  float xOffset2 = xOffset1;

  // Bouncier
  float xOffset1Wow = easeOutElastic(fract(beat));
  float xOffset2Wow = impulseHard(fract(beat));
  xOffset1 = mix(xOffset1, xOffset1Wow, smoothstep(0., 1., frequencyReact) * bassLevel);
  xOffset2 = mix(xOffset2, xOffset2Wow, smoothstep(.4, 1., frequencyReact) * bassLevel);

  float rowShift;
  if (iQuantity < 2.) {
    // Scissor‑like movement: offset even rows left, odd rows right.
    rowShift = 2. * (mod(row, 2.) - .5);  // -1 even, +1 odd
  } else if (iQuantity < 3.) {
    // Scissor 2x: -2, -1, 1, 2, 1, -1, -2, ...
    // https://oeis.org/A057079
    rowShift = 2. * sin((2. * row + 1.) * PI / 6.);
  } else {
    // Scissor 3x: -3, -2, -1, 1, 2, 3, 2, -2, -3, ...
    rowShift = round(-3. * cos(PI / 5. * row));
  }

  xOffset1 *= rowShift;
  xOffset2 *= rowShift;

  // Dance half the rings in the opposite direction
  if (iWowTrigger) {
    xOffset1 = -xOffset1;
  }

  // Duplicate coords to apply different movement offsets
  vec2 uv2 = vec2(uv);

  uv.x += xOffset1;
  uv2.x += xOffset2;

  // Local hex‑cell space [‑1, 1]
  uv = 2. * fract(uv) - 1.;
  uv.y *= SQRT3 * .5;

  uv2 = 2. * fract(uv2) - 1.;
  uv2.y *= SQRT3 * .5;

  float ringfat = DEFAULT_RINGFAT;
  float depth = max(smoothstep(.2, 1., levelReact),
                    1. - clamp(ringfat / .2, 0., 1.));
  depth = mix(depth, 1., iWowTriggerValue);

  // Map volumeRatio to a more manageable range
  // Linear ramp to CV_DEFAULT, then smoothstep to CV_OVERDRIVE at VOL_CEILING
  float volumeRatioClamped = mix(
      mix(0., CV_DEFAULT, clamp(volumeRatio, 0., 1.)),
      CV_OVERDRIVE,
      smoothstep(1., VOL_CEILING, volumeRatio)
    );

  float ringfatMod = mix(ringfat, volumeRatioClamped, levelReact);
  float ringfatWow = mix(WOWTRIGGER_RINGFAT_MIN, WOWTRIGGER_RINGFAT_MAX, levelReact);
  ringfatMod = mix(ringfatMod, ringfatWow, iWowTriggerValue);
  float rri = 1. - ringfatMod / 2.;

  float l1lo, l1hi, l2lo, l2hi, l3lo, l3hi, l4lo, l4hi;

  if (!iWowTrigger) {
    l1lo = SPLIT_RADIUS;
    l1hi = OUTER_RING_HIGH;
    l2lo = SPLIT_RADIUS;
    l2hi = OUTER_RING_HIGH;
    l3lo = SPLIT_RADIUS;
    l3hi = INNER_RING_LOW;
    l4lo = SPLIT_RADIUS;
    l4hi = INNER_RING_LOW;
  } else {
    l1lo = INNER_RING_LOW;
    l1hi = OUTER_RING_HIGH;
    l2lo = INNER_RING_LOW;
    l2hi = OUTER_RING_HIGH;
    l3lo = INNER_RING_LOW;
    l3hi = OUTER_RING_HIGH;
    l4lo = INNER_RING_LOW;
    l4hi = OUTER_RING_HIGH;
  }

  // Slowly-moving spatial lighting for 3D shapes, one full rotation every 24 beats
  float lightAngle = iBeatTime * PI / 12. + uvPreTransform.x * PI;

  float bs1Offset = -iTranslate.x * uvPreTransform.x - iTranslate.y * uvPreTransform.y;
  float bs2Offset = bs1Offset + OFFSET_OFFSET;

  float bs = iBeatTime / BEAT_SAW_SPEED;
  if (iWowTrigger) {
    bs *= WOW_SPEED_MULTIPLE;
  }

  float hexMix = iWow2;
  float smoothMargin = SMOOTH_MARGIN;

  float saw1 = fract(bs + bs1Offset);
  float inner1 = getLeadingEdge(saw1, rri, l1lo, l1hi);
  float outer1 = getTrailingEdge(saw1, rri, l1lo, l1hi);
  float ring1_circle = ring(uv, DOT_RADIUS, inner1, outer1, smoothMargin, -a, depth, lightAngle);
  float ring1_hex = hexring(uv, HEX_RADIUS, inner1, outer1, smoothMargin, -a, depth, lightAngle);
  float ring1 = mix(ring1_circle, ring1_hex, hexMix);

  float saw2 = fract(bs + bs1Offset + .5);
  float inner2 = getLeadingEdge(saw2, rri, l2lo, l2hi);
  float outer2 = getTrailingEdge(saw2, rri, l2lo, l2hi);
  float ring2_circle = ring(uv2, DOT_RADIUS, inner2, outer2, smoothMargin, -a, depth, lightAngle);
  float ring2_hex = hexring(uv2, HEX_RADIUS, inner2, outer2, smoothMargin, -a, depth, lightAngle);
  float ring2 = mix(ring2_circle, ring2_hex, hexMix);

  float saw3 = fract(bs + bs2Offset + .25);
  float inner3 = getLeadingEdge(saw3, rri, l3lo, l3hi);
  float outer3 = getTrailingEdge(saw3, rri, l3lo, l3hi);
  float ring3_circle = ring(uv, DOT_RADIUS, inner3, outer3, smoothMargin, -a, depth, lightAngle);
  float ring3_hex = hexring(uv, HEX_RADIUS, inner3, outer3, smoothMargin, -a, depth, lightAngle);
  float ring3 = mix(ring3_circle, ring3_hex, hexMix);

  float saw4 = fract(bs + bs2Offset + .75);
  float inner4 = getLeadingEdge(saw4, rri, l4lo, l4hi);
  float outer4 = getTrailingEdge(saw4, rri, l4lo, l4hi);
  float ring4_circle = ring(uv2, DOT_RADIUS, inner4, outer4, smoothMargin, -a, depth, lightAngle);
  float ring4_hex = hexring(uv2, HEX_RADIUS, inner4, outer4, smoothMargin, -a, depth, lightAngle);
  float ring4 = mix(ring4_circle, ring4_hex, hexMix);

  float shape1 = clamp(ring1, 0., 1.) + clamp(ring3, 0., 1.);
  float shape2 = clamp(ring2, 0., 1.) + clamp(ring4, 0., 1.);

  float secondColorMix = iWow1;
  vec3 mixedColor = oklab_mix(iColorRGB, iColor2RGB, secondColorMix);
  fragColor = vec4(shape1 * iColorRGB, shape1) +
              vec4(shape2 * mixedColor, shape2);
}
