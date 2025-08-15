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
uniform vec3[PANEL_COUNT] panelNormals;
uniform vec3[PANEL_COUNT] panelV0;
uniform vec3[PANEL_COUNT] panelV1;
uniform vec3[PANEL_COUNT] panelV2;
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

    // Determine which panel triangle contains this point using 3D barycentric test
    // Choose among all containing panels the one with nearest center (isotropic distance)
    int chosen = -1;
    float bestCenterDist = 1e9;
    const float EPS = 1e-8;
    const float EPS2 = 0.0;
    for (int i = 0; i < panelCount; i++) {
        // triangle vertices centered
        vec3 a = panelV0[i] - vec3(.5);
        vec3 b = panelV1[i] - vec3(.5);
        vec3 c = panelV2[i] - vec3(.5);

        // 3D barycentric via dot products
        vec3 v0 = b - a;
        vec3 v1 = c - a;
        vec3 v2 = model - a;
        float d00 = dot(v0, v0);
        float d01 = dot(v0, v1);
        float d11 = dot(v1, v1);
        float d20 = dot(v2, v0);
        float d21 = dot(v2, v1);
        float denom = d00 * d11 - d01 * d01;
        if (abs(denom) > EPS) {
            float inv = 1.0 / denom;
            float v = (d11 * d20 - d01 * d21) * inv;
            float w = (d00 * d21 - d01 * d20) * inv;
            float u = 1.0 - v - w;
            if (u >= -EPS2 && v >= -EPS2 && w >= -EPS2) {
                vec3 cc = panelCenters[i] - vec3(.5);
                float centerDist = length((model - cc) * (axisLengths / maxAxis));
                if (centerDist < bestCenterDist) {
                    bestCenterDist = centerDist;
                    chosen = i; // inside or on edge; nearest center wins
                }
            }
        }
    }
    vec3 base = vec3(0.0);
    if (chosen >= 0) {
        // Base color: map panel normal XYZ to RGB for chosen panel
        vec3 nrm = panelNormals[chosen];
        base = 0.5 * (nrm + vec3(1.0));
    }

    // Keep circles overlay in primary color
    vec3 circles = iColorRGB * bright;
    vec3 col = mix(base, circles, clamp(bright, 0.0, 1.0));

    // // DEBUG OVERLAY: draw bright lines along panel edges using segment distances in 3D
    // // don't delete this, we'll uncomment sometimes to debug
    // float edgeLines = 0.0;
    // float edgeWidth = 0.01; // normalized thickness
    // for (int i = 0; i < panelCount; i++) {
    //     vec3 a = panelV0[i] - vec3(.5);
    //     vec3 b = panelV1[i] - vec3(.5);
    //     vec3 c = panelV2[i] - vec3(.5);

    //     vec3 ab = b - a; float abLen2 = max(dot(ab, ab), 1e-8);
    //     vec3 bc = c - b; float bcLen2 = max(dot(bc, bc), 1e-8);
    //     vec3 ca = a - c; float caLen2 = max(dot(ca, ca), 1e-8);

    //     float t;
    //     // distance to segment AB
    //     t = clamp(dot(model - a, ab) / abLen2, 0.0, 1.0);
    //     float dAB = length((a + t * ab) - model);
    //     // BC
    //     t = clamp(dot(model - b, bc) / bcLen2, 0.0, 1.0);
    //     float dBC = length((b + t * bc) - model);
    //     // CA
    //     t = clamp(dot(model - c, ca) / caLen2, 0.0, 1.0);
    //     float dCA = length((c + t * ca) - model);

    //     float m = 0.0;
    //     m = max(m, smoothstep(edgeWidth, edgeWidth * 0.6, dAB));
    //     m = max(m, smoothstep(edgeWidth, edgeWidth * 0.6, dBC));
    //     m = max(m, smoothstep(edgeWidth, edgeWidth * 0.6, dCA));
    //     edgeLines = max(edgeLines, m);
    // }
    // col = max(col, vec3(edgeLines));
    // bright = max(bright, edgeLines);

    fragColor = vec4(col, clamp(.5 + bright, 0., 1.));
}
