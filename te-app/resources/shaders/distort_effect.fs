// tell the preprocessor and any control management scripts that this is a post effect shader
// and doesn't use the common controls.
#define TE_EFFECTSHADER
#define TE_NOPOSTPROCESSING

// Texture from the preceding pattern or effect
uniform sampler2D iDst;

uniform float basis;
uniform float size; // Warping intensity (0 = no warp, higher = more warp)

// uniform float iDepth;


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

// Domain warping function
vec2 domainWarp(vec2 p, float intensity) {
    if (intensity <= 0.0) {
        return p;
    }

    // Create two FBM patterns offset from each other
    vec2 q = vec2(fbm(p + vec2(0.0, 0.0)),
                  fbm(p + vec2(5.2, 1.3)));

    // Use the first pattern to warp the input for the second pattern
    vec2 r = vec2(fbm(p + 4.0 * q + vec2(1.7, 9.2)),
                  fbm(p + 4.0 * q + vec2(8.3, 2.8)));

    // Apply warping with controllable intensity
    return p + intensity * r;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // Normalize coordinates to [0, 1]
    vec2 uv = fragCoord.xy / iResolution;

    // Scale coordinates for noise (adjust scale as needed)
    vec2 noiseCoord = uv * 4.0;

    // Apply domain warping
    vec2 warpedCoord = domainWarp(noiseCoord, size * 0.1);

    // Convert back to texture coordinates
    vec2 warpedUV = warpedCoord / 4.0;

    // Clamp to avoid sampling outside texture bounds
    warpedUV = clamp(warpedUV, 0.0, 1.0);

    // Convert UV back to pixel coordinates for texelFetch
    ivec2 warpedPixel = ivec2(warpedUV * iResolution);

    // Sample the warped pixel from the backbuffer
    fragColor = texelFetch(iDst, warpedPixel, 0);
}
