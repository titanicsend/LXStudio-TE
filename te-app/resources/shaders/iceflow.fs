// Adapted from https://www.shadertoy.com/view/MdtGWH

uniform float focus;
#include <include/constants.fs>
#include <include/colorspace.fs>

const float NOISE_ITERATIONS = 8.;
const float LAYERS = 3.;

// coordinate offset weights
vec3 offsets = vec3(0.5, 0.4, 1.5);

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

    fragColor = vec4(getPaletteColor_oklab(density), density);
}
