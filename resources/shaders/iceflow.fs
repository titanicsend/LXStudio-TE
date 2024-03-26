// Adapted from https://www.shadertoy.com/view/MdtGWH

uniform float focus;


const float NOISE_ITERATIONS = 8.;
const float LAYERS = 3.;

// coordinate offset weights
vec3 offsets = vec3(0.5, 0.4, 1.5);

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// get density of "ice" field at the specified point
float mapDensity( vec3 p) {
    float prev = 0.;
    float acc = 0.;
    float totalWeight = 0.;
    
    // stack up a few ice octaves
    for(float i = 0.; i < NOISE_ITERATIONS; i++) {
        float mag = dot(p, p);
        p = abs(p) / mag - offsets;
        float w = exp(-i / iQuantity);
        float diff = abs(mag-prev);  diff = diff * diff;
        acc += w * exp(-iQuantity * diff);
        totalWeight += w;
        prev = mag + w;
    }   
    return max(0., acc / totalWeight);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {

    // Rescale and position for best look on the vehicle
    vec2 uv = -0.5+0.5 * (fragCoord.xy / iResolution.y);
    uv += vec2(0.0,0.5);
    
    // move the center
    float k = 0.2f + iWow1 * bassLevel;

    float s1 = sin(iTime);  float c1 = cos(iTime * 0.5);
    vec2 pointShift = vec2(s1 + k * c1, c1 + k * s1);
    pointShift *= 0.3333;

    float density = 0.;
    offsets.z -= sinPhaseBeat;

    for(float i = 1.; i <= LAYERS; i++){
        density += mapDensity(vec3(uv, density) + vec3(pointShift / i, 0.));
    }

    density = clamp(density*density,0.,1.);
    vec3 col = vec3(iColorHSB.x,
                    (1.0 - density*density) * iColorHSB.y,
                    density * iColorHSB.z);
    col = hsv2rgb(col);

    fragColor = vec4(col, density);
}
