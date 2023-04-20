
const float step = 0.5;  // star spacing

// Minkowski distance at fractional exponents makes the nice 4-pointed star!
float minkowskiDistance(vec2 uv, float p) {
   return pow(pow(abs(uv.x), p) + pow(abs(uv.y), p), 1.0 / p);
}

// normalized HSV to RGB
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {

    // normalize and shift origin to center (-0.5-0.5 coordinate range)
    vec2 uPos = -0.5+(fragCoord.xy / iResolution.xy );

    // star size pulses with the music!
    float pulse = iScale * ((iWow1 * (-0.5+beat)) + .618);
    float dance = 0.0125 + (0.5 * bassLevel * iWow1);
    float brightness = 0.;

    // for each star
    for (float i = 1.; i <= iQuantity; i++) {
        // time offset for each star.
        float t = iTime + step*i;

        // calculate point offset and rescale for our normalized coord range
        vec2 point = vec2(0.92 * sin(t) + 0.08 * cos(t * 6.0),
                          -0.3 + (0.65 * sin(t * 0.85) + dance * sin(t * 2.0)));
        point = uPos - point/2.0;

        // if we're rotating, give the individual stars slightly different rates
        if (iRotationAngle != 0.) {
            float theta = iRotationAngle + 0.4 * i;
            mat2 rot = mat2(cos(theta), -sin(theta),
                            sin(theta), cos(theta));
            point = point *  rot;
        }

        // add brightness contribution of each star
        brightness += pulse / minkowskiDistance(point,0.375) / (i + 1.);
    }

    // minkowski distance approaches infinity as point approaches origin so
    // we need to scale the curve back to something reasonable
    brightness = max(0.0,log(brightness * iWow2));

    vec3 col = vec3(iColorHSB.x,
                    iColorHSB.y * clamp(2.25-brightness,0.0,1.0),
                    iColorHSB.z * clamp(brightness * brightness,0.0,1.0));

    fragColor = vec4(hsv2rgb(col),col.z);
}
