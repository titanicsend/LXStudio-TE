#define TE_NOPOSTPROCESSING
#define TE_EFFECTSHADER

#define PI 3.14159265359

// Input texture from the preceding pattern or effect
uniform sampler2D iDst;

// Uniforms for controlling the effect (set from Java class)
uniform float size;       // Size of pixels (higher = more pixelated)
uniform float separation; // Amount of RGB channel separation
uniform float speed;      // Speed of RGB animation

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 res = iResolution;

    // Smooth transition for pixelation - gradually increase from 1.0 to size
    float smoothSize = smoothstep(0.0, 2.0, size);
    float blockSize = mix(1.0, max(1.0, floor(size + 0.5)), smoothSize);

    // Anchor the pixel grid to texel centers so it never slides as size changes
    vec2 posCentered    = fragCoord - 0.5;
    vec2 blockBaseCent  = floor(posCentered / blockSize) * blockSize;
    vec2 blockCenter    = blockBaseCent + 0.5;

    // Clamp & fetch via indirection map at the block center
    vec2  clampedCenter = clamp(blockCenter, vec2(0.5), res - vec2(0.5));
    vec4  sampledBlock  = _getMappedPixel(iDst, ivec2(clampedCenter));

    // Also sample the current pixel for smooth transition
    vec4  sampledCurrent = _getMappedPixel(iDst, ivec2(fragCoord));

    // Blend between original and pixelated based on smoothSize
    vec4 sampled = mix(sampledCurrent, sampledBlock, smoothSize);
    vec3  col = sampled.rgb;
    float alpha = sampledCurrent.a; // Always use current pixel's alpha

    // --- RGB channel separation with circular rotation ---
    float smoothSeparation = smoothstep(0.0, 0.15, separation);

    if (smoothSeparation > 0.0) {
        // Fractional pos inside current block, stable for all pixels in the block
        vec2 blockUV = (posCentered - blockBaseCent) / blockSize;  // 0..1

        // Calculate rotation angle based on speed
        float angle = iTime * speed;
        float sp = 2.0 * PI / 3.0; // 120 degrees between R, G, B

        // Each color channel rotates on a circle, separated by 120 degrees
        float circleRadius = smoothSeparation * 0.3;

        vec2 c1 = vec2(0.5) + circleRadius * vec2(sin(angle), cos(angle));
        vec2 c2 = vec2(0.5) + circleRadius * vec2(sin(angle + sp), cos(angle + sp));
        vec2 c3 = vec2(0.5) + circleRadius * vec2(sin(angle + 2.0*sp), cos(angle + 2.0*sp));

        // When separation is 0, radius should be large (full coverage = no effect)
        // When separation increases, radius shrinks to show the separated channels
        float baseRadius = 1.0;
        float minRadius = 0.25;
        float radius = mix(baseRadius, minRadius, smoothSeparation);
        float softK  = 0.15;

        // Calculate masks for each channel
        float m1 = 1.0 - smoothstep(radius - softK, radius + softK, length(blockUV - c1));
        float m2 = 1.0 - smoothstep(radius - softK, radius + softK, length(blockUV - c2));
        float m3 = 1.0 - smoothstep(radius - softK, radius + softK, length(blockUV - c3));

        // Apply the masks with smooth fade-in
        col.r *= mix(1.0, m1, smoothSeparation);
        col.g *= mix(1.0, m2, smoothSeparation);
        col.b *= mix(1.0, m3, smoothSeparation);
    }

    fragColor = vec4(col, alpha);
}