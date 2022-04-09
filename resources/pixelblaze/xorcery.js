/*
  Xorcery

  An XOR in 3D space based on the 'block reflections' pattern.
*/

worldscale = 10000000

export function beforeRender(delta) {
  t1 = time(.1)
  t2 = time(.1) * PI2
  t3 = time(.5)
  t4 = time(.2) * PI2
}

function xorf(v1, v2) {
	v1 *= 65536
	v2 *= 65536
	return (v1 ^ v2) / 65536
}

export function render3D(index, x, y, z) {
	x /= worldscale
	y /= worldscale
	z /= worldscale

	y += time(.1) //blittery waterfalls!

  m = .3 + triangle(t1) * .2
  h = sin(t2)
  h += (wave((xorf(5 * (x - .5), 5 * (y - .5))) / 50 *
    (triangle(t3) * 10 + 4 * sin(t4)) % m))
  v = (abs(h) + abs(m) + t1) % 1
  v = triangle(v * v)
  h = triangle(h) * .2 + triangle(x + y + z) * .2 + .45
  v = v * v

  hsv(h, 1, v)
}

export function render2D(index, x, y) {
  render3D(index, x, y, 0)
}

// Repeat the top line of the matrix 4X for a more granular 1D
export function render(index) {
  pct = index / pixelCount
  render3D(index, 4 * pct, 0, 0)
}
