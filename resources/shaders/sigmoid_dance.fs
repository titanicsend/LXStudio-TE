// #iUniform vec3 iColorRGB = vec3(0.964, 0.144, 0.519)
// #iUniform vec3 iColor2RGB = vec3(0.226, 0.046, 0.636)
// #iUniform float iSpin = 0.0 in {-1.0, 1.0}
// #iUniform float iSpeed = 1.0 in {1.0, 10.0}
// #iUniform float iScale = 2.0 in {0.5, 4.0}
// #iUniform float iWow1 = 0.0 in {0.0, 1.0}
// #iUniform vec2 iTranslate = vec2(0.0, 0.0)

// #ifdef GL_ES
// precision mediump float;
// #endif
// uniform vec2 iResolution;
// uniform vec2 iMouse;
// uniform float iTime;



const float PI = 3.14159265359;

float plot(vec2 st, float pct){
  return  smoothstep( pct-0.02, pct, st.y) -
          smoothstep( pct, pct+0.02, st.y);
}

float logisticSigmoid (float x, float a){
  // n.b.: this Logistic Sigmoid has been normalized.

  float epsilon = 0.0001;
  float min_param_a = 0.0 + epsilon;
  float max_param_a = 1.0 - epsilon;
  a = max(min_param_a, min(max_param_a, a));
  a = (1./(1.-a) - 1.);

  float A = 1.0 / (1.0 + exp(0. -((x-0.5)*a*2.0)));
  float B = 1.0 / (1.0 + exp(a));
  float C = 1.0 / (1.0 + exp(0.-a));
  float y = (A-B)/(C-B);
  return y;
}

float horizS(vec2 st, float w, float a) {
  float l = logisticSigmoid(st.y, a);
  float r = logisticSigmoid(st.y-w, a);
  return st.x < r && st.x > l ? 1.0 : 0.0;
}

float vertS(vec2 st, float w, float a) {
  float l = logisticSigmoid(abs(st.x), a);
  float r = logisticSigmoid(abs(st.x)-w, a);
  // return st.y < r ? 1.0 : 0.0;
  // return st.y > l ? 1.0 : 0.0;
  return abs(st.y) < l && abs(st.y) > r ? 1.0 : 0.0;
}

vec2 rotate(vec2 st, float a) {
    st = mat2(cos(a), -sin(a), sin(a), cos(a)) * (st-.5);
    return st + .5;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 st = fragCoord.xy/iResolution.xy;

    st = rotate(st, iSpin * iTime);

    st = st * 2. * iScale - iScale;

    st -= iTranslate;

    // st.x *= 2.;
    // st *= 4.;
    // st = fract(st);

    vec3 color = vec3(0.);
    // color.r = 1. - smoothstep(0., .01, abs(st.x));
    // color.g = 1. - smoothstep(0., .01, abs(st.y));

    // float norm_x = iMouse.x/iResolution.x;
    // float norm_y = iMouse.y/iResolution.y;

    float norm_x = iWow1;
    float norm_y = iWow2;

    // norm_x = 0.5;
    // norm_y = 0.787;

    float s1 = vertS(st, norm_x, norm_y);
    // float pct = vertS(st, 0.5, 0.787);
    color += s1 * iColorRGB;

    if (iWow1 > 0.0) {
        vec2 st2 = st * 2.;
        st2.x *= 2.;
        st2 = fract(st2);
        float s2 = vertS(st2, norm_x, norm_y);

        color += min(s1, s2) * iColor2RGB;
        // color += max(s1, s2) * iColor2RGB;
        // color += s1 * s2 * iColor2RGB;
        // color += s2 * iColor2RGB;
    }


    // float pct = plot(st,l);
    // color = (1.0-pct)*color+pct*vec3(0.0,1.0,0.0);

    fragColor = vec4(color,1.0);
}
