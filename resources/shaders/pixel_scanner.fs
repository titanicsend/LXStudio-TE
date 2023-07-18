int n = int(iQuantity);
vec3 lineCol = iColorRGB;
vec3 baseCol = iColor2RGB;
float scl = 0.01;

float pi = 3.14;

mat2 rotate(float a) {
    return mat2(cos(a), -sin(a), sin(a), cos(a));
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord/iResolution.xy;
    uv *= rotate(-iRotationAngle);

    float off = -fract(.5 * iTime);
    float lg = -fract(log(scl*uv.x + 1.) / log(scl*1. + 1.) * float(n) + off)+1.;
    float mask = distance(lg, 0.5)*2.;

    vec3 col = mix(baseCol, lineCol, smoothstep(0.8, .9, mask)) ;

    fragColor = vec4(col, 1.);
}