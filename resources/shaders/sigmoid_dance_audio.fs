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

#define TEXTURE_SIZE 512.0
#define CHANNEL_COUNT 16.0
#define pixPerBin (TEXTURE_SIZE / CHANNEL_COUNT)
#define halfBin (pixPerBin / 2.0)

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

    st = rotate(st, iRotationAngle);
    st = st * 2. * iScale - iScale;
    st -= iTranslate;

    float index = mod(st.x * TEXTURE_SIZE * 2.0, TEXTURE_SIZE);
    // The second row of is normalized waveform data
    // we'll just draw this over the spectrum analyzer.  Sound data
    // coming from LX is in the range -1 to 1, so we scale it and move it down
    // a bit so we can see it better.
    float wave = (0.5 * (1.0+texelFetch(iChannel0, ivec2(index, 1), 0).x)) - 0.25;

    // subdivide fft data into bins determined by iQuantity
    float p = floor(index / pixPerBin);
    float tx = halfBin+pixPerBin * p;
    // get frequency data pixel from texture
    float freq  = texelFetch(iChannel0, ivec2(tx, 0), 0).x;

    // float dist = abs(halfBin - mod(index, pixPerBin)) / halfBin;
    // // since we're using dist to calculate desatuation for a specular
    // // reflection effect, we'll modulate it with beat, to change
    // // apparent shininess and give it some extra bounce.
    // dist = dist - (beat * beat);

    float norm_x = wave;
    float norm_y = freq;

    vec3 color = vec3(0.);
    float s1 = vertS(st, norm_x, norm_y);
    color += s1 * iColorRGB;

    fragColor = vec4(color,1.0);
}