scale = .5


var energy
export function sliderEnergy(v) {
  energy = v
}

export function beforeRender(delta) {
  t1 = measure()
  t2 = time(.1)
}

export function render3D(index, x, y, z) {
  //center coordinates
  x -= 0.5
  y -= 0.5
  z -= 0.5
  //get pixel distance from center
  r = sqrt(x*x + y*y + z*z) * scale
  //make colors
  h = r * 3
  //blast wave - a triangle's peak moving based on the center
  //clipped to 75% of the waveform - v goes negative: +0.25 to -0.75
  v = triangle(r - t1) - .75
  //trailing the outward burst are random white sparks
  //between 0-12.5% chance depending on distance to peak
  spark = triangle(r - t1 + .05) - energy  > random(2)
  if (spark) {
    rgb(1,1,1) //sparks are white
  } else {
    v = v*4 //bring the triangle's peak back to 0-1 range
    v = v*v*v //gives more definition to the wave, preserve negatives
    // hsv(h,1,v)

    paint(h)
    setAlpha(v)
  }
}
