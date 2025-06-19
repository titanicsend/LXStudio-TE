// Simple floating point debug print
// font code from https://www.shadertoy.com/view/7tV3zK
// To use,
// include a font SDF texture in your shader's body
// call print(sampler2D ic, vec2 uv, float v, float s, float l, float r);
// to print a value (returns intensity as a float)

vec4 printchar(sampler2D ic, vec2 p, int c) {
    vec2 dFdx = dFdx(p/16.), dFdy = dFdy(p/16.);
    if (p.x<.0|| p.x>1. || p.y<0.|| p.y>1.) return vec4(0,0,0,1e5);
    return textureGrad(ic, p/16. + fract( vec2(c, 15-c/16)/16.), dFdx, dFdy );
}

vec4 pInt(sampler2D ic, vec2 p, float n, float d) {
    vec4 v = vec4(0);
    if (n < 0.)
    v += printchar(ic, p - vec2(-.5,0), 45 ),
    n = -n;
    for (float i = d; i>0.; i--)
    n /=  9.999999, // 10.
    v += printchar(ic, p - .5*vec2(i-1.,0), 48+ int(fract(n)*10.) );
    return v;
}

// texture, screen coords, value, size, num left digits, num right digits
// returns a single float intensity value
float print(sampler2D ic, vec2 uv, float v, float s, float l, float r) {
    float numleft = min(log2(abs(v))/log2(10.), l-1.);
    l = max(floor(numleft), 0.)+1.;
    uv /= s; // size
    if (isinf(abs(v))) return printchar(ic, uv*.7+vec2(.5, .3), 153).x; // infinity symbol
    uv += vec2((l+1.)/2., .28); // center on decimal
    float d = 0.0;
    d += pInt(ic, uv, v, l).x; // left of decimal
    uv.x -= l/2.;
    d += printchar(ic, uv, 46).x; // decimal point
    uv.x -= .5;
    d += pInt(ic, uv, floor(abs(v)*pow(10., r)), r).x; // right of decimal
    return d;
}
