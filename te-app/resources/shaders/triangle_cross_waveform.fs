/*
#iUniform vec3 iColorRGB=vec3(0.9647, 0.1451, 0.5176)
// #iUniform vec3 iColor2RGB=vec3(0., 0., 0.)
#iUniform vec3 iColor2RGB=vec3(.226,.046,.636)
// #iUniform vec3 iColor2RGB=vec3(.964,.144,.519)
#iUniform float iSpin=0. in{-1.,1.}
#iUniform float iSpeed=1. in{1.,10.}
#iUniform float iSize=.25 in{.1,.9}
#iUniform float iQuantity=8. in{1., 16.}
#iUniform float iScale=1. in {0.5, 4.}
#iUniform float iRotationAngle=0. in {0., 3.14}
#iUniform vec2 iTranslate=vec2(0., 0.)
#iUniform float bassLevel=0. in {0., 1.}
#iUniform float trebleLevel=0. in {0., 1.}
#iUniform float levelReact=0. in {0., 1.}
#iUniform float frequencyReact=0. in {0., 1.}
#iUniform float volumeRatio=0. in {0., 1.}
*/

const float PI=3.14159265359;
const float TWO_PI=6.28318530718;
const int NUM_STEMS=5;

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

vec2 random2(vec2 st){
  st=vec2(dot(st,vec2(127.1,311.7)),
  dot(st,vec2(269.5,183.3)));
  return-1.+2.*fract(sin(st)*43758.5453123);
}

float box(in vec2 _st,in vec2 _size){
  _size=vec2(.5)-_size*.5;
  vec2 uv=smoothstep(_size, _size+vec2(.001), _st);
  uv*=smoothstep(_size, _size+vec2(.001), vec2(1.)-_st);
  return uv.x*uv.y;
}

float xcross(in vec2 st,float xsize,float ysize,float outer,float inner){
  float pct=0.;
  pct+=box(st,vec2(outer*xsize,outer*xsize/4.));
  pct+=box(st,vec2(outer*ysize/4.,outer*ysize));

  pct-=box(st,vec2(inner*xsize,inner*xsize/4.));
  pct-=box(st,vec2(inner*ysize/4.,inner*ysize));

  pct+=box(st,vec2(ysize/4.,ysize));
  pct+=box(st,vec2(xsize,xsize/4.));

  return pct;
}

float offsetAmount = trebleLevel * levelReact;

float nest_xcross(float prev_pct,in vec2 st,float xsize,float ysize,float outer,float inner,float alpha,float baseSize,float inc,float stemMult){
  float f1=baseSize+inc;
  float f2=baseSize+2.*inc;
  float f3=baseSize+3.*inc;
  float f4=baseSize+4.*inc;

  float a = alpha*stemMult;
  float pct=prev_pct;
  pct=a * xcross(st+offsetAmount*random2(vec2(baseSize, .01*iTime)),xsize*baseSize,ysize*baseSize,outer,inner)-pct;
  pct=a * xcross(st+offsetAmount*random2(vec2(f1, .01*iTime)),xsize*f1,ysize*f1,outer,inner)-pct;
  pct=a * xcross(st+offsetAmount*random2(vec2(f2, .01*iTime)),xsize*f2,ysize*f2,outer,inner)-pct;
  pct=a * xcross(st+offsetAmount*random2(vec2(f3, .01*iTime)),xsize*f3,ysize*f3,outer,inner)-pct;
  pct=a * xcross(st+offsetAmount*random2(vec2(f4, .01*iTime)),xsize*f3,ysize*f3,outer,inner)-pct;
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

float noise(vec2 st){
  vec2 i=floor(st);
  vec2 f=fract(st);
  vec2 u=f*f*(3.-2.*f);
  return mix(mix(dot(random2(i+vec2(0.,0.)),f-vec2(0.,0.)),
  dot(random2(i+vec2(1.,0.)),f-vec2(1.,0.)),u.x),
  mix(dot(random2(i+vec2(0.,1.)),f-vec2(0.,1.)),
  dot(random2(i+vec2(1.,1.)),f-vec2(1.,1.)),u.x),u.y);
}
#define TEXTURE_SIZE 512.
#define CHANNEL_COUNT 16.

float pixPerBin = TEXTURE_SIZE/CHANNEL_COUNT;
float halfBin = (TEXTURE_SIZE/CHANNEL_COUNT) / 2.;

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
  float outer=1.2+levelReact*1.5*bassLevel;
  float inner=1.1+levelReact*2.*bassLevel;

  float yoffset=.7;
  st+=vec2(.5,yoffset);

  int stemIdx = 0;
  float stemMultiplier = 1.0;

  for(int i=0;i<int(iQuantity);i++){

    stemIdx = i % 5;
    if (stemIdx == 0) {
      stemMultiplier = stemBass;
    } else if (stemIdx == 1) {
      stemMultiplier = stemDrumHits;
    } else if (stemIdx == 2) {
      stemMultiplier = stemVocals;
    } else if (stemIdx == 3) {
      stemMultiplier = stemOther;
    } else if (stemIdx == 4) {
      stemMultiplier = stemDrumHits;
    }
    stemMultiplier = mix(1.0, stemMultiplier, iWow1);

    if(i%2==0){
      pct = nest_xcross(pct,st,xsize,ysize,outer,inner,.5,1.+1.5*float(i),.5, stemMultiplier);
    }else{
      pct2 = nest_xcross(pct2,st,xsize,ysize,outer,inner,.5,1.+1.5*float(i),.5, stemMultiplier);
    }
  }
  st-=vec2(.5,yoffset);

  // original triangle shape
  float f=shape(st,3);
  // setup radial noise
  float a=atan(st.y,st.x);
  float normAngle=a/TWO_PI;

  // The audio texture size is 512x2
  // mapping to screen depends on iScale and iQuantity - here
  // we use iQuantity to figure out which texture pixels are relevant
  float index=mod(normAngle*TEXTURE_SIZE*2.,TEXTURE_SIZE);
  // The second row of is normalized waveform data
  // we'll just draw this over the spectrum analyzer.  Sound data
  // coming from LX is in the range -1 to 1, so we scale it and move it down
  // a bit so we can see it better.
  float wave=(.5*(1.+texelFetch(iChannel0,ivec2(index,1),0).x))-.25;

  // scaling factor on triangle distance field
  float triangle_scale=1.1;
  //triangle_scale = frequencyReact;
  float triangle_dist=f/triangle_scale;

  // innermost step function we apply to triangle distance field
  float r=.15;
  r+=frequencyReact*wave;
  r+=levelReact*.2*volumeRatio;
  float tri_inner_mask=r+noise1d(.35,3.*iTime);

  float tri_inner_margin=.04;// - 0.04 * volumeRatio;
  float tri_inner_start=tri_inner_mask+tri_inner_margin;
  float tri_inner_thickness=.1;

  float tri_outer_margin=.01;
  float tri_outer_start=tri_inner_start+tri_inner_thickness+tri_outer_margin;
  float tri_outer_thickness=.01;

  float inner_mask=step(tri_inner_mask,triangle_dist);
  float outer_triangle_edge=step(tri_outer_start+tri_outer_thickness+.07,triangle_dist);

  // mask out 1st color
  pct*=1.-inner_mask;
  pct+=step(tri_inner_start,triangle_dist)-step(tri_inner_start+tri_inner_thickness,triangle_dist);
  // apply 1st color
  color+=pct*iColorRGB;

  // mask out the second color
  pct2*=1.-inner_mask;
  pct2+=.4*step(tri_outer_start,triangle_dist)-step(tri_outer_start+tri_outer_thickness,triangle_dist);
  pct2+=step(tri_outer_start+2.*tri_outer_thickness,triangle_dist)-step(tri_outer_start+3.*tri_outer_thickness,triangle_dist);
  // apply 2nd color
  color+=pct2*mix(iColorRGB, iColor2RGB, iWow2);

  fragColor=vec4(color,1.);
}
