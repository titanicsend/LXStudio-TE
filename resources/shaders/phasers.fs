

// Controls
// Speed - beam rotation speed
// Spin/Angle - canvas rotation
// Quantity - beam splitter / number of beams
// Size - beam width / focus
// iWow1 - beat reactivity
// iWow2 - glow / fog level
// WowTrigger - center beam and strobe blast
uniform float iCanvasAngle = 0.0;
uniform float beamWidth;

const float PI = 3.141592653589793;
const float halfpi = PI / 2.;
const float TAU = PI * 2.;
const float xPos1 = -0.285;
const float xPos2 = 0.315;
const float yCenter = 0.3;

// accumulator for "Wow Trigger" effect
float zapper = 0.0;

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
  mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
  return rotationMatrix * point;
}

// 2D circle signed distance function
 float circle(vec2 p, float radius){
	return length(p) - radius;
}

 // Probably not really that random but its
 // distribution, whatever it be, looks nice for fog.
 float rand(vec2 p) {
 	return fract(sin(dot(p, vec2(12.543,514.123)))*4732.12);
 }

 // value noise
 float noise(vec2 p) {
 	vec2 f = smoothstep(0.0, 1.0, fract(p));
 	vec2 i = floor(p);
 	float a = rand(i);
 	float b = rand(i+vec2(1.0,0.0));
 	float c = rand(i+vec2(0.0,1.0));
 	float d = rand(i+vec2(1.0,1.0));
 	return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
 }

 // fractal noise (composed of several octaves of our value noise)
 float fbm(vec2 p) {
     float a = 0.5;
     float r = 0.0;
     for (int i = 0; i < 6; i++) {
         r += a*noise(p);
         a *= 0.5;
         p *= 2.0;
     }
     return r;
 }

// Generates "phaser" beam cone at specified position and angle,
// emphasizing the center of the beam, and splitting it <beams> ways.
float laser(vec2 p, vec2 offset, float angle, float beams) {

   // Rotate (spin the whole beam generator) around its origin
   p += offset;
   p *= mat2(cos(angle), -sin(angle),
             sin(angle), cos(angle));


    // get angle relative to current origin
    float radius = length(p) * ((iWowTrigger) ? 1.0-beat : 1.0);
    beams = floor(beams);
	float theta = atan(p.x, p.y);

    // draw constant width radial line for "laser beam"
    float bend = iWow1 * sin(21.0 * radius +  TAU * beat);
    float pulse = iWow1 * beat;
    float lzr =  smoothstep(beamWidth,0.0 , radius * abs(bend+sin(theta * beams / 2.0)));

	// generate a bright cone for glow effect
	float glw = max(0.0,0.5+0.5*(bend + sin(halfpi - theta*beams)));
    glw = pow(0.9*glw,iScale/beams);

    // add circular strobe burst on iWowTrigger
    zapper += (iWowTrigger) ? smoothstep(0.0,-0.1,circle(p, beat/4.0)) : 0.0 ;

	return clamp(((lzr+glw) * smoothstep(0.051,-.0001,circle(p,radius))) - pulse,0.0,1.0);
}

 // mix of fbm noise to simulate fog
 // 2 layers + 3rd used to mix from https://www.shadertoy.com/view/7tBSR1
 // makes a nice, smooth fog.
float clouds(vec2 uv) {
  vec2 t = vec2(iTime*0.5,iTime);
  float c1 = fbm(fbm(uv*3.0)*0.75+uv*3.0+t/3.0);
  float c2 = fbm(fbm(uv*2.0)*0.5+uv*7.0+t/3.0);
  float c3 = fbm(fbm(uv*10.0-t)*0.75+uv*5.0+t/6.0);
  float r = mix(c1, c2, c3*c3);
  return r * r * r;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord) {
  vec2 uv = -0.5+fragCoord/iResolution.x;
  uv = rotate(uv,iCanvasAngle);

  // beam+splitter combo
  float l = laser(uv, vec2(xPos2, fract(yCenter)), iRotationAngle, iQuantity);

  // background fog texture
  float c = clouds(uv);

  // Calculate the final color
  // laser beam always travels through *all* the fog, even if
  // the fog (controlled by iWow2) is turned down
  vec3 col = iColorRGB * ((8.0 * l * c) + (c * iWow2) + zapper);

  // alpha is taken from the brightest color channel value.
  fragColor = vec4(col, 1.);
}
