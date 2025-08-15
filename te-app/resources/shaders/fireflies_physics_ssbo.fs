#version 430
#pragma LXCategory("Combo FG")

// SSBO for instance data 
layout(std430, binding = 0) buffer InstanceData {
    mat4 prevModel[];  // Previous transform matrices
    mat4 currModel[];  // Current transform matrices  
    float radius[];    // Sphere radii
};

// Uniforms
uniform float alpha;           // Interpolation factor [0,1]
uniform int instanceCount;     // Number of active instances
uniform float roomMinX;        // normalized [0,1]
uniform float roomMinY;        // normalized [0,1]
uniform float roomMaxX;        // normalized [0,1]
uniform float roomMaxY;        // normalized [0,1]

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = ( fragCoord.xy / iResolution.xy );
    vec2 p = (uv*2.-1.) * vec2(iResolution.x/iResolution.y,1.);

    // Visual scale multiplier for normalized radius
    float radiusScale = 15.0; // Scale up for better visibility

    // Calculate the contribution of each physics-driven firefly to the current pixel
    float lit = 0.;
    for (int i = 0; i < instanceCount && i < prevModel.length() && i < currModel.length() && i < radius.length(); ++i) {
        // Interpolate between previous and current transforms
        mat4 worldMatrix = mix(prevModel[i], currModel[i], alpha);
        
        // Extract position from transform matrix (last column)
        vec3 worldPos = worldMatrix[3].xyz;
        
        // Use engine-provided normalized coords directly (no clamping)
        vec2 normalizedPos = worldPos.xy;
        vec2 screenPos = (normalizedPos*2.-1.) * vec2(iResolution.x/iResolution.y,1.);

        // Convert normalized radius to screen space
        // Account for aspect ratio and coordinate system scaling
        float aspectRatio = iResolution.x / iResolution.y;
        float effectiveRadius = radiusScale * radius[i]; // Use SSBO radius directly

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
