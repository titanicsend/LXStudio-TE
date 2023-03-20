uniform float energy;
uniform float iCanvasAngle;

const float PI = 3.141592653589793;
const float halfpi = PI / 2.;
const float twopi = PI * 2.;
const float xPos1 = -0.285;
const float xPos2 = 0.315;
const float yCenter = 0.3;

#define glow iWow2

// accumulator for "Wow Trigger" effect
float zapper = 0.0;

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
  mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
  return rotationMatrix * point;
}

// 2D (square) Domain repeats
vec2 repeat2D(vec2 p, vec2 size) {
	return mod(p + size*0.5,size) - size*0.5;
}

// polar domain repeats around coordinate origin
// (looks like pie slices on the car)
void modPolar(inout vec2 p, float repetitions) {
	float angle = twopi/repetitions;
	float a = atan(p.y, p.x) + angle/2.;
	a = mod(a,angle) - angle/2.;
	p = vec2(cos(a), sin(a))*length(p);
}

// 2D circle distance function
 float circle(vec2 p, float radius){
	return radius - length(p);
}

 // rand [0,1]
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
     for (int i = 0; i < 7; i++) {
         r += a*noise(p);
         a *= 0.5;
         p *= 2.0;
     }
     return r;
 }

// Generates "phaser" beam cone at specified position and angle,
// emphasizing the center of the beam, and splitting it <beams> ways.
float laser(vec2 p, vec2 offset, float angle, float beams) {

   // scale the beam generator positions so they converge in a way
   // that looks about right. (never mind pesky physics!)
   offset *= 1./pow(iScale,0.25);

   // Rotate (spin the whole beam generator)
   p += offset;
   p *= mat2(cos(angle), -sin(angle),
             sin(angle), cos(angle));


    // get angle relative to current origin
	float theta = atan(p.x, p.y);

	// sine wave used for both light and glow
	float wave = 0.5+0.5*sin(halfpi - theta*beams);

    // narrow the glow a little
    float glw =  pow(wave,40./beams) / iScale;

    // narrow the actual laser beam (a lot!)
    //float lzr = glw + smoothstep(0.0035,0.,1.-glw);
    float lzr = glw + smoothstep(.15,0.,1.-glw);

    // add part of the original wave fn for extra glow
    // the divisor
    zapper += (iWowTrigger) ? smoothstep(0.165,0.225,circle(p, beat/2.45)) : 0.0 ;
	return lzr+glw;
}

 // mix of fractal noises to simulate fog
 // elegant 2 layers + 3rd used to mix from https://www.shadertoy.com/view/7tBSR1
float clouds(vec2 uv) {
  vec2 t = vec2(iTime*0.5,iTime);
  float c1 = fbm(fbm(uv*3.0)*0.75+uv*3.0+t/3.0);
  float c2 = fbm(fbm(uv*2.0)*0.5+uv*7.0+t/3.0);
  float c3 = fbm(fbm(uv*10.0-t)*0.75+uv*5.0+t/6.0);
  float r = mix(c1, c2, c3*c3);
  return r*r*r;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord) {
  vec2 uv = -0.5+fragCoord/max(iResolution.x,iResolution.y);

  uv = rotate(uv,iCanvasAngle);

  if (iWow1 > 0.0) {
    uv.x = fract(2.0 * uv.x);
    uv = repeat2D(uv,vec2(1.0-iWow1));
    modPolar(uv,1.0 + iWow1 * 12.0);
  }

  uv *= iScale;

  // noise to modulate beam brightness
  float n = 1.+(3.*noise(vec2(iTime*11.,iTime * 5.)));

  // control beam movement in x and y
  float beatSine = -1 + 2 * sinPhaseBeat;
  float xOffset = energy * beatSine;
  float yOffset = yCenter + energy * -beatSine;

  // generate a beam for each end of the vehicle
    vec2 offset  = vec2(xPos1+xOffset, fract(yOffset));
	float l = n * laser(uv, offset, iRotationAngle, iQuantity);

    offset = vec2(xPos2-xOffset, fract(yOffset));
    l += n * laser(uv, offset, iRotationAngle, iQuantity);

  // add fog, compose final color and go
  float c = clouds(uv);
  vec3 col = iColorRGB * c * glow;
  col += iColorRGB * l * (c+(0.5*energy*beat));
  col = col + zapper;  // add iWowTrigger effect

  // alpha is taken from the brightest color channel value.
  fragColor = vec4(col,sqrt(max(col.r,max(col.g,col.b))));
}
