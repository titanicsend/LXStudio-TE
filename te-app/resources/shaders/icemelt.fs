// This is a silly shader, but it looks really nice.
// Water running down a melting ice cliff.
#pragma name "Icemelt"
#pragma iChannel1 "resources/shaders/textures/icecliff.png"

#pragma TEControl.QUANTITY.Range(0.6,0.2,1.0)
#pragma TEControl.LEVELREACTIVITY.Disable
#pragma TEControl.FREQREACTIVITY.Disable
#pragma TEControl.ANGLE.Disable
#pragma TEControl.SPIN.Disable
#pragma TEControl.SIZE.Disable
#pragma TEControl.WOW1.Disable
#pragma TEControl.WOW2.Disable
#pragma TEControl.WOWTRIGGER.Disable

float random(float x) {
    return fract(sin(x) * 10000.);
}

float noise(vec2 p) {
    return random(p.x + p.y * 10000.);
}

vec2 sw(vec2 p) { return vec2(floor(p.x), floor(p.y)); }
vec2 se(vec2 p) { return vec2(ceil(p.x), floor(p.y)); }
vec2 nw(vec2 p) { return vec2(floor(p.x), ceil(p.y)); }
vec2 ne(vec2 p) { return vec2(ceil(p.x), ceil(p.y)); }

float smoothNoise(vec2 p) {
    vec2 interp = smoothstep(0., 1., fract(p));
    float s = mix(noise(sw(p)), noise(se(p)), interp.x);
    float n = mix(noise(nw(p)), noise(ne(p)), interp.x);
    return mix(s, n, interp.y);
}

float fbmNoise(vec2 p) {
    float x = smoothNoise(p      );
    x += smoothNoise(p * 2. ) / 2.;
    x += smoothNoise(p * 4. ) / 4.;
    x += smoothNoise(p * 8. ) / 8.;
    x /= 1. + 1./2. + 1./4. + 1./8.;
    return x;
}

float movingNoise(vec2 p) {
    float x = fbmNoise(p + iTime);
    float y = fbmNoise(p - iTime);
    return fbmNoise(p + vec2(x, y));
}

float waterNoise(vec2 p) {
    float x = movingNoise(p);
    float y = movingNoise(p + 100.);
    return movingNoise(p + vec2(x, y));
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    float t = 20. + iTime;
    vec2 uv = fragCoord.xy / iResolution.xy;
    float n = waterNoise(vec2(0.0, iTime) + uv * (vec2(40.0,20.0)));

    // Optional: make a Ken Burns documentary (w/slow pan) for this texture!
    uv.x = fract(uv.x + 0.1 * sin(iTime / 32.0));

    // Sample the texture, which is largely blue and white. We'll use
    // the red channel as a proxy for the a bump map of the ice cliff.
    // The less red is present, the farther from white we'll be.  This
    // makes a good appoximation of the areas where water would naturally
    // flow.
    vec3 under = texture(iChannel1,uv).rgb;
    float val = under.r * under.r;
    // Layer water noise over the texture in low areas
    float mask = smoothstep(iQuantity - 0.05, iQuantity + 0.05, val);
    n *= 1.0 - mask;

    // Now sample the texture again using the water noise value to
    // calculate offset due to "refraction" from the water.
    under = texture(iChannel1,uv + (n / 52.)).rgb;

    // Ice is pretty white -- let's make detail more visible by
    // increase the contrast of the final image.
    under = under * under * under;

    // mix texture color with water color value for final color.
    fragColor = vec4(mix(under, vec3(.1, .3, 1.), n / 24.0),1.0);
}