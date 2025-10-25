// Posterize / Solarize post-effect (TE)
// Quantizes tones; optional solarization by inverting above a luma threshold.
#define TE_EFFECTSHADER
#define TE_NOPOSTPROCESSING

uniform sampler2D iDst;

uniform int   levels;        // >= 2
uniform float mixAmt;        // 0..1
uniform int   solarize;      // 0/1
uniform float solThreshold;  // 0..1, on luma
uniform int   preserveLuma;  // 0/1

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

float quantize(float v, float n) {
    v = clamp(v, 0.0, 1.0);
    float steps = max(2.0, n);
    return floor(v * (steps - 1.0) + 0.5) / (steps - 1.0);
}

vec3 posterize(vec3 rgb, float n, bool keepLuma) {
    if (!keepLuma) {
        return vec3(quantize(rgb.r, n), quantize(rgb.g, n), quantize(rgb.b, n));
    }

    float Y = luma(rgb);
    float Yq = quantize(Y, n);
    vec3 g = vec3(Y);
    vec3 gq = vec3(Yq);
    float t = 0.5;
    vec3 approx = mix(rgb, g, t);
    float oldL = max(1e-5, luma(approx));
    return approx * (Yq / oldL);
}

vec3 solarizeOp(vec3 rgb, float threshold) {
    float Y = luma(rgb);
    vec3 inv = 1.0 - rgb;
    return (Y > threshold) ? inv : rgb;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    ivec2 P = ivec2(gl_FragCoord.xy);
    vec4 src = texelFetch(iDst, P, 0);

    vec3 work = src.rgb;

    if (solarize == 1) {
        work = solarizeOp(work, clamp(solThreshold, 0.0, 1.0));
    }

    vec3 post = posterize(work, float(levels), preserveLuma == 1);

    vec3 outRGB = mix(src.rgb, post, clamp(mixAmt, 0.0, 1.0));
    fragColor = vec4(outRGB, src.a);
}
