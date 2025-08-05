// Probably the sillies shader I've made so far.
// Water running down a melting ice cliff.
#pragma name "Icemelt"
#pragma iChannel1 "resources/shaders/textures/icecliff.png"

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
    float n = waterNoise(vec2(0.0, iTime) + uv * (vec2(60.0,60.0)));

    // Optional: make a Ken Burns documentary (w/slow pan) for this texture!
    //uv.x += 0.1 * sin(iTime / 16.0);

    // no water on very prominent ice features
    vec3 under = texture(iChannel1,uv).rgb;
    float val = under.r * under.r;
    n = (val >= iWow1) ? n * max(0.0,iWow1 - val) : n;
    under = texture(iChannel1,uv + (n / 52.)).rgb;

    fragColor = vec4(mix(under, vec3(.1, .2, 1.), n / 24.0),1.0);
}