#include <include/constants.fs>
#include <include/colorspace.fs>

// build 2D rotation matrix
mat2 rotate2D(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

void mainImage(out vec4 o,vec2 u) {
    // normalize and center coordinates
    vec3 ir = vec3(iResolution,1.0);
    vec3 v=vec3(u,1.)/ir-.5;

    // warp incoming coordinates so the tunnel has a nice bounce along
    // with the beat, controlled by iWow1.
    float l = 2.0 * distance(v.xy, vec2(0.0, 0.0));
    float warp = (0.03 * sin(4. * beat + 3.0 * l ));
    v.xy += iWow1 * warp;

    // our normal rotation thing
    v.xy *= rotate2D(-iRotationAngle);

    // align tunnel edges w/car vertices
    v.y += 0.15;

    // This is where we set up our cell grid. iScale controls
    // the size of the grid cells
    vec3 s=.5/abs(v);
    s.z = min(s.y,s.x);
    vec3 i=ceil(800 * s.z *(s.y<s.x?v.xzz:v.zyz));
    vec3 j=fract(i *= iScale);

    // i is grid cell, j is position within cell
    i -= j;
    // p controls position and movement of the grid cells.
    vec3 p=vec3(9 ,floor(iTime*(9. + 8.*sin(i).x)),0) + i;

    // Scale brightness based on distance from center to give
    // the illusion of fading with distance.
    float brightness = 1.0 / s.z * 3.0;

    // Calculate the color based on cell position with time-based
    // cycling effect.  The choice of parameters is totally arbitrary,
    // but it uses the whole range of the TE palettes and looks nice.
    float color = fract(iTime + i.x / i.y);

    // Render our rectangle filled tunnel.  Actually, what we're doing
    // is drawing a single rectangle over and over again as we map
    // incoming pixels to our grid pattern.
    // iQuantity controls color vs. darkness, and thus grid density.
    brightness *= (fract(100.*sin(p.x*8.+ p.y)) <= iQuantity &&
      j.x < 0.7 && j.y < 0.85) ? 1. + (1.0 - iQuantity) : 0.;

    // Get final color from palette and add beat reactivity.
    float alphaReact = (beat * beat);
    brightness = min(brightness, 1.0) - (alphaReact * iWow2);
    o = vec4(getGradientColor(color), brightness);
}