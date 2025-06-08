#pragma name "StrangePinwheel"
#pragma TEControl.SIZE.Range(3.0,0.1,5.0)
#pragma TEControl.QUANTITY.Range(4.0,3.0,24.0)
#pragma TEControl.WOW1.Range(1.0,0.0,1.0)
#include <include/constants.fs>
#include <include/colorspace.fs>

mat2 rot(float a) {
    float s=sin(a), c=cos(a);
    return mat2(c,s,-s,c);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    // normalize, center, correct aspect ratio
    vec2 uv = fragCoord.xy/iResolution.xy - 0.5;
    uv.x *= iResolution.x/iResolution.y;
    uv *= rot(iRotationAngle);
    uv *= iScale;

    float timebase = mod(iTime * 0.15, 2.0) * 10.0;
    float t1 = timebase;

    float d = 1.0 - abs(sin(length(uv) - t1)); // waves
    float a = abs(mod(atan(uv.y, uv.x) - t1, 2.0 - abs(sin(iTime * 0.2)))); // rays

    float b = fract(max(a, d));
    //vec3 color = hsv2rgb(vec3(b - d, a + b / 2., b * b * b));
    vec3 color = getGradientColor(b - d);

    fragColor = vec4(color, b * b * b);
}