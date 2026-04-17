// Kaleidoscope post-effect shader (TE)
// Reads from iDst and remaps UV into mirrored angular wedges.
#define TE_EFFECTSHADER
#define TE_NOPOSTPROCESSING

uniform sampler2D iDst;

// Controls
uniform float mixAmt;     // 0.0 = original image, 1.0 = full kaleidoscope
uniform int   segments;   // >= 1
uniform float angle;      // global wedge rotation (radians)
uniform float rotate;     // texture rotation inside wedges (radians)
uniform float zoom;       // radial zoom (1.0 = identity)
uniform vec2  center;     // normalized [0..1] center
uniform int   mirror;     // 1 = reflective fold, 0 = wrap
uniform float featherPx;  // fade to black near frame edges (pixels)

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 res = iResolution;
    vec2 uv  = fragCoord / res;

    // Sample original pixel via the indirection map
    vec4 original = _getMappedPixel(iDst, ivec2(fragCoord));

    // Shift to center in normalized space
    vec2 cuv = uv - center;

    // Convert to polar
    float r = length(cuv);
    float th = atan(cuv.y, cuv.x); // [-pi, pi]

    // Apply global wedge rotation
    th -= angle;

    // Sector width
    float sector = 6.28318530718 / float(max(1, segments)); // 2*pi / N

    // Fold or wrap into [0, sector)
    float a = mod(th, sector);
    if (mirror == 1) {
        float halfS = 0.5 * sector;
        if (a > halfS) a = sector - a; // reflect
    }

    // Texture rotation within wedge
    a += rotate;

    // Radial zoom (avoid div by 0)
    float z = max(1e-5, zoom);
    float rr = r / z;

    // Back to cartesian around center
    vec2 sampleUV = center + rr * vec2(cos(a), sin(a));

    // Sample via the indirection map at the remapped coordinates
    ivec2 sampleCoord = ivec2(clamp(sampleUV * res, vec2(0.0), res - 1.0));
    vec4 kaleidoscope = _getMappedPixel(iDst, sampleCoord);

    // Optional edge feather near image bounds (fade to black)
    if (featherPx > 0.0) {
        float fx = min(fragCoord.x, res.x - fragCoord.x);
        float fy = min(fragCoord.y, res.y - fragCoord.y);
        float f  = clamp(min(fx, fy) / featherPx, 0.0, 1.0);
        kaleidoscope.rgb *= f;
    }

    // Mix between original and kaleidoscope effect
    fragColor = mix(original, kaleidoscope, mixAmt);
}
