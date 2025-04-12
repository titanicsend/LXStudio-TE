#pragma name "ArtOfCode"
#pragma TEControl.SIZE.Range(3.0,0.1,5.0)
#pragma TEControl.QUANTITY.Range(4.0,3.0,24.0)
#pragma TEControl.WOW1.Range(1.0,0.0,1.0)
// #pragma TEControl.WOW2.Disable
// #pragma TEControl.WOW1.Disable
// #pragma TEControl.WOWTRIGGER.Disable
// #pragma TEControl.LEVELREACTIVITY.Disable
// #pragma TEControl.FREQREACTIVITY.Disable
//// uncomment these to preview in VSCode ShaderToy extension.
//#iUniform vec3 iColorRGB=vec3(.964,.144,.519)
//#iUniform vec3 iColor2RGB=vec3(.226,.046,.636)
//#iUniform float iSpeed=.5 in{.25,3.}
//#iUniform float iScale=1. in{.25,5.}
//#iUniform float iQuantity=3. in{1.,9.}
//#iUniform float iWow2=2.5 in{1.,7.}
//#iUniform float iWow1=.2 in{0.,2.}
//#iUniform float iRotationAngle=0. in{0.,6.28}
//const bool iWowTrigger = false;

//#pragma name"ArtOfCode"
//#pragma TEControl.SIZE.Range(3.,.1,5.)
//#pragma TEControl.QUANTITY.Range(4.,3.,24.)
//#pragma TEControl.WOW1.Range(1.,0.,1.)
//// #pragma TEControl.WOW2.Disable
//// #pragma TEControl.WOW1.Disable
//// #pragma TEControl.WOWTRIGGER.Disable
//// #pragma TEControl.LEVELREACTIVITY.Disable
//// #pragma TEControl.FREQREACTIVITY.Disable

//#include <include/constants.fs>
//#include <include/colorspace.fs>

#define PI 3.14159265359
#define TWO_PI 6.28318530718

#define TEXTURE_SIZE 512.0
#define CHANNEL_COUNT 16.0
#define pixPerBin (TEXTURE_SIZE / CHANNEL_COUNT)
#define halfBin (pixPerBin / 2.0)

vec3 palette(float t){
    vec3 a=vec3(.5,.5,.5);
    vec3 b=vec3(.5,.5,.5);
    vec3 c=vec3(1.,1.,1.);
    vec3 d=vec3(.263,.416,.557);

    return a+b*cos(6.28318*(c*t*d));
}

vec3 mixPalette(vec3 c1,vec3 c2,float t) {
    float mixFactor=.5+sin(t);
    return mix(c1,c2,mixFactor);
}

vec3 rgb2hsb(in vec3 c){
    vec4 K=vec4(0.,-1./3.,2./3.,-1.);
    vec4 p=mix(vec4(c.bg,K.wz),
    vec4(c.gb,K.xy),
    step(c.b,c.g));
    vec4 q=mix(vec4(p.xyw,c.r),
    vec4(c.r,p.yzx),
    step(p.x,c.r));
    float d=q.x-min(q.w,q.y);
    float e=1.e-10;
    return vec3(abs(q.z+(q.w-q.y)/(6.*d+e)), d/(q.x+e), q.x);
}
vec3 hsb2rgb(in vec3 c){
    vec3 rgb=clamp(abs(mod(c.x*6.+vec3(0.,4.,2.),
    6.)-3.)-1.,
    0.,
    1.);
    rgb=rgb*rgb*(3.-2.*rgb);
    return c.z*mix(vec3(1.),rgb,c.y);
}


float sdEquilateralTriangle(in vec2 p,in float r){
    const float k=sqrt(3.);
    p.x=abs(p.x)-r;
    p.y=p.y+r/k;
    if(p.x+k*p.y>0.)p=vec2(p.x-k*p.y,-k*p.x-p.y)/2.;
    p.x-=clamp(p.x,-2.*r,0.);
    return-length(p)*sign(p.y);
}

float noise(in float x,in float ts){
    float amplitude=.2*pow(x,3.);
    float frequency=2.;
    float y=sin(x*frequency);
    // float t = 0.01*(-iTime*130.0);
    float t=.01*(-ts*130.);
    y+=sin(x*frequency*2.1+t)*4.5;
    y+=sin(x*frequency*1.72+t*1.121)*4.;
    y+=sin(x*frequency*2.221+t*.437)*5.;
    y+=sin(x*frequency*3.1122+t*4.269)*2.5;
    y*=amplitude*.06;
    return y;
}

vec2 rotate2D(vec2 _st,float _angle){
    _st-=.5;
    _st=mat2(cos(_angle),-sin(_angle),
    sin(_angle),cos(_angle))*_st;
    _st+=.5;
    return _st;
}

void mainImage(out vec4 fragColor,in vec2 fragCoord){
    vec2 uv=-1.+2.*fragCoord/iResolution.xy;

    vec2 uv0=uv;
    vec3 finalColor=vec3(0.);

    for(float i=0.;i<iQuantity;i++){


        if(iWowTrigger){
            uv=fract(uv*1.1)-.5;
        }
        // uv = fract(uv * 1.1) - 0.5;
        // uv = fract(uv * .9) - 0.5;

        // uv.y *= -1.;
        uv=rotate2D(uv,iWow1*PI);

        // uv0.y *= -1.;
        // uv.y *= -1. * i;
        // uv.y += 0.2;
        uv.y+=.02*abs(sin(noise(i,iTime*.02)));
        // uv.y += 0.02 * noise(i, iTime*0.02);

         float a = atan(uv.y,uv.x);
        float normAngle = a / TWO_PI;
        float index = mod(normAngle * TEXTURE_SIZE * 2.0, TEXTURE_SIZE);
        float wave = (0.5 * (1.0+texelFetch(iChannel0, ivec2(index, 1), 0).x)) - 0.25;

        float d=1.;
        d*=sdEquilateralTriangle(uv,.9) + frequencyReact*wave;
        d*=exp(-sdEquilateralTriangle(uv0,.9)) - levelReact*wave;
        d+=.3*noise(i*d,iTime*.1);

        // float d = length(uv);
        // float d = length(uv) * exp(-length(uv0));

        vec3 col = mixPalette(iColorRGB, iColor2RGB, iTime);
        if (int(i) % 2 == 0) {
            col=mixPalette(iColor2RGB,iColorRGB,iTime);
        }
        //vec3 col=palette(length(uv0)+i*2.*iTime*.1);

        d=sin(d*2.+iTime*.5)/8.;
        d-=noise(i*d*iWow2,iTime*.1);
        // d = noise(i*d*8., iTime*0.1);
        d=abs(d);

        // d = 0.02 / d;
        float neonDampening = 1.5 - 0.5*iWow2;
        d = pow(0.01 / d, neonDampening);

        finalColor+=col*d;

        // uv.y -= 0.2 * i;
        // uv.y /= -1. * i;
    }

    finalColor*=.6;

    fragColor=vec4(finalColor,1.);
}