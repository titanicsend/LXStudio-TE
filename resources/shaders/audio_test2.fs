// TE Audio Test Shader
// 
// NOTE: If we flip y axis direction in the shader framework, as we should,
// remember to take the "uv.y = 1-uv.y" out of this shader

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // create pixel coordinates
	vec2 uv = fragCoord.xy / iResolution.xy;
	//uv.y = 1.-uv.y;   // flip y axis direction

    // sound texture size is 512x2
    int tx = int(uv.x*512.0);

	// first row is frequency data 
	float freq  = texelFetch( iChannel0, ivec2(tx,0), 0 ).x;
	
	// convert frequency data to shifting colors, just because we can!
	vec3 hsv = vec3(0.5+0.5 * sin(iTime + 6.28* freq),1.,1.);
	vec3 col = hsv2rgb(hsv);
	
	// make a simple spectrum analyzer display
	col *= (freq > uv.y) ? 1. : 0.;

    // second row is normalized sound waveform data
	// just draw this over the spectrum analyzer.  Sound data
	// coming from LX is in the range -1 to 1, so we scale it and move it down
	// a bit so we can see it better.
    float wave = (0.5 * (1.0+texelFetch( iChannel0, ivec2(tx,1), 0 ).x)) - 0.25;
	col += 1.0 - smoothstep( 0.0, 0.035, abs(wave - uv.y));

	// return pixel color
	fragColor = vec4(col,1.0);
}