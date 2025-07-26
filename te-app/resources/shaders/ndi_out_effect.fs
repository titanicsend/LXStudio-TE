#version 410

out vec4 fragColor;

// Incoming frame texture
uniform sampler2D iDst;

// Normalized model coordinates
uniform sampler2D lxModelCoords;

//uniform vec2 iResolution;

void main() {
//    vec2 coords = texelFetch(lxModelCoords, ivec2(gl_FragCoord.xy), 0).xy;
//
//    if (isnan(coords.r)) {
//        fragColor = vec4(0.0);
//        return;
//    }

    // Scale the coordinates to the resolution specified by Chromatik
    // (which can be changed via the --resolution command line option)
//    coords *= iResolution;

    vec4 color = texelFetch(iDst, ivec2(gl_FragCoord.xy), 0);

    fragColor = color;
}
