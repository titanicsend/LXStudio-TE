/*
This glue file implements a bunch of Pixelblaze compatibility APIs and parts of the framework.
 */
var Glue = Java.type("titanicsend.pattern.pixelblaze.Glue");
var LXColor = Java.type("heronarts.lx.color.LXColor");
var ColorType = Java.type("titanicsend.pattern.TEPattern.ColorType");
var Noise = Java.type("heronarts.lx.utils.Noise");
var System = Java.type("java.lang.System");
var TEMath = Java.type("titanicsend.util.TEMath");

/* Globals available in pattern code */
var global = this;
var point;

/* Internal globals used by glue */
var __now, __points, __colors;
var __lastControls = {};

/* Math functions and constants as globals */
["E", "LN2", "LN10", "LOG2E", "LOG10E", "PI", "SQRT1_2", "SQRT2", "abs", "acos", "acosh", "asin", "asinh",
"atan", "atanh", "atan2", "cbrt", "ceil", "clz32", "cos", "cosh", "exp", "expm1", "floor", "fround",
"imul", "log", "log1p", "log10", "log2", "max", "min", "pow", "round", "sign", "sin", "sinh", "sqrt",
"tan", "tanh", "trunc"].forEach(k => global[k] = Math[k])

var PI2 = Math.PI * 2;

/* Constants */
var PRIMARY = ColorType.PRIMARY;
var SECONDARY = ColorType.SECONDARY;
var BACKGROUND = ColorType.BACKGROUND;

/* Point / coordinate API */
function isEdgePoint() {
  return __pattern.model.isEdgePoint(point.index)
}

//
// Math functions
//
function random(v) {
  return Math.random() * v
}

function clamp(v, min, max) {
  return Math.min(max, Math.max(min, v))
}

function frac(float v) {
    return Glue.fract(v);
}

function hypot(x, y) {
  return sqrt(x*x + y*y)
}

function mix(float x, float y, float a) {
  return Glue.mix(x, y, a);
}

function mod(float x, float y) {
    return Glue.mod(x, y);
}

function prng(v) { /* TODO: Implement */ }

function prngSeed(v) { /* TODO: Implement */ }

function smoothstep(float edge0, float edge1, float x) {
  return Glue.smoothstep(edge0, edge1, x);
}

function trunc(v) {  /* TODO: Implement */ }

//
// Array functions
//
function array(n) {
  var a = new Array(n);
  for (var i = 0; i < n; i++) a[i] = 0.0;
  return a;
}
//Iterate over an array and call fn(value, index, array) for each element.
function arrayForEach(a, fn) { a.forEach(fn) }

//Return the length/size of an array. Note that the a.length form is not a function call.
function arrayLength(a) { return a.length }

// Iterate over the src array and call fn(value, index, array) for each element.
// Return values are then stored in dest. The dest array may be smaller than src,
// in which case extra results are calculated and then discarded.
function arrayMapTo(src, dest, fn) { src.mapTo(dest, fn) }

//Modify an array in place by calling fn(value, index, array) for each element and storing the return values.
function arrayMutate(a, fn) { a.mutate(fn) }

// Returns a value by calling fn(accumulator, value, index, array) (the reducer)
// for each element of the array, resulting in a single value (the accumulator).
// The accumulator parameter is seeded with initialValue and updated with the return value
// from each call to fn.Examples:
function arrayReduce(a, fn, initialValue) {return 0 /* TODO:  a.reduce(fn, initialValue) */ }

// Replace the elements of an array with any number of arguments, starting at index 0.
// The argument list must fit into the array. If the array is larger than the arguments,
// the remaining array elements are unchanged.
function arrayReplace(a, ...) { /* TODO : a.replace(...)  */ }

//Replace some elements of an array with any number of arguments, starting at offset.
//The argument list must fit into the array at the given offset. The other array elements are unchanged.
function arrayReplaceAt(a, offset, ...) { /* TODO : a.replace(offset, ...) */ }

//Sort an array of numbers in ascending order.
function arraySort(a) { /* TODO : a.sort()  */ }

// Sort an array using fn(v1, v2) to compare element values. The compare function should
// return a negative number if v1 is less than v2. The order of equal elements is
// not guaranteed to be preserved.
function arraySortBy(a, fn) { /* TODO : a.sortBy(fn) */ }

//Returns the sum of all elements in an array. Tip: this can be used to
// calculate an average: average=arraySum(a) / arrayLength(a)
function arraySum(a) { /* TODO: Implement */ }

//
// Waveform functions
//
function bezierQuadratic(t, p0, p1, p2) { /* TODO: Implement */ }

function bezierCubic(t, p0, p1, p2) { /* TODO: Implement */ }

function perlin(x, y, z, seed) { /* TODO: Implement */ }

function perlinFbm(x, y, z, lacunarity, gain, octaves) { /* TODO: Implement */ }

function perlinRidge(x, y, z, lacunarity, gain, offset, octaves) { /* TODO: Implement */ }

function perlinTurbulence(x, y, z, lacunarity, gain, octaves) { /* TODO: Implement */ }

function setPerlinWrap(x, y, z) { /* TODO: Implement */ }

function square(n,dutyCycle) {
  return Glue.square(n,dutyCycle)
}

function time(interval) {
  return ((__now / 65536) % interval) / interval
}

function triangle(v) {
  return TEMath.trianglef(v)
}

function wave(v) {
  return TEMath.wavef(v)
}

//
// Color and Painting functions
//
function hsv(h, s, v) {
  return __color = Glue.hsv(h, s, v);
}
function rgb(r, g, b) {
  return __color = Glue.rgb(r, g, b);
}
function rgba(r, g, b, a) {
  return __color = Glue.rgba(r, g, b, a);
}

function paint(v) {
  return __color = __pattern.getGradientColor(v);
}

function swatch(v) {
  return __color = __pattern.getSwatchColor(v);
}

function getHue() {
  return LXColor.h(__color)/360
}

function getSaturation() {
  return LXColor.s(__color)/100
}

function getBrightness() {
  return LXColor.b(__color)/100
}

function setAlpha(v) {
  __color = Glue.setAlpha(__color, v);
}

//
// TE-specific sound reactivity functions
//
function isBeat() {
  return __pattern.getLX().engine.tempo.beat();
}

function measure() {
  return __pattern.measure()
}
function wholeNote() {
  return __pattern.wholeNote();
}
function phrase() {
  return __pattern.phrase();
}

function getBassLevel() {
  return __pattern.getBassLevel();
}

function getTrebleLevel() {
  return __pattern.getTrebleLevel();
}

function getBassRatio() {
  return __pattern.getBassRatio();
}

function getTrebleRatio() {
  return __pattern.getTrebleRatio();
}

//
// TE-specific getter functions for the common controls
//
function getRotationAngleFromSpeed() {
    return __pattern.getRotationAngleFromSpeed();
}

function getRotationAngleFromSpin() {
    return __pattern.getRotationAngleFromSpin();
}

function getStaticRotationAngle() {
    return __pattern.getStaticRotationAngle();
}

function getCurrentColor() {
   return __pattern.getCurrentColor();
}

function getTime() {
    return __pattern.getTime();
}

function getTimeMs() {
    return __pattern.getTimeMs();
}

function getSpeed() {
    return __pattern.getSpeed();
}

function getXPos() {
    return __pattern.getXPos();
}

function getYPos() {
    return __pattern.getYPos();
}

function getSize() {
    return __pattern.getSize();
}

function getQuantity() {
    return __pattern.getQuantity();
}

/**
 * As in Java, for for most uses, getRotationAngle() is recommended, but if you
 * need direct access to the spin control value, here it is.
 */
function getSpin() {
    return __pattern.getSpin();
}

function getWow1() {
    return __pattern.getWow1();
}

function getWow2() {
    return __pattern.getWow2();
}

function getWowTrigger() {
    return __pattern.getWowTrigger();
}

/* Pixelblaze compatibility framework glue */

function sentenceCase(text) {
  var result = text.replace(/([A-Z])/g, " $1");
  result = result.replace(/_/g, " ");
  result = result.replace(/  /g, " ");
  result = result.trim();
  var words = result.split(" ").map(function (word) {
    return word.charAt(0).toUpperCase() + word.substring(1)
  });
  result = words.join(" ");
  return result;
}

function glueRegisterControls() {
  for (var key in global) {
    if (typeof global[key] == "function") {
      if (key.startsWith("slider")) {
        // System.out.println("found " + key);
        let label = sentenceCase(key.substring(6))
        __pattern.addSlider(key, label)
      }
    }
  }
}
function glueInvokeControls() {
  var value;
  for (var key in global) {
    if (typeof global[key] == "function") {
      if (key.startsWith("slider")) {
        value = __pattern.getSlider(key);
        if (__lastControls[key] !== value) {
          __lastControls[key] = value;
          try {
            global[key](value);
          } catch (err) {
            //ignore
          }
        }
      }
    }
  }
}

function glueBeforeRender(delta, now, points, colors) {
  pixelCount = points.length;
  __now = now;
  __points = points;
  __colors = colors;
  glueInvokeControls();
  if (typeof beforeRender === "function") {
    beforeRender(delta);
  }
}

function glueRender() {
  var r;
  if (typeof render3D !== 'undefined') {
    r = render3D;
  } else if (typeof render2D !== 'undefined') {
    r = render2D;
  } else {
    r = render;
  }
  var xOffs = __pattern.getXPos();
  var yOffs = -__pattern.getYPos();
  var i;
  for (i = 0; i < __points.length; i++) {
    __color = 0;
    point = __points[i];
    r(i, point.xn + xOffs, point.yn + yOffs, point.zn);
    __colors[point.index] = __color;
  }
}

