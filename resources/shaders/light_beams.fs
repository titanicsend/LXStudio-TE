//fork of https://www.shadertoy.com/view/XtVczV

// http://bit.ly/supersinfulsilicon
// @Carandiru on twitter - follow me!

void pMod1(inout float p, float size) {
	float halfsize = size*0.5;
	p = mod(p + halfsize, size) - halfsize;
}

void pMod3(inout vec3 p, vec3 size) {

	p = mod(p - size*0.5, size) - size*0.5;
}

float sphere(vec3 p, float radius){
    return length(p)-radius;

}


float map(vec3 p)
{
    vec3 q = p;
	vec3 qa = p;
    pMod3(q, vec3(0.8, 1., 0.23));
	pMod3(qa, vec3(0.8, 1., 0.18));
    pMod1(p.x, 1.);

    float s1 = sphere(p, 0.75);
    float s2 = sphere(q, 0.5);
    float s3 = sphere(qa, 0.555);

  	float df1 = min(min(s1, s2),s3); // Union

    return df1;
}


float trace(vec3 origin, vec3 r)
{
    float t = 0.0;
    for (int i = 0; i < 64; ++i) {
        vec3 p = origin + r * t;
        float d = map(p);
        t += d*{%color2[.22,.15,2]};
    }
    return t;
}


void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord/iResolution.xy;
    vec3 color = vec3(0.324, 0.12, 0.536);
    uv = uv *2.-1.; // Remap the space to -1. to 1.
    uv.x *= iResolution.x/iResolution.y;


   	float FOV = {%zoom[1.25,0,5]};
   	vec3 ray = normalize(vec3(uv, FOV));


    vec3 origin = vec3({%speed[2.5,0,5]}*iTime, 0.0, -1.75);
    float t = trace(origin, ray);

    float expFog = {%fog[.5,0,5]} * 0.5 / (t*t* 0.95);

    vec3 fc = vec3(expFog);

    if ( t < -0.9f ) {
        color = 0.25-(0.5/t+color);
        vec3 maincolor = exp(-(fc+color)*{%color3[3.6,1.5,15]});
        color = (1.0f - maincolor);
        color = cos(color) * maincolor;
    }
    else
        color = vec3(0);

    fragColor = vec4(color,dot(color, vec3(0.5)) * exp(-expFog));
}
