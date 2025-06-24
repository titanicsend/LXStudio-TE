#pragma name "DotPolkaAuto"
// Converted from https://www.shadertoy.com/view/XldXRN

#define TE_NOTRANSLATE

#ifdef SHADER_TOY
#iUniform vec3 iColorRGB = vec3(.964,.144,.519)
#iUniform vec3 iColor2RGB = vec3(.226,.046,.636)
#iUniform float iScale=8. in {.5, 22.}
#iUniform float iQuantity = .8 in {0, 1}
#iUniform float levelReact = 0. in {0, 1}
#iUniform float frequencyReact = 0. in {0, 1}
#iUniform float iWow1 = 0. in {0, 1}
#iUniform float iWow2 = 0. in {0, 1}
const bool iWowTrigger = false;

#pragma teignore
#iUniform float bassRatio = 1. in {0, 5}
#iUniform float trebleRatio = 1. in {0, 5}
#iUniform float iRingThickness = .25 in {0, 1}
#pragma endteignore

#else
const float iRingThickness = .25;
uniform float iBeatTime;
#endif

const float PI = 3.14159265;

const float DOT_SIZE = .6; // Dot size % of max
const float SQRT3 = 1.73205080;
const float SQRT3_INV = 0.57735027;

 // sqrt(3) / 2 is the radius of a unit hexagon inscribed in a circle, the
 // biggest circle we can display without clipping the hex edges.
const float DOT_RADIUS = DOT_SIZE * SQRT3 * .5;

// -- easing functions to control shape movement -------------------------------

// squished cosine: f(x, l) is in [0, 1] for x in [0, l], 1 in [l, 1].
float cosquish(float x, float l) {
  return max(.5 - cos(fract(x) * PI / l) * .5, step(l, fract(x)));
}

// squished lerp
float linsquish(float x, float l) {
  return step(fract(x), l) * fract(x) / l;
}

// https://github.com/Michaelangel007/easing?tab=readme-ov-file#cleanup---out-elastic
float easeOutElastic(float t) {
  return 1. + exp2(-10. * fract(t)) * sin((-40. * fract(t) - 3.) * PI / 6.);
}

// experimental tightening spring, looks like crap
float chaos(float x) {
  return (1. - exp2(-16.0 * fract(x))) + exp2(-8.0 * fract(x)) * sin(mix(4., 50., fract(x)) * fract(x) * PI / 6.0);
}

// impulse fxn, but harder
float impulseHard(float x) {
  return sin(8. * fract(x) * PI) * (8. * fract(x) * exp(1. - 8. * fract(x))) + 1. - exp2(-64.0 * fract(x));
}

// -- shape drawing functions --------------------------------------------------

float disc(vec2 uv, float radius, float smoothMargin) {
  return smoothstep(0., -2. * smoothMargin, length(uv) - radius);
}

float normalizeLevel(float x) {
  return clamp(1. - exp(-.7701635 * (x - .1)), 0., 1.);
}

float ring(vec2 uv, float radius, float thickness, float smoothMargin) {
  float k = radius * thickness;
  float d = length(uv);
  return smoothstep(radius , radius - 2.* smoothMargin, d) -
    smoothstep(radius - k , radius - k - 2.*smoothMargin, d);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {

  #ifdef SHADER_TOY
    float SLOWDOWN = 1.;
    float beat = fract(iTime * 1.66666667 * SLOWDOWN); // 100 BPM
    float beat64 = fract(iTime * 1.66666667 * SLOWDOWN * .015625);
    float beatCount = ceil(iTime * 1.66666667 * SLOWDOWN);
  #else
    // Use iBeatTime uniform instead of global beatCount
    float beat = fract(iBeatTime);
    float beat64 = fract(iBeatTime * .015625);
    float beatCount = iBeatTime;
  #endif

  // Normalised coordinate: keep vertical FOV fixed.
  vec2 uv = (fragCoord * 2. - iResolution.xy) / iResolution.y;
  
  uv *= iScale;
  // float t = iTime * SLOWDOWN;
  float beat2 = fract(beat64 * 32.);
  float beatCount2 = beatCount * .5;

  float t = beat2;
  float c = beatCount2;

  // Periodic 60 degree rotation.
  float stepIndex = mod(ceil(c), 6.);
  float a = PI * .333333 * stepIndex;
  uv *= mat2(cos(a), -sin(a), sin(a), cos(a));

  // Hexagonal axial stretching.
  uv.y /= SQRT3 * .5;
  uv -= .5;

  // Stagger rows by .5 for a hexagonal grid. Keep this even if you
  // change/disable/whatever the row offset below.
  float row = ceil(uv.y);
  uv.x += (.5 * row);

  // Time-based row offset, controls how the dots move. Should be a smooth fxn
  // on [0, N] for fract(t), where N is the number of positions the dots move
  // per step (usually 1).
  float xOffset1;
  float xOffset2;
  float ss = .8;

  // // linear ramp envelope filter
  // xOffset1 = linsquish(fract(t), .65);
  // xOffset2 = linsquish(fract(t), .85);

  // cos envelope filter
  xOffset1 = cosquish(fract(t), .35);
  xOffset2 = cosquish(fract(t), .49);

  // xOffset1 = easeOutElastic(t);
  // xOffset2 = chaos(t);

  // Scissor‑like movement: offset even rows left, odd rows right.
  float leftOrRight = 2. * (mod(row, 2.) - .5);  // -1 even, +1 odd

  float xOffset1Wow = easeOutElastic(fract(t));
  float xOffset2Wow = impulseHard(fract(t));

  xOffset1 = mix(xOffset1, xOffset1Wow, iWow2);
  xOffset2 = mix(xOffset2, xOffset2Wow, iWow2);

  xOffset1 *= leftOrRight;
  xOffset2 *= leftOrRight;

  if (iWowTrigger) {
    xOffset2 *= -1.;
  }

  vec2 uv2 = vec2(uv);

  uv.x += xOffset1;
  uv2.x += xOffset2;

  // Local hex‑cell space [‑1,1].
  uv = 2. * fract(uv) - 1.;
  uv.y *= SQRT3 * .5;

  uv2 = 2. * fract(uv2) - 1.;
  uv2.y *= SQRT3 * .5;

  float dotSize1 = mix(iQuantity, normalizeLevel(bassRatio), levelReact);
  float dotSize2 = mix(iQuantity, normalizeLevel(trebleRatio), levelReact);

  float dotRadius1 = dotSize1 * SQRT3 * .5;
  float dotRadius2 = dotSize1 * SQRT3 * .5;

  // float smoothMargin = dotRadius * .025;


  float shape1, shape2, smoothMargin;
  if (iWowTrigger) {
    // ring shapes
    smoothMargin = .05;
    shape1 = ring(uv, dotRadius1, iRingThickness, smoothMargin);
    shape2 = ring(uv2, dotRadius2, iRingThickness, smoothMargin);
  } else {
    // disc shapes
    smoothMargin = .125;
    shape1 = disc(uv, dotRadius1, smoothMargin);
    shape2 = disc(uv2, dotRadius2, smoothMargin);
  }

  fragColor = vec4(shape1 * iColorRGB + shape2 * iColor2RGB, shape1 + shape2);

  vec4 fragColor1 = vec4(shape1 * iColorRGB, shape1);
  vec4 fragColor2 = iWow1 * vec4(shape2 * iColor2RGB, shape2);
  fragColor = fragColor1 + fragColor2;
}
