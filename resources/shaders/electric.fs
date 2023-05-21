//fork of https://www.shadertoy.com/view/3sXSD2

const float PI = 3.1415926;

mat2 r2d(in float rad) {
    float c = cos(rad);
    float s = sin(rad);
    return mat2(vec2(c, s), vec2(-s, c));
}

// using a slightly adapted implementation of iq's simplex noise from
// https://www.shadertoy.com/view/Msf3WH with hash(), noise() and fbm()
//
// iQuantity sets noise field amplitude
vec2 hash(in vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)),
             dot(p, vec2(269.5, 183.3)));

    return -1. + (1.0 + 5.0 * iQuantity) * fract(sin(p) * 43758.5453123);
}

float noise(in vec2 p) {
    const float K1 = .366025404;
    const float K2 = .211324865;

    vec2 i = floor(p + (p.x + p.y) * K1);

    vec2 a = p - i + (i.x + i.y) * K2;
    vec2 o = step(a.yx, a.xy);
    vec2 b = a - o + K2;
    vec2 c = a - 1. + 2. * K2;

    vec3 h = max(.5 - vec3(dot(a, a), dot(b, b), dot(c, c)), 0.0);

    vec3 n = h * h * h * h * h * vec3(dot(a, hash(i)),
                                          dot(b, hash(i + o)),
                                          dot(c, hash(i + 1.)));

    return dot(n, vec3(70.));
}

float fbm(in vec2 p) {

    mat2 rot = r2d(0.4799655);  // 27.5 degrees

    float d = noise(p); p *= rot;
    d += .5 * noise(p); p *= rot;
    d += .25 * noise(p); p *= rot;
    d += .125 * noise(p); p *= rot;
    d /= (1. + .5 + .25 + .125);
    return .5 + .5 * d;
}

vec2 mapToScreen(in vec2 p, in float scale) {
    vec2 res = p;
    res = res * 2. - 1.;
    res.x *= iResolution.x / iResolution.y;
    res *= scale;
    return res;
}

vec2 cart2polar(in vec2 cart)
{
    float r = length(cart);
    float phi = atan(cart.y, cart.x);
    return vec2(r, phi);
}

vec2 polar2cart(in vec2 polar)
{
    float x = polar.x * cos(polar.y);
    float y = polar.x * sin(polar.y);
    return vec2(x, y);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = mapToScreen(fragCoord.xy / iResolution.xy, 2.5 * 1.0/max(0.4,iScale));

    uv *= r2d(iRotationAngle);
    float len = length(uv);

    // distort UVs a bit
    uv = cart2polar(uv);
    uv.y += 2.5 * iWow1 * (.5 + .5 * sin(cos(uv.x) * len));
    uv = polar2cart(uv);

    float thickness = 3.0 + iScale;
    float d1 = abs(uv.x * thickness / (uv.x + fbm(uv + 1.25 * iTime)));
    float d2 = abs(uv.y * thickness / (uv.y + fbm(uv - 1.5 * iTime)));

    float size = 0.1 + iQuantity / 2.0;

    vec3 col = clamp(iWow2 * d1 * size,0.,1.2) * iColorRGB;
    col += clamp(iWow2 * d2 * size,0.,1.2) * iColor2RGB;

    fragColor = vec4(col, 1.);
}
