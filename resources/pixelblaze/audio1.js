
var c = 0;

var energy

export function sliderEnergyLevel(v) {
  energy = v;
}

export function beforeRender(delta) {
  if (isBeat())
    c = 1
  else
    c *= .95
}
export function render3D(index, x, y, z) {

  var f = isEdgePoint() ? point.frac : ( y ) * 5;

  paint(  f + (measure()))

  if (c > y)
    swatch( isEdgePoint() ? EDGE : PANEL);

  setAlpha(energy)

}
