#pragma name "NeonMess"

#pragma TEControl.SIZE.Range(2.0,0.5,10)

#include <include/constants.fs>
#include <include/colorspace.fs>

// Euler axis/angle rotation.  Very handy.
#define rot3D(p,axis,t) mix(axis*dot(p,axis),p,cos(t))+sin(t)*cross(p,axis)

// cosine palette mapping
#define H(h)  (cos( h/2. + vec3(31,10,20) ) * .6)

// output scaling
#define L(c)  log(1.+c*c)

void mainImage( out vec4 O, vec2 U) {
    O = vec4(0);
    vec3 c=vec3(0);

    vec3 rd = normalize( vec3(U-.5*iResolution.xy, iResolution.y))*10.;
    rd *= iScale;

    float sc,dotp,totdist=0.,
    t=iTime / 3.;

    for (float i = 0.; i < 150.; i++) {
        vec4 p = vec4(rd * totdist, 0.);

        p.xyz += vec3(0,0,-18.);
        p.xyz = rot3D(p.xyz, normalize(vec3(sin(t / 7.),cos(t / 11.),0)), -iRotationAngle);
        sc = 1.;  //scale factor

        for (float j = 0.; j < 6.; j++) {
            p = abs(p) * 0.56 -  vec4(.025 * cos(p.xy / 5.), .01 * sin(p.zw / 3.));

            dotp = max(1. / dot(p, p),.4);
            sc *= dotp ;

            p.zw = length(p.xz) < length(p.zw) ? p.xz : p.zw;  //reflection
            p = abs(p) * dotp - 1.;
        }

        float dist = abs(length(p) - .1) / sc; // distance estimate
        float stepsize = dist + 0.0001;        // distance plus a little extra
        totdist += stepsize;                   // move the distance along rd

        //accumulate color, fading with distance and iteration count
        c += mix(vec3(1), H(L(sc)),.6) * .014 *  exp(-i*i*stepsize*stepsize/2.);
    }

    c *= c;
    c = 1. - exp(-c);
    float b = 1.25 * max(c.r, max(c.g, c.b)); // brightness
    O = vec4(getGradientColor(b),b * b);
}
