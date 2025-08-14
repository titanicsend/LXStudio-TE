// BUFFER A (0.61) of Screen Welding by QuantumSuper
// draw points on lots of different parabolas & use unclamped buffer as heatmap history
//
#pragma name "MetalGrinder"

#define PARTICLE_COUNT 500.
#define PI 3.14159265359

mat2 r2d(float a) {
    float c=cos(a),s=sin(a);
    return mat2(c, s, -s, c);
}

float hash21(vec2 p){
    p = fract(p*vec2(13.81, 741.76));
    p += dot(p, p+42.23);
    return fract(p.x*p.y);
}

vec2 parametricParabola(float t, vec2 seed){
    float d = .1 + 3.*hash21(.678*seed.yx); //y-stretch
    float c = sign(t) * (.01 + 2.*hash21(.987*seed)); //maximum shift
    float b = abs(c) + hash21(.285*seed) + .001; //x-stretch
    float a = c*c/b/b; //origin height
    t -= c/b;
    return vec2( b*t+c, (a-t*t)*d);
}

float lightUp(float dist, vec2 modif){
    return 6.*smoothstep(.025*modif.x, .0, dist)+clamp(.00008/dist/dist,.0,1.)+(1.+modif.y)*.0001/dist; //combined semi-hard shape with semi-soft & soft glow
}

vec2 moveGrinder(){ //coordinate shift to create shapes
    float t = iTime/2.;
    vec2 v =  cos(t)*vec2(cos(t*1.5), sin(t*3.) ) + sin(t) * vec2( cos(t*2.), sin(t*.75) );
    return v;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord){

    vec2 uv = 3. * (2.*fragCoord-iResolution.xy) / max(iResolution.x, iResolution.y);
    uv += moveGrinder(); //shape definitions

    // Draw particles on deterministic parabolic paths
    vec3 finalColor = vec3(0);

    for (float n=0.;n++<PARTICLE_COUNT;){
        float time = iTime/2. + n/PARTICLE_COUNT;
        vec2 seed = vec2(ceil(time)*.123,ceil(time)*.456);
        float speed = sign(.5-hash21(seed+.123*n)) * (2.+2.5*hash21(seed*n*.456));
        vec2 myMod = vec2( hash21(seed/n*.123), 5.+25.*hash21(seed/n*.456)) * r2d(iRotationAngle);
        vec3 color = fract(-time) * vec3( 1.+fract(time), .5+.6*fract(-time), .2+fract(-time)*fract(-time));
        finalColor += color * lightUp( length( uv - parametricParabola( .7*fract(time)*speed, seed*n)), myMod);
    }


    fragColor = vec4(finalColor * finalColor,1.);
}