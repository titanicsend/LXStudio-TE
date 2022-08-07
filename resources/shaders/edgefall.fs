uniform float energy;
uniform float glow;
uniform vec4[6] lines;

const float PI = 3.14159265359;

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
     for (int i = 0; i < 6; i++) {
         r += a*noise(p);
         a *= 0.5;
         p *= 2.0;
     }
     return r;
 }

 // mix of fractal noises to simulate fog - borrowed from phasers.fs pattern,
 // adapted to do interesting things to line segments
float clouds(vec2 uv) {
  vec2 t = vec2(iTime,iTime * -0.15);
  float c1 = fbm(fbm(uv*3.0)*0.75+uv*3.0+t/3.0);
  float c2 = fbm(fbm(uv*2.0)*0.5+uv*7.0+t/3.0);
  float c3 = fbm(fbm(uv*10.0-t)*0.75+uv*5.0+t/6.0);
  float r = mix(c1, c2, c3*c3);
  return r*r;
}

// from fabrice neyret: makes interesting, pointy-ended lines
float glowline1(vec2 U, vec4 seg) {
    seg.xy -= U; seg.zw -= U;
    float a = mod ( ( atan(seg.y,seg.x) - atan(seg.w,seg.z) ) / PI, 2.);  
    return pow(min( a, 2.-a ),glow);
}

// normal 2D distance-from-line-segment function
float glowline2(vec2 p, vec4 seg) {
    vec2 ld = seg.xy - seg.zw;
    vec2 pd = p - seg.zw;
    
    float bri = 1. - length(pd - ld*clamp( dot(pd, ld)/dot(ld, ld), 0.0, 1.0) );    
    return pow(bri,glow);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{   // normalize coords to range -0.5 to 0.5
    vec2 uv = fragCoord.xy / iResolution.xy - .5;
     
    vec3 color = vec3(0.0);
    float fog = clouds(uv);
    
    // draw some line segments
    int segNo = 0;
    color += fog * glowline2( uv, lines[segNo]) * iPalette[segNo]; segNo++;
    color += fog * glowline2( uv, lines[segNo]) * iPalette[segNo]; segNo++;
    color += fog * glowline2( uv, lines[segNo]) * iPalette[segNo]; segNo++;
    color += fog * glowline2( uv, lines[segNo]) * iPalette[segNo]; segNo++;
    color += fog * glowline2( uv, lines[segNo]) * iPalette[segNo]; segNo++;
    color += fog * glowline2( uv, lines[segNo]) * iColorRGB;

    fragColor = vec4(color,1.0);
}