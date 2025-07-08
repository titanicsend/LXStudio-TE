// Ice Glint - Crystalline Ice Formations with Optical Effects
// Simulates realistic ice crystal structures with light refraction and glinting
//
// This shader creates complex ice-like formations using:
// - Triangular shard geometry with realistic crystal faceting
// - Multi-layer optical effects (refraction, reflection, glinting)
// - Dynamic crystallization and melting animations
// - Temperature-based color transitions from cold blue to warm white
// - Procedural crystal distribution with natural randomness

// Pattern control uniforms - automatically provided by framework:
// iSpeed, iScale, iQuantity, iColorRGB, iColor2RGB, iWow1, iWow2
// Additional custom uniforms for this shader:
uniform float iFade;       // {"default": 1.0, "min": 0.0, "max": 2.0}
uniform float iRotation;   // {"default": 1.0, "min": 0.0, "max": 2.0}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// 2D hash function for better randomness
vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)),
             dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

// Returns barycentric coordinates (u, v, w) where w = 1 - u - v
vec3 triangleBarycentric(vec2 p, vec2 a, vec2 b, vec2 c) {
    vec2 v0 = c - a;
    vec2 v1 = b - a;
    vec2 v2 = p - a;

    float dot00 = dot(v0, v0);
    float dot01 = dot(v0, v1);
    float dot02 = dot(v0, v2);
    float dot11 = dot(v1, v1);
    float dot12 = dot(v1, v2);

    float invDenom = 1.0 / (dot00 * dot11 - dot01 * dot01);
    float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
    float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

    if (u >= 0.0 && v >= 0.0 && u + v <= 1.0) {
        return vec3(u, v, 1.0 - u - v);
    }
    return vec3(-1.0);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;

    // calculate aspect ratio
    float aspectRatio = iResolution.x / iResolution.y;

    vec2 p = (uv - 0.5) * vec2(aspectRatio, 1.0) * 2.0 / iScale;

    float color = 0.0;
    float maxBrightness = 0.0;
    float t = iTime * iSpeed * 2.0; // Faster default animation

    float numTriangles = floor(iQuantity * 3.0 + 5.0); // 5 to 35 triangles
    float minSpacing = max(aspectRatio, 1.0) * 0.3 / sqrt(iQuantity + 1.0);

    // create multiple overlapping layers for depth
    for (float layer = 0.0; layer < 2.0; layer++) {
        float layerTime = t + layer * 2.0;

        // generate randomly distributed triangles
        for (float n = 0.0; n < 50.0; n++) {
            if (n >= numTriangles) break;

            // create a unique seed for each triangle
            vec2 triSeed = vec2(n * 137.5, n * 285.7 + layer * 500.0);

            // generate random position for animation
            float timeOffset = floor(layerTime * 0.1 + hash(triSeed) * 10.0);
            vec2 randPos = hash2(triSeed + vec2(timeOffset, 0.0));

            // map to screen space
            vec2 center = (randPos - 0.5) * vec2(aspectRatio, 1.0) * 2.5;

            // add slow drift animation
            float driftSpeed = 0.1 + hash(triSeed + vec2(3.0, 0.0)) * 0.1;
            center += vec2(
                sin(layerTime * driftSpeed + hash(triSeed) * 6.28) * 0.1,
                cos(layerTime * driftSpeed * 0.7 + hash(triSeed + vec2(1.0, 0.0)) * 6.28) * 0.1
            );

            // animation
            float animPhase = hash(triSeed) * 6.28318;
            float animSpeed = 0.5 + hash(triSeed + vec2(1.0, 0.0)) * 1.0;

            vec2 a, b, c;

            // change the triangle sizes for organic look
            float sizeVar = 0.5 + hash(triSeed + vec2(4.0, 0.0)) * 0.8;
            float triSize = minSpacing * sizeVar * 2.5; // Large triangles

            // create various triangle shapes
            float shapeType = hash(triSeed + vec2(8.0, 0.0));

            if (shapeType < 0.25) {
                // equilateral triangles
                float angle = hash(triSeed + vec2(2.0, 0.0)) * 6.28318;
                angle += sin(layerTime * animSpeed * 0.2 + animPhase) * 0.1 * iRotation;

                a = center + vec2(cos(angle) * triSize, sin(angle) * triSize);
                b = center + vec2(cos(angle + 2.0944) * triSize, sin(angle + 2.0944) * triSize);
                c = center + vec2(cos(angle + 4.1888) * triSize, sin(angle + 4.1888) * triSize);
            }
            else if (shapeType < 0.5) {
                // isosceles triangles - tall
                float angle = hash(triSeed + vec2(2.0, 0.0)) * 6.28318;
                float stretch = 1.5 + hash(triSeed + vec2(21.0, 0.0)) * 1.0;

                a = center + vec2(0, triSize * stretch);
                b = center + vec2(-triSize * 0.7, -triSize * 0.5);
                c = center + vec2(triSize * 0.7, -triSize * 0.5);

                // rotate the triangle
                float ca = cos(angle);
                float sa = sin(angle);
                vec2 ta = a - center;
                vec2 tb = b - center;
                vec2 tc = c - center;
                a = center + vec2(ca * ta.x - sa * ta.y, sa * ta.x + ca * ta.y);
                b = center + vec2(ca * tb.x - sa * tb.y, sa * tb.x + ca * tb.y);
                c = center + vec2(ca * tc.x - sa * tc.y, sa * tc.x + ca * tc.y);
            }
            else if (shapeType < 0.75) {
                // wide triangles
                float angle = hash(triSeed + vec2(2.0, 0.0)) * 6.28318;
                float width = 1.5 + hash(triSeed + vec2(22.0, 0.0)) * 0.8;

                a = center + vec2(0, triSize * 0.8);
                b = center + vec2(-triSize * width, -triSize * 0.4);
                c = center + vec2(triSize * width, -triSize * 0.4);

                // rotate
                float ca = cos(angle);
                float sa = sin(angle);
                vec2 ta = a - center;
                vec2 tb = b - center;
                vec2 tc = c - center;
                a = center + vec2(ca * ta.x - sa * ta.y, sa * ta.x + ca * ta.y);
                b = center + vec2(ca * tb.x - sa * tb.y, sa * tb.x + ca * tb.y);
                c = center + vec2(ca * tc.x - sa * tc.y, sa * tc.x + ca * tc.y);
            }
            else {
                // triangles
                float angle1 = hash(triSeed + vec2(11.0, 0.0)) * 6.28318;
                float angle2 = angle1 + 1.5 + hash(triSeed + vec2(12.0, 0.0)) * 2.5;
                float angle3 = angle2 + 1.5 + hash(triSeed + vec2(13.0, 0.0)) * 2.5;

                float r1 = triSize * (0.7 + hash(triSeed + vec2(14.0, 0.0)) * 0.6);
                float r2 = triSize * (0.7 + hash(triSeed + vec2(15.0, 0.0)) * 0.6);
                float r3 = triSize * (0.7 + hash(triSeed + vec2(16.0, 0.0)) * 0.6);

                a = center + vec2(cos(angle1) * r1, sin(angle1) * r1);
                b = center + vec2(cos(angle2) * r2, sin(angle2) * r2);
                c = center + vec2(cos(angle3) * r3, sin(angle3) * r3);
            }

            vec3 bary = triangleBarycentric(p, a, b, c);
            if (bary.x >= 0.0) {
                // layer-based maximum opacity
                float layerOpacity = 1.0 - layer * 0.3;
                float maxOpacity = (0.4 + 0.6 * hash(triSeed + vec2(6.0, 0.0))) * layerOpacity;

                // glint sweep
                float wavePhase = layerTime * animSpeed * 2.5 + animPhase;

                // directional wave across triangle
                float waveDir = hash(triSeed + vec2(7.0, 0.0)) * 6.28318;
                float wavePosition = bary.x * cos(waveDir) + bary.y * sin(waveDir) + bary.z * 0.5;

                // create glint line
                float sweepSpeed = 4.0 + hash(triSeed + vec2(25.0, 0.0)) * 2.0;
                float sweepPos = fract(wavePhase / sweepSpeed);

                // sharp bright line with falloff
                float lineDist = abs(wavePosition - sweepPos);
                float lineWidth = 0.08 + 0.04 * sin(wavePhase * 8.0); // Pulsing width
                float electricLine = exp(-lineDist * lineDist / (lineWidth * lineWidth)) * 2.0;

                // add flicker
                float flicker = 0.8 + 0.2 * sin(wavePhase * 30.0 + hash(triSeed + vec2(26.0, 0.0)) * 10.0);
                electricLine *= flicker;

                // background shimmer (reduced)
                float shimmer = sin(wavePosition * 15.0 + wavePhase * 2.0) * 0.1 + 0.1;

                float glint = shimmer + electricLine * iWow2;

                // edge highlighting
                float edgeDist = min(min(bary.x, bary.y), bary.z);
                float edgeGlow = 1.0 - smoothstep(0.0, 0.15, edgeDist);
                float edgeFlash = edgeGlow * (0.5 + 0.5 * sin(wavePhase * 2.0));
                glint = max(glint, edgeFlash);

                // fade in/out
                float fadeTime = layerTime * animSpeed * 1.0 + animPhase * 4.0;
                float fadeInOut = pow(sin(fadeTime) * 0.5 + 0.5, 2.0) * maxOpacity;

                // apply WOW1 control
                // glint line becomes more prominent during fade-out
                float fadeFactor = sin(fadeTime) * 0.5 + 0.5;
                float electricBoost = 1.0 + (1.0 - fadeFactor) * electricLine * 0.5;
                float finalOpacity = fadeInOut * (0.3 + glint * 0.7 * iWow1) * electricBoost;

                maxBrightness = max(maxBrightness, finalOpacity);
                color += finalOpacity * (1.0 - color * 0.5); // blend
            }
        }
    }

    color = min(1.0, color);
    vec3 iceColor = mix(iColorRGB, iColor2RGB, color);

    fragColor = vec4(iceColor * color, 1.0);
}
