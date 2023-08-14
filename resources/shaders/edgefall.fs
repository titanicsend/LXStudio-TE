precision mediump float;
float currentGlow;

/*
  This comment is here to make sure the missing controls script
  iWowTrigger
  picks up iWowTrigger, which this pattern actually does support.
  TODO - add real preprocessor syntax for declaring weird controls.
*/

#define LINE_COUNT 52
uniform vec4[LINE_COUNT] lines;

const float PI = 3.14159265359;
const float pulseWidth = 0.3;

// fog system borrowed from phasers.fs
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
     float a = 1.0;  // 0.5;
     float r = 0.0;
     for (int i = 0; i < 5; i++) {
         r += a*noise(p);
         a *= 0.5;
         p *= 2.0;
     }
     return r;
 }

 // mix of fractal noises to simulate fog - borrowed from phasers.fs pattern,
 // adapted to do interesting things to line segments
float corona(vec2 uv) {
  vec2 t = vec2(-iTime,iTime * -0.15);
  float c1 = fbm(fbm(uv*3.0)*0.75+uv*3.0+t/3.0);
  float c2 = fbm(fbm(uv*2.0)*0.5+uv*7.0+t/3.0);
  float c3 = fbm(fbm(uv*10.0-t)*0.75+uv*5.0+t/6.0);
  float r = clamp(mix(c1, c2, c3*c3),0.,1.);
  return r*r;
}

// from fabrice neyret: makes interesting, pointy-ended lines
float glowline(vec2 U, vec4 seg) {
    seg.xy -= U; seg.zw -= U;
    float a = mod ( ( atan(seg.y,seg.x) - atan(seg.w,seg.z) ) / PI, 2.);
    return pow(min( a, 2.-a ),currentGlow/6.);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    // normalize coordinates
    vec2 uv = -1. + 2. * fragCoord / iResolution.xy;
    uv.x *= iResolution.x / iResolution.y;
    uv *= 0.5;

    // gentle music reactivity - traveling wave moves upward with beat
    currentGlow = abs(beat - (0.5 + uv.y));
    currentGlow = (currentGlow <= pulseWidth) ? (pulseWidth - currentGlow) / pulseWidth : 0.0;
    currentGlow = iScale - (iScale * iWow1  * currentGlow);

    // draw some line segments
    vec3 color = vec3(0.0);
    float alpha = 0.0;
    float bri = 0.0;
    float fog = corona(uv);

    for (int i = 0; i < LINE_COUNT; i++) {
       bri = glowline( uv, lines[i]);
       color += fog * bri * mix(iColorRGB,mix(iColorRGB,iColor2RGB,fract(bri)),iWow2);
       alpha += bri;
    }

    fragColor = vec4(color,1.0);
}