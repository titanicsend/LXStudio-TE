// MUCH FASTER replacement for the Spiral Diamonds java pattern
//
uniform float speedAngle;

mat2 rot(float a) {
    float s=sin(a), c=cos(a);
    return mat2(c,s,-s,c);
}

// #define r(a) mat2(cos(a),-sin(a),sin(a),cos(a))

void mainImage( out vec4 fragColor, vec2 fragCoord ) {
    // normalize coords to the range (-1,1)
    vec2 uv = (2.0 * fragCoord - iResolution.xy) / iResolution.xy;
    uv.y += .5;

    float warp =  30.0 * trebleRatio;
    float warpAmt = frequencyReact * 0.08;

    // warp coordinate system in a waving flag pattern w/trebleLevel.
    // Note that this reproduces a subtle bug in the original java pattern.
    // The already altered uv.x coordinate is used to calculate the warp for
    // the y coordinate. It makes the coordinate distortion just a little weirder
    // so I kept it.
    uv.x -= warpAmt * sin(uv.y * warp);
    uv.y += warpAmt * cos(uv.x * warp);

    uv *= iScale;

    // move to car center on y axis and repeat pattern over x axis
    // at tiling interval to show two spirals at the default scale,
    // one on each end of car.
    float interval = 1.2;
    uv.x = mod(uv.x,interval) - 0.5 * interval;

    uv *= rot(-iRotationAngle);

    // This pattern actually draws 4 symmetrical patterns of lines moving outward.
    // To keep it looking like a spiral, we need to rotate the lines in each
    // quadrant a little so they fit together. The required angle changes with
    // iQuantity, so we adjust it using the expression below. (Thank you, Excel
    // regression solver!)
    // Note that the old java SpiralDiamonds didn't do this. It was too slow.
    // Look at it carefully and you can see the spiral lines drift apart at the ends
    // of squarocity's range.
    vec2 s = sign(uv) * rot(0.6321 * pow(iQuantity, -0.8794));
    float squarocity = speedAngle + (levelReact * bassRatio * 3.14159);

    // This version does a little antialiasing that wasn't possible in the original.
    float dx = abs(sin(iQuantity * log(dot(uv, s)) + atan(s.y,s.x) - squarocity));
    fragColor = vec4(iColorRGB * smoothstep( .7, .5, dx),1.0);
}