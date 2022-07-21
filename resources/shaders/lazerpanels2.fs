uniform vec3 color;
uniform float vTime;
uniform float glow;
uniform float vScan;
uniform float hScan;
uniform float rotate;
uniform float beamCount1;
uniform float beamCount2;
uniform float yPos1;
uniform float yPos2;
uniform float energy;

const float halfpi = 3.1415926 / 2.;
const float xPos1 = -0.285;
const float xPos2 = 0.315;
const float yCenter = 0.3;

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

 // fractal noise
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

// beam emphasizing laser generator!
float laser(vec2 p, vec2 offset, float angle, float beams) {

   // Rotate
   p += offset;
   p *= mat2(cos(angle), -sin(angle),
             sin(angle), cos(angle));

    // get angle relative to current origin
	float theta = atan(p.x, p.y);

	// sine wave used for both light and glow
	float wave = 0.5+0.5*sin(halfpi - theta*beams);

    // narrow the glow a little
    float glw = pow(wave,40/beams );

    // narrow the actual laser beam (a lot!)
    float lzr = glw + smoothstep(0.0035,0.,1.-glw);

    // add part of the original wave fn for extra glow
    // the divisor
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

  // noise to modulate beam brightness
  float n = 1.+(3.*noise(vec2(iTime*11.,iTime * 5.)));

  // control beam movement in x and y
  float xOffset = hScan * sin(beat * 10.);
  float yOffset = yCenter + (vScan * fract(vTime));
  float angle = vTime * rotate;

  // generate a beam for each end of the vehicle
    vec2 offset  = vec2(xPos1+xOffset, fract(-yPos1 + yOffset));
	float l = n * laser(uv, offset, angle, beamCount1);

    offset = vec2(xPos2-xOffset, fract(-yPos2 + yOffset));
    l += n * laser(uv, offset, angle, beamCount2);

  // add fog and go
  // TODO - what do do about alpha?  We just blast the whole surface for now.
  float c = clouds(uv);
  vec3 fog = color * c * glow;
  fragColor = vec4(fog+(color * l * (c+(0.5*energy*beat))),1.);
}
