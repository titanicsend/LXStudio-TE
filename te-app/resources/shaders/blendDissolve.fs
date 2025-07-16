#version 410

out vec4 fragColor;

// Blend destination texture: the bus from previous channels
uniform sampler2D iDst;

// Blend source texture: total output for one channel
uniform sampler2D iSrc;

// Amount of source to be applied, e.g. channel fader
uniform float level;

void main() {
    vec4 colorSrc = texelFetch(iSrc, ivec2(gl_FragCoord.xy), 0);
    vec4 colorDst = texelFetch(iDst, ivec2(gl_FragCoord.xy), 0);

    fragColor = mix(colorDst, colorSrc, level);
}
