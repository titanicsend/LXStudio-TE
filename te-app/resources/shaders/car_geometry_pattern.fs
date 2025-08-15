// CarGeometryTest (initial version)
//
// Placeholder shader to verify wiring and compilation. We'll add car-geometry-powered
// normals in a subsequent step. For now, render a simple palette-driven gradient so the
// pattern is visibly working when selected.

#include <include/constants.fs>
#include <include/colorspace.fs>

// Incoming panel centers from Java (normalized model coords)
#define PANEL_COUNT 68
uniform int panelCount;
uniform vec3[PANEL_COUNT] panelCenters;
uniform float panelRadius; // unused once iScale drives radius; kept for compatibility
uniform vec3 axisLengths;  // physical axis lengths for anisotropy correction

const float ballBright = .9;

// % of ball edge to smooth
const float smoothMargin = .25;

// ball size as % of model size
const float ballSizeScale = 1. / 28.;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // Per-pixel normalized car model coordinates
    vec3 model = _getModelCoordinates().xyz; // [0,1]
    model -= vec3(.5);

    // Sphere radius driven by iScale (0..1), relative to longest axis
    float maxAxis = max(axisLengths.x, max(axisLengths.y, axisLengths.z));
    float radius = ballSizeScale * iScale; // interpreted as a fraction of the normalized space

    // How many circles (balls really since they're in 3D now) to draw
    // hijact the levelReact control
    int howmany = int(ceil(levelReact * PANEL_COUNT));

    // Draw a small ball  at each panel's centroid using 3D distance
    float accum = 0.0;
    for (int i = 0; i < howmany + 1; i++) {
    /* for (int i = 0; i < 10. * levelReact; i++) { */
        if (i >= panelCount) break;

        vec3 c = panelCenters[i].xyz; // normalized centroid
        c -= vec3(.5);
        c -= vec3(-iTranslate.x, iTranslate.y, iWow1);

        vec3 delta = model.xyz - c;
        // Scale by axis ratios so distances are isotropic in physical space
        vec3 scaled = delta * (axisLengths / maxAxis);
        float d = length(scaled);

        // Normalize by sqrt(3) so the diagonal of the unit cube maps to ~1
        /* float ball = step(d / sqrt(3.), radius); */
        float ball = smoothstep(radius, radius - radius * smoothMargin, d / sqrt(3.));

        accum += ballBright * ball;

    }

    float bright = accum;

    // Color using current palette: show circles in primary color over a dim base from XYZ
    vec3 base = model; // dim geometry preview, helps confirm model-space addressing
    vec3 circles = iColorRGB * bright;
    vec3 col = mix(base * 0.2, circles, bright);

    fragColor = vec4(col, bright);
}
