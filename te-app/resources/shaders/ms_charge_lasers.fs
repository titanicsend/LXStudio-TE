#include <include/constants.fs>
#include <include/colorspace.fs>

uniform float progress;

// It's yet another KITT like pattern!
void mainImage( out vec4 fragColor, in vec2 fragCoord )  {
    float tailPct = .9;         // length of the tail in 0..1
    float pct1;
    float bri;

    // Normalize incoming pixel coords
    vec2 uv = -1. + 2. * fragCoord.xy / iResolution.xy;
    uv = 0.5 * abs(uv);

    // build the moving wave
    pct1 = 1.0 - (progress - uv.x);
    pct1 = (pct1 > 1.0) ? 0.0 : pct1;
    bri =  clamp(pct1,0.,1.);
    bri = bri * bri * bri;

    vec3 c1 = mix(iColorRGB,iColor2RGB,abs(sin(12.5 * uv.x * uv.y))) * bri;

    // add a fizzling sparkle effect to the end of the wave
    // (this is just a super cheap way of making noise, really)
    if (bri < 0.6 && pct1 > 0) {
        float ss = iTime * 9.5; // sparkle speed
        vec2 light = vec2(cos(ss), sin(ss));
        vec2 direction = normalize(texture(iChannel1,uv).xy - .5);
        float sparkle = dot(direction, light);
        sparkle = step(0.975,sparkle);
        c1 = (c1 + bri * sparkle);
    }

    fragColor = vec4(c1,1.0);
}
