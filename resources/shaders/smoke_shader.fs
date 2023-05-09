// fork of https://www.shadertoy.com/view/NllBzl#
// This version re-ported to GLSL, preserving the controls from
// @Jeff's Java translation for TE.

// this pattern doesn't generate a continuous, unbounded image
// so x/y translation (via the offset controls) is disabled.
#define TE_NOTRANSLATE

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// rotate (2D) a point about the specified origin by <angle> radians
vec2 rotate(vec2 point, vec2 origin, float angle) {
    float c = cos(angle);  float s = sin(angle);
    mat2 rotationMatrix = mat2(c, -s, s, c);
    return origin + rotationMatrix * (point - origin);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // normalize incoming coordinates to the range -1 to 1.0 in GLSL
    // then scale according to control.
    vec2 uv = 2.0 * fragCoord.xy / iResolution.y - 1.0;

    // build desaturation gradient from top to bottom of model
    // desaturation hits 0 (full color) about halfway down.
    float satGradient = smoothstep(-0.15, 0.4, uv.y);

    // translate for best viewing position, scale, and adjust
    // so position stays about the same while zooming
    uv.y += 1.0;
    uv *= iScale;
    //uv.y += iScale - 1.0;

    // motion of the field is modulated by audio level
    float phase = (iTime * 0.5) + iWow1 * bassLevel;

    for (float i = 1.0; i < floor(iQuantity); i++) {
        vec2 frequency = uv * i * i;
        uv.y += i * 0.1 / i *
        sin(frequency.x + phase) * sin(frequency.y + phase);
    }

    // Adjust output contrast and value
    // Allow some overdrive of color at high end of brightness
    vec3 col = iColorHSB;
    uv.y = pow(clamp(uv.y + 0.15, 0.0, 1.0), iWow2);
    col.y -= mix(0.0, uv.y, satGradient);

    // use our output brightness as alpha
    fragColor = vec4(hsv2rgb(col), uv.y);
}