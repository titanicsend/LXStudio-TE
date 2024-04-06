// override the default x/y offset control behavior
#define TE_NOTRANSLATE
uniform float freqShift;

const float PI = 3.1415926;
const float TAU = 2.0 * PI;

float T;

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
// higher bassLevel increases the amount of "dirt" in the noise field
float turbulenceNoise(vec2 uv) {
    float dirt = bassLevel * levelReact * 0.3;
    float curve = 4.0 * max(0.0005,freqShift);
    float k = 4.0;


    float res = 0.;
    float c = 0.5 + dirt;

    for (int i = 0; i < 8; i++) {
        uv -= iTranslate * 0.25 + curve;
        uv = rotate2D(curve + k + 0.00001 * iTime) * k * uv;
        res += c * noise(uv);;
        c *= 0.5 + dirt;
        dirt *= 0.5;
        curve *= 0.5;
    }

    return res;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // normalize, scale, rotate
    vec2 uv = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.x;

    vec2 warp = vec2(60,-60) * bassLevel;
    float warpAmt = levelReact * 0.02;
    uv += warpAmt * sin(uv * warp);

    uv *= iScale;
    uv = uv * rotate2D(-iRotationAngle);

    float res = turbulenceNoise(uv);
    uv = rotate2D(1.65 * noise(uv * 5. + 0.1 * iTime)) * uv;

    // Quantity controls the density of the lines derived from the noise field
    // Volume ratio alters the number of lines drawn
    float lineDensity = iQuantity + 40. * freqShift;
    float line = smoothstep(0., 1., abs(res + sin(TAU * res + lineDensity * (uv.x+uv.y))));
    line = smoothstep(0., 1., line);

    // Wow Trigger runs the TE special dual ring pulse generator, which draws only on
    // the wavy lines (and not on the background fog.)
    if (iWowTrigger) {
        uv /= exp(beat * PI);
        line *= min(1.0, 0.75 / pow(abs(2.1 - circle(uv)),0.75));
    }

    // Wow1 controls the mix of lines vs. noise field background
    float bri = mix(res, line, iWow1);
    // Wow2 controls the mix of foreground color vs. gradient
    vec3 col = bri * mix(iColorRGB, mix(iColor2RGB, iColorRGB,smoothstep(0.1,0.8, bri * bri)), iWow2);

    fragColor = vec4(col, 1.);
}
