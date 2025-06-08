#pragma name "Vortex1"

#define M iMouse
#define R iResolution.xy
#define PI 3.14159265358979
#define TAU 6.28318530717958

// generate 2D rotation matrix
mat2 r2d(float a) {
    float c=cos(a),s=sin(a);
    return mat2(c, s, -s, c);
}

void mainImage( out vec4 RGBA, in vec2 XY ) {
    vec3 c = vec3(0);

    float t = -.54+(mod(iTime,30.)*TAU)/3600.;
    float n = (cos(t) > 0.) ? sin(t): 1./sin(t); // t to sin/csc
    float e = n*2.;                              // exponent
    float z = iScale * clamp(pow(500., n), 1e-16, 1e+18); // zoom

    // normalize coords
    vec2 uv = (XY-.5*iResolution.xy)/iResolution.y*2; // screen coordsRo
    uv *= iScale;
    uv *= r2d(-iRotationAngle);

    vec2 u = uv*z;                // coords with zoom
    float ro = iRotationAngle,   // rotation
    cr = -iRotationAngle,            // counter rotation
    a = atan(u.y, u.x)-ro,        // screen arc

    i = a/TAU,                 // arc to range between +/-0.5
    r = exp(log(length(u))/e), // radius | slightly faster than pow(length(u), 1./e)
    sc = ceil(r-i),            // spiral contour
    s = pow(sc+i, 2.),         // spiral gradient
    vd = cos((sc*TAU+a)/n),    // visual denominator
    ts = cr+s/n*TAU;           // segment with time

    c += sin(ts/2.); // spiral 1
    c *= cos(ts);    // spiral 2

    c *= pow(abs(sin((r-i)*PI)), abs(e)+5.);    // smooth edges & thin near inf
    c *= .2+abs(vd);                            // dark folds
    c = min(c, pow(length(u)/z, -1./n));        // dark gradient

    vec3 rgb = mix(iColorRGB,iColor2RGB,fract(abs(vd+1.)));
    RGBA = vec4(c*2.*rgb, 1.);
}
