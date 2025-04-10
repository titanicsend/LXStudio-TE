/*
  Spotlights / rotation 3D
  
  This pattern demonstrates one way to rotationally transform 3D space, which
  results in the impression we're rotating whatever pattern was generated.
  
  3D example: https://youtu.be/uoAJg5J6F1Q
  
  This pattern assumes a 3D installation that's been mapped in the Mapper tab,
  but degrades to somewhat less interesting projections in 2D and 1D. 
*/


scale = 1 / (PI * PI) // How wide the "spotlights" are
speed = 1             // How fast they rotate around

export function beforeRender(delta) {
  // We could just use sin(time()) to output -1..1, but that's almost too smooth
  t1 = 2 * triangle(time(.03 / speed)) - 1
  t2 = 2 * triangle(time(.04 / speed)) - 1
  t3 = 2 * triangle(time(.05 / speed)) - 1
  t4 = time(.02 / speed)

  // The axis we'll rotate around is a vector (t1, t2, t3) - each -1..1.
  // The angle to rotate about it is a 0..2*PI sawtooth.
  setupRotationMatrix(t1, t2, t3, t4 * PI2)
}

export function render3D(index, _x, _y, _z) {
  // Shift (0, 0, 0) to be the center of the world, not the rear-top-left
  x = _x - 0.5; y = _y - 0.5; z = _z - 0.5

  /*
    In beforeRender(), setupRotationMatrix() calculated a rotation matrix for
    this frame. rotate3D() now applies it to the current pixel's [shifted]
    position. As seen below, this sets rotated global variables rx, ry, and rz.
    You could also return an array of [rx, ry, rz], but that adds one slightly
    slower step to an already computationally-intense pattern.
  */
  rotate3D(x, y, z)

  /*
    `dist` is the distance (in world units) from a cone's surface to this 
    pixel. Positive values are inside the cone. If you try a different scale
    for x vs y, you'll see elliptical cones.
  */
  dist = abs(rz) - sqrt(rx * rx / scale + ry * ry / scale)

  dist = clamp(dist, -1, 1) // Try commenting this out.. Whoa!

  //  magenta,  white center,  sub-pixel rendered border

  v = pow((1 + dist), 4)
  paint(dist*1.5)
  setAlpha(v )
}

// A planar slice of this pattern will look like a projection surface that
// someone's waving a flashlight at.
export function render2D(index, x, y) {
  render3D(index, x, y, 0)
}

// In 1D it's a frenetic swooping region
export function render(index) {
  render3D(index, index / pixelCount * 2, 0, 0)
}



/*
  setupRotationMatrix()
  Takes a vector (ux, uy, uz) which will be the axis to rotate around,
    and an angle in radians.
  Computes a 3D rotation matrix and stores it in a global named R
  
  https://en.wikipedia.org/wiki/Rotation_matrix
*/

var R = array(3); for (i=0; i<3; i++) R[i] = array(3)  // init 3x3, R[r][c]

function setupRotationMatrix(ux, uy, uz, angle) {
  // Rescale ux, uy, uz to make sure it's a unit vector, length = 1
  length = sqrt(ux * ux + uy * uy + uz * uz)
  ux /= length; uy /=length; uz /= length

  // Precompute a few reused values
  cosa = cos(angle); sina = sin(angle)
  ccosa = 1 - cosa
  xyccosa = ux * uy * ccosa
  xzccosa = ux * uz * ccosa
  yzccosa = uy * uz * ccosa
  xsina = ux * sina; ysina = uy * sina; zsina = uz * sina

  R[0][0] = cosa + ux * ux * ccosa
  R[0][1] = xyccosa - zsina
  R[0][2] = xzccosa + ysina
  R[1][0] = xyccosa + zsina
  R[1][1] = cosa + uy * uy * ccosa
  R[1][2] = yzccosa - xsina
  R[2][0] = xzccosa - ysina
  R[2][1] = yzccosa + xsina
  R[2][2] = cosa + uz * uz * ccosa
}

/*
  rotate3D()
  Takes 3 coordinates (x, y, z) and expects R to be a global rotation matrix.
  Sets globals rx, ry, and rz as the rotated point's new coordinates.
  (Globals are used for speed and convenience in the Pixelblaze lang)
*/
var rx, ry, rz
function rotate3D(x, y, z) {
  rx = R[0][0] * x + R[0][1] * y + R[0][2] * z
  ry = R[1][0] * x + R[1][1] * y + R[1][2] * z
  rz = R[2][0] * x + R[2][1] * y + R[2][2] * z
}
