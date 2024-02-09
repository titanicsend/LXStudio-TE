#pragma name "Vortex"
#pragma TEControl.YPOS.Value(-0.15)
#pragma TEControl.SIZE.Range(1.0,5.0,0.1)
#pragma TEControl.QUANTITY.Range(4.0,3.0,6.0)
#pragma TEControl.WOW1.Range(0.6,0.0,5.0)
#pragma TEControl.WOW2.Range(0.6,0.0,5.0)
//#pragma TEControl.WOW1.Disable
#pragma TEControl.WOWTRIGGER.Disable
// Wow2 controls audio reactivity

#include <include/constants.fs>
#include <include/colorspace.fs>

mat2 rot(float a) {
    float s=sin(a), c=cos(a);
    return mat2(c,s,-s,c);
}

#define PI 3.141592653589793
#define HALF_PI 1.5707963267948966

float stroke(float x, float s, float w) {
    float d = step(s, x + w * .5) - step(s, x - w * .5);
    return clamp(d, 0., 1.);
}

float fill(float x, float size) {
    return 1. - step(size, x);
}

float circleSDF(vec2 st) {
    return length(st-.5)*2.;
}

float rectSDF(vec2 st, vec2 s) {
    st = st*2.-1.;
    return max(abs(st.x/s.x), abs(st.y/s.y));
}

float triSDF(vec2 st) {
    st = (st*2.-1.)*2.;
    return max(abs(st.x) * 0.866025 + st.y * 0.5, -st.y * 0.5);
}

float rhombSDF(vec2 st) {
    return max(triSDF(st), triSDF(vec2(st.x, 1.-st.y)));
}

float flip(float v, float pct) {
    return mix(v, 1.-v, pct);
}

vec2 rotate(vec2 st, float a) {
    st = mat2(cos(a), -sin(a), sin(a), cos(a)) * (st-.5);
    return st + .5;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {

    float vol = max(0.01, volumeRatio);

    float bVal = bassRatio / vol;
    float tVal = trebleRatio / vol;
    //float bVal = max(0.01, bassRatio / volumeRatio);
    //float tVal = max(0.01, trebleRatio / volumeRatio);

    bVal *= iWow1;
    tVal *= iWow2;

    float norm_x = max(0.01, tVal);
    float norm_y = max(0.01, 1. - bVal * 0.5);
    //float norm_x = max(0.1, 1. - bVal * 0.5);
    //float norm_y = max(0.1, tVal);

    //// hack: UI debug params
    //float norm_x = iWow1; //.9 - bassRatio;
    //float norm_y = iWow2; .9 - trebleRatio;

    vec2 st = fragCoord.xy/iResolution.xy;

    //st.x *= iResolution.x/iResolution.y;

    vec3 color = vec3(0.);
    float pct = 0.;
    color = vec3(pct);

    st -= .5;

    float r = dot(st,st);
    float a = (atan(st.y, st.x)/PI);
    vec2 uv = vec2(a,r);

    //float index = mod(normAngle * TEXTURE_SIZE * 2.0, TEXTURE_SIZE);
    //float wave = (0.5 * (1.0+texelFetch(iChannel0, ivec2(index, 1), 0).x)) - 0.25;

    //vec2 grid = vec2(5.* norm_x, log(r)*20.*norm_y);
    vec2 grid = vec2(5. + norm_x, log(r)*20.*norm_y);
    // vec2 grid = vec2(5.* norm_x, log(r)*20.*norm_y);
    // vec2 grid = vec2(9., log(r)*20.);

    vec2 uv_i = floor(uv * grid);
    uv.x += .5 * mod(uv_i.y, 2.);
    // uv.x += /*.5*/ norm_x * mod(uv_i.y, 2.);
    vec2 uv_f = fract(uv * grid);

    float rhomb = rhombSDF(uv_f);
    float tri = triSDF(uv_f);

    // float shape = abs(sin(u_time))*rhomb + (1.-sin(2.*u_time))*tri;
    float shape = norm_x * rhomb + norm_y * tri;
    // float shape = 0.9 * rhomb + 0.5 * tri;

    color += fill(shape, .8);// * step(.75, 1. - r);

    //vec3 rgb = mix(iColorRGB,mix(iColorRGB,iColor2RGB,mod(r*8.,4.) / 4.),iWow2);
    vec3 rgb = mix(iColorRGB,iColor2RGB,fract(r * 20. * volumeRatio));
    color *= rgb;

    fragColor = vec4(color,1.0);
}