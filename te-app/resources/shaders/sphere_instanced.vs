#version 430 core

// Vertex attributes
layout(location = 0) in vec3 aPos;    // Sphere vertex position
layout(location = 1) in vec3 aNormal; // Sphere vertex normal

// Instance data from SSBO
layout(std430, binding = 0) buffer InstanceData {
    struct Instance {
        mat4 prevModel;  // Previous transform matrix
        mat4 currModel;  // Current transform matrix
        float radius;    // Sphere radius
        float padding;   // Alignment padding
    };
    Instance instances[];
};

// Uniforms
uniform mat4 iProjectionMatrix;
uniform mat4 iViewMatrix;
uniform float alpha;        // Interpolation factor [0,1]
uniform float iTime;
uniform vec3 iColorRGB;
uniform float iWow1;
uniform float iWow2;

// Outputs to fragment shader
out vec3 worldPos;
out vec3 worldNormal;
out float sphereRadius;
out vec3 instanceColor;

void main() {
    // Get instance data
    Instance inst = instances[gl_InstanceID];
    
    // Interpolate between previous and current transforms
    mat4 worldMatrix = mix(inst.prevModel, inst.currModel, alpha);
    
    // Scale the unit sphere by the instance radius
    vec3 scaledPos = aPos * inst.radius;
    
    // Transform to world space
    vec4 worldPosition = worldMatrix * vec4(scaledPos, 1.0);
    worldPos = worldPosition.xyz;
    
    // Transform normal to world space (assuming uniform scaling)
    worldNormal = normalize(mat3(worldMatrix) * aNormal);
    
    // Pass radius to fragment shader
    sphereRadius = inst.radius;
    
    // Add some color variation based on instance ID and wow parameters
    float colorVariation = sin(float(gl_InstanceID) * 0.1 + iTime * 0.5) * 0.3 + 0.7;
    instanceColor = iColorRGB * colorVariation;
    
    // Transform to clip space
    gl_Position = iProjectionMatrix * iViewMatrix * worldPosition;
}
