// A kali-style iterative fractal
//   (https://www.fractalforums.com/new-theories-and-research/very-simple-formula-for-fractal-patterns/)
// that tries to do for rectangles what @andrewlook's shaders did for triangles.
// The "artistic intention" is to sorta depict what's going on in all the circuitry inside Titanic's End
//
// Wow2 controls audio reactivity
#pragma name "Circuitry"
#pragma TEControl.SIZE.Range(2.0,10.0,0.1)
#pragma TEControl.QUANTITY.Range(4.0,3.0,6.0)
#pragma TEControl.WOW1.Range(0.0,0.0,0.25)
#pragma TEControl.WOW2.Disable
#pragma TEControl.WOWTRIGGER.Disable
#pragma TEControl.LEVELREACTIVITY.Disable
#pragma TEControl.FREQREACTIVITY.Disable

#include <include/constants.fs>
#include <include/colorspace.fs>

mat2 rot(float a) {
    float s=sin(a), c=cos(a);
    return mat2(c,s,-s,c);
}

float minIteration;
float bandLevel;

float fractal(vec2 p) {
    float dist = 999., minDist = dist;
    minIteration=0.;

    p.y+=iTime / 3.0;
    p.x+=sin(iTime * 0.1);
    p.y=fract(p.y*1.05);

    for (float i = 0.; i < iQuantity; i++) {
        p = abs(p);
        // adjust viewing region
        // part.
        p = p/clamp(p.x*p.y,0.15,5.) - vec2(1.525,1.5);

        // animated manhattan distance gives size changing rectangular
        // features
        float m = abs(p.x);;
        if (m < dist) {
            dist = m + max(0.025,abs(sin(PI * fract(iTime)+float(i))));
            minIteration = i;
        }
        // using minimum of manhattan and euclidean distance over all iterations
        // gives bright glow to areas of concentration.  If you zoom out a bit
        // you can really see that it's a "normal" fractal. (Zoom in and it
        // just looks like rectangles.)
        minDist = min(minDist,length(p));
    }

    // scale the fractal results to smaller (hand tuned) values to use
    // when calculating color
    return exp(-5. * dist)+exp(-8. * minDist);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    // normalize, center, correct aspect ratio
    vec2 uv = fragCoord.xy/iResolution.xy - 0.5;
    uv.x *= iResolution.x/iResolution.y;

    uv *= iScale;
    uv *= rot(iRotationAngle);
    
    // draw the fractal, antialiased by drawing it multiple times
    // at very small coordinate offsets.  For the car, we don't need
    // many iterations, but a few makes it look much smoother.
    float aa=1.;

    // distance between samples.
    vec2 sc=1./iResolution.xy/aa;

    float c = 0.0;
    for (float i=-aa; i < aa; i++) {
        for (float j =- aa; j < aa; j++) {
            vec2 p = uv + vec2(i,j) * sc;
            c += fractal(p);
        }
    }
    c = c/2.;

    fragColor = vec4(getGradientColor(minIteration/floor(iQuantity - 1.0)),max(iWow1,c));
}
