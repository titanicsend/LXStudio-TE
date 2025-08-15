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
uniform float zGround;             // normalized Z position of ground plane [0,1]
uniform float groundH;             // normalized thickness of ground plane

// ---- helpers ----
float perspScale(float zNorm) {
    // zNorm in [0,1], >0.5 is "near", <0.5 is "far"
    return 1.0 + (zNorm - 0.5) * depthScaleFactor;
}

// Project a 2D point (in normalized room coords) at depth z into screen UV space.
vec2 projectXY(vec2 xyNorm, float zNorm) {
    float s = perspScale(zNorm);
    return 0.5 + (xyNorm - 0.5) * s;
}

// Rasterize an axis-aligned rectangle in UV space given two corners
float rect(vec2 minUV, vec2 maxUV, vec2 uv) {
    return step(minUV.x, uv.x) * step(uv.x, maxUV.x) *
           step(minUV.y, uv.y) * step(uv.y, maxUV.y);
}

// add once with your helpers
float saturate(float x){ return clamp(x, 0.0, 1.0); }

// Ground layer rendering function
vec4 renderGroundLayer(vec2 uv) {
    // --- GROUND PLANE: full trapezoid with depth gradient (front -> back) ---
    // Project the ground line (Y = roomMinY) at NEAR (front) and FAR (back) depths
    float zNear = roomMaxZ; // visually closest
    float zFar  = roomMinZ; // visually farthest

    // Y positions in screen-UV of the ground line at near/far
    float yNear = projectXY(vec2(0.5, roomMinY), zNear).y;
    float yFar  = projectXY(vec2(0.5, roomMinY), zFar ).y;

    // X extents in screen-UV of the ground line at near/far
    float xNearL = projectXY(vec2(roomMinX, roomMinY), zNear).x;
    float xNearR = projectXY(vec2(roomMaxX, roomMinY), zNear).x;
    float xFarL  = projectXY(vec2(roomMinX, roomMinY), zFar ).x;
    float xFarR  = projectXY(vec2(roomMaxX, roomMinY), zFar ).x;

    // Sort Y so the mask works even if perspective flips
    float y0 = min(yNear, yFar);
    float y1 = max(yNear, yFar);

    // Interp factor along Y inside the trapezoid (0 = near, 1 = far)
    float k = saturate((uv.y - y0) / max(1e-5, (y1 - y0)));

    // Interpolated left/right X at this row (linear in screen space is good enough)
    float xL = mix(xNearL, xFarL, k);
    float xR = mix(xNearR, xFarR, k);

    // Trapezoid mask
    float inside = step(y0, uv.y) * step(uv.y, y1) *
                   step(xL, uv.x) * step(uv.x, xR);

    // Depth-aware shading
    float zAt   = mix(zNear, zFar, k);
    float scale = clamp(perspScale(zAt), 0.5, 1.7);

    // Color/brightness: light gray near → dark gray far
    vec3  nearCol = vec3(0.45, 0.45, 0.45);  // Light gray
    vec3  farCol  = vec3(0.15, 0.15, 0.15);  // Dark gray
    vec3  gcol    = mix(nearCol, farCol, k);

    // Add a subtle edge fade to avoid hard borders
    float edgeSoft = 1.0
        * smoothstep(0.0, 0.01, uv.x - xL)
        * smoothstep(0.0, 0.01, xR - uv.x)
        * smoothstep(0.0, 0.01, uv.y - y0)
        * smoothstep(0.0, 0.01, y1 - uv.y);

    float intensity = (0.4 * scale) * edgeSoft;  // Reduced intensity for transparency

    vec4 groundColor = vec4(gcol * intensity, inside * intensity * 0.5) * inside;  // 0.5 opacity

    // Optional: draw a thin "contact" line at the near edge to anchor the plane visually
    float lineT = 0.006 * scale;
    float nearLine = smoothstep(0.0, lineT, abs(uv.y - yNear));
    float nearMask = step(xNearL, uv.x) * step(uv.x, xNearR);
    vec3  lineCol  = vec3(0.6, 0.6, 0.6);  // Gray line
    groundColor += vec4(lineCol * 0.2 * (1.0 - nearLine), 0.15 * (1.0 - nearLine)) * nearMask;

    return groundColor;
}

// Balls layer rendering function
vec4 renderBallsLayer(vec2 uv) {
    vec2 p = (uv*2.-1.) * vec2(iResolution.x/iResolution.y,1.);

    // Visual scale multiplier for normalized radius
    float radiusScale = 1.0; // use normalized radius directly

    // Calculate the contribution of each physics-driven firefly to the current pixel
    float lit = 0.;
    for (int i = 0; i < posCount; ++i) {
        // Sample exact texel i from the 1D row (raw physics coordinates)
        vec3 rawPos = texelFetch(physicsPosTex, ivec2(i, 0), 0).xyz;
        float circleRadius = texelFetch(physicsSizeTex, ivec2(i, 0), 0).r; // normalized radius (0..1 scale matching coords)
        
        // Use engine-provided normalized coords directly (no clamping)
        vec2 normalizedPos = rawPos.xy;
        float normalizedZ = rawPos.z;
        vec2 uvPos = projectXY(normalizedPos, normalizedZ);        // project in UV
        vec2 screenPos = (uvPos*2.-1.) * vec2(iResolution.x/iResolution.y,1.);

        // Apply Z-based perspective scaling to radius (same rule as positions)
        float zPerspectiveScale = perspScale(normalizedZ);
        float effectiveRadius = radiusScale * circleRadius * zPerspectiveScale * 4.0; // Convert 0-1 to screen space (-1 to 1)

        // Brightness of the firefly at a given pixel location is inversely
        // proportional to the distance from firefly center.
        // We restrict light falloff range by making it so that distances
        // greater than the desired radius go negative, and get clamped to 0.
        float l = max(0.0, 1.0 - length(p - screenPos) / effectiveRadius);
        // Sharpen brightness curve so tail decay will look more natural
        lit += l * l;
    }

    // Cool the entire backbuffer by a small amount (firefly tail effect)
    vec4 ballsColor = max(vec4(0.), texelFetch(iBackbuffer, ivec2(gl_FragCoord.xy), 0) - iWow1 / 10.);

    // iWow2 controls firefly glow intensity
    float glowIntensity = iWow2;
    
    // Enhanced glow effect with intensity control
    float enhancedLit = lit * glowIntensity;
    
    // Color mixing for firefly effect (exact copy from fireflies_physics.fs)
    float colorMix = (iColor2RGB == vec3(0.)) ? 0. : 0.3;
    vec3 color = min(1.0, enhancedLit) * mix(iColorRGB, mix(iColorRGB, iColor2RGB, fract(enhancedLit)), colorMix);

    // Add heat where the fireflies are with glow intensity
    ballsColor += vec4(color * glowIntensity, 1.0);

    return ballsColor;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = ( fragCoord.xy / iResolution.xy );
    
    // Initialize fragment color
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    
    // Layer 1: Ground (back layer, transparent)
    vec4 groundLayer = renderGroundLayer(uv);
    
    // Layer 2: Balls (front layer, opaque)
    vec4 ballsLayer = renderBallsLayer(uv);
    
    // Composite layers: ground first (back), then balls (front)
    // Alpha blending: result = back + front * (1 - back.alpha)
    fragColor.rgb = groundLayer.rgb + ballsLayer.rgb * (1.0 - groundLayer.a);
    fragColor.a = max(groundLayer.a, ballsLayer.a);
}


