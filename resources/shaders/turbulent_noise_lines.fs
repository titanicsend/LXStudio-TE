// override the default x/y offset control behavior
#define TE_NOTRANSLATE

const float PI = 3.1415926;
const float TAU = 2.0 * PI;

// circle function from nimitz @ ShaderToy
// returns distance from a point to a cirular pulse
float circle(vec2 p) {
    float r = length(p);
    r = log(sqrt(r));
    return abs(mod(r * 4., TAU) - PI) * 3. + .5;
}

// build 2D rotation matrix
mat2 rotate2D(float a) {
    return mat2(cos(a), -sin(a), sin(a), cos(a));
}

float hash(vec2 uv) {
    return fract(12345. * sin(dot(uv, vec2(12.34, 56.78))));
}

// simple value noise generator
float noise(vec2 uv) {
    vec2 f = fract(uv);
    f = f * f * (3. - 2. * f);
    vec2 p = floor(uv);
    float res = mix(mix(hash(p), hash(p + vec2(1., 0.)), f.x),
                    mix(hash(p + vec2(0., 1.)), hash(p + vec2(1., 1.)), f.x),
                    f.y);
    return res;
}

// several octaves of value noise make a cloudy-looking turbulent field
float turbulenceNoise(vec2 uv) {
    float k = 4.0;

    //uv = rotate2D(k + 0.00001 * iTime) * uv;
    float res = 0.;
    float c = 0.5;

    for (int i = 0; i < 8; i++) {
        uv -= iTranslate  * 0.2;
        uv = rotate2D(k + 0.00001 * iTime) * k * uv;
        res += c * noise(uv);
        c *= 0.5;
    }

    return res;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {

    // normalize, scale, rotate
    vec2 uv = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.x;
    uv *= iScale;
    uv = uv * rotate2D(-iRotationAngle);

    float res = turbulenceNoise(uv);
    uv = rotate2D(1.65 * noise(uv * 5. + 0.1 * iTime)) * uv;

    // Quantity controls the density of the lines derived from the noise field
    float line = smoothstep(0., 1., abs(res + sin(TAU * res + iQuantity * uv.x)));
    line = smoothstep(0., 1., line);

    // Wow Trigger runs the TE special dual ring pulse generator, which draws only on
    // the wavy lines (and not on the background fog.)
    if (iWowTrigger) {
        uv /= exp(beat * PI);
        line *= max(1.0, 0.75 / pow(abs(2.1 - circle(uv)),0.75));
    }

    // Wow1 controls the mix of lines vs. noise field background
    float bri = mix(1.5 * res, line, iWow1);
    // Wow2 controls the mix of foreground color vs. gradient
    vec3 col = bri * mix(iColorRGB, mix(iColor2RGB, iColorRGB,smoothstep(0.1,0.8, bri * bri)), iWow2);

    fragColor = vec4(col, 1.);
}
