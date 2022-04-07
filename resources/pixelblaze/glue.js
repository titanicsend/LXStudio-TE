var glue = Java.type("titanicsend.pattern.pixelblaze.Glue");

var pixelCount = 0;

function teInit(pixelCount_) {
  pixelCount = pixelCount_;
}

function abs(n) {
  return Math.abs(n);
}

function array(n) {
  var a = new Array(n);
  for (var i = 0; i < n; i++) a[i] = 0.0;
  return a;
}

function floor(n) {
  return Math.floor(n);
}

function hsv(h, s, v) {
  return glue.hsv(h, s, v);
}

function random(max) {
  return glue.random(max);
}
