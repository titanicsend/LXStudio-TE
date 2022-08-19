#define PI     3.14159265
#define PI2     6.2831853

vec3 primaryGradient(float v) {
  v *= 2.0;
  if (v < 1.0) {
    return mix(iPalette[TE_BACKGROUND], iPalette[TE_TRANSITION], v);
  } else {
    return mix(iPalette[TE_TRANSITION], iPalette[TE_PRIMARY], v - 1.0);
  }
}

vec3 secondaryGradient(float v) {
  return mix(iPalette[TE_SECONDARY_BACKGROUND], iPalette[TE_SECONDARY], v);
}

vec3 primaryToSecondaryGradient(float v) {
  return mix(iPalette[TE_PRIMARY], iPalette[TE_SECONDARY], v);
}

// normalized HSV to RGB
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
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
    float t2 = time(.1) * PI2;
    float t3 = time(.523);
    float t4 = time(.343) * PI2;
    float scale = 5.0;


    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord/iResolution.xy;



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


    v = abs(h) + abs(m) + t1;
    v = mod(v, 1.0);
    v = triangle(v * v);
    //v = v * v;

    //original hsv calculates teal through purple colors (.45 - .85):
    // h = triangle(h) * .2 + triangle(x + y + z) * .2 + .45
    // hsv(h, 1, v)


    //for paint(), don't downscale the range
    h = triangle(triangle(h) * .2 + triangle(x + y + z) * .2 + iTime * .05);

    // Output to screen
//     fragColor = vec4(hsv2rgb(vec3(mod(h, 1.0), 1, v)), 1.0);

    fragColor = vec4(primaryToSecondaryGradient(h), v);
}