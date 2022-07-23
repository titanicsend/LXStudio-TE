#define SNAKE
#define PRESET 0 // 0-1

vec2 rot(in vec2 uv, float a)
{
    a = radians(a);
    float c, s;
    c = cos(a);
    s = sin(a);
    return vec2(uv.x * c + uv.y * s, uv.x * -s + uv.y * c);
}

float period;
vec4 f(in vec2 uv, in float radius, float radius2, float rev)
{
    period = 6.28 * clamp(rev, 1., 4.);
    for (float a = 0.15; a < 100.; a += .015)
    {
        if (a >= period)
            break;
        float c = cos(a);
        float s = sin(a);
        float c4 = cos(4. * mix(a, sin(a - iTime), abs(sin(iTime / 10.))));
    	float p;
        if (PRESET == 0)
        	p = radius * (exp(c) - 2. * c4 - pow(sin(a / 12.), 5.));
        else
        	p = dot(vec2(.2,.9), vec2(c, s)) + dot(vec2(c4,.1), vec2(cos(a * radius), sin(a * radius)));
        vec2 um = vec2(c + sin(iTime)* .15, sin(a) * cos(iTime) * 1.5);
        #ifdef SNAKE
        if (a - .15 > 5.6 && a - .15 < 6.)
            um += (.1*(.4 - (6. - (a - .15))) / .4) * sin(a + iTime * 100.);
        #endif
        vec2 pos = uv - p * um;
        pos *= clamp(abs(sin(iTime / 25. - 15.)) * 2., .3, 1.5);
        float len = length(pos);
        #ifdef SNAKE
        if (a - .15 > 5.6 && a - .15  < 6.)
            len -= c * 0.01;

        	/* sin(a / 4.) is the worm effect */
        	if (a - .15 < .2 && a - .15 > .02)
                len = length((pos - sin(len) * vec2(.3, .0)) / vec2(-exp(a * .2) / 2., (.6 - (a - .15)) * 5.));
        	if (a - .15 >= .2)
            {
        		if (len < radius2 * sin(a / 2. + .2))
                    return vec4(vec3(pos, len), a);
            }
        	else if (len < radius2 * sin((a - .15) * 10. + .8))
        #else
        //len = length(pos * p); /* like whip */
        if (len < radius * sin(a / 2.))
        #endif
        	return vec4(vec3(pos, len), a);
    }
    return vec4(0);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
	vec2 uv = 2. * fragCoord.xy / iResolution.xy - 1.;
	uv.x *= iResolution.x / iResolution.y;
    uv.y += 0.2;
    uv = rot(uv, 90.);
    vec3 c = vec3(0);
    vec4 b;
    if (PRESET == 0)
    	b = f(uv, .2, 0.1, 1.);
    else
    	b = f(uv, 2., abs(sin(iTime / 10.) * 0.4 + 0.2) * .8, 1.8);
    if (b.z > 0.)
    {
        b.w -= .15;
        #ifdef SNAKE
    		if (b.w < 5.6 && b.w > .2)
            {
                c = texture(iChannel0, b.xz, 0.).xyz - vec3(0, 0, .2);
                c = mix(c, (texture(iChannel0, b.xy, 0.).x + .5) * vec3(.8), .75 * smoothstep(.5, 1., sin(texture(iChannel0, b.xy, 0.).x + b.w  * 40. + 13. * b.z)));
            }
    		else if (b.w > .02 && b.w < 5.6)
    			c = texture(iChannel0, (b.xw) * 2., 0.).xyz * vec3(1.2, 1.1, .1);
            else
                c = texture(iChannel1, b.xz * 2., 0.).xyz + vec3(0, 0, -.2);
    		c = pow(clamp(c, 0.0, 1.0),vec3(0.65));
            c = c * .6 + .4 * c * c * (3. - 2. * c);
    		c = mix(c, vec3(dot(c, vec3(.33))), -.5) * (1. - (b.w - 1.15) / period);
    	#else
    		c = normalize(b.xyz);
    	#endif
    }
    fragColor =  vec4(c, 1);
}