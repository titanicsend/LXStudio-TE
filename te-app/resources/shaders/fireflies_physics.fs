#pragma LXCategory("Combo FG")

// CPU-provided physics data textures
uniform sampler2D physicsPosTex;   // RG32F, width = posCount, height = 1 (XY positions)
uniform sampler2D physicsSizeTex;  // R32F, width = posCount, height = 1 (circle radii)
uniform int posCount;              // number of particles
uniform float roomMinX;            // normalized [0,1]
uniform float roomMinY;            // normalized [0,1]
uniform float roomMaxX;            // normalized [0,1]
uniform float roomMaxY;            // normalized [0,1]

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = ( fragCoord.xy / iResolution.xy );
    vec2 p = (uv*2.-1.) * vec2(iResolution.x/iResolution.y,1.);

    // Visual scale multiplier for normalized radius
    // Since radius is already properly normalized (e.g., radius 2.0 -> 0.2), use modest scaling
    float radiusScale = 1.0; // use normalized radius directly

    // Calculate the contribution of each physics-driven firefly to the current pixel
    float lit = 0.;
    for (int i = 0; i < posCount; ++i) {
        // Sample exact texel i from the 1D row (raw physics coordinates)
        vec2 rawPos = texelFetch(physicsPosTex, ivec2(i, 0), 0).xy;
        float circleRadius = texelFetch(physicsSizeTex, ivec2(i, 0), 0).r; // normalized radius (0..1 scale matching coords)
        
        // Use engine-provided normalized coords directly (no clamping)
        vec2 normalizedPos = rawPos;
        vec2 screenPos = (normalizedPos*2.-1.) * vec2(iResolution.x/iResolution.y,1.);

        // Convert normalized radius to screen space
        // Account for aspect ratio and coordinate system scaling
        float aspectRatio = iResolution.x / iResolution.y;
        float effectiveRadius = radiusScale * circleRadius * 4.0; // Convert 0-1 to screen space (-1 to 1)

        // Brightness of the firefly at a given pixel location is inversely
        // proportional to the distance from firefly center.
        // We restrict light falloff range by making it so that distances
        // greater than the desired radius go negative, and get clamped to 0.
        float l = max(0.0, 1.0 - length(p - screenPos) / effectiveRadius);
        // Sharpen brightness curve so tail decay will look more natural
        lit += l * l;
    }

    // Cool the entire backbuffer by a small amount (firefly tail effect)
    fragColor = max(vec4(0.), texelFetch(iBackbuffer, ivec2(gl_FragCoord.xy), 0) - iWow1 / 10.);

    // iWow2 controls firefly glow intensity
    float glowIntensity = iWow2;
    
    // Enhanced glow effect with intensity control
    float enhancedLit = lit * glowIntensity;
    
    // Color mixing for firefly effect
    float colorMix = (iColor2RGB == vec3(0.)) ? 0. : 0.3;
    vec3 color = min(1.0, enhancedLit) * mix(iColorRGB, mix(iColorRGB, iColor2RGB, fract(enhancedLit)), colorMix);

    // Add heat where the fireflies are with glow intensity
    fragColor += vec4(color * glowIntensity, 1.0);

    // Draw red room border for normalized rectangle [roomMin, roomMax]
    float edgeThickness = 0.003;
    float left   = step(0.0, uv.x - roomMinX) * step(uv.x - roomMinX, edgeThickness);
    float right  = step(0.0, roomMaxX - uv.x) * step(roomMaxX - uv.x, edgeThickness);
    float bottom = step(0.0, uv.y - roomMinY) * step(uv.y - roomMinY, edgeThickness);
    float top    = step(0.0, roomMaxY - uv.y) * step(roomMaxY - uv.y, edgeThickness);
    float border = max(max(left, right), max(top, bottom));
    fragColor += vec4(border * vec3(1.0, 0.0, 0.0), border);
}


