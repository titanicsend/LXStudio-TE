precision mediump float;

#include <include/constants.fs>
#include <include/colorspace.fs>

const float maxRays = 8.0;    // number of rays
float exposure;    // higher is brighter
float falloff;     // higher is faster falloff
float diff;        // amount of fake "diffraction" color
float speed = 2.0; // base movement speed.

vec4 light(vec2 position, float pulse, float timeOffset) {
    vec4 ret = vec4(iColorRGB,1.0);

    // small brightly lit sphere in center
    float dist = length(position);
    pulse = mod(pulse + timeOffset, 1.0);
    ret.rgb *= ((1. - iScale)/dist) * 0.08 * (1.0 - pulse);

    // create several moving rays at interesting offsets(golden ratio conjugate .618 is
    // used here because... it looks about right), and slightly perturb color
    // for a diffraction grating-like look
    float ang = atan(position.y, position.x);
    // convert angle to a value between 0 and TAU
    if (ang < 0.0) {ang += TAU;}

    float offset = clamp(abs(position.x/position.y),1.,TAU);
	float deltaAngle = TAU / iQuantity;

	float s = iRotationAngle;
   
    for (float n = 0.0; n < maxRays; n += 1.0) {
        // iQuantity controls the number of rays
        if (n >= iQuantity) {
            break;
        }
        float rayang = deltaAngle * n + s + offset;
        rayang = mod(rayang, TAU);

        float bri = clamp(exposure - abs(ang - rayang), 0.0, 1.0);
        bri -=(falloff * dist);
        
        if (bri > 0.0) {
            vec2 uv = floor(vec2(10000.,1.) * position);
            ret.rgb += getGradientColor(n/iQuantity) * bri;
        }
    }
    ret *= smoothstep(0.5, 0.0, dist);    
    return ret;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )  {
    // normalize and center coordinates
    vec2 position = ( fragCoord.xy / iResolution.xy ) - 0.5;
    position.y += 0.2;

    // controls magnitude of all beat-linked featues
    float beatVal = levelReact * beat;

    // roughly circular "bounce" with beat
    float b = (TAU * fract(iTime + beatVal));
    vec2 offset = 0.05 * vec2(sin(b),cos(b));

    // change average level of diffraction effect
    // with iWow1
    float k = 0.2 * iWow1;
	falloff = 0.65 - k;
	exposure = (0.5 * iWow1) + 0.3 + k;
    diff = 0.05 + (0.5 * iWow1);

    // display stars!
    //	First the lower panels
    fragColor = light(position - offset + vec2(0.333, 0.125),beatVal,0.0)
        + light(position + offset + vec2(-0.285, 0.15),beatVal,0.5);

    // reduce movement scale for upper panels
    offset /= 3.;
    exposure *= 0.75;
	
	// draw upper panel stars
    fragColor +=
        light(position + offset + vec2(0.09, -0.2),beatVal,0.2)
        + light(position - offset + vec2(-0.09, -0.2),beatVal,0.75);
     
	fragColor = clamp(fragColor,0.,1.);
}
