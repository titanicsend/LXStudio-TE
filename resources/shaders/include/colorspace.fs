// Color Space Conversions and Blending

// convert RGB to HSV
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// convert HSV to RGB
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// from IQ's oklab shader
// Takes two RGB colors, performs linear interpolation between them in the the Oklab
// color space and returns an RGB color that is a mix of the two.
//
// Oklab is a perceptually uniform color space, so the interpolation should have
// a very even appearance with minimal color banding and uniform brightness
//
// Original oklab paper by Björn Ottosson: https://bottosson.github.io/posts/oklab
// LMS (Long, Medium, Short) color space: https://en.wikipedia.org/wiki/LMS_color_space
vec3 oklab_mix( vec3 colA, vec3 colB, float h ) {
    const mat3 kCONEtoLMS = mat3(
    0.4121656120,  0.2118591070,  0.0883097947,
    0.5362752080,  0.6807189584,  0.2818474174,
    0.0514575653,  0.1074065790,  0.6302613616);
    const mat3 kLMStoCONE = mat3(
    4.0767245293, -1.2681437731, -0.0041119885,
    -3.3072168827,  2.6093323231, -0.7034763098,
    0.2307590544, -0.3411344290,  1.7068625689);

    // rgb to oklab transfer function
    vec3 lmsA = pow( kCONEtoLMS*colA, vec3(1.0/3.0));
    vec3 lmsB = pow( kCONEtoLMS*colB, vec3(1.0/3.0));

    // interpolate in oklab color space
    vec3 lms = mix( lmsA, lmsB, h );

    // now back to rgb
    return kLMStoCONE*(lms*lms*lms);
}

// Shortest arc distance HSV color interpolator. Roughly
// equivalent to what Chromatik is doing in Java.
// Takes two RGB colors, performs linear interpolation in HSV
// color space and returns an RGB color that is a mix of the two.
vec3 hsv_mix(vec3 colA,vec3 colB, float h) {
    const float black_threshold = 0.005;
    vec3 hsv1 = rgb2hsv(colA);
    vec3 hsv2 = rgb2hsv(colB);

    // if either color is black, we can just do a straight mix on
    // the rgb color, avoiding complication due to black's undefined hue
    // and saturation in the hsv color space.
    if (hsv1.z < black_threshold || hsv2.z < black_threshold) {
        return mix(colA, colB, h);
    }

    // otherwise, interpolate the hue along shortest arc of the color wheel
    float hue = (mod(mod((hsv2.x-hsv1.x), 1.) + 1.5, 1.)-0.5)*h + hsv1.x;
    return hsv2rgb(vec3(hue, mix(hsv1.yz, hsv2.yz, h)));
}

// Given a target value in the range 0.0 to 1.0 denoting a position in the
// current palette, interpolate in hsv color space (using the shortest hue
// distance, and checking for black) and return the resulting color as an
// RGB vec3.
vec3 getPaletteColor_hsv(float h) {
    float fIndex = fract(h) * (iPaletteSize - 1.0);
    int index = int(fIndex);
    return hsv_mix(iPalette[index], iPalette[index + 1], fract(fIndex));
}

// Given a target value in the range 0.0 to 1.0 denoting a position in the
// current palette, interpolate in hsv color space and return the resulting
// color as an RGB vec3.
vec3 getPaletteColor_oklab(float h) {
    float fIndex = fract(h) * (iPaletteSize - 1.0);
    int index = int(fIndex);
    return oklab_mix(iPalette[index], iPalette[index + 1], fract(fIndex));
}


