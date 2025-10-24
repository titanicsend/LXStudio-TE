// Hue Orbit post-effect (TE)
// Rotate hue in HSL; optional lightness preservation and saturation gain.
#define TE_EFFECTSHADER

uniform sampler2D iDst;

uniform float angle;   // radians
uniform float mixAmt;  // 0..1
uniform int   keepL;   // 0/1
uniform float satGain; // 0..2

float hue2rgb(float p, float q, float t) {
    if (t < 0.0) t += 1.0;
    if (t > 1.0) t -= 1.0;
    if (t < 1.0 / 6.0) return p + (q - p) * 6.0 * t;
    if (t < 1.0 / 2.0) return q;
    if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6.0;
    return p;
}

vec3 hsl2rgb(vec3 hsl) {
    float h = hsl.x;
    float s = hsl.y;
    float l = hsl.z;
    if (s <= 1e-6) {
        return vec3(l);
    }
    float q = (l < 0.5) ? (l * (1.0 + s)) : (l + s - l * s);
    float p = 2.0 * l - q;
    return vec3(
        hue2rgb(p, q, h + 1.0 / 3.0),
        hue2rgb(p, q, h),
        hue2rgb(p, q, h - 1.0 / 3.0)
    );
}

vec3 rgb2hsl(vec3 c) {
    float maxc = max(max(c.r, c.g), c.b);
    float minc = min(min(c.r, c.g), c.b);
    float l = 0.5 * (maxc + minc);

    float h = 0.0;
    float s = 0.0;

    float d = maxc - minc;
    if (d > 1e-6) {
        s = (l > 0.5) ? (d / (2.0 - maxc - minc)) : (d / (maxc + minc));
        if (maxc == c.r) {
            h = (c.g - c.b) / d + (c.g < c.b ? 6.0 : 0.0);
        } else if (maxc == c.g) {
            h = (c.b - c.r) / d + 2.0;
        } else {
            h = (c.r - c.g) / d + 4.0;
        }
        h /= 6.0;
    }
    return vec3(h, s, l);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    ivec2 P = ivec2(gl_FragCoord.xy);
    vec4 src = texelFetch(iDst, P, 0);

    vec3 hsl = rgb2hsl(src.rgb);

    float rot = angle / 6.28318530718; // radians -> turns
    float oldL = hsl.z;

    hsl.x = fract(hsl.x + rot);
    hsl.y = clamp(hsl.y * satGain, 0.0, 1.0);
    if (keepL == 1) {
        hsl.z = oldL;
    }

    vec3 rotated = hsl2rgb(hsl);

    vec3 outRGB = mix(src.rgb, rotated, clamp(mixAmt, 0.0, 1.0));
    fragColor = vec4(outRGB, src.a);
}
