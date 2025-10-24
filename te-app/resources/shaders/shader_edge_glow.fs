// Edge Glow / Outline post-effect (TE)
// Detect edges (Sobel on luma), blur the edge mask, add glow, and mix over source.
#define TE_EFFECTSHADER

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

vec3 sampleOffset(vec2 uv, vec2 offset, vec2 invRes) {
    vec2 coord = clamp(uv + offset * invRes, vec2(0.0), vec2(1.0));
    return texture(iDst, coord).rgb;
}

float sobelEdge(vec2 uv, vec2 invRes) {
    vec3 tl = sampleOffset(uv, vec2(-1.0, -1.0), invRes);
    vec3 tc = sampleOffset(uv, vec2( 0.0, -1.0), invRes);
    vec3 tr = sampleOffset(uv, vec2( 1.0, -1.0), invRes);
    vec3 ml = sampleOffset(uv, vec2(-1.0,  0.0), invRes);
    vec3 mc = sampleOffset(uv, vec2( 0.0,  0.0), invRes);
    vec3 mr = sampleOffset(uv, vec2( 1.0,  0.0), invRes);
    vec3 bl = sampleOffset(uv, vec2(-1.0,  1.0), invRes);
    vec3 bc = sampleOffset(uv, vec2( 0.0,  1.0), invRes);
    vec3 br = sampleOffset(uv, vec2( 1.0,  1.0), invRes);

    float tlL = luma(tl), tcL = luma(tc), trL = luma(tr);
    float mlL = luma(ml), mcL = luma(mc), mrL = luma(mr);
    float blL = luma(bl), bcL = luma(bc), brL = luma(br);

    float gx = (trL + 2.0 * mrL + brL) - (tlL + 2.0 * mlL + blL);
    float gy = (blL + 2.0 * bcL + brL) - (tlL + 2.0 * tcL + trL);
    float mag = sqrt(gx * gx + gy * gy);

    return clamp(edgeGain * mag - edgeThreshold, 0.0, 1.0);
}

float radialBlurMask(vec2 uv, vec2 invRes, float radiusPx, float blurSoftness) {
    if (radiusPx <= 0.5) {
        return sobelEdge(uv, invRes);
    }

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

    float center = sobelEdge(uv, invRes);
    acc += center;
    wsum += 1.0;

    for (int s = 1; s <= steps; ++s) {
        float t = float(s) / float(steps);
        float w = exp(-falloff * t * t);
        float distPx = t * radiusPx;
        for (int d = 0; d < DIRS; ++d) {
            vec2 sampleUV = clamp(uv + dirs[d] * (distPx * invRes), vec2(0.0), vec2(1.0));
            acc += sobelEdge(sampleUV, invRes) * w;
            wsum += w;
        }
    }

    return acc / max(wsum, 1e-5);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 res = vec2(textureSize(iDst, 0));
    vec2 invRes = 1.0 / res;
    vec2 uv = (fragCoord + 0.5) * invRes;
    uv = clamp(uv, vec2(0.0), vec2(1.0));

    vec4 src = texture(iDst, uv);

    float mask = radialBlurMask(uv, invRes, glowRadiusPx, blurAmt);

    vec3 edgeColor = src.rgb;
    vec3 glow = edgeColor * (mask * glowStrength);
    vec3 comp = (edgeOnly == 1) ? glow : clamp(src.rgb + glow, 0.0, 1.0);

    vec3 outRGB = mix(src.rgb, comp, clamp(mixAmt, 0.0, 1.0));
    fragColor = vec4(outRGB, src.a);
}
