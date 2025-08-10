#include <include/constants.fs>
#include <include/colorspace.fs>

float spacing = 0.2;   // star spacing
float indent = 0.06;     // shape indent for stars
float numPoints = 5.;

// make 2d rotation matrix
mat2 r2d(float a) {
    float c=cos(a),s=sin(a);
    return mat2(c, s, -s, c);
}

// Random-ish number in the range [-1, 1]
float rand(vec2 p) {
    return fract(sin(dot(p, vec2(12.543,514.123)))*4732.12);
}

// sawtooth to [0,1] sinusoidal wave
float wave(float n) {
    return 0.5+(sin(TAU * abs(fract(n))) * 0.5);
}

// Minkowski distance at fractional exponents makes the nice 4-pointed star!
// NOTE: Not currently used, but keeping here for reference.
float minkowskiDistance(vec2 uv, float p) {
    return pow(pow(abs(uv.x), p) + pow(abs(uv.y), p), 1.0 / p);
}

// New star SDF from https://www.shadertoy.com/view/4tfGWr
// NOTE:  I love the minkowski distance stars, but the SDF way
// is a lot faster and draws cleaner stars.
float drawStar(vec2 o, float size, float startAngle) {
    vec2 q = o;
    q *= r2d(-startAngle); ;

    float angle = atan( q.y, q.x ) / TAU;
    float segment = angle * numPoints;
    float segmentI = floor(segment);
    float segmentF = fract(segment);

    angle = (segmentI + 0.5) / numPoints;
    angle += ((segmentF > 0.5) ? -indent : indent);
    angle *= TAU;

    vec2 outline;
    outline.y = sin(angle);
    outline.x = cos(angle);
    float dist = abs(dot(outline, q));

    float ss = size;
    float r = numPoints * ss;

    return smoothstep( r, r + 0.005, dist );
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec3 color = vec3(0.0);

    // normalize and shift origin to center (-0.5-0.5 coordinate range)
    vec2 uPos = -0.5+(fragCoord.xy / iResolution.xy );

    vec3 starColor;
    vec3 finalColor = vec3(0.0);
    float t = iTime / 8.0;
    float v = iQuantity * 2.0;

    for (float j = 0; j < v; j++) {
        // each group of stars gets its own starting time offset
        t += TAU * .618;
        // and its own color (higher iWow2 == more monochromeness)
        starColor = mix(getGradientColor(j / v),iColorRGB,iWow2);
        for (float i = 1.; i <= v; i++) {
            float pct = 0.5 * step (0.5, i/v);
            // shape the beat sawtooth so its peak will be very short
            float pulse = 1.0 - fract(beat + pct);
            pulse = iWow1 * pulse * pulse;
            // distance offset for each star.
            float d = spacing * i;

            // calculate point offset and rescale for our normalized coord range
            vec2 point = cos(t*d)*vec2(cos(t*1.5), sin(t*3.) ) + sin(t*d) * vec2( cos(t*2.), sin(t*.75) );
            point = uPos - point / 2.2;

            // hash value based on [i,j], precalculated since we use it twice.  Saves
            // a call to rand() in the inner loop.
            float rij = rand(vec2(i,j));

            // give the individual stars slightly different spin rates
            float theta = iRotationAngle * rij;

            // add brightness contribution of each star, including beat reactivity
            float beatScale = (abs(rand(vec2(j,i))) < 0.6) ? iScale + 2.0 *  pulse : iScale;
            float beatBlink = (abs(rij) < 0.2) ? 1.0 + iWow1 * (1.0 - wave(4.0 * pulse)) : 1.0;
            float starBright = beatBlink * (1.0 - drawStar(point, 0.001 * beatScale, theta));
            finalColor = mix(finalColor, starColor * starBright, starBright);
        }
    }

    fragColor = vec4(finalColor,0.996);
}