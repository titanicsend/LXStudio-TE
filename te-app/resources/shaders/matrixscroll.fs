#pragma name "MatrixScroll"
#pragma TEControl.SIZE.Range(3.0,0.1,5.0)
#pragma TEControl.QUANTITY.Range(4.0,3.0,24.0)
#pragma TEControl.WOW1.Range(1.0,0.0,1.0)

#include <include/constants.fs>
#include <include/colorspace.fs>

#define R fract(100.*sin(p.x*8.+ p.y))

void mainImage(out vec4 o,vec2 u) {
    vec3 ir = vec3(iResolution,1.0);
    vec3 v=vec3(u,1.)/ir-.5;

    vec3 s=.5/abs(v);
    vec3 i=ceil(8e2*(s.z=min(s.y,s.x))*(s.y<s.x?v.xzz:v.zyz));
    vec3 j=fract(i*=.01);
    vec3 p=vec3(9,int(iTime*(9.+8.*sin(i-=j).x)),0)+i;

    o-=o;
    o.g=R/s.z;
    p*=j;
    o*= R > 0. && j.x<.7 && j.y<.85 ? 1.:0.;
    o = vec4(getGradientColor(o.g), o.g);
}