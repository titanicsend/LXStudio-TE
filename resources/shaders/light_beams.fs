// Adapted for TE from:
// http://bit.ly/supersinfulsilicon
// @Carandiru on twitter - follow me!

const float PI = 3.1415924;
const float TAU = PI * 2.0;
const float QUARTER_WAVE = PI / 2.0;

// build 2D rotation matrix
mat2 rotate(float a) {
    return mat2(cos(a), -sin(a), sin(a), cos(a));
}

float wave(float n) {
    return 0.5 + 0.5 * sin(-QUARTER_WAVE + TAU * fract(n));
}

void pMod1(inout float p, float size) {
    float halfsize = size * 0.5;
    p = mod(p + halfsize, size) - halfsize;
}

void pMod3(inout vec3 p, vec3 size) {
    p = mod(p - size * 0.5, size) - size * 0.5;
}

float sphere(vec3 p, float radius) {
    return length(p) - radius;
}

float map(vec3 p) {
    vec3 q = p;
    vec3 qa = p;
    pMod3(q, vec3(0.8, 1., 0.23));
    pMod3(qa, vec3(0.8, 1., 0.18));
    pMod1(p.x, 1.);

    float s1 = sphere(p, 0.75);
    float s2 = sphere(q, 0.5);
    float s3 = sphere(qa, 0.555);

    float df1 = min(min(s1, s2), s3); // Union

    return df1;
}

float trace(vec3 origin, vec3 r) {
    float t = 0.0;
    for (int i = 0; i < 64; ++i) {
        vec3 p = origin + r * t;
        float d = map(p);
        t += d * 0.22;
    }
    return t;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    uv = uv * 2. - 1.; // Remap the space to -1. to 1.
    uv *= rotate(-iRotationAngle);
    uv.x *= iResolution.x / iResolution.y;

    float FOV = iScale;
    vec3 ray = normalize(vec3(uv, FOV));
    vec3 origin = vec3(iTime, 0.0, 0.0);

    float t = trace(origin, ray);

    float expFog = 1.5 / (t * t * 0.95);
    float bri = (t <= 0.0) ? 3.25 * exp(expFog + 4.5 / t) : 0.0;

    // Wow2 controls the mix of foreground color vs. gradient
    vec3 col = bri * mix(iColorRGB, mix(iColor2RGB, iColorRGB,wave(bri * 2.0)), iWow2);

    fragColor = vec4(col, 1.);
}