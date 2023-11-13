#pragma name "Circuitry"

#include <include/constants.fs>
#include <include/colorspace.fs>

mat2 rot(float a) {
    float s=sin(a), c=cos(a);
    return mat2(c,s,-s,c);
}

vec3 fractal(vec2 p) {    
    p.x += 0.3333 * sin(beat*TWO_PI);
       
    float ot1 = 1000., ot2=ot1, it=0.;
    
    for (float i = 0.; i < 4.; i++) {
        p=abs(p);
        p=p/clamp(p.x*p.y,0.15,5.)-vec2(1.5,1.);
        float m = abs(p.x+sin(iTime));
        if (m<ot1) {
            ot1=m+step(fract(iTime*1.+float(i)*.05),.15*abs(p.y));
            it=i;
        }
        ot2=min(ot2,length(p));
    }
    
    ot1=exp(-30.*ot1);
    ot2=exp(-130.*ot2);
    return hsv2rgb(vec3(it*0.13+.5,.7,1.))*sqrt(ot1+ot2);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = fragCoord.xy/iResolution.xy-.5;
    uv.x *= iResolution.x/iResolution.y;

    // draw the fractal, antialiased by supersampling
    // we don't need tons of this on the car
    // but a little makes it much smoother looking
    float aa=1.;

    // distance between samples
    vec2 sc=1./iResolution.xy/aa;
    
    vec3 c=vec3(0.);
    for (float i=-aa; i < aa; i++) {
        for (float j =- aa; j < aa; j++) {
            vec2 p = uv + vec2(i,j) * sc;
            c += fractal(p);
        }
    }
    c = c/(aa*aa*0.35);
    fragColor = vec4(c, 1.);
}
