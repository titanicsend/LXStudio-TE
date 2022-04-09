var glue = Java.type("titanicsend.pattern.pixelblaze.Glue");

var global = this;
["E", "LN2", "LN10", "LOG2E", "LOG10E", "PI", "SQRT1_2", "SQRT2", "abs", "acos", "acosh", "asin", "asinh",
"atan", "atanh", "atan2", "cbrt", "ceil", "clz32", "cos", "cosh", "exp", "expm1", "floor", "fround", "hypot",
"imul", "log", "log1p", "log10", "log2", "max", "min", "pow", "round", "sign", "sin", "sinh", "sqrt",
"tan", "tanh", "trunc"].forEach(k => global[k] = Math[k])

var PI2 = Math.PI * 2;

function random(v) {
  return Math.random() * v
}

function array(n) {
  var a = new Array(n);
  for (var i = 0; i < n; i++) a[i] = 0.0;
  return a;
}

function time(interval) {
  return ((new Date().getTime() / 65536) % interval) / interval
}

function wave(v) {
  return (sin(v*PI*2) + 1)/2
}

function triangle(v) {
  v = v * 2 % 2;
  if (v < 0)
    v += 2
  return v < 1 ? v : 2 - v
}

function hsv(h, s, v) {
  return glue.hsv(h, s, v);
}
