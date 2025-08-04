#include <include/constants.fs>
#include <include/colorspace.fs>

// build 2D rotation matrix
mat2 rotate(float a) {
    return mat2(cos(a), -sin(a), sin(a), cos(a));
}

float time(float interval) {
    return mod(iTime / 65.536, interval) / interval;
}

float wave(float v) {
    return (sin(v*PI*2.0) + 1.0)/2.0;
}

float triangle(float v) {
    v = mod(v * 2.0, 2.0);
    if (v < 0.0)
        v += 2.0;
    return v < 1.0 ? v : 2.0 - v;
}

float xorf(float v1, float v2) {
    v1 *= 65536.0;
    v2 *= 65536.0;
    return float( int(v1) ^ int(v2)) / 65536.0;
}

float measure() {
    return beat;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    float t1 = time(.1);
    float t2 = time(.1) * TAU;
    float t3 = time(.523);
    float t4 = time(0.343) * TAU;
    float scale = iScale;

    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord/iResolution.xy;
    uv -= 0.5;
    uv.x = abs(uv.x);
    uv *= rotate(iRotationAngle);

    float m, h, v, x, y, z;

    y = uv.y + time(.1) + sinPhaseBeat * .1; //bounce / fall
    // y += time(.1) //blittery waterfalls!
    z = triangle(uv.x * 2.0 ) + triangle(uv.y + .25) ; //breathing + fake depth
    x = uv.x;

    m = .3 + triangle(t1) * .2; //.3 to .5

    h = sin(t2) + wave( 1.0 +
        mod(
            xorf(scale * (x - .5), xorf(scale * (z - .5),scale * (y - .5))) //xor coordinates
            / 50.0 * (triangle(t3) * 10.0 + 4.0 * sin(t4)) //vary the density/detail
        , m) //variable modulus wrapping and range
    );

    v = abs(h) + abs(m) + beat;
    v = mod(v, 1.0);
    v = 1.0 - iWow1 * triangle(v);

    //for paint(), don't downscale the range
    h = triangle(triangle(h) * .2 + triangle(x + y + z) * .2 + iTime * .05);

    fragColor = vec4(getGradientColor(h), v * v);
}