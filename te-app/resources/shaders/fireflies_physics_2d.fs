#pragma LXCategory("Combo FG")

// CPU-provided physics data textures
uniform sampler2D physicsPosTex;   // RGB32F, width = posCount, height = 1 (XYZ positions)
uniform sampler2D physicsSizeTex;  // R32F, width = posCount, height = 1 (circle radii)
uniform int posCount;              // number of particles
uniform float roomMinX;            // normalized [0,1]
uniform float roomMinY;            // normalized [0,1]
uniform float roomMaxX;            // normalized [0,1]
uniform float roomMaxY;            // normalized [0,1]
uniform float roomMinZ;            // normalized [0,1]
uniform float roomMaxZ;            // normalized [0,1]
uniform float depthScaleFactor;    // scale factor for Z-based perspective

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
        vec3 rawPos = texelFetch(physicsPosTex, ivec2(i, 0), 0).xyz;
        float baseRadius = texelFetch(physicsSizeTex, ivec2(i, 0), 0).r; // normalized radius (0..1 scale matching coords)
        
        // Use engine-provided normalized coords directly (no clamping)
        vec2 normalizedPos = rawPos.xy;
        float normalizedZ = rawPos.z;
        vec2 screenPos = (normalizedPos*2.-1.) * vec2(iResolution.x/iResolution.y,1.);

        // Apply Z-based perspective scaling to radius
        // Z range: 0.0 (far/back) to 1.0 (near/front) in normalized coordinates
        // Scale radius based on Z: closer objects (higher Z) appear larger
        float zPerspectiveScale = 1.0 + (normalizedZ - 0.5) * depthScaleFactor;
        float circleRadius = baseRadius * zPerspectiveScale;

        // Convert normalized radius to screen space
        // Account for aspect ratio and coordinate system scaling
        float aspectRatio = iResolution.x / iResolution.y;
        float effectiveRadius = radiusScale * circleRadius * 4.0; // Convert 0-1 to screen space (-1 to 1) 4 is arbitrary

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

    // Draw 3D wireframe room bounds with grid patterns
    float edgeThickness = 0.001;
    float gridThickness = 0.0005;
    
    // Draw wireframe for multiple Z-depth planes to show 3D perspective
    for (float zPlane = roomMinZ; zPlane <= roomMaxZ; zPlane += (roomMaxZ - roomMinZ) * 0.25) {
        // Calculate perspective scaling for this Z plane
        float zPerspective = 1.0 + (zPlane - 0.5) * depthScaleFactor * 0.3;
        
        // Scale room bounds based on Z perspective
        float scaledMinX = 0.5 + (roomMinX - 0.5) * zPerspective;
        float scaledMaxX = 0.5 + (roomMaxX - 0.5) * zPerspective;
        float scaledMinY = 0.5 + (roomMinY - 0.5) * zPerspective;
        float scaledMaxY = 0.5 + (roomMaxY - 0.5) * zPerspective;
        
        // Room boundary edges
        float left   = step(0.0, uv.x - scaledMinX) * step(uv.x - scaledMinX, edgeThickness);
        float right  = step(0.0, scaledMaxX - uv.x) * step(scaledMaxX - uv.x, edgeThickness);
        float bottom = step(0.0, uv.y - scaledMinY) * step(uv.y - scaledMinY, edgeThickness);
        float top    = step(0.0, scaledMaxY - uv.y) * step(scaledMaxY - uv.y, edgeThickness);
        
        // Grid pattern inside the room bounds
        float roomWidth = scaledMaxX - scaledMinX;
        float roomHeight = scaledMaxY - scaledMinY;
        float gridSize = 0.1; // Grid every 10% of room size
        
        float gridX = 0.0;
        float gridY = 0.0;
        
        // Vertical grid lines
        for (float i = 0.25; i <= 0.75; i += 0.25) {
            float gridLineX = scaledMinX + roomWidth * i;
            gridX += step(0.0, uv.x - gridLineX) * step(uv.x - gridLineX, gridThickness);
            gridX += step(0.0, gridLineX - uv.x) * step(gridLineX - uv.x, gridThickness);
        }
        
        // Horizontal grid lines
        for (float j = 0.25; j <= 0.75; j += 0.25) {
            float gridLineY = scaledMinY + roomHeight * j;
            gridY += step(0.0, uv.y - gridLineY) * step(uv.y - gridLineY, gridThickness);
            gridY += step(0.0, gridLineY - uv.y) * step(gridLineY - uv.y, gridThickness);
        }
        
        // Only show grid inside room bounds
        float insideRoom = step(scaledMinX, uv.x) * step(uv.x, scaledMaxX) * 
                          step(scaledMinY, uv.y) * step(uv.y, scaledMaxY);
        float grid = (gridX + gridY) * insideRoom;
        
        float planeBorder = max(max(left, right), max(top, bottom));
        float totalWireframe = max(planeBorder, grid);
        
        // Dark red color for all wireframe elements
        vec3 wireframeColor = vec3(0.4, 0.0, 0.0); // Dark red
        
        float intensity = 0.3 + 0.2 * (zPlane - roomMinZ) / (roomMaxZ - roomMinZ); // Slight depth variation
        fragColor += vec4(totalWireframe * wireframeColor * intensity, totalWireframe * intensity);
    }
}


