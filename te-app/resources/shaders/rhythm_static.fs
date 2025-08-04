#include <include/constants.fs>
#include <include/colorspace.fs>
#define LAYERS 3

// build 2D rotation matrix
mat2 rotate(float a) {
    return mat2(cos(a), -sin(a), sin(a), cos(a));
}

// fast integer hash
// adapted from https://www.shadertoy.com/view/ttc3zr
int murmurHash(int h) {
    h ^= h >> 16;
    h *= 0x85ebca6b;
    h ^= h >> 13;
    h *= 0xc2b2ae35;
    h ^= h >> 16;
    return h;
}

// use int hash to generate a random 2D vector
vec2 randomVec2(ivec2 seed) {
    int seed2 = int(seed.x) * 73856093 + int(seed.y) * 19349663;
    int hash = murmurHash(seed2);
    int ri = hash & 0xFFFFFFFF;
    float rf = float(ri) / float(0xFFFF);
    float theta = rf * TAU;
    return vec2(1, 0) * rotate(theta);
}

// build a nice animated composite static field from multiple layers of noise
void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    // center, rotate and restore original position
    vec2 uv = fragCoord - (iResolution.xy * 0.5);
    uv *= rotate(iRotationAngle);
    uv += iResolution.xy * 0.5;

    // we keep the numbers large b/c our fast hash function/prng works
    // with integers, and we need enough space to keep precision and
    // avoid collisions.
    uv += vec2(1000000, 1000000.0);

    //float bassEffect = bassRatio * levelReact;
    //float trebleEffect = trebleRatio * frequencyReact;
    float bassEffect = bassRatio * levelReact;
    float trebleEffect = trebleRatio * frequencyReact;

    // Size control manages overall scale - from dots to HUGE blocks.
    int startingPower = 1 + int(iScale) + int(clamp(2.*bassEffect, 0. ,4.));
    float sum = 0.0;

    // subdivide the field into layers of randomly colored powers-of-two sized blocks

    // random rotation vector for each layer.  Declared outside the loop so
    // we can use it for final color too.
    vec2 rVec;
    for(int layer = 0; layer < LAYERS; layer++) {
        float power = float(startingPower) + float(layer);
        float scale = pow(2.0, power);
        ivec2 iuv = ivec2(uv / scale);
        rVec = randomVec2(iuv);

        float timeFactor = 8.0;
        if (iWowTrigger) {
            timeFactor *= (1. + 0.05*trebleEffect);
        }

        // rotate unit vector based on time to generate smooth movement,
        // and reduce the brightness with each layer.
        float value = dot(rVec, vec2(1, 0) * rotate(iTime*timeFactor)) / (pow(2.0, float(layer)) * 2.0);

        // last (topmost) layer is the smallest. Brighten it to make it more visible
        sum += (layer == LAYERS - 1) ? 2.0 * value : value;
    }

    // interpolate random color from current palette
    vec3 color = getGradientColor(fract(rVec.x + rVec.y));

    // threshold values by iQuantity, gamma adjust, and flash the whole thing to the beat
    // with depth controlled by WOW1.
    sum *= 1.25 * step(1.0-(iQuantity*(1.+trebleEffect)), sum) * (1.0 - (beat * iWow1));
    fragColor = vec4(color, sum);
}