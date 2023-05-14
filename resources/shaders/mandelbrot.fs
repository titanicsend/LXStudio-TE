// Animated Mandelbrot set for Titanic's End
// Just the usual z <= z^2 + c thing, with a little extra work
// on making it look better at the low physical resolution
// we use on the car.

// magic numbers for scaling result
#define magic1 1.25
#define magic2 1.9

// build 2D rotation matrix
mat2 rotate(float a) {
    return mat2(cos(a), -sin(a), sin(a), cos(a));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {

    // normalize, center, rotate, scale...
    vec2 uv = (fragCoord - .5 * iResolution.xy) / iResolution.y;
    uv *= rotate(-iRotationAngle);
    uv *= iScale;

    vec2 z = vec2(0.0);
    float dist, d1, d2;

    // define complex path for the orbits -- using tan() makes everything
    // jump outward towards infinity once in a while.
    vec2 path1 = 0.5 * vec2(0.23 * tan(iTime * 0.47), sin(iTime));
    vec2 path2 = 0.5 * vec2(cos(-iTime), 0.1 * tan(iTime * 0.25));

    // render a few iterations of the mandelbrot set
    for (float i = 0.; i < 50.; ++i) {
        z *= 2.0;
        // GLSL complex multiply from shadertoy
        z = mat2(z, -z.y, z.x) * z + uv;
        float p = magic1 / length(z - path1) + magic2 / length(z - path2);

        // set up so we can light the interior of the set for better visibility.
        // (it's kind of hard to get a good looking Mandelbrot set at low
        // visibility.
        dist = max(dist, p);
        d1 = dist;
        d2 = max(5. - dist, p);
    }

    // scale result by magic number and calculate color
    d1 *= 0.05; d2 *= 0.037; d1 = d1 + iWow1 * d2;

    vec3 col = min(1.1,d1) * iColorRGB;
    // light the background w/color 2

    col = mix(iWow2 * iColor2RGB,col,d1);
    fragColor = vec4(col, d1);
}