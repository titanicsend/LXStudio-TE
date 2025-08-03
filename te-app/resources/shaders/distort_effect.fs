// tell the preprocessor and any control management scripts that this is a post effect shader
// and doesn't use the common controls.
#define TE_EFFECTSHADER
#define TE_NOPOSTPROCESSING

// Texture from the preceding pattern or effect
uniform sampler2D iDst;

uniform float size;
uniform float depth; // Warping intensity (0 = no warp, higher = more warp)

// Simple 2D noise function
float noise(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

// Smooth noise using bilinear interpolation
float smoothNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    // Smooth interpolation
    f = f * f * (3.0 - 2.0 * f);

    // Sample corners
    float a = noise(i);
    float b = noise(i + vec2(1.0, 0.0));
    float c = noise(i + vec2(0.0, 1.0));
    float d = noise(i + vec2(1.0, 1.0));

    // Bilinear interpolation
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Fractal Brownian Motion
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    // Add multiple octaves
    for (int i = 0; i < 5; i++) {
        value += amplitude * smoothNoise(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }

    return value;
}

// Generate domain warp offset
vec2 getDomainWarpOffset(vec2 p, float intensity) {
    if (intensity <= 0.0) {
        return vec2(0.0);
    }

    // Create two FBM patterns offset from each other
    vec2 q = vec2(fbm(p + vec2(0.0, 0.0 + sin(iTime * .5))),
                  fbm(p + vec2(5.2, 1.3 + cos(iTime * .2))));

    // Use the first pattern to warp the input for the second pattern
    vec2 r = vec2(fbm(p + size * sin(iTime) * q + vec2(1.7, 9.2)),
                  fbm(p + size * cos(iTime * 2.) * q + vec2(8.3, 2.8)));

    // Return warp offset scaled by intensity
    return intensity * (r - 0.5) * 0.2; // Center around 0 and scale
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    fragColor = texelFetch(iDst, ivec2(gl_FragCoord.xy), 0);

    // If no warping, just output original
    if (depth <= 0.0) {
        return;
    }

    vec3 modelCoords = _getModelCoordinates().xyz;
    vec2 uv = vec2((modelCoords.z < 0.5) ? modelCoords.x : 1. + (1. - modelCoords.x), modelCoords.y);
    uv.x *= 0.5;

    // Normalize coordinates for noise calculation
    vec2 noiseCoord = uv; // Scale for noise frequency

    // Calculate domain warp offset
    vec2 warpOffset = getDomainWarpOffset(noiseCoord, depth);

    // Apply warp to the sampling coordinates
    vec2 warpedUV = (uv + warpOffset) * 2.0;

    // Clamp to avoid sampling outside texture bounds
    warpedUV = clamp(warpedUV, 0.0, 1.0);

    // Convert back to pixel coordinates for texelFetch
    ivec2 warpedPixel = ivec2(warpedUV * iResolution);

    // Sample the warped pixel from the backbuffer
    fragColor = ((1. - depth) * fragColor) + (depth * texelFetch(iDst, warpedPixel, 0));
}