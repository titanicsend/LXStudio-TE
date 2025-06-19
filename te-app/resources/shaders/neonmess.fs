#pragma name "NeonMess"

#pragma TEControl.SIZE.Range(0.4,1.1,0.25)
// Wow1 controls the "black level"
#pragma TEControl.WOW1.Range(0.25,0.0,1.0)

#pragma TEControl.FREQREACTIVITY.Disable
#pragma TECONTROL.QUANTITY.Disable
#pragma TEControl.WOW2.Disable
#pragma TEControl.WOWTRIGGER.Disable

//#pragma iChannel1 "resources/shaders/textures/fontgenerator.png"

#include <include/constants.fs>
#include <include/colorspace.fs>
//#include <include/debug.fs>

#define ITERATIONS 100.0

// 3D axis/angle rotation from FabriceNeyret2
// (comment from https://www.shadertoy.com/view/WtjcDt)
// takes the point to be rotated, a vector describing the axis of rotation,
// and the angle to rotate by in radians.
#define rot3D(p,axis,t) mix(axis*dot(p,axis),p,cos(t))+sin(t)*cross(p,axis)

void mainImage( out vec4 O, vec2 U) {
    O = vec4(0);
    float acc = 0.0;
    float beatWave = 0.0;
    float signal = bassLevel;

    vec2 uv = U / iResolution.xy;

    vec3 rd = normalize( vec3(U-.5*iResolution.xy, iResolution.y))*20.;

    float sc,dotp,totdist=0.,
    t = iTime,
    // avoid starting at 0 angle. Way too bright.
    theta = -iRotationAngle + 0.1;


    for (float i = 0.; i < ITERATIONS; i++) {
        vec4 p = vec4(rd * totdist, 0.);
        beatWave = 0.0;

        // center the point in the scene, a short distance from the camera
        p.xyz += vec3(0,0,-18);
        p.xyz = rot3D(p.xyz, normalize(vec3(sin(t / 7.),cos(t / 11.),0.)), theta);
        sc = 1.0;

        for (float j = 0.; j < 6.; j++) {
            // field generator - offset point by sin/cos curve
            p = abs(p) * 0.56 -
              vec4(.025 * cos(t / 3. + p.xy / 6.0), .015 * sin(t / 2 - p.zw / 3.0));

            dotp = max(1. / dot(p, p),iScale);
            sc *= dotp ;

            p.zw = length(p.xz) < length(p.zw) ? p.xz : p.zw;  //reflection


            if (abs(sc - signal) < 0.081) {
                beatWave -= min(.006,signal / 100.0);
            }

            p = abs(p) * dotp - 1.;
        }

        float dist = abs(length(p) - .1) / sc; // distance estimate
        float stepsize = dist + 0.0001;        // plus a little extra
        totdist += stepsize;                   // move the distance along rd

        //accumulate density, fading with distance and iteration count
        acc += ((levelReact * beatWave) + stepsize) * exp(-i*i*stepsize*stepsize/ 2.0);
    }

    acc = 1.0 - acc;
    float bri = smoothstep(-0.5 + iWow1, 1.0, acc);
    O = vec4(getGradientColor(fract(acc)),bri);

    // Debug print: Show the signal level on the car
    //float x = print(iChannel1, uv - vec2(0.15, 0.2), signal, 0.075, 2.0, 4.0);
    //vec4 txt = x * vec4(0.0,1.0,1.0,1.0);
    //O = mix(O, txt, txt.a);
}
