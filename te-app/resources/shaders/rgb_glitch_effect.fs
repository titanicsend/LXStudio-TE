#define PI 3.1415926535897932384626433832795

uniform sampler2D iDst; // input texture
uniform float basis;


// smooth radial reflections
vec2 Kaleidoscope(vec2 uv, float reflections) {
    // use high frequency content to modulate the reflection
    // angle.  This creates an interesting "folding" effect.
    float k = 0.0;
    float angle = PI / (reflections + k);

    float r = length(uv*.5);
    float a = atan(uv.y, uv.x) / angle;
    a = mix(fract(a), 1.0 - fract(a), mod(floor(a), 2.0)) * angle;
    return vec2(cos(a), sin(a)) * r;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
     vec2 uv = fragCoord / iResolution.xy;
    uv = Kaleidoscope(uv,3.0);

    vec2 finalCoords = iResolution.xy * uv;
    fragColor = _getMappedPixel(iDst, ivec2(finalCoords));
}