#define R fract(1e2 * sin(p.x * 8. + p.y))

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec3 v = vec3(fragCoord / iResolution.xy, 1.0) - 0.5;

    // v.z controls the overall scale - need to adjust actual control
    // values to make this more sane.
    v.z *= iScale * 0.1;

    vec3 s = 0.5 / abs(v);
    s.z = min(s.y, s.x);

    vec3 i = ceil(8e2 * (s.z) * (s.y < s.x ? v.xzz : v.zyz));
    i *= 0.1;

    vec3 j = fract(i);
    i -= j;

    vec3 p = j * vec3(9, int(iTime * (9.0 + 8.0 * sin(i).x)), 0) + i;
    vec3 color = mix(iColorRGB,iColor2RGB,vec3(R / s.z));

    // the constants compared to R, j.x, and j.y control how filled in each block is
    // higher values give more fill, lower values, more dither.
    fragColor = vec4(color * (R > 0.15 && j.x < 0.675 && j.y < 0.675 ? 1.0 : 0.0), 1.0);
}