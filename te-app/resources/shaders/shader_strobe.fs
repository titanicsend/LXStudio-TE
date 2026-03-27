// Strobe post-effect shader
// Adapted from heronarts.lx.effect.StrobeEffect (Heron Arts LLC)
// This is an effect, therefore does not use common controls
#define TE_EFFECTSHADER
// Bypass TE post-processing color and alpha adjustments
#define TE_NOPOSTPROCESSING

// Texture from the preceding pattern or effect
uniform sampler2D iDst;

// Strobe multiplier 0-1 (0=black, 1=full brightness)
uniform float strobe;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // Original code (commented out for testing):
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    vec4 color = texelFetch(iDst, pixel, 0);

    // Multiply RGB by strobe factor (pass-through at 1.0, blackout at 0.0)
    fragColor = vec4(color.rgb * strobe, color.a);
}