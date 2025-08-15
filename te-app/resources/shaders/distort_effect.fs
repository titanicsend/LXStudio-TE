// tell the preprocessor and any control management scripts that this is a post effect shader
// and doesn't use the common controls.
#define TE_EFFECTSHADER
#define TE_NOPOSTPROCESSING

// Texture from the preceding pattern or effect
uniform sampler2D iDst;

uniform float depth; // Warping intensity (0 = no warp, higher = more warp)
uniform float size;
uniform float speed;


// Simple 2D noise function
float noise(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float time;

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
vec2 getDomainWarpOffset(vec2 p) {

    // Create two FBM patterns offset from each other
    vec2 q = vec2(fbm(p + vec2(0.0, 0.0 + (1. + sin(time * .5)))),
    fbm(p + vec2(5.2, 1.3 + (1. + cos(time * .2)))));

    float t1 = (1. + sin(time));
    // Use the first pattern to warp the input for the second pattern
    vec2 r = vec2(fbm(p + 4.0 * t1 * (1.0 + size) * q + vec2(1.7 + sin(time * 1.2), 9.2)),
    fbm(p + 4.0 * (1. + sin(time*2.)) * (1.0 + size) * q + vec2(8.3 + cos(time * 1.5), 2.8)));

    return (r - 0.5) * depth; // Scale the offset by depth
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    time = iTime * speed;

    vec2 uv = -0.5 + fragCoord / iResolution.xy;
    fragColor = texelFetch(iDst, ivec2(gl_FragCoord.xy), 0);

    // Normalize coordinates for noise calculation
    vec2 noiseCoord = uv * size; // Scale for noise frequency

    // Calculate domain warp offset
    vec2 warpOffset = depth * getDomainWarpOffset(noiseCoord);

    // Apply warp to the sampling coordinates and translate back to
    // the range [0, 1] for texture sampling
    vec2 warpedUV = fract(abs(uv + 0.5 + warpOffset));

    // Convert back to pixel coordinates for texelFetch
    vec2 warpedPixel = vec2(warpedUV * iResolution.xy);

    // Sample the warped pixel from the backbuffer
    fragColor = _getMappedPixel(iDst,ivec2(warpedPixel));
}