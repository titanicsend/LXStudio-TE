#define R fract(1e2 * sin(p.x * 8. + p.y))

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // shadertoy line:
    //     vec3 v = vec3(fragCoord / iResolution, 1) - 0.5;
    // our iResolution is a vec2, not a vec3, so I try to change to the below:
    vec3 v = vec3(fragCoord.x / iResolution.x - 0.5, fragCoord.y / iResolution.y - 0.5, 1);

    vec3 s = 0.5 / abs(v);
    s.z = min(s.y, s.x);

    vec3 i = ceil(8e2 * (s.z) * (s.y < s.x ? v.xzz : v.zyz));
    i *= 0.1;

    vec3 j = fract(i);
    i -= j;

    vec3 p = vec3(9, int(iTime * (9.0 + 8.0 * sin(i).x)), 0) + i;
    fragColor.g = R / s.z;
    p *= j;
    fragColor *= (R > 0.5 && j.x < 0.6 && j.y < 0.8 ? 1.0 : 0.0);
}