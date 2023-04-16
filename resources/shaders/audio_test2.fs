// TE Audio Test Shader
// Spectrum Analyzer and Waveform Display
//
#define TEXTURE_SIZE 512.0
#define CHANNEL_COUNT 16.0
#define pixPerBin (TEXTURE_SIZE / CHANNEL_COUNT)
#define halfBin (pixPerBin / 2.0)

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return rotationMatrix * point;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec3 col;
    // normalize coordinates
    vec2 uv = fragCoord.xy / iResolution.xy;

    // translate coords to roughly vehicle origin, then scale and rotate according
    // to common control settings.
    uv -= vec2(0.5, 0.5);
    uv = rotate(uv, iRotationAngle);
    uv += vec2(0.5, 0.5);
    uv *= 1.0/iScale;

    // The audio texture size is 512x2
    // mapping to screen depends on iScale and iQuantity - here
    // we use iQuantity to figure out which texture pixels are relevant
    float index = mod(uv.x * TEXTURE_SIZE * 2.0 * iQuantity, TEXTURE_SIZE);

    // The second row of is normalized waveform data
    // we'll just draw this over the spectrum analyzer.  Sound data
    // coming from LX is in the range -1 to 1, so we scale it and move it down
    // a bit so we can see it better.
    float wave = (0.5 * (1.0+texelFetch(iChannel0, ivec2(index, 1), 0).x)) - 0.25;

    // iWowTrigger solos the waveform display in the current primary color
    if (iWowTrigger) {
        col = iColorRGB * (1.0-smoothstep(0.0, 0.06, abs(wave - uv.y)));
    }
    else {
        // subdivide fft data into bins determined by iQuantity
        float p = floor(index / pixPerBin);
        float tx = halfBin+pixPerBin * p;
        float dist = abs(halfBin - mod(index, pixPerBin)) / halfBin;

        // since we're using dist to calculate desatuation for a specular
        // reflection effect, we'll modulate it with beat, to change
        // apparent shininess and give it some extra bounce.
        dist = dist - (beat * beat);

        // display frequency bands in shifting colors, just because we can!
        vec3 hsvLayer1 = vec3(0.5 + 0.5 * sin(iTime+p*0.618), dist, 0.5);
        vec3 hsvLayer2 =
        vec3(hsvLayer1.x, max(hsvLayer1.y,1.0-dist), hsvLayer1.z +1.0- dist);

        // iWow2 controls "palette compliance". Technically, this is one
        // of the many wrong ways to mix color, but it works for this purpose.
        // More iWow2 == more color
        col = hsv2rgb(mix(hsvLayer2, iColorHSB, mix(0.0, 0.66667, 1.0-iWow2)));

        // get frequency data pixel from texture
        float freq  = texelFetch(iChannel0, ivec2(tx, 0), 0).x;

        // make a simple beat reactive spectrum analyzer display
        col *= ((freq > uv.y) ? uv.y/freq : 0.);

        // iWow1 controls the brightness of the waveform display. Higher iWow1
        // means less waveform
        col += (1.0 - iWow1) *  (1.0-smoothstep(0.0, 0.04, abs(wave - uv.y)));
    }

    // return pixel color
    fragColor = vec4(col, max(col.r, max(col.g, col.b)));
}