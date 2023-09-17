// Oceanscape for climate data visualzation. 2021-2022 ZRanger1
//
//
// Excellent water droplet effect from:
// "The Drive Home" by Martijn Steinrucken aka BigWings/CountFrolic - 2017
// https://www.shadertoy.com/view/MdfBRX
//
// License: Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License

vec3 sunColor;
float waveHeight;
float waveTime;
float Roughness = 7.25;
float wind = 1.0;
float rain = 2.5;
float snow = 0.;

float freezeLevel = 0.0;
float steamLevel = 0.25;

const float pi = 3.14159;
const float twoPi = 6.283185;
const float dstFar = 200.;     // far clipping distance
const float dstNear = 0.25;     // near clipping (aquarium window) distance

const float dropletSpacing = 1.0;
const float seaIce = 0.75;

// colors
vec3 cloudBaseColor;
vec3 groundBaseColor;

// constants used in PRNG functions for noise field
// generators.
const vec4 cHashA4 = vec4 (0., 1., 57., 58.);
const vec3 cHashA3 = vec3 (1., 57., 113.);
const float cHashM = 43758.5453123;

// Global variables
vec3 sunDir,cloudDisp, waterDisp;
float tCur,fCloud;

// random 2 element vector (range 0 to 1)
vec2 Hashv2f (float p) {
  return fract (sin (p + cHashA4.xy) * cHashM);
}

// random 4 element vector (range 0 to 1)
vec4 Hashv4f (float p) {
  return fract (sin (p + cHashA4) * cHashM);
}

// random 4 element vector (range 0 to 1) using 3 element vector seed
vec4 Hashv4v3 (vec3 p)
{
  const vec3 cHashVA3 = vec3 (37.1, 61.7, 12.4);
  const vec3 e = vec3 (1., 0., 0.);
  return fract (sin (vec4 (dot (p + e.yyy, cHashVA3), dot (p + e.xyy, cHashVA3),
  dot (p + e.yxy, cHashVA3), dot (p + e.xxy, cHashVA3))) * cHashM);
}

// generate 1D noise field value from single seed
float noise1D1 (float p) {
  float i, f;
  i = floor (p);  f = fract (p);
  f = f * f * (3. - 2. * f);
  vec2 t = Hashv2f (i);
  return mix (t.x, t.y, f);
}

// generate 1D noise value from 2 element vector seed
float noise1D2 (vec2 p) {
  vec2 i, f;
  i = floor (p);  f = fract (p);
  f = f * f * (3. - 2. * f);
  vec4 t = Hashv4f (dot (i, cHashA3.xy));
  return mix (mix (t.x, t.y, f.x), mix (t.z, t.w, f.x), f.y);
}

// generate 1D noise value from 3 element vector seed
float noise1D3 (vec3 p) {
  vec3 i, f;
  i = floor (p);  f = fract (p);
  f *= f * (3. - 2. * f);
  vec4 t1 = Hashv4v3 (i);
  vec4 t2 = Hashv4v3 (i + vec3 (0., 0., 1.));
  return mix (mix (mix (t1.x, t1.y, f.x), mix (t1.z, t1.w, f.x), f.y),
  mix (mix (t2.x, t2.y, f.x), mix (t2.z, t2.w, f.x), f.y), f.z);
}

// fbm noise, 1 in, 1 out
float fbm1 (float p) {
  float f, a;
  f = 0.;  a = 1.;
  for (int i = 0; i < 5; i ++) {
    f += a * noise1D1 (p);
    a *= 0.5;  p *= 2.;
  }
  return f;
}

// fbm noise, 3 in, one out
float fbm3 (vec3 p) {
  const mat3 mr = mat3 (0., 0.8, 0.6, -0.8, 0.36, -0.48, -0.6, -0.48, 0.64);
  float f, a, am, ap;
  f = 0.;  a = 0.5;
  am = 0.5;  ap = 4.;
  p *= 0.5;
  for (int i = 0; i < 6; i ++) {
    f += a * noise1D3 (p);
    p *= mr * ap;  a *= am;
  }
  return f;
}

//  Noise generator - 3 out, 1 in... DAVE HOSKINS
vec3 N31(float p) {
  vec3 p3 = fract(vec3(p) * vec3(.1031,.11369,.13787));
  p3 += dot(p3, p3.yzx + 19.19);
  return fract(vec3((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y, (p3.y+p3.z)*p3.x));
}

float triangle(float t) {
  return -1.+4.*(0.5-abs(mod(t,twoPi) / twoPi - 0.5));
}

float sawtooth(float t) {
  return cos(t+cos(t))+sin(2.*t)*.2+sin(4.*t)*.02;
}

float AsymmetricSawtooth(float time,float rise, float fall) {
  float period = rise + fall;
  float r = rise / period;  // how much of the cycle is rise time
  float t = mod(time,period) / period;  // how far into the cycle
  t =(t < r) ? mod(time,rise) / rise : 1.-(mod(time-rise,fall) / fall);

  return t;
}

float DeltaSawtooth(float t) {
  return 0.4*cos(2.*t)+0.08*cos(4.*t) - (1.-sin(t))*sin(t+cos(t));
}

vec3 SkyGrndCol (vec3 ro, vec3 rd) {
  vec3 p, q, skyBg, clCol, col;
  float colSum, attSum, s, att, a, dDotS, ds;

  const float cloudLo = 100., cloudThickness = 1./50., attenuationFactor = 0.25; //0.06;
  const int nLay = 30;

  // if the ray reaches the land in the far background, render it in the appropriate land color
  if (rd.y < 0.015 * fbm1 (16. * rd.x)) col = groundBaseColor * (0.25 + 0.5 *
  noise1D2 (1000. * vec2 (rd.x, rd.y)));

  // otherwise it's some combination of cloud/sky and sun colors.  We're not trying
  // to trace volumetric clouds or perfect reflections, so we just throw a ray upward and
  // approximate how much cloud it passes through to determine how the light gets dispersed and colored.
  else {
    fCloud = clamp (fCloud, 0., 1.);
    dDotS = max (dot (rd, sunDir), 0.);
    ro += cloudDisp;
    p = ro;
    p.xz += (cloudLo - p.y) * rd.xz / rd.y;
    p.y = cloudLo;
    ds = 1. / (cloudThickness * rd.y * (2. - rd.y) * float (nLay));
    colSum = 0.;
    attSum = 0.;
    s = 0.;  att = 0.;

    for (int j = 0; j < nLay; j ++) {
      q = p + rd * s;
      q.z *= 0.7;
      att += attenuationFactor * max (fCloud - fbm3 (0.02 * q), 0.125);
      a = (1. - attSum) * att;
      colSum += a * (q.y - cloudLo) * cloudThickness;
      attSum += a;  s += ds;
      if (attSum >= 1.) break;
    }
    colSum += 0.5 * min ((1. - attSum) * pow (dDotS, 3.), 1.);
    clCol = vec3 (1.0) * colSum + 0.175 * sunColor;
    skyBg = mix (sunColor, cloudBaseColor,rd.y);
    col = clamp (mix (skyBg, 1.6 * clCol, attSum), 0., 1.);
  }
  return col;
}

vec3 SeaFloorCol (vec3 rd) {
  vec2 p;
  float w, f;
  p = 5. * rd.xz / rd.y;
  w = 1.;
  f = 0.;
  for (int j = 0; j < 4; j ++) {
    f += w * noise1D2 (p);
    w *= 0.5;  p *= 2.;
  }
  return mix (vec3 (0.01, 0.04, 0.02), vec3 (0, 0.05, 0.05),
  smoothstep (0.4, 0.7, f));
}

// calculates wave height at a given 3D position
float WaveHt (vec3 p) {
  const mat2 qRot = mat2 (1.6, -1.2, 1.2, 1.6);
  vec4 t4, ta4, v4;
  vec2 q2, t2, v2;
  float wFreq, wAmp, pRough, ht;

  wFreq = 0.16;
  wAmp = waveHeight;
  pRough = Roughness;
  q2 = p.xz + waterDisp.xz;
  ht = 0.;

  // add several layers of noisy waves
  for (int j = 0; j < 5; j ++) {
    t2 = waveTime * vec2 (1.1, -1.1);
    t4 = vec4 (q2 + t2.xx, q2 + t2.yy) * wFreq;
    t2 = 2.0 * vec2 (noise1D2 (t4.xy), noise1D2 (t4.zw)) - 1.;
    t4 += vec4 (t2.xx, t2.yy);
    ta4 = abs(sin (t4));
    v4 = (1. - ta4) * (ta4 + abs (cos (t4)));
    v2 = pow (1. - pow (v4.xz * v4.yw, vec2 (0.65)), vec2 (pRough));
    ht += (v2.x + v2.y) * wAmp;
    q2 *= qRot;  wFreq *= 1.9;  wAmp *= 0.22;
    pRough = 0.75 * pRough + 0.2;
  }
  return ht;
}


// Ray marching function for wave field
float WaveRay(vec3 ro, vec3 rd) {
  vec3 p;
  float dHit, h, s, sLo, sHi;

  s = 0.;
  sLo = 0.;
  dHit = dstFar;
  for (int j = 0; j < 150; j ++) {
    p = ro + s * rd;
    h = p.y - WaveHt (p);
    if (h < 0.) break;
    sLo = s;
    s += max (0.2, h) + 0.005 * s;
    if (s > dstFar) break;
  }
  if (h < 0.) {
    sHi = s;
    for (int j = 0; j < 7; j ++) {
      s = 0.5 * (sLo + sHi);
      p = ro + s * rd;
      h = step (0., p.y - WaveHt(p));
      sLo += h * (s - sLo);
      sHi += (1. - h) * (s - sHi);
    }
    dHit = sHi;
  }
  return dHit;
}

float WaveOutRay (vec3 ro, vec3 rd) {
  vec3 p;
  float dHit, h, s, sLo, sHi;

  s = 0.;
  sLo = 0.;
  dHit = dstFar;
  ro.y *= -1.;
  rd.y *= -1.;

  for (int j = 0; j < 150; j ++) {
    p = ro + s * rd;
    h = p.y + WaveHt (p);
    if (h < 0.) break;
    sLo = s;
    s += max (0.2, h) + 0.005 * s;
    if (s > dstFar) break;
  }

  if (h < 0.) {
    sHi = s;
    for (int j = 0; j < 7; j ++) {
      s = 0.5 * (sLo + sHi);
      p = ro + s * rd;
      h = step (0., p.y + WaveHt (p));
      sLo += h * (s - sLo);
      sHi += (1. - h) * (s - sHi);
    }
    dHit = sHi;
  }
  return dHit;
}

// returns vector normal to a point on the wave field surface
vec3 WaveNf (vec3 p, float d) {
  vec2 e = vec2 (max (0.1, 5e-5 * d * d), 0.);
  float h = WaveHt (p);
  return normalize (vec3 (h - WaveHt (p + e.xyy), e.x, h - WaveHt (p + e.yyx)));
}

// return color contribution for the chill winds of
// the ice age.
float doIcewind(vec3 pos) {

  pos *= 4.0;
  pos.xz += wind + (vec2(2.25,2) * tCur);

  return fbm3(pos);
}

float doSteam(vec3 pos) {
  float t,f;

  float h = (pos.y > -0.1) ? 1.: 0.3;

  t = tCur * 0.333;
  pos *= 20.0;
  pos.y -= t + 0.25 * wind;
  pos.xz += t + 0.12 * wind;

  f = fbm3(pos); f *= f;
  f *= h;
  return f;
}

// do all the actual work
vec3 ShowScene (vec3 ro, vec3 rd) {
  vec3 col, vn, rdd, refCol,foamCol;
  float dstWat, dstWindow,dif;
  float bk, sh, foamFac, noiseVal;
  const float eta = 0.75;
  bool doReflect;

  // find distance to wave height field, and set flag if we hit anything
  dstWat = dstWindow = WaveRay (ro, rd);
  doReflect = (dstWat < dstFar);

  // get vector normal to the wave at the current location and see what colors
  // we pick up from clouds, sky and sun
  if (doReflect) {
    ro += rd * dstWat;
    vn = WaveNf (ro, dstWat);
    rdd = rd;
    rd = reflect (rd, vn);
  }

  col = SkyGrndCol (ro, rd);
  foamCol = mix(col,vec3(0.5),0.3);

  // add in the ocean's own color, taken from the sea floor noise map, and
  // the "foam" effect, which is based on wave height and still more noise.
  if (doReflect) {
    refCol = col;
    rd = refract (rdd, vn, eta);
    col = SeaFloorCol (rd);
    col = mix (col, 0.8 * refCol, pow (1. -  abs(dot (rdd, vn)), 4.));


    // foam effect falls off with distance, otherwise it creates antialiasing problems
    dstWat = clamp(50./dstWat,0.,1.);
    noiseVal = fbm3(8.*rd);
    float ff1 = dstWat * pow(clamp (WaveHt(ro) + noiseVal - 0.93, 0., 1.), 4.);
    float ff2 = seaIce + sin(twoPi*noiseVal);
    foamFac = mix(ff1,ff2,freezeLevel);
    col = mix (col, foamCol, foamFac);
  }

  if (dstWindow <= dstNear) {
    float uwc = clamp(noiseVal,0.2,1.);
    float d = clamp(1.+rd.y*1.14,0.,1.); d = d * d;
    col = 0.85 * mix(uwc*SeaFloorCol(rd),vec3(cloudBaseColor),d);
  }

  // icy fog and choking smoke, at appropriate times.
  if (freezeLevel > 0.0) col = mix(col,vec3(0.75),freezeLevel * doIcewind(rd));
  if (steamLevel > 0.0) {
    float stm = doSteam(rd);
    col = mix(col,sunColor-stm*0.1,steamLevel * stm);
  }

  return col;
}

float snowLayer(vec2 uv,float scale) {
  float w = smoothstep(1., 0., -uv.y*(scale/4.));

  uv += (vec2(5.,4.) * tCur) /scale;

  uv.x += (sin(uv.y+tCur*.31))/scale + (0.4 * wind/scale);
  uv *= scale;

  vec2 s = floor(uv);
  vec2 f=fract(uv);

  vec2 p = .3+.35*sin(7.*fract(sin((s+scale)* mat2(3,3,6,5))*5.))-f;

  float d = length(p);
  float k = min(d,3.0);

  k = smoothstep(0.,k,sin((f.x+f.y)*snow)* .03);

  return k*w/scale*1.25*wind;
}

// add several layers of flying snow to the scene
float doSnowstorm(vec2 uv) {
  return snowLayer(uv,20.) +
  snowLayer(uv,15.) +
  snowLayer(uv,10.) +
  snowLayer(uv,7.) +
  snowLayer(uv,5.);
}

// Excellent water droplet effect from:
// "The Drive Home" by Martijn Steinrucken aka BigWings/CountFrolic - 2017
// https://www.shadertoy.com/view/MdfBRX
//
// Returns coordinate offset caused by water droplet refraction
vec2 GetDrops(vec2 uv, float seed) {

  float t = tCur + 30.;
  vec2 o = vec2(0.);

  uv.y += t*.05;

  uv *= vec2(10., 2.5) * dropletSpacing;
  vec2 id = floor(uv);
  vec2 bd = fract(uv);

  vec3 n = N31((id.x + id.y+seed) * 1546.3524);

  bd -= 0.5;
  bd.y *= 4.;

  // drop horizontal spacing interval.
  // larger == bigger spaces == less rain.
  bd.x += (n.x-.5)*rain;

  t += n.z * 6.28;

  float ts = 1.5;
  vec2 trailPos = vec2(bd.x*ts, (fract(bd.y*ts*2.-t*2.)-.5)*.5);

  // make drops slide down at changing rates
  bd.y += 2.*wind*(-0.5+AsymmetricSawtooth(t,2.,4.));

  float d = length(bd);                            // distance to main drop

  float trailMask = smoothstep(-.2, .2, bd.y);     // mask out drops that are below the main
  trailMask *= bd.y;                               // fade dropsize
  float td = length(trailPos*max(.5, trailMask));  // distance to trail drops

  float mainDrop = smoothstep(.2, 0., d);
  float dropTrail = smoothstep(.1, .02, td);

  dropTrail *= trailMask;
  o = mix(bd*mainDrop, trailPos, dropTrail);        // mix main drop and drop trail

  return o;
}

// adjust angle of ray of origin to add water droplet effect
vec2 doWaterDroplets(vec2 uv) {
  // stack up a few raindrops at various speeds
  vec2 offs = GetDrops(uv, 1.)+
  GetDrops(uv*1.4, 10.) +
  GetDrops(uv*2.4, 25.);

  // Fade drops towards bottom of display
  float fade = 0.5+uv.y; fade *= fade;
  offs *= fade;
  return offs;
}

// Color Space Conversions
vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
  mat3 vuMat;
  vec3 col, ro, rd;
  vec2 vEl, vAz;
  float el, az, zmFac, a,viewY;

  // scale pixel coords to range (-1,1), and correct for aspect ratio
  vec2 uv = 2. * fragCoord.xy / iResolution.xy - 1.;
  uv.x *= iResolution.x / iResolution.y;

  tCur = iTime;
  waveTime = iTime;

  cloudBaseColor = 0.5 * vec3 (0.25, 0.5, 0.7);
  groundBaseColor = vec3 (0.05, 0.08, 0.05);

  sunColor = vec3(0.5) + 0.3 * iColorRGB; //hsv2rgb(vec3(abs(uv.x),0.8,1.0));
  waveHeight = 0.677;

  // light dispersion factors for clouds and water
  cloudDisp = 10. * tCur * vec3 (1., 0., 1.);
  waterDisp = 0.2 * waveTime * vec3 (-1., 0., 1.);

  // where is the sun?  Right here!
  sunDir = normalize (vec3 (0.2, 0.5, 0.5));
  fCloud = 0.5 + 0.2 * sin(0.022 * twoPi * tCur);

  // set up camera angle
  float cameraHeight = 0.15;
  az = 0.01; // * tCur;
  zmFac = iScale;
  el = 0.025 * pi;

  // set up camera vectors, eye position, and viewport transform matrix
  vEl = vec2 (cos (el), sin (el));
  vAz = vec2 (cos (az), sin (az));
  rd = normalize (vec3 (uv, zmFac));

  vuMat = mat3 (1., 0., 0., 0., vEl.x, - vEl.y, 0., vEl.y, vEl.x) *
  mat3 (vAz.x, 0., vAz.y, 0., 1., 0., - vAz.y, 0., vAz.x);
  rd = rd * vuMat;
  ro = vec3 (0., 0., -20.) * vuMat;

  // set initial camera height
  ro.y += cameraHeight;

  // add raindrop effect to display window
  //rd.xy -= doWaterDroplets(uv);

  // raymarch the wavescape
  col = ShowScene (ro, rd);

  // add snowstorm if needed
  //if (snow > 0.0) col += doSnowstorm(uv);

  // gamma correct, and handle fade transition
  fragColor = vec4(pow(clamp(col, 0., 1.),vec3(0.6)), 1.);
}
