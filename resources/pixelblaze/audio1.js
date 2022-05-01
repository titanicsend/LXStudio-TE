
var t1, waveBase, beat, lacunarity, gain, ridgeOffset, octaves, a;

var energy;
export function sliderEnergyLevel(v) {
  energy = v;
}

export function beforeRender(delta) {
  t1 = time(.1 * 256)
}

export function render3D(index, x, y, z) {

  var f = isEdgePoint() ? point.frac : triangle( z*2 + y );

  waveBase = ( 4 + measure() * 4) % 1
  beat = (triangle(x * .5 + y * .8 - waveBase  + .6) - .5) * 4 * energy
  beat = clamp(beat, 0, 1)

  lacunarity = 2;
  gain = .7 +  beat / 4
  ridgeOffset = .78 + beat / 20
  octaves = 6

  a = -1 + 4 * Noise.stb_perlin_ridge_noise3(
      x * .2 + t1*256, y, z ,
      lacunarity, gain, ridgeOffset, octaves);

  paint(  f + a)
  setAlpha(a)

}
