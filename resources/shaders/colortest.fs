// A kali-style iterative fractal
//   (https://www.fractalforums.com/new-theories-and-research/very-simple-formula-for-fractal-patterns/)
// that tries to do for rectangles what @andrewlook's shaders did for triangles.
// The "artistic intention" is to sorta depict what's going on in all the circuitry inside Titanic's End
//
// Wow2 controls audio reactivity
#pragma name "Colortest"

#include <include/constants.fs>
#include <include/colorspace.fs>

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    // normalize coordinates to [0,1] 
    vec2 uv = fragCoord.xy/iResolution.xy;
    float col = min(0.995,uv.x) + iPaletteOffset;

    if (abs(uv.y - 0.1) <= 0.005) {
        fragColor = vec4(0.0);
    }
    else if (uv.y < 0.1) {
        fragColor = vec4(getPaletteColor(int(col * iPaletteSize)), 0.995);
    }
    else if (iWow1 > 0.6) {
        fragColor = vec4(getGradientColor_oklab(col), 0.995);
    }
    else if (iWow1 > 0.3) {
        fragColor = vec4(getGradientColor_hsv(col), 0.995);
    }
    else {
        fragColor = vec4(getGradientColor_linear(col), 0.995);
    }

}

