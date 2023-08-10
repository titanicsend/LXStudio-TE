// uncomment these to preview in VSCode ShaderToy extension.
// #iUniform vec3 iColorRGB = vec3(0.964, 0.144, 0.519)
// #iUniform vec3 iColor2RGB = vec3(0.226, 0.046, 0.636)
// #iUniform float iSpeed = 0.5 in {0.25, 3.0}
// #iUniform float iScale = 1.0 in {0.25, 5.0}
// #iUniform float iQuantity = 3.0 in {1.0, 9.0}
// #iUniform float iWow2 = 2.5 in {1.0, 3.0}
// #iUniform float iWow1 = 0.9 in {0.5, 2.0}
// #iUniform float iRotationAngle = 0.0 in {0.0, 6.28}

const float PI = 3.141592653589793;
const float HALF_PI = 1.5707963267948966;
const float TWO_PI = 6.28318530718;
const float TAU = 6.28318530718;

vec2 rotate(vec2 point, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return rotationMatrix * point;
}

float sdEquilateralTriangle( in vec2 p, in float r ) {
    const float k = sqrt(3.0);
    p.x = abs(p.x) - r;
    p.y = p.y + r/k;
    if( p.x+k*p.y>0.0 ) p = vec2(p.x-k*p.y,-k*p.x-p.y)/2.0;
    p.x -= clamp( p.x, -2.0*r, 0.0 );
    return -length(p)*sign(p.y);
}

float noise(in float x, in float ts) {
  float amplitude = 0.2 * pow(x, 3.);
  float frequency = 2.;
  float y = sin(x * frequency);
  // float t = 0.01*(-iTime*130.0);
  float t = 0.01*(-ts*130.0);
  y += sin(x*frequency*2.1 + t)*4.5;
  y += sin(x*frequency*1.72 + t*1.121)*4.0;
  y += sin(x*frequency*2.221 + t*0.437)*5.0;
  y += sin(x*frequency*3.1122+ t*4.269)*2.5;
  y *= amplitude*0.06;
  return y;
}

float numIters = iQuantity;
float waveFreq = 8.0 + iWow1 * 2.0 * volumeRatio;
const float defaultYOffset = -0.17;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    float size = iScale;
    size += iWow1 * 0.1 * bassRatio;

    // normalize coordinates
    vec2 uv = fragCoord.xy / iResolution.xy;
    uv -= 0.5;
    uv -= vec2(0., defaultYOffset);
    uv = rotate(uv, iRotationAngle);
    uv.x *= iResolution.x/iResolution.y;
    uv *= 1.0/size;

    float fractFactor = 0.86;
    fractFactor = 0.7 + iWow1 * 0.9 * trebleLevel;

    vec2 uv0 = uv;
    vec3 finalColor = vec3(0.0);
    float outerTriangleDF = exp(-sdEquilateralTriangle(uv0, 0.9));
    for (float i = 0.0; i < numIters; i++) {
        uv = fract(uv * fractFactor) - 0.5;

        uv.y *= -1.;

        float d = 1.;
        d *= sdEquilateralTriangle(uv, 0.9);
        d *= outerTriangleDF;
        d += 0.3*noise(i*d, iTime*0.1);

        d = sin(d*waveFreq + iTime)/waveFreq;
        d -= noise(i*d*8., iTime*0.1);
        d = abs(d);

        float neonDampening = 1.5;
        neonDampening = iWow2;
        d = pow(0.01 / d, neonDampening);

        vec3 col = int(i) % 2 == 0 ? iColorRGB : iColor2RGB;
        finalColor += col * d;
    }

    finalColor *= {%brightnessDampening[.9,.1,1.0]};
    fragColor = vec4(finalColor,1.0);
}