//fork of https://www.shadertoy.com/view/MslGWN

//CBS
//Parallax scrolling fractal galaxy.
//Inspired by JoshP's Simplicity shader: https://www.shadertoy.com/view/lslGWr

#define PI 3.1415926535897932384626433832795

// TE Library routines

// normalized HSV to RGB
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// rotate (2D) a point about the specified origin by <angle> radians
vec2 rotate(vec2 point, vec2 origin, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return origin + rotationMatrix * (point - origin);
}

// rotate (2D) a point about the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
    return rotate(point,vec2(0.0), angle);
}

// http://www.fractalforums.com/new-theories-and-research/very-simple-formula-for-fractal-patterns/
float field(in vec3 p, float s, int nIter) {
    float strength = 11. + .03 * log(1.e-6 + fract(sin(iTime) * 4373.11));
    float accum = s;
    float prev = 0.;
    float tw = 0.;
    for (int i = 0; i < nIter; ++i) {
        float mag = dot(p, p);
        p = abs(p) / mag + vec3(-.5, -.4, -1.5);
        float w = exp(-float(i) / 7.);
        accum += w * exp(-strength * pow(abs(mag - prev), 4.2));
        tw += w;
        prev = mag;
    }
    return pow(clamp(5. * accum / tw - 1.7, 0.0, 1.0), 0.001 + 1.0 / iQuantity);
}

vec3 nrand3(vec2 co) {
    vec3 a = fract(cos(co.x * 8.3e-3 + co.y) * vec3(1.3e5, 4.7e5, 2.9e5));
    vec3 b = fract(sin(co.x * 0.3e-3 + co.y) * vec3(8.1e5, 1.0e5, 0.1e5));
    vec3 c = mix(a, b, 0.5);
    return c;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = 2. * fragCoord.xy / iResolution.xy - 1.;
    vec2 uvs = uv * iResolution.xy / max(iResolution.x, iResolution.y);
    uvs = rotate(uvs,iRotationAngle);  // rotate canvas
    uvs.y += 0.5;   // center noise field on vehicle
    uvs *= iScale;  // scale canvas

    // calculate movement paths for both parallax layers
    vec3 basePath = vec3(sin(iTime / 16.), sin(iTime / 12.), sin(iTime / 128.));

    vec3 p = vec3(uvs / 4., 0) + vec3(1., -1.3, 0.);
    p += .2 * basePath;

    vec3 p2 = vec3(uvs / (4. + sin(iTime * 0.11) * 0.2 + 0.2 + sin(iTime * 0.15) * 0.3 + 0.4), 1.5) + vec3(2., -1.3, -1.);
    p2 += 0.265 * basePath;

    // Sample the audio FFT results. (NOTE: TE currently supplies a 16 band fft)
    float freqs[4];
    freqs[0] = texture(iChannel0, vec2(0.065, 0.25)).x;  // eq band 1
    freqs[1] = texture(iChannel0, vec2(0.188, 0.25)).x;  // eq band 3
    freqs[2] = texture(iChannel0, vec2(0.4325, 0.25)).x; // eq band 5
    freqs[3] = texture(iChannel0, vec2(0.75, 0.25)).x;   // eq band 12

    // build noise fields for our two layers
    float t = field(p, 0.5, 26);
    float t2 = field(p2, 1.1, 26);

    // vary color by field density and frequency content
    vec3 c1 = iColorHSB;
    float k = sin(iTime / 4.0);          // color movement for hubble mode
    c1.x = mix(c1.x, t + k, iWow2);      // hue (+ hubble mode on Wow 2 control)
    c1.y -= (t * freqs[2] * freqs[2]);   // saturation (at basically vocal freqencies)
    c1.z *= t + (t * iWow1 * freqs[0]);  // brightness modulated by low frequency content

    vec3 c2 = iColor2HSB;
    k = sin(PI + iTime / 4.0);           // color movement for hubble mode
    c2.x = mix(c2.x, t2 + k, iWow2);     // hue (+ hubble mode)
    c2.y -= (t2 * freqs[3] * freqs[3]);  // saturation (on high frequency content)
    c2.z *= t2 + (t2 * iWow1 * beat);    // brightness modulated by LX beat

    // Generate two starfield layers from noise fields
    // Thanks to http://glsl.heroku.com/e#6904.0
    vec2 seed = p.xy * 1.5;
    seed = floor(seed * iResolution.x);
    vec3 rnd = nrand3(seed);
    vec3 starcolor = vec3(pow(rnd.y, 40.0));

    vec2 seed2 = p2.xy * 1.5;
    seed2 = floor(seed2 * iResolution.x);
    vec3 rnd2 = nrand3(seed2);
    starcolor += vec3(pow(rnd2.y, 40.0));

    // compute final color
    //vec3 finalColor = mix(hsv2rgb(c1), hsv2rgb(c2), min(t,t2)) + starcolor;
    //vec3 finalColor = mix(hsv2rgb(c1), hsv2rgb(c2), iWow2) + starcolor;
    vec3 finalColor = hsv2rgb(c1) + hsv2rgb(c2) + starcolor;

    fragColor = vec4(finalColor, 1.);
}
