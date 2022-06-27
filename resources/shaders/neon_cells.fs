//fork of https://www.shadertoy.com/view/3scGDX

#define PI 3.1415926

float pat(vec2 uv,float p,float q,float s,float glow)
{
	q += (0.5+sin(iTime*s)*{%width[.5,.5,1.5]});
    float z = cos(q * PI * uv.x) * cos(p * PI * uv.y) + cos(q * PI * uv.y) * cos(p * PI * uv.x);
    float dist=abs(z)*(1.0/glow);
    return dist;
}


void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    float speedMultiplier = {%doubleSpeed[bool]} ? 2. : 1.;
    float glow = {%glow[.1,.2,.5]};
   	vec2 uv = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.y;
    float d = pat(uv, 5.0, 2.0, speedMultiplier, glow);		// layer1
    if ({%doubleLyer[bool]})
    	d *= pat(uv, 3.0, 7.0, 0.25 * speedMultiplier, glow);		// layer2

    vec3 col = vec3(0.5,1.1,0.4)*0.5/d;
    fragColor = vec4(col,1.0);
}
