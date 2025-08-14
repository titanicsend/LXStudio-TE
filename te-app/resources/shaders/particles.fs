// Particle distribution shader
// Particles are distributed evenly across the model as single lit points.
// Uses iQuantity as the total particle count N.

#pragma name "Particles"
#pragma LXCategory("Combo FG")
#pragma TEControl.QUANTITY.Range(1000.0, 0.0, 200000.0)

// Custom uniforms supplied by the pattern
uniform float iModelPointCount;     // total number of valid model pixels
uniform float iParticlesPerPixel;   // iQuantity / iModelPointCount

// Simple stable hash from 3D model coordinate
float hash31(vec3 p) {
    // project to 2D then hash
    vec2 q = vec2(dot(p, vec3(127.1, 311.7, 74.7)),
                  dot(p, vec3(269.5, 183.3, 246.1)));
    return fract(sin(dot(q, vec2(12.9898,78.233))) * 43758.5453);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // normalized 3D position of current model pixel
    vec3 pos = _getModelCoordinates().xyz;

    // fallback if uniform not set (defensive)
    float ppp = (iModelPointCount > 0.5) ? iParticlesPerPixel : 0.0;

    // decide if this pixel hosts a particle
    float on = 0.0;
    if (ppp >= 1.0) {
        on = 1.0; // more than one particle per pixel saturates to a single lit point
    } else if (ppp > 0.0) {
        float r = hash31(pos);
        on = step(r, ppp);
    }

    vec3 col = mix(vec3(0.0), iColorRGB, on);
    fragColor = vec4(col, on);
}

