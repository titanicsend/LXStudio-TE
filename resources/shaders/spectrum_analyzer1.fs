// TE Audio Test shader.  Based on.
//https://www.shadertoy.com/view/NlKSRw

const vec3 BAR_COLOR = vec3(0.8, 0.2, 0.3);
const float BAR_COUNT = 20.0;
const float BAR_WIDTH = 0.75;

const float FREQ_COUNT = 512.0;

const float PI = 3.14159265359;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord/iResolution.xy;
	//uv.y = 1.-uv.y;	

    float barIndex = floor(uv.x * BAR_COUNT);
    float barHeight = texelFetch(iChannel0, ivec2(barIndex * FREQ_COUNT / BAR_COUNT, 0), 0).x;
    
    float horizontal = smoothstep(1.0 - BAR_WIDTH, 1.0 - BAR_WIDTH, abs(sin(uv.x * PI * BAR_COUNT)));
    float vertical = smoothstep(1.0 - barHeight, 1.0 - barHeight,uv.y);
    
    // Output to screen
    fragColor = vec4(BAR_COLOR * horizontal * vertical, 1.0);
}