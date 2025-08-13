// Utility shader for comparing various color blending methods
// Displays the un-blended palette in a strip at the bottom of
// the screen, and the blended gradient above it.
// Wow1 selects the color blending method.
// 0.0 for linear, 0.3 for HSV, 0.6 for Oklab

#pragma name "Colortest"
// this shader should really live with the rest of the test/utility patterns,
// but that puts it inconveniently at the bottom of a long, long list. So for now,
// with no category, it's near the top.  When done testing, uncomment the following
// line to put it in its proper place.
#pragma LXCategory("Utility")
#pragma TEControl.LEVELREACTIVITY.Disable
#pragma TEControl.FREQREACTIVITY.Disable
#pragma TEControl.WOWTRIGGER.Disable
#pragma TEControl.WOW2.Disable

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

