#include <include/constants.fs>
#include <include/colorspace.fs>

float step = 0.05;   // star spacing
float indent = 0.06;     // shape indent for stars
float numPoints = 5.;

// make 2d rotation matrix
mat2 r2d(float a) {
    float c=cos(a),s=sin(a);
    return mat2(c, s, -s, c);
}

// Minkowski distance at fractional exponents makes the nice 4-pointed star!
float minkowskiDistance(vec2 uv, float p) {
    return pow(pow(abs(uv.x), p) + pow(abs(uv.y), p), 1.0 / p);
}

// Probably not really that random...
float rand(vec2 p) {
    return fract(sin(dot(p, vec2(12.543,514.123)))*4732.12);
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

    float ss = size; // * (1. + 0.2 * sin(iTime * hash(size) * 20. ) );
    float r = numPoints * ss;

    float star = smoothstep( r, r + 0.005, dist );
    return star;
}
void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec3 color = vec3(0.0);

    // normalize and shift origin to center (-0.5-0.5 coordinate range)
    vec2 uPos = -0.5+(fragCoord.xy / iResolution.xy );

    // star size pulses with the music!
    float pulse = iScale * ((iWow1 * (-0.5+beat)) + .618);
    float dance = 0.0025 + (0.5 * bassLevel * iWow1);
    float brightness = 0.;

    vec3 starColor;
    vec3 finalColor = vec3(0.0);
    float tOffset = 0.0;
    float starTime = iTime / 8.0;
    float v = iQuantity * 2.0;

    for (float j = 0; j < v; j++) {
        tOffset += TAU * 10.618;
        starColor = getGradientColor(j / v);
        for (float i = 1.; i <= v; i++) {
            // time offset for each star.
            float d = 0.05 * i;
            float t = (tOffset + starTime) + d;
            float pulse = iWow1 * beat;

            // calculate point offset and rescale for our normalized coord range
            vec2 point = cos(t*d)*vec2(cos(t*1.5), sin(t*3.) ) + sin(t*d) * vec2( cos(t*2.), sin(t*.75) );
            point = uPos - point / 2.2;

            // if we're rotating, give the individual stars slightly different rates
            float theta = iRotationAngle + 0.5 * i * j;

            // add brightness contribution of each star
            float beatBlink = (rand(vec2(i,j)) < 0.5) ? 1.0 - pulse : 1.0;
            float beatScale = (rand(vec2(j,i)) < 0.5) ? iScale + 2.0 * pulse : iScale;
            float starBright = beatBlink * (1.0 - drawStar(point, 0.001 * beatScale, theta));
            finalColor = mix(finalColor, starColor * starBright, starBright);


        }
    }

    fragColor = vec4(finalColor,0.996);
}