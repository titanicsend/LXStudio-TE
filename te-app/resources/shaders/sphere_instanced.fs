#version 430 core
#pragma LXCategory("Combo FG")

// Inputs from vertex shader
in vec3 worldPos;
in vec3 worldNormal;
in float sphereRadius;
in vec3 instanceColor;

// Uniforms
uniform float iTime;
uniform vec3 iColorRGB;
uniform vec3 iColor2RGB;
uniform float iWow1;
uniform float iWow2;
uniform float roomMinX;
uniform float roomMinY;
uniform float roomMaxX;
uniform float roomMaxY;
uniform vec2 iResolution;

// Output
out vec4 fragColor;

void main() {
    // Normalize the normal (in case of non-uniform scaling)
    vec3 normal = normalize(worldNormal);
    
    // Simple directional lighting
    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
    float lightIntensity = max(0.2, dot(normal, lightDir)); // Ambient + diffuse
    
    // Add some fresnel-like effect for sphere edges
    vec3 viewDir = normalize(-worldPos); // Assuming camera at origin
    float fresnel = pow(1.0 - max(0.0, dot(normal, viewDir)), 2.0);
    
    // Glow effect based on iWow2
    float glowIntensity = iWow2 * (1.0 + fresnel * 2.0);
    
    // Color mixing between primary and secondary colors
    float colorMix = (iColor2RGB == vec3(0.)) ? 0. : 0.3;
    vec3 baseColor = mix(instanceColor, mix(instanceColor, iColor2RGB, 0.5), colorMix);
    
    // Apply lighting and glow
    vec3 finalColor = baseColor * lightIntensity * glowIntensity;
    
    // Add some particle-like transparency based on distance from center
    float alpha = glowIntensity * 0.8;
    
    fragColor = vec4(finalColor, alpha);
}
