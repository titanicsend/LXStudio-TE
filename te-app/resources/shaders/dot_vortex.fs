// we're working w/3d coordinates and will need to handle
// the translation controls from here instead of in
// the shader framework.
#define TE_NOTRANSLATE

#include <include/constants.fs>
#include <include/colorspace.fs>

// Draws "dots" in a moving pattern of multiple spirals.  The
// dots can also be rings or arc segments, depending on control
// settings.
//
// Really cool spiral segmentation technique, plus nice way of
// parameterizing moving waves from:
// https://www.shadertoy.com/view/lsdBzX
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    float  c = 0.0;// brightness accumulator
    float n = 2.0 -  abs(sin(iTime / 60.));// main animation timing driver
    float e = n * 2.;// exponent for spiral gradient
    float z = pow(500., n);// gently animated autozoom

    // here, we use the model's full 3D coordinates
    // to rotate about the X axis.  Visually, this
    // is the car's long axis.
    vec3 p3d = _getModelCoordinates().xyz;

    // center coordinate origin
    p3d -= 0.5;

    // build matrix
    float c1 = cos(-iRotationAngle);
    float s1 = sin(-iRotationAngle);
    mat3 rotX = mat3(1, 0, 0, 0, c1, -s1, 0, s1, c1);

    // apply3D rotation
    p3d = vec3(rotX * p3d.xyz);

    // scale to [0,1] range
    vec2 uv = 2. * (p3d.xy - iTranslate);

    // align hole in spiral center with DJ booth
    uv.y += 0.5;

    vec2 u = uv*iScale*z;            // apply scale and autozoom
    float ro = -PI / 2.;             // rotation offset
    float cr = sin(iTime / 20.0) * TAU;// counter rotation

    float a = atan(u.y, u.x)-ro;     // angle from center

    float i = a/TAU;                 // shift atan result to +/-0.5

    float r = exp(log(length(u))/e); // radius
    float sc = ceil(r-i);            // spiral contour
    float s = pow(sc+i, 2. * iWow1); // spiral gradient (dot shape)
    float vd = cos((sc*TAU+a)/n);    // dot density (really field scale)
    float ts = cr+s/n*TAU;           // segment spiral in time

    c += sin(ts / 2.);               // spiral 1 wave
    c *= cos(ts);             // spiral 2 wave
    // we could add more waves!  It gets pretty hard to see at low res though.

    // control darkness between spirals
    c *= pow(abs(sin((r-i)*PI)),  abs(e)+1.);
    c *= .2+abs(vd);                         // brighten the dots
    c = min(c, pow(length(u)/z, 1./n));      // sharpen color gradient
    c = c * 3.;                              // and brighten the whole mess

    vec3 rgb = getGradientColor(fract(n + vd + 1.));

    fragColor = vec4(rgb, c);
}
