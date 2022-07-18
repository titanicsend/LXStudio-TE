precision mediump float;

uniform vec3 color;
uniform float energy;
uniform float basis;  // beat scaled w/user selected time division

const float pi = 3.14159265359;
const float two_pi = (pi * 2.0);

const float numRays = 3.0;    // actually sqrt of the number of rays
float exposure;    // higher is brighter
float falloff;     // higher is faster falloff
float diff;        // amount of fake "diffraction" color
float speed = 2.0; // base movement speed.

float rand(int seed, float ray) {
    return mod(sin(float(seed)*363.5346+ray*674.2454)*6743.4365, 1.0);
}

vec4 light(vec2 position, float pulse) {
    vec4 ret = vec4(color,1.0);
    
    // small brightly lit sphere in center
    float dist = length(position);    
    ret.rgb *= (1./dist) * (0.08 * pulse); 

    // create several rays at interesting offsets(golden ratio 1.618 is
    // used here because... it looks about right), and slightly perturb color
    // for a diffraction grating-like look
    float ang = atan(position.y, position.x);    
    float offset = clamp(abs(position.x/position.y),1.,two_pi);
	
	float s = iTime * (speed * basis);
   
    for (float n = 1.0; n <= numRays; n += 1.0) {    
        float rayang = 1.618 * n + (s) + offset;
        rayang = mod(rayang, two_pi);
        
		// extend rays to both sides of circle
        if (rayang < ang - pi) {rayang += two_pi;}
        if (rayang > ang + pi) {rayang -= two_pi;}
        
        float bri = exposure - abs(ang - rayang);
        bri -=(falloff * dist);
        
        if (bri > 0.0) {
            vec2 uv = floor(vec2(10000.,1.) * position);
            ret.rgb += vec3(1.+diff*rand(8644, n),
                            1.+diff*rand(4567, n),
                            1.+diff*rand(7354, n)) * bri;
        }
    }
    ret *= smoothstep(0.5, 0.0, dist);    
    return ret;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )  {
    // normalize and center coordinates
    vec2 position = ( gl_FragCoord.xy / iResolution.xy ) - 0.5;
    position.y *= iResolution.y/iResolution.x;

    // roughly circular "bounce" with beat
    float b = two_pi * basis;
    vec2 offset = 0.05 * vec2(sin(iTime+b),cos(b));

    // change average star size and level of diffraction effect
    // with energy
    float k = 0.2 * energy;
	falloff = 0.75 - k;
	exposure = 0.575 + k;
    diff = 0.5 + (0.5 * energy);

    // display stars!
    //	First the lower panels
    fragColor = light(position - offset + vec2(0.333, 0.125),sinPhaseBeat)
        + light(position + offset + vec2(-0.285, 0.15),sinPhaseBeat);

    // change movement scale for upper panels
    offset /= 3.;
    exposure = 0.2 + k;
	
	// draw upper panel stars
    fragColor +=
        light(position + offset + vec2(0.09, -0.2),1.-sinPhaseBeat)
        + light(position - offset + vec2(-0.09, -0.2),sinPhaseBeat);
     
	fragColor = clamp(fragColor,0.,1.);

    // alpha, derived from brightness, for LX blending.
    fragColor.a = max(fragColor.r,max(fragColor.g,fragColor.b));
}
