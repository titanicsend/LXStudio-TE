const float PI = 3.14159265359;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return rotationMatrix * point;
}

// Density field generator. This emperically derived mystery
// distance function makes a field of clean, nicely formed discrete
// color "blobs" that look good when kaleidoscoped.
// Set the number of kaleidoscope slices to 1 to see the raw field
// output.
float field2(vec2 p, vec2 center, float r) {
    float d = length(p - center) / r;
    d = clamp(1.0/exp(d), 0.0, 0.995);
    return d * d;
}

// smooth radial reflections
vec2 Kaleidoscope(vec2 uv, float reflections) {
    // use high frequency content to modulate the reflection
    // angle.  This creates an interesting "folding" effect.
    float k = frequencyReact * 3. * sin(trebleLevel * PI);
    float angle = PI / (reflections + k);

    float r = length(uv*.5);
    float a = atan(uv.y, uv.x) / angle;
    a = mix(fract(a), 1.0 - fract(a), mod(floor(a), 2.0)) * angle;
    return vec2(cos(a), sin(a)) * r;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // save the original aspect ratio for later use, and normalize coords
    vec2 aspect = iResolution.xy / min(iResolution.x, iResolution.y);
    vec2 uv = (fragCoord.xy * 2.0 - iResolution.xy) / min(iResolution.x, iResolution.y);

    // rotate according to current control settings
    uv = rotate(uv,iRotationAngle);

    // generate radial reflections about the origin
    uv = Kaleidoscope(uv, iQuantity);

    // ratio of level at current pixel's eq band to EMA volume from engine
    // Wow2 acts as an overall level adjustment.
    float level = levelReact * volumeRatio;
    float intensity = levelReact * bassRatio;

    // modulate overall scale by level just a little for more movement.
    // (too much looks jittery)
    uv *= iScale + 0.1 * level;

    // iterate to generate a density field based on a sample
    // of random coordinates around the current pixel.
    float final_density = 0.0;
    for (int i = 0; i < 128; i++) {
        // handy noise vector which we'll use for position and color
        // accumulation. (Switched to the latest fashionable noise
        // source - precomputed noise texture - it's faster and doesn't have
        // the problems with numerical precision that the old hash function did.)
        vec4 noise = texelFetch(iChannel1, ivec2(i, i * 2),0);
        vec2 pos = texelFetch(iChannel1, ivec2(i, i+2),0).xy;

        // "velocity" moves the field outward at a rate based on bass
        // content.
        vec2 velocity = noise.y + (0.15 * abs(fract(pos)) * level);
        pos += iTime * velocity * .1;
        pos = fract(pos);

        // normalize pos to display aspect ratio even though it's fake because
        // it looks better!
        pos = (pos * 2.0 - 1.0) * aspect;

        // generate blob radius based on audio level at fake position
        // (the size of the field blob at the current location.)
        // Wow1 controls the max size.
        float radius = clamp(intensity, 0.125 * abs(pos.x), iWow1 * 3.);

        // accumulate field density
        float density = field2(uv, pos, radius);
        final_density += density;
    }

    // gamma correct density and apply color
    float colorMix = mod(final_density * 2.0, 1.0);
    vec3 final_color = mix(iColor2RGB,iColorRGB,colorMix);
    fragColor = vec4(final_color,pow(colorMix,1.5 + iWow2));
}