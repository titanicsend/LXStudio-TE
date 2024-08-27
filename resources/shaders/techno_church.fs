/*
#iUniform vec3 iColorRGB=vec3(.964,.144,.519)
#iUniform vec3 iColor2RGB=vec3(.226,.046,.636)
#iUniform float bassLevel=0.in{0.,1.}
#iUniform float trebleLevel=0.in{0.,1.}
#iUniform float volumeRatio=0.in{0.,1.}

#iUniform float iSpin=0. in{-1.,1.}
#iUniform float iScale=1.in{.01,4.}
#iUniform float iRotationAngle=0.in{0.,3.14}
#iUniform vec2 iTranslate=vec2(0.,0.)

#iUniform float iSpeed=1. in{1.,10.}
#iUniform float iQuantity=8. in{1., 16.}
#iUniform float levelReact=0. in {0., 1.}
#iUniform float frequencyReact=0. in {0., 1.}
#iUniform float iWow2=0.5 in {0., 1.}
#iUniform float iWow1=.1 in{0.,1.}
*/

#pragma name "TechnoChurch"
#pragma TEControl.XPOS.Range(0.07,-1.0,1.0)
#pragma TEControl.YPOS.Range(-0.03,-1.0,1.0)
#pragma TEControl.SPEED.Range(1.0,1.0,10.0)
#pragma TEControl.QUANTITY.Range(8.0,1.0,16.0)
#pragma TEControl.LEVELREACTIVITY.Range(0.0,0.0,1.0)
#pragma TEControl.FREQREACTIVITY.Range(0.0,0.0,1.0)
#pragma TEControl.WOW1.Range(0.0,0.0,1.0)
#pragma TEControl.WOW2.Range(0.0,0.0,1.0)

const float PI=3.14159265359;
const float TWO_PI=6.28318530718;
const float HALF_PI=PI/2.;

vec2 rotate(vec2 point,float angle){
    mat2 rotationMatrix=mat2(cos(angle),-sin(angle),sin(angle),cos(angle));
    return rotationMatrix*point;
}

float shape(in vec2 st,in int sides){
    // Angle and radius from the current pixel
    float a=atan(st.x,st.y)+PI;
    float r=TWO_PI/float(sides);

    // Shaping function that modulate the distance
    float d=cos(floor(.5+a/r)*r-a)*length(st);

    return d;
}

float box(in vec2 _st,in vec2 _size, float noiseDist){
    _size=vec2(.5) + noiseDist -_size*.5;
    vec2 uv=smoothstep(_size,
    _size+vec2(.001),
    _st);
    uv*=smoothstep(_size,
    _size+vec2(.001),
    vec2(1.)-_st);
    return uv.x*uv.y;
}

float xcross(in vec2 st,float xsize,float ysize,float outer,float inner, float noiseDist){

    float pct=0.;
    pct+=box(st,vec2(outer*xsize,outer*xsize/4.),noiseDist);
    pct+=box(st,vec2(outer*ysize/4.,outer*ysize),noiseDist);

    pct-=box(st,vec2(inner*xsize,inner*xsize/4.),noiseDist);
    pct-=box(st,vec2(inner*ysize/4.,inner*ysize),noiseDist);

    pct+=box(st,vec2(ysize/4.,ysize),noiseDist);
    pct+=box(st,vec2(xsize,xsize/4.),noiseDist);

    return pct;
}

float nest_xcross(float prev_pct,in vec2 st,float xsize,float ysize,float outer,float inner,float alpha,float base,float inc, float noiseDist){
    float f1=base+inc;
    float f2=base+2.*inc;
    float f3=base+3.*inc;
    float f4=base+4.*inc;

    float pct=prev_pct;
    pct=alpha*xcross(st,xsize*base,ysize*base,outer,inner, noiseDist)-pct;
    pct=alpha*xcross(st,xsize*f1,ysize*f1,outer,inner,noiseDist)-pct;
    pct=alpha*xcross(st,xsize*f2,ysize*f2,outer,inner,noiseDist)-pct;
    pct=alpha*xcross(st,xsize*f3,ysize*f3,outer,inner,noiseDist)-pct;
    pct=alpha*xcross(st,xsize*f3,ysize*f3,outer,inner,noiseDist)-pct;
    return pct;
}

float noise1d(in float x,in float ts){
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

vec2 random2(vec2 st){
    st=vec2(dot(st,vec2(127.1,311.7)),
    dot(st,vec2(269.5,183.3)));
    return-1.+2.*fract(sin(st)*43758.5453123);
}

float noise(vec2 st){
    vec2 i=floor(st);
    vec2 f=fract(st);
    vec2 u=f*f*(3.-2.*f);
    return mix(mix(dot(random2(i+vec2(0.,0.)),f-vec2(0.,0.)),
    dot(random2(i+vec2(1.,0.)),f-vec2(1.,0.)),u.x),
    mix(dot(random2(i+vec2(0.,1.)),f-vec2(0.,1.)),
    dot(random2(i+vec2(1.,1.)),f-vec2(1.,1.)),u.x),u.y);
}

void mainImage(out vec4 fragColor,in vec2 fragCoord){
    vec2 st=fragCoord.xy/iResolution.xy;
    st.x*=iResolution.x/iResolution.y;

    st=st-vec2(.5);
    st=rotate(st,iRotationAngle)/iScale;
    st-=iTranslate;

    vec3 color=vec3(0.);
    float pct=0.;
    float pct2=0.;

    float xsize=.1+levelReact*.5*trebleLevel;
    float ysize=.25-levelReact*.2*trebleLevel;
    float outer=1.2 + (.2 * sin(iTime))+levelReact*1.5*bassLevel;
    float inner=1.1 + (.3 * cos(iTime))+levelReact*2.*bassLevel;

//    float xsize=.1;
//    float ysize=.25;
//    float outer=1.2+(.2*sin(iTime));
//    float inner=1.1+(.3*cos(iTime));

    float a=atan(st.y,st.x);
    float m=abs(mod(a+iTime*2.,3.14*2.)-3.14)/3.6;
    m+=noise(st+iTime*.1)*.5;
    float noiseDist = 0.;
    noiseDist+=sin(a*50.)*noise(st+iTime*.2)*0.05*iWow1;
    noiseDist+=(sin(a*20.)*0.05*iWow1*pow(m,2.));

    vec2 st2 = st;
    float angle = 0.;
    for(int i=0;i<int(iQuantity);i++){
        angle = 0. + iWow2 * (TWO_PI - noise1d(PI, float(i)));

        st2 = rotate(st2, angle);
        // center the drawing space after rotation, but before drawing
        st2+=.5;
        if(i%2==0){
            pct=nest_xcross(pct,st2,xsize,ysize,outer,inner,.5,1.+1.5*float(i),.5, noiseDist);
        }else{
            pct2=nest_xcross(pct2,st2,xsize,ysize,outer,inner,.5,1.+1.5*float(i),.5, noiseDist);
        }
        // de-center before next rotation
        st2-=.5;
    }

    // apply 1st color
    color+=pct*iColorRGB;

    // apply 2nd color
    color+=pct2*iColor2RGB;

    fragColor=vec4(color,1.);
}