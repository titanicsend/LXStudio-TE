precision mediump float;

const float pi = 3.14159265359;
const float two_pi = (pi * 2.0);

const float numRays = 3.0;    // actually sqrt of the number of rays
float exposure = 0.6;   // higher is brighter
float falloff = 0.45;   // higher is faster falloff
float diff = 0.75;      // amount of fake "diffraction" color
float speed = 2.0;

float rand(int seed, float ray) {
    return mod(sin(float(seed)*363.5346+ray*674.2454)*6743.4365, 1.0);
}

vec4 light(vec2 position, float pulse) {
    vec4 ret = vec4(1.0);
    
    // small brightly lit sphere in center
    float dist = length(position);    
    ret.rgb *= (1./dist) * (0.08 * pulse); 
    
    
    // create several rays at interesting offsets(golden ratio 1.618 is
    // used here because... it looks about right), and slightly perturb color
    // for a diffraction grating-like look
    float ang = atan(position.y, position.x);    
    float offset = clamp(abs(position.x/position.y),1.,two_pi);
	
	float s = iTime * (speed * beat);
   
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
	float b = 0.05 * sin(two_pi * beat);
	

    // display stars!
    //	First the lower panels
    fragColor = light(position + vec2(0.333, 0.125) + b,sinPhaseBeat)
        + light(position + vec2(-0.285, 0.15) + b,sinPhaseBeat);

    // change scale etc for upper panels
    exposure = 0.25;
    falloff = 0.6;
	b /= 3.;
	
	// draw upper panel stars
    fragColor +=
        light(position + vec2(0.08, -0.2)-b,1.-sinPhaseBeat)
        + light(position + vec2(-0.08, -0.2)-b,sinPhaseBeat);
     
	fragColor = clamp(fragColor,0.,1.); 
    // alpha, cheezily derived from brightness, for LX blending. 
    fragColor.a = max(fragColor.r,max(fragColor.g,fragColor.b));
}
