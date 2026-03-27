// Edge Glow / Outline post-effect (TE)
// Detect edges (Sobel on luma), blur the edge mask, add glow, and mix over source.
#define TE_EFFECTSHADER
#define TE_NOPOSTPROCESSING

uniform sampler2D iDst;

// Tunables
uniform float edgeThreshold; // 0..1
uniform float edgeGain;      // ~0.5..10
uniform float glowRadiusPx;  // 0..32
uniform float blurAmt;       // 0..1 softness control
uniform float glowStrength;  // 0..2
uniform int   edgeOnly;      // 1=only outlines, 0=overlay
uniform float mixAmt;        // 0..1 effect mix

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

vec3 sampleAt(ivec2 coord) {
    return _getMappedPixel(iDst, coord).rgb;
}

float sobelEdge(vec2 uv) {
    // uv is in [0..1] normalized coords; convert to pixel coords for sampling
    vec2 res = iResolution;
    ivec2 p = ivec2(uv * res);

    float tlL = luma(sampleAt(p + ivec2(-1, -1)));
    float tcL = luma(sampleAt(p + ivec2( 0, -1)));
    float trL = luma(sampleAt(p + ivec2( 1, -1)));
    float mlL = luma(sampleAt(p + ivec2(-1,  0)));
    float mcL = luma(sampleAt(p + ivec2( 0,  0)));
    float mrL = luma(sampleAt(p + ivec2( 1,  0)));
    float blL = luma(sampleAt(p + ivec2(-1,  1)));
    float bcL = luma(sampleAt(p + ivec2( 0,  1)));
    float brL = luma(sampleAt(p + ivec2( 1,  1)));

    float gx = (trL + 2.0 * mrL + brL) - (tlL + 2.0 * mlL + blL);
    float gy = (blL + 2.0 * bcL + brL) - (tlL + 2.0 * tcL + trL);
    float mag = sqrt(gx * gx + gy * gy);

    return clamp(edgeGain * mag - edgeThreshold, 0.0, 1.0);
}

float radialBlurMask(vec2 uv, float radiusPx, float blurSoftness) {
    if (radiusPx <= 0.5) {
        return sobelEdge(uv);
    }

    vec2 invRes = 1.0 / iResolution;

    const int DIRS = 8;
    vec2 dirs[DIRS] = vec2[](
        vec2( 0.0, -1.0), vec2( 0.7071, -0.7071),
        vec2( 1.0,  0.0), vec2( 0.7071,  0.7071),
        vec2( 0.0,  1.0), vec2(-0.7071,  0.7071),
        vec2(-1.0,  0.0), vec2(-0.7071, -0.7071)
    );

    float softness = clamp(blurSoftness, 0.0, 1.0);
    float falloff = mix(12.0, 2.0, softness);
    int steps = int(clamp(floor(radiusPx), 1.0, 12.0));

    float acc = 0.0;
    float wsum = 0.0;

    float center = sobelEdge(uv);
    acc += center;
    wsum += 1.0;

    for (int s = 1; s <= steps; ++s) {
        float t = float(s) / float(steps);
        float w = exp(-falloff * t * t);
        float distPx = t * radiusPx;
        for (int d = 0; d < DIRS; ++d) {
            vec2 sampleUV = clamp(uv + dirs[d] * (distPx * invRes), vec2(0.0), vec2(1.0));
            acc += sobelEdge(sampleUV) * w;
            wsum += w;
        }
    }

    return acc / max(wsum, 1e-5);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 res = iResolution;
    vec2 uv = fragCoord / res;
    uv = clamp(uv, vec2(0.0), vec2(1.0));

    // Sample the original pixel via the indirection map
    vec4 src = _getMappedPixel(iDst, ivec2(fragCoord));

    float mask = radialBlurMask(uv, glowRadiusPx, blurAmt);

    vec3 edgeColor = src.rgb;
    vec3 glow = edgeColor * (mask * glowStrength);
    vec3 comp = (edgeOnly == 1) ? glow : clamp(src.rgb + glow, 0.0, 1.0);

    vec3 outRGB = mix(src.rgb, comp, clamp(mixAmt, 0.0, 1.0));
    fragColor = vec4(outRGB, src.a);
}
