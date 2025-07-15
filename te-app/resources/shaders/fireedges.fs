#define MAX_LINE_COUNT 104
uniform int lineCount;
uniform vec4[MAX_LINE_COUNT] lines;

#include <include/constants.fs>
#include <include/colorspace.fs>

// Noise settings:
const float MaxLength = .1;
const float Dumping = 10.0;

vec3 hash3(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
    dot(p, vec3(269.5, 183.3, 246.1)),
    dot(p, vec3(113.5, 271.9, 124.6)));

    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

// 3D value noise function. Note: this is probably too many octaves
// for TE's resolution. Great for projection mapping though.
float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);

    vec3 u = f * f * (3.0 - 2.0 * f);

    float n0 = dot(hash3(i + vec3(0.0, 0.0, 0.0)), f - vec3(0.0, 0.0, 0.0));
    float n1 = dot(hash3(i + vec3(1.0, 0.0, 0.0)), f - vec3(1.0, 0.0, 0.0));
    float n2 = dot(hash3(i + vec3(0.0, 1.0, 0.0)), f - vec3(0.0, 1.0, 0.0));
    float n3 = dot(hash3(i + vec3(1.0, 1.0, 0.0)), f - vec3(1.0, 1.0, 0.0));
    float n4 = dot(hash3(i + vec3(0.0, 0.0, 1.0)), f - vec3(0.0, 0.0, 1.0));
    float n5 = dot(hash3(i + vec3(1.0, 0.0, 1.0)), f - vec3(1.0, 0.0, 1.0));
    float n6 = dot(hash3(i + vec3(0.0, 1.0, 1.0)), f - vec3(0.0, 1.0, 1.0));
    float n7 = dot(hash3(i + vec3(1.0, 1.0, 1.0)), f - vec3(1.0, 1.0, 1.0));

    float ix0 = mix(n0, n1, u.x);
    float ix1 = mix(n2, n3, u.x);
    float ix2 = mix(n4, n5, u.x);
    float ix3 = mix(n6, n7, u.x);

    float ret = mix(mix(ix0, ix1, u.y), mix(ix2, ix3, u.y), u.z) * 0.5 + 0.5;
    return ret * 2.0 - 1.0;
}

// SDF for line segment
float udSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

// Distance to the nearest fire line segment
float distToFireLines(vec2 p) {
    float minDist = 1e6;
    for (int i = 0; i < MAX_LINE_COUNT; i++) {
        if (i >= lineCount) break;
        vec4 line = lines[i];
        float d = udSegment(p, line.xy, line.zw);
        minDist = min(minDist, d);
    }
    return minDist;
}

float normalizeScalar(float value, float max) {
    return clamp(value, 0.0, max) / max;
}

vec3 color(vec2 p) {
    // generate animated noise field
    vec3 coord =  vec3(10.0 * p, iTime * 0.25);
    float n = abs(noise(coord));
    n += 0.5 * abs(noise(coord * 2.0));
    n += 0.25 * abs(noise(coord * 4.0));

    // save noise field value for later use
    float nv = n;

    // iQuantity controls the density of the noise field which
    // controls how much fire we see.
    n *= 1000. * iQuantity;
    float dist = distToFireLines(p);

    // if we just color the fire as a function of the inverse of distance,
    // the center lines will be pure, obnoxious white.  So we cheat a little
    // and use a noise function to decide color the center of the fire.
    // It's still "hot", but it's not full brightness white.
    if (dist <= 0.001) dist += abs(noise(coord)) / 20.0;

    float k = normalizeScalar(dist, 0.5);
    n *= dist / pow(1. - k, 15.0);

    vec3 fireColor = vec3(1.0, 0.25, 0.08) / n;

    // try not to overdrive the palette - n can get quite close
    // to zero, which makes the resulting colors um... all white.
    //
    // The default fire color uses this trick to drive its base
    // red all the way to white heat, but it's way too strong for
    // the palette colors. So we both limit the division
    // range and clamp the resulting color when we use the palette.
    //
    // iWow1 controls the allowed range of palette colors. (iWow1 = 0.0
    // just allows the first color, 1.0 uses all the colors in
    // the palette)
    vec3 color = getGradientColor(k * (4.0 * iWow1)) / max(0.9,n);
    color = min(color, vec3(1.0));

    // iWow2 controls the mix of palette-colored fire vs. fire-colored fire.
    vec3 col = mix(color, fireColor, iWow2);
    return col;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    //vec2 coord  = _getModelCoordinates().xy;
    vec2 q = fragCoord.xy / iResolution.xy;
    vec2 coord = 2.0 * q - 1.0;

    vec3 col = color(coord);
    col = pow(col, vec3(0.75)); // gamma correction

    fragColor = vec4(col, 0.995);
}
