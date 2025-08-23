// tell the preprocessor and any control management scripts that this is a post effect shader
// and doesn't use the common controls.
#define TE_EFFECTSHADER
#define TE_NOPOSTPROCESSING

uniform float basis;
uniform sampler2D iDst;

float interval = 0.035;    // Shake frequency
float amplitude = 0.1;    // Maximum shake offset (in normalized coordinates)

float randomOffset(float x) {
    return amplitude * (fract(sin(x * 12.9898 + 78.233) * 43758.5453) - 0.5);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // early out if effect is inactive
    fragColor = texelFetch(iDst, ivec2(gl_FragCoord.xy), 0);
    if (basis <= 0.0) {
       return;
    }
    // Normalize coords
    vec2 uv = fragCoord / iResolution.xy;

    // Vary shake w/time-based seed, updating every 'interval' seconds
    float timeSeed = floor(iTime / interval);

    // Generate offsets for x and y
    float dx = randomOffset(timeSeed);
    float dy = randomOffset(timeSeed + 40.0);

    // Apply strength to the offset to control shake magnitude
    vec2 offset = vec2(dx, dy) * basis;

    // Add offset to UV coordinates to create shaking effect
    vec2 shakedUV = uv + offset;

    // Clamp coordinates to avoid sampling outside texture bounds
    //shakedUV = clamp(shakedUV, 0.0, 1.0);

    // Sample the scene texture at the shifted coordinates
    vec4 col = _getMappedPixel(iDst, ivec2(shakedUV * iResolution));
    fragColor = mix(fragColor,col,smoothstep(0.0,0.01,basis));
}