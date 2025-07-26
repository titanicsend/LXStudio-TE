// This is an effect, therefore does not use common controls
#define TE_EFFECTSHADER
// Bypass TE post-processing color and alpha adjustments
#define TE_NOPOSTPROCESSING

// Texture from the preceding pattern or effect
uniform sampler2D iDst;

// Amount of sustain 0-1
uniform float iSustain;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    vec4 color = texelFetch(iDst, pixel, 0);
    vec4 prev = texelFetch(iBackbuffer, pixel, 0);

    // Calculate an amount to decay in this frame
    float decay = pow((1.0 - clamp(iSustain, 0.0, 1.0)), 5.);

    // Apply decay to backbuffer color
    vec4 faded = clamp(prev - decay, 0.0, 1.0);

    // Jump to current frame color values if they are higher
    fragColor = max(faded, color);
}
