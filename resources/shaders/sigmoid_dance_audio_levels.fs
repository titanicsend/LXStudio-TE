// #iUniform vec3 iColorRGB = vec3(0.964, 0.144, 0.519)
// #iUniform vec3 iColor2RGB = vec3(0.226, 0.046, 0.636)
// #iUniform float iSpin = 0.0 in {-1.0, 1.0}
// #iUniform float iSpeed = 1.0 in {1.0, 10.0}
// #iUniform float iScale = 2.0 in {0.5, 4.0}
// #iUniform float iWow1 = 0.0 in {0.0, 1.0}
// #iUniform vec2 iTranslate = vec2(0.0, 0.0)

uniform float avgVolume;
uniform float iScaledLo;
uniform float iScaledHi;

const float PI = 3.14159265359;

float logisticSigmoid (float x, float a){
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
  return abs(st.y) < l && abs(st.y) > r ? 1.0 : 0.0;
}

vec2 rotate(vec2 st, float a) {
    st = mat2(cos(a), -sin(a), sin(a), cos(a)) * (st-.5);
    return st + .5;
}

#define TEXTURE_SIZE 512.0
#define CHANNEL_COUNT 16.0
#define pixPerBin (TEXTURE_SIZE / CHANNEL_COUNT)
#define halfBin (pixPerBin / 2.0)

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 st = fragCoord.xy/iResolution.xy;

    st = rotate(st, iRotationAngle) / iScale;
    st -= 0.5 / iScale;

    vec3 color = vec3(0.);
    bool isColor2Black = distance(iColor2RGB, vec3(0.)) < 0.1;

    float norm_y = 0.05 + bassLevel * iWow1;
    float norm_x = 0.05 + trebleLevel * iWow1;
    //float norm_x = iScaledLo;
    //float norm_y = iScaledHi;

    float index = mod(st.x * TEXTURE_SIZE * 2.0, TEXTURE_SIZE);
    float wave = (0.5 * (1.0+texelFetch(iChannel0, ivec2(index, 1), 0).x)) - 0.25;
    float p = floor(index / pixPerBin);
    float tx = halfBin+pixPerBin * p;
    float freq  = texelFetch(iChannel0, ivec2(tx, 0), 0).x;

    norm_x += iWow2 * wave;
    norm_y += iWow2 * freq;

    if (iWowTrigger) {
        norm_x *= norm_x;
        norm_y *+ norm_y;
    }

    float horizOffset = 0.6;
    float vertOffset = 0.3;

    if (iQuantity <= 1.0) {
        float s1 = vertS(st, norm_x, norm_y);
        color += s1 * iColorRGB;
    } else {
        vec2 st01 = abs(st + vec2(horizOffset, vertOffset));
        vec2 st02 = abs(st + vec2(horizOffset, -vertOffset));
        vec2 st03 = abs(st + vec2(-horizOffset, vertOffset));
        vec2 st04 = abs(st + vec2(-horizOffset, -vertOffset));
        float a0 = vertS(st01, norm_x, norm_y);
        float a1 = vertS(st02, norm_x, norm_y);
        float a2 = vertS(st03, norm_x, norm_y);
        float a3 = vertS(st04, norm_x, norm_y);
        //color += max(a0, a1) * iColorRGB;
        //color += max(a2, a3) * iColorRGB;

        float pct = a0 + a1 + a2 + a3;
        if (pct > 0.) {
            if (pct > 1. && !isColor2Black) {
                color += iColor2RGB;
            } else {
                color += iColorRGB;
            }
        }
    }
    fragColor = vec4(color,1.0);
}
