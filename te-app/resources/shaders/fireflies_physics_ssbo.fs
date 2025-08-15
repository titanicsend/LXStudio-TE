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
uniform float roomMinZ;        // normalized [0,1]
uniform float roomMaxZ;        // normalized [0,1]
uniform float radiusScale;     // Overall radius multiplier

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = ( fragCoord.xy / iResolution.xy );
    vec2 p = (uv*2.-1.) * vec2(iResolution.x/iResolution.y,1.);

    // Calculate the contribution of each 3D orbital firefly to the current pixel
    float lit = 0.;
    for (int i = 0; i < instanceCount && i < prevModel.length() && i < currModel.length() && i < radius.length(); ++i) {
        // Interpolate between previous and current transforms
        mat4 worldMatrix = mix(prevModel[i], currModel[i], alpha);
        
        // Extract position from transform matrix (last column)
        vec3 worldPos = worldMatrix[3].xyz;
        
        // Use engine-provided normalized coords directly (3D coordinates)
        vec2 normalizedPos = worldPos.xy;
        float normalizedZ = worldPos.z;
        vec2 screenPos = (normalizedPos*2.-1.) * vec2(iResolution.x/iResolution.y,1.);

        // Z-based size scaling: closer objects (higher Z) appear larger
        // Map Z from [roomMinZ, roomMaxZ] to [0, 1]
        float zDepth = (normalizedZ - roomMinZ) / (roomMaxZ - roomMinZ);
        float depthScale = 0.3 + (zDepth * 2.0); // Scale from 0.3x to 2.3x
        
        // Convert normalized radius to screen space with depth scaling
        float effectiveRadius = radiusScale * radius[i] * depthScale * 25.0; // Enhanced visibility

        // Add subtle pulsing based on Z position
        float pulse = 1.0 + sin(iTime * 2.0 + normalizedZ * 10.0) * 0.1;
        effectiveRadius *= pulse;

        // Brightness calculation with Z-based intensity
        float distance = length(p - screenPos);
        float l = max(0.0, 1.0 - distance / effectiveRadius);
        
        // Z-based brightness: closer orbs are brighter
        float zIntensity = 0.5 + zDepth * 0.8;
        lit += l * l * zIntensity;
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

    // Draw a warm-colored center point to show the gravity center
    vec2 centerScreen = (vec2(0.5, 0.5)*2.-1.) * vec2(iResolution.x/iResolution.y,1.);
    float centerDist = length(p - centerScreen);
    float centerGlow = max(0.0, 1.0 - centerDist / 0.05) * 0.3; // Small central glow
    fragColor += vec4(centerGlow * vec3(1.0, 0.8, 0.6), centerGlow); // Warm center color
}
