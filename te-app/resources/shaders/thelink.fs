#pragma name "TheLink"
#pragma TEControl.SIZE.Range(3.0,0.1,5.0)
#pragma TEControl.QUANTITY.Range(4.0,3.0,24.0)
#pragma TEControl.WOW1.Range(1.0,-1.0,1.0)
#pragma TEControl.WOW2.Range(1.0,-1.0,1.0)
// #pragma TEControl.WOW2.Disable
// #pragma TEControl.WOW1.Disable
// #pragma TEControl.WOWTRIGGER.Disable
// #pragma TEControl.LEVELREACTIVITY.Disable
// #pragma TEControl.FREQREACTIVITY.Disable
//// uncomment these to preview in VSCode ShaderToy extension.
//#iUniform vec3 iColorRGB=vec3(.964,.144,.519)
//#iUniform vec3 iColor2RGB=vec3(.226,.046,.636)
//#iUniform float iSpeed=.5 in{.25,3.}
//#iUniform float iScale=1. in{.25,5.}
//#iUniform float iQuantity=3. in{1.,9.}
// #iUniform float iWow2=.0 in{-1.,1.}
// #iUniform float iWow1=.0 in{-1.,1.}
//#iUniform float iRotationAngle=0. in{0.,6.28}
//const bool iWowTrigger = false;

//#pragma name"ArtOfCode"
//#pragma TEControl.SIZE.Range(3.,.1,5.)
//#pragma TEControl.QUANTITY.Range(4.,3.,24.)
//#pragma TEControl.WOW1.Range(1.,0.,1.)
//// #pragma TEControl.WOW2.Disable
//// #pragma TEControl.WOW1.Disable
//// #pragma TEControl.WOWTRIGGER.Disable
//// #pragma TEControl.LEVELREACTIVITY.Disable
//// #pragma TEControl.FREQREACTIVITY.Disable

#define PI 3.141592653589793
#define HALF_PI 1.5707963267948966
#define TWO_PI 6.28318530718

// uniform vec2 iResolution;
// uniform vec2 iMouse;
// uniform float iTime;

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

vec3 bridge(vec3 c, float d, float s, float w) {
    c *= 1. - stroke(d,s,w*2.);
    return c + stroke(d,s,w);
}

void mainImage(out vec4 fragColor,in vec2 fragCoord) {
    vec2 st = gl_FragCoord.xy/iResolution.xy;

    st.x *= iResolution.x/iResolution.y;

    float norm_x = frequencyReact * trebleRatio; //iMouse.x / iResolution.x;
    float norm_y = levelReact * bassRatio; //iMouse.y / iResolution.y;


    // st = st * 20. - 10.;
    // st = st * 2. - 1.;

    vec3 color = vec3(0.);
    float pct = 0.;
    color = vec3(pct);

    st = st.yx;
    st.x = mix(1. - st.x, st.x, step(.5, st.y));
    vec2 o = vec2(.1,.0);
    vec2 s = vec2(1.);
    float a = radians(45.);
    float l = rectSDF(rotate(st+o,a + (norm_x*TWO_PI)),s+norm_y);
    float r = rectSDF(rotate(st-o,-a + (norm_x*TWO_PI)),s+norm_y);

    color += stroke(l, .3, .1);
    color = bridge(color, r, .3, .1);
    color += fill(rhombSDF(abs(st.yx-vec2(.0,.5))), .1);

   fragColor=vec4(color,1.);
}
