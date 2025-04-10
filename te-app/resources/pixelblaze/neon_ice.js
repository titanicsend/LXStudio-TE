

export function beforeRender(delta) {
  t1 = time(.1)

}

export function render3D(index, x, y, z) {
  h = wave(t1 + x) * .2 + wave(t1 + y) * .2 + .5
  s = 1
  v = wave(index / 50000 + time(.02))
  hsv(h, s, v)
}
