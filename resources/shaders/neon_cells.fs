//fork of https://www.shadertoy.com/view/3scGDX

#define PI 3.1415926

float pat(vec2 uv,float p,float q,float s, float glow)
{
	q += (0.5+sin(iTime * s)*{%width[.5,.5,1.5]});
    float z = cos(q * PI * uv.x) * cos(p * PI * uv.y) + cos(q * PI * uv.y) * cos(p * PI * uv.x);
    float dist=abs(z)*(1.0/glow);
    return dist;
}

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return rotationMatrix * point;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    float glow = {%glow[.1,.2,.5]};
   	vec2 uv = (fragCoord.xy - 0.5 * iResolution.xy) / iResolution.y;
    uv = rotate(uv, iRotationAngle) * iScale;

    float d = pat(uv, 5.0, 2.0,1.0, glow);		// layer1

    // Wow2 mixes in second layer of cells
    d = mix(d,d * pat(uv, 3.0, 7.0, 0.5, glow), iWow2);
    fragColor = vec4(iColorRGB,0.5 / d);
}
