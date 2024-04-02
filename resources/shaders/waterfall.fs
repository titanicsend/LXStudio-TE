// Simplex noise-based waterfall, based on https://www.shadertoy.com/view/ttlXDN
// Wow1 controls background fade level
//
#pragma name "Waterfall"
#pragma iChannel1 "resources/shaders/textures/lichen.png"
#pragma TEControl.WOW1.Range(0.4,0.0,1.0)

#define PI 3.14159265359

mat2 rot(float angle) {
    return mat2(cos(angle), -sin(angle),
    sin(angle), cos(angle));
}

// Ashima noise
// https://github.com/ashima/webgl-noise/blob/master/src/noise3Dgrad.glsl
// modified to allow rotation for water swirls.
// TODO - could probably use simpler/faster texture noise for this, if I could get
// TODO - the rotation to work properly.

// alternate the direction of rotation in a checkerboard pattern
// to make the fake swirls look a little more real
float checkersign(vec2 uv) {
    uv = floor(uv);
    return sign(mod(uv.x + uv.y, 2.) - .5);
}

vec3 mod289(vec3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 mod289(vec4 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 permute(vec4 x) {
    return mod289(((x*34.0)+1.0)*x);
}

vec4 taylorInvSqrt(vec4 r)
{
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v, out vec3 gradient, float time)
{
    const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
    const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);

    // First corner
    vec3 i  = floor(v + dot(v, C.yyy) );
    vec3 x0 =   v - i + dot(i, C.xxx) ;

    // Other corners
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min( g.xyz, l.zxy );
    vec3 i2 = max( g.xyz, l.zxy );

    //   x0 = x0 - 0.0 + 0.0 * C.xxx;
    //   x1 = x0 - i1  + 1.0 * C.xxx;
    //   x2 = x0 - i2  + 2.0 * C.xxx;
    //   x3 = x0 - 1.0 + 3.0 * C.xxx;
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy; // 2.0*C.x = 1/3 = C.y
    vec3 x3 = x0 - D.yyy;      // -1.0+3.0*C.x = -0.5 = -D.y

    // Permutations
    i = mod289(i);
    vec4 p = permute( permute( permute(
    i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
    + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))
    + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));

    // Gradients: 7x7 points over a square, mapped onto an octahedron.
    // The ring size 17*17 = 289 is close to a multiple of 49 (49*6 = 294)
    float n_ = 0.142857142857; // 1.0/7.0
    vec3  ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,7*7)

    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)

    vec4 x = x_ *ns.x + ns.yyyy;
    vec4 y = y_ *ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4( x.xy, y.xy );
    vec4 b1 = vec4( x.zw, y.zw );

    //vec4 s0 = vec4(lessThan(b0,0.0))*2.0 - 1.0;
    //vec4 s1 = vec4(lessThan(b1,0.0))*2.0 - 1.0;
    vec4 s0 = floor(b0)*2.0 + 1.0;
    vec4 s1 = floor(b1)*2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
    vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;

    vec3 p0 = vec3(a0.xy,h.x);
    vec3 p1 = vec3(a0.zw,h.y);
    vec3 p2 = vec3(a1.xy,h.z);
    vec3 p3 = vec3(a1.zw,h.w);

    //Normalise gradients
    vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    // add rotation
    x0.xy *= rot(time*checkersign(a0.xy));
    x1.xy *= rot(time*checkersign(a0.zw));
    x2.xy *= rot(time*checkersign(a1.xy));
    x3.xy *= rot(time*checkersign(a1.zw));

    // Mix final noise value
    vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    vec4 m2 = m * m;
    vec4 m4 = m2 * m2;
    vec4 pdotx = vec4(dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3));

    // Determine noise gradient
    vec4 temp = m2 * m * pdotx;
    gradient = -8.0 * (temp.x * x0 + temp.y * x1 + temp.z * x2 + temp.w * x3);
    gradient += m4.x * p0 + m4.y * p1 + m4.z * p2 + m4.w * p3;
    gradient *= 42.0;

    return 42.0 * dot(m4, pdotx);
}

// generate fade mask for edges of waterfall
float get_mask(vec2 uv, float width) {
    uv.y *= 4.;
    uv.y -= 0.5;
    uv.x *= .5;
    uv.x *= pow(uv.y, width * 0.9);
    uv.x = abs(uv.x);
    return (smoothstep(0.65, 1., uv.x) * step(0., uv.y));
}

// noise field generator
//   params.x = amplitude
//   params.y = frequency
//   params.z = rotation
float fbm(vec3 p, vec3 params) {
    float value = 0.;
    vec3 gradient = vec3(0.);
    vec3 grad;

    // 4 octaves of noise
    for (int i = 0; i < 4; i++) {
        value += params.x * snoise(params.y*p - gradient, grad, iTime*params.z);
        grad.z = 0.;
        gradient += params.x*grad*.5;
        params.y *= 2.;
        params.x *= .5;
        params.z *= 2.;
    }
    return value;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = fragCoord/iResolution.xy;
    // save a copy of the original coordinates for later
    vec2 ouv = uv;

    // center the waterfall so it's pouring down over the DJ booth
    uv.y += 0.05;
    uv.x = uv.x*2.-1.;
    uv *= 2.8;

    // get pixel from background texture
    vec3 background = texture(iChannel1, ouv).rgb;
    background *= background * iWow1;

    // add extra "splashiness" at the top of the DJ booth by
    // changing the width of the waterfall in a narrow band
    float sm = max(0.,(0.3 - abs(ouv.y - 0.45)) / 0.3);
    float splash = 0.4 - (sm * 0.35);

    // shape the waterfall so it gets generally wider as it goes down
    float mask = get_mask(uv,splash);
    vec3 p = vec3(uv, 0.);
    p.x *= pow(p.y, splash);
    p.y = pow(p.y, 0.75);

    // generate noise field for the waterfall
    vec3 noiseParams = vec3(1.25,2.5,1.75);
    float noise = fbm(p + vec3(0., iTime*.6, 0.), noiseParams);
    noise = noise*.5+.5;

    // add mist to the bottom of the waterfall with another noise field.
    float fl = max(0.,(0.3 - ouv.y)) / 0.3;
    p = vec3(ouv,0.);
    noiseParams = vec3(0.8,4.0,0.6);
    float fn = fbm(p + vec3(0., -iTime*.13, iTime *.2), noiseParams);
    fn = 0.5 + 0.5 * fn;
    vec3 mist = vec3(1.25 * fn) * fl;

    // create final blended water color, add in the mist, and done!
    vec3 col = 2. * noise * mix(iColor2RGB,iColorRGB,noise);
    col = mix(col, background, mask);
    fragColor = vec4(col + mist,1.0);
}