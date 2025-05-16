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

    // TODO: Review/fix this blend logic:

    // RGB: Scale down source RGB by fader and source alpha, add to destination RGB
    vec3 rgb = colorDst.rgb + (colorSrc.rgb * level * colorSrc.a);

    // Alpha: maximum of source or destination alpha
    float a = max(colorDst.a, colorSrc.a);

    // Constrain all values to [0,1]
    fragColor = clamp(vec4(rgb, a), 0.0, 1.0);
}
