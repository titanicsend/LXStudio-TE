uniform float avgVolume;

const float PI = 3.14159265359;

//  Hash function from Dave Hoskin's Hash without Sine
// https://www.shadertoy.com/view/4djSRW
vec4 hash42(vec2 p) {
    vec4 p4 = fract(vec4(p.xyxy) * vec4(.1031, .1030, .0973, .1099));
    p4 += dot(p4, p4.wzxy+33.33);
    return fract((p4.xxyz+p4.yzzw)*p4.zywx);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// Density field generator. This emperically derived mystery
// distance function makes a field of clean, nicely formed discrete
// color "blobs" that look good when kaleidoscoped.
// Set the number of kaleidoscope slices to 1 to see the raw field
// output.
float field2( vec2 p, vec2 center, float r ) {
    float d = length( p - center ) / r;
    d = min(1.0/exp(d * d),0.995) ;
    return d * d * d;
}

// smooth radial reflections.
vec2 Kaleidoscope(vec2 uv, float reflections) {
    float angle = PI / reflections;
    float r = length(uv*.5);

    // very convieniently, we can handle rotation in here too
    float a = -iRotationAngle + atan(uv.y, uv.x) / angle;

    a = mix(fract(a), 1.0 - fract(a), mod(floor(a), 2.0)) * angle;
    return vec2(cos(a), sin(a)) * r;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 aspect = iResolution.xy / min(iResolution.x, iResolution.y);
    vec2 uv = (fragCoord.xy * 2.0 - iResolution.xy) / min(iResolution.x, iResolution.y);

    // generate radial reflections about the origin
    // (and add a little extra coordinate distortion first)
    uv += iWow2 * 0.05 * sin(length(uv) * 24.0);
    uv = Kaleidoscope(uv, iQuantity);

    float bandLevel = texture(iChannel0, vec2(uv.x, 0)).r/avgVolume;
    uv *= iScale + 0.2 * bandLevel;

    vec3 final_color = vec3(0.0);

    // iterate to generate a density field based on a sample
    // of random coordinates around the current pixel.
    float final_density = 0.0;
    for (int i = 0; i < 128; i++) {
        // handy noise vector which we'll use for position and color
        // accumulation
        vec4 noise  = hash42(vec2(float(i) + 0.5, 0.5));
        vec2 pos = noise.xy;

        // "velocity" moves the field based on what's actually going on in the music
        // at the current "real" pixel location
        vec2 velocity = 2.0 + (0.15 * abs(pos) * bandLevel);

        pos += iTime * velocity * 0.2;
        pos = mix(fract(pos), fract(pos), mod(floor(pos), 2.0));

        // normalize pos to display aspect ratio even though it's fake because
        // it looks better!
        pos = (pos * 2.0 - 1.0) * aspect;

        // "intensity" is the audio level at the current random sample position
        float intensity = texture(iChannel0, vec2(pos.y, 0)).r/avgVolume;

        // generate blob radius based on audio level at fake position
        // iWow1 controls reaction intensity (which is the size of the random field
        // blob at the current location.)
        float radius = (1.0 + iWow1) * intensity * clamp(noise.w, 0.3, 0.8);
        radius = clamp(radius, 0.1*abs(pos.x), iWow1);

        // accumulate field density
        float density = field2(uv, pos, radius);
        final_density += density;
        final_color += density * noise.xyz;
    }

    // gamma correct density and apply color
    final_density = pow(clamp(final_density - 0.1, 0.0, 1.0), 2.2);
    fragColor = vec4( final_color *  final_density, 1.0);
}