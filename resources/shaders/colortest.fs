// A kali-style iterative fractal
//   (https://www.fractalforums.com/new-theories-and-research/very-simple-formula-for-fractal-patterns/)
// that tries to do for rectangles what @andrewlook's shaders did for triangles.
// The "artistic intention" is to sorta depict what's going on in all the circuitry inside Titanic's End
//
// Wow2 controls audio reactivity
#pragma name "Colortest"

#include <include/constants.fs>
#include <include/colorspace.fs>

mat2 rot(float a) {
    float s=sin(a), c=cos(a);
    return mat2(c,s,-s,c);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    // normalize coordinates
    vec2 uv = fragCoord.xy/iResolution.xy;

    if (abs(uv.y - 0.1) <= 0.005) {
        fragColor = vec4(0.0);
    }
    else if (uv.y < 0.1) {
        fragColor = vec4(iPalette[int(iPaletteSize * uv.x)], 0.995);
    }
    else if (iWow1 > 0.6) {
        fragColor = vec4(getPaletteColor_oklab(uv.x), 0.995);
    }
    else if (iWow1 > 0.3) {
        fragColor = vec4(getPaletteColor_hsv(uv.x), 0.995);
    }
    else {
        fragColor = vec4(getPaletteColor_linear(uv.x), 0.995);
    }

}

