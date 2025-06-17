#include <include/constants.fs>
#include <include/colorspace.fs>

// generate 2D rotation matrix
mat2 r2d(float a) {
    float c=cos(a),s=sin(a);
    return mat2(c, s, -s, c);
}

void mainImage( out vec4 RGBA, in vec2 XY ) {
    float  c = 0.0;                          // brightness accumulator
    float t = sin(iTime / 6.) * TAU;
    float n = 2.0 -  abs(sin(iTime / 60.));
    float e = n * 2.;
    float z = pow(500., n);                  // zoom factor

    vec4 p3d = _getModelCoordinates();
    p3d -= 0.5;  // center coordinate origin
    float c1 = cos(iRotationAngle);
    float s1 = sin(iRotationAngle);
    mat3 rotX = mat3(1, 0, 0, 0, c1, -s1, 0, s1, c1);
    // apply rotation
    p3d = vec4(rotX * p3d.xyz, p3d.w);

    // scale to [0,1] range
    vec2 uv = 2. * p3d.xy;
    //uv *= r2d(-iRotationAngle);
    uv.y += 0.25; // align hole in spirals with DJ booth

    vec2 u = uv*iScale*z;       // coords with zoom
    float ro = -PI / 2.,        // rotation
    cr = sin(iTime / 20.0) * TAU,      // counter rotation
    a = atan(u.y, u.x)-ro,      // screen arc

    i = a/TAU ,                 // arc to range between +/-0.5
    r = exp(log(length(u))/e),  // radius
    sc = ceil(r-i),             // spiral contour
    s = pow(sc+i, 2. * iWow1),         // spiral gradient (dot shape)
    vd = cos((sc*TAU+a)/n),     // visual denominator
    ts = cr+s/n*TAU;            // segment with time

    c += sin(ts / 2.); // spiral 1
    c *= cos(ts);      // spiral 2

    c *= pow(abs(sin((r-i)*PI)), abs(e)+1.);    // smooth edges
    c *= .2+abs(vd);                            // dark folds
    c = min(c, pow(length(u)/z, 1./n));         // dark gradient
    c += c * 2;

    vec3 rgb = getGradientColor(fract(n + vd + 1.));
    RGBA = vec4(rgb, c);
}
