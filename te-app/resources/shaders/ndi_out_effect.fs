#version 410

out vec4 fragColor;

// Incoming frame texture
uniform sampler2D iDst;

// Normalized model coordinates
uniform sampler2D lxModelCoords;

void main() {
    vec4 color = texelFetch(iDst, ivec2(gl_FragCoord.xy), 0);

    fragColor = color.bgra;
}
