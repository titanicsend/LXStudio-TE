#define MAX_LINE_COUNT 70
uniform int lineCount;
uniform vec4[MAX_LINE_COUNT] lines;

const float MarchDumping = 1.0;
const float Far = 62.82;
const int MaxSteps = 32;
const float FOV = 0.4;
const vec3 Eye = vec3(0.14, 0.0, 3.4999998);
const vec3 Direction = vec3(0.0, 0.0, -1.0);
const vec3 Up = vec3(0.0, 1.0, 0.0);

// Noise settings:
const float Power = 5.059;
const float MaxLength = 0.9904;
const float Dumping = 10.0;

#define PI 3.141592
#define HALF_PI 1.57079632679

const float DEG_TO_RAD = PI / 180.0;
const float TIME_FACTOR = 0.3;
const float ROTATION_DIST = 16.0;

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

float sdBox(vec3 p, vec3 b) {
    vec3 d = abs(p) - b;
    return min(max(d.x, max(d.y, d.z)), 0.0) + length(max(d, 0.0));
}

vec3 rotateY(vec3 p, float a) {
    float sa = sin(a);
    float ca = cos(a);
    return vec3(ca * p.x + sa * p.z, p.y, ca * p.z - sa * p.x);
}

float getAngle(float x) {
    return ((1.0 - x) * 100.0 - 15.0) * DEG_TO_RAD;
}


float map(vec3 p) {
    vec3 q = p;;
    return sdBox(q, vec3(1.0, 1.0, 0.0001));
}

vec2 castRay(vec3 ro, vec3 rd) {
    float tmin = 0.0;
    float tmax = Far;

    float precis = 0.002;
    float t = tmin;
    float m = -1.0;

    for (int i = 0; i < MaxSteps; i++) {
        float res = map(ro + rd * t);
        if (res < precis || t > tmax) {
            break;
        }
        t += res * MarchDumping;
        m = 1.0;
    }

    if (t > tmax) {
        m = -1.0;
    }
    return vec2(t, m);
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
    vec3 coord = vec3(p, iTime * 0.25);
    float n = abs(noise(coord));
    n += 0.5 * abs(noise(coord * 2.0));
    n += 0.25 * abs(noise(coord * 4.0));
    n += 0.125 * abs(noise(coord * 8.0));

    n *= (100.001 - Power);
    float dist = distToFireLines(p);
    float k = normalizeScalar(dist, MaxLength);
    n *= dist / pow(1.001 - k, Dumping);

    vec3 col = vec3(1.0, 0.25, 0.08) / n;
    return pow(col, vec3(2.0));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // Normalized coordinates: -1 to 1, aspect-corrected
    vec2 q = fragCoord.xy / iResolution.xy;
    vec2 coord = 2.0 * q - 1.0;
    coord.x *= iResolution.x / iResolution.y;
    // Optionally scale/shift as desired for your effect

    vec3 col = color(coord);
    col = pow(col, vec3(0.4545)); // gamma correction

    fragColor = vec4(col, 1.0);
}
