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

vec3 mixPalette( vec3 c1, vec3 c2, float t )
{
    float mixFactor = 0.5 + sin(t);
    return mix(c1, c2, mixFactor);
}
vec3 rgb2hsb( in vec3 c ){
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz),
                 vec4(c.gb, K.xy),
                 step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r),
                 vec4(c.r, p.yzx),
                 step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)),
                d / (q.x + e),
                q.x);
}
vec3 hsb2rgb( in vec3 c ){
    vec3 rgb = clamp(abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),
                             6.0)-3.0)-1.0,
                     0.0,
                     1.0 );
    rgb = rgb*rgb*(3.0-2.0*rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
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

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // normalize coordinates
    vec2 uv = fragCoord.xy / iResolution.xy;
    uv -= 0.5;
    uv = rotate(uv, iRotationAngle);
    uv.x *= iResolution.x/iResolution.y;
    uv *= 1.0/iScale;
    
    vec2 uv0 = uv;
    vec3 finalColor = vec3(0.0);

    vec3 c1hsb = rgb2hsb(iColorRGB);
    vec3 c2hsb = rgb2hsb(iColor2RGB);
    
    for (float i = 0.0; i < iQuantity; i++) {
        uv = fract(uv * iWow1) - 0.5;

        uv.y *= -1.;
        // uv0.y *= -1.;
        // uv.y *= -1. * i;
        // uv.y += 0.2;
        // uv.y += 0.02 * abs(sin(noise(i, iTime*0.02)));
        // uv.y += 0.02 * noise(i, iTime*0.02);

        float d = 1.;
        d *= sdEquilateralTriangle(uv, 0.9);
        d *= exp(-sdEquilateralTriangle(uv0, 0.9));
        d += 0.3*noise(i*d, iTime*0.1);

        float paletteIdx = length(uv0) + i*2. * iTime*.1;
        vec3 col = hsb2rgb(mixPalette(c1hsb, c2hsb, paletteIdx));

        d = sin(d*8. + iTime*0.5)/8.;
        d -= noise(i*d*8., iTime*0.1);
        d = abs(d);

        d = pow(0.01 / d, iWow2);

        finalColor += col * d;

        // uv.y -= 0.2 * i;
        // uv.y /= -1. * i;
    }

    // finalColor *= 0.6;
    
    fragColor = vec4(finalColor,1.0);
}