// A kali-style iterative fractal
//   (https://www.fractalforums.com/new-theories-and-research/very-simple-formula-for-fractal-patterns/)
// that tries to do for rectangles what @andrewlook's shaders did for triangles.
// The "artistic intention" is to sorta depict what's going on in all the circuitry inside Titanic's End
//
// Wow2 controls audio reactivity
#pragma name "Circuitry"
#pragma TEControl.SIZE.Range(2.0,5.0,0.1)
#pragma TEControl.QUANTITY.Range(4.0,3.0,6.0)
#pragma TEControl.WOW2.Value(0.6)
#pragma TEControl.WOW1.Disable
#pragma TEControl.WOWTRIGGER.Disable

#include <include/constants.fs>
#include <include/colorspace.fs>

mat2 rot(float a) {
    float s=sin(a), c=cos(a);
    return mat2(c,s,-s,c);
}

float minIteration;
float bandLevel;

float fractal(vec2 p) {
    // Rock! (oscillate on x a little with the beat.)
    p.x += bandLevel;

    float dist = 999., minDist = dist;
    minIteration=0.;

    for (float i = 0.; i < iQuantity; i++) {
        p = abs(p);
        // adjust viewing region in x and y to get the most "interesting"
        // part.
        p = p/clamp(p.x*p.y,0.15,5.) - vec2(1.525,1.5);

        // animated manhattan distance gives size changing rectangular
        // features
        float m = abs(p.x+sin(iTime * 2.));
        if (m < dist) {
            dist = m + smoothstep(0.1,abs(p.y), fract(beat+i*.5));
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
    return exp(-20.*dist)+exp(-10.*minDist);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    // normalize, center, correct aspect ratio
    vec2 uv = fragCoord.xy/iResolution.xy - 0.5;
    uv.x *= iResolution.x/iResolution.y;
    uv *= iScale + 0.05 * (-0.5+sinPhaseBeat);
    uv *= rot(iRotationAngle);

    // Generate an easy-to-track audio reactive value
    // n.b.  this is kind of underbaked, so feel free to improve it!
    // slightly improved to dodge near-infinite coord shift at very low volume
    float vol = trebleLevel + bassLevel;
    bandLevel = (vol > 0.1) ? iWow2 * (-0.5 + max(trebleLevel, bassLevel) / vol) : 0.0;

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
    c = c/(aa*aa*0.33);
    fragColor = vec4(c * vec3(mix(iColorRGB, iColor2RGB, mod(minIteration,2.0))), c);
}
