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
uniform float iScaledLo;
uniform float iScaledHi;

const float PI = 3.14159265359;

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

    st = rotate(st, iRotationAngle);
    st = st * 2. * iScale - iScale;
    st -= iTranslate;

    vec3 color = vec3(0.);

    float norm_x = iWow1;
    float norm_y = iWow2;

    float horizOffset = 0.6;
    float vertOffset = 0.3;

    if (iQuantity <= 1.0) {
        float s1 = vertS(st, norm_x, norm_y);
        color += s1 * iColorRGB;
    } else if (iQuantity <= 2.0) {
        vec2 st1 = abs(st + vec2(horizOffset, 0.0));
        float a0 = vertS(st1, norm_x, norm_y);
        float a1 = vertS(st1, norm_x*0.8, norm_y*0.8);
        float a2 = vertS(st1, norm_x*0.6, norm_y*0.6);

        //color += (a0 + a1 + a2) * iColorRGB;
        color += min(a0, min(a1, a2)) * iColorRGB;
        //color += max(a0, max(a1, a2)) * iColor2RGB;

        vec2 st2 = abs(st + vec2(-horizOffset, 0.0));
        float a3 = vertS(st2, norm_x, norm_y);
        float a4 = vertS(st2, norm_x*0.8, norm_y*0.8);
        float a5 = vertS(st2, norm_x*0.6, norm_y*0.6);

        //color += (a3 + a4 + a5) * iColorRGB;
        color += min(a3, min(a4, a5)) * iColorRGB;
        //color += max(a3, max(a4, a5)) * iColor2RGB;
    } else {
        vec2 st01 = abs(st + vec2(horizOffset, vertOffset));
        vec2 st02 = abs(st + vec2(horizOffset, -vertOffset));
        vec2 st03 = abs(st + vec2(-horizOffset, vertOffset));
        vec2 st04 = abs(st + vec2(-horizOffset, -vertOffset));
        float a0 = vertS(st01, norm_x, norm_y);
        float a1 = vertS(st02, norm_x, norm_y);
        float a2 = vertS(st03, norm_x, norm_y);
        float a3 = vertS(st04, norm_x, norm_y);

        //color += (a0 + a1) * iColorRGB;
        color += max(a0, a1) * iColorRGB;
        //color += max(a0, a1) * iColor2RGB;

        //color += (a2 + a3) * iColorRGB;
        color += max(a2, a3) * iColorRGB;
        //color += max(a2, a3) * iColor2RGB;
    }
    fragColor = vec4(color,1.0);
}
