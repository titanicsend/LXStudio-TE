#define MAX_LINE_COUNT 70
uniform int lineCount;
uniform vec4[MAX_LINE_COUNT] lines;

// Noise settings:
const float MaxLength = .1;
const float Dumping = 10.0;

#define PI 3.141592
#define HALF_PI 1.57079632679

vec3 hash3(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
    dot(p, vec3(269.5, 183.3, 246.1)),
    dot(p, vec3(113.5, 271.9, 124.6)));

    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

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

float udSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

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
    vec3 coord =   vec3(10.0 * p, iTime * 0.25);
    float n = abs(noise(coord));
    n += 0.5 * abs(noise(coord * 2.0));
    n += 0.25 * abs(noise(coord * 4.0));
    //n += 0.125 * abs(noise(coord * 8.0));

    n *= 1000. * iWow1;
    float dist = distToFireLines(p);
    float maxDist = 0.5;
    if (dist <= 0.001) dist = abs(noise(coord)) / 20.0;

    float k = normalizeScalar(dist, maxDist);
    n *= dist / pow(1.001 - k, 15.0);

    vec3 col = vec3(1.0, 0.25, 0.08) / n;
    return pow(col, vec3(1.5));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    //vec2 coord  = _getModelCoordinates().xy;
    vec2 q = fragCoord.xy / iResolution.xy;
    vec2 coord = 2.0 * q - 1.0;

    vec3 col = color(coord);
    col = pow(col, vec3(0.5)); // gamma correction

    fragColor = vec4(col, 1.0);
}
