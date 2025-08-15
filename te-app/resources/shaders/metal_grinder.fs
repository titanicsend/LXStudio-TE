// BUFFER A (0.61) of Screen Welding by QuantumSuper
// draw points on lots of different parabolas & use unclamped buffer as heatmap history
//
#pragma name "MetalGrinder"
#pragma TEControl.SIZE.Range(2.0,10.0,0.1)
#pragma TEControl.QUANTITY.Range(400.0,100.0,500.0)
#pragma TEControl.SPIN.Range(0.0,-5.,5.)    // kinda angular momentum
#pragma TEControl.WOW1.Range(0.75,0.1,1.25)   // lifetime of a particle in seconds

#pragma TEControl.WOWTRIGGER.Disable
#pragma TEControl.LEVELREACTIVITY.Disable
#pragma TEControl.FREQREACTIVITY.Disable

#include <include/constants.fs>
#include <include/colorspace.fs>

float lifetime = iWow1;         // lifetime of a particle in seconds
vec2 gravity = vec2(0., -5.6);  // gravitational constant
vec3 baseHot = vec3(1.0, 0.55, 0.2);  // colors for fire-colored fire
vec3 baseCool = vec3(1.0, 0.9, 0.6);

// 2d rotation matrix
mat2 r2d(float a) {
    float c=cos(a),s=sin(a);
    return mat2(c, s, -s, c);
}

// Functions from Dave Hoskins's hash w/o sine on shadertoy.
float hash21(vec2 p) {
    vec3 p3  = fract(vec3(p.xyx) * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx+33.33);
    return fract((p3.xx+p3.yz)*p3.zy);
}

// Calculates the brighness contribution of a particle falling of with distance
// from the emitter, plus a small amount of noise for variation.
float sparkColor(float dist, vec2 colorShift){
    return 6.*smoothstep(.025*colorShift.x, .0, dist)+clamp(.00008/dist/dist,.0,1.)+(1.+colorShift.y)*.0001/dist;
}

vec2 moveGrinder() {
    float t = iTime/2.;
    // calculate elliptical path for grinder
    vec2 v = cos(t)*vec2(cos(t*1.5), sin(t*3.) ) + sin(t) * vec2( cos(t*2.), sin(t*.75) );
    // scale, translate and limit path so it roughly circles the DJ booth
    v =  vec2(0.,0.6) + vec2(1.225, 1.0) * v;
    v.y = (v.y < 0.25) ? 0.25 : v.y;
    return v;
}

// Set velocity and angle for a particle, depending on its ID and cycle index.
// ID is the particle's index in the current cycle, and cycle index
// is the number of times the particle has been spawned.
vec2 initVelocity(float id, float cycIdx) {
    // Spray particles in all directions, adding fake angular momentum
    // if the spin control is on.
    float ang = iRotationAngle / (0.1 * id) + hash21(vec2(TAU * id, PI * cycIdx)) * TAU;
    // Vary particle speed
    vec2 spd = 1.0 + 4.5 * hash22(vec2(cycIdx * 16.182, id*20.1765));
    return vec2(cos(ang), sin(ang)) * spd;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord){
    // normalize and scale coordinates
    vec2 uv = 3.0 * (2.*fragCoord-iResolution.xy) / max(iResolution.x, iResolution.y);
    uv += moveGrinder(); //shape definitions

    // Draw particles with ballistic motion (origin, initial velocity, gravity)
    vec3 finalColor = vec3(0);

    for (float n = 1.; n <= iQuantity;n++){
        float id = n;

        // Per-particle phase so not all spawn at once
        float phase = hash21(vec2(id, 17.23));
        float cycles = iTime / lifetime + phase;
        float cycIdx = floor(cycles);
        float t = fract(cycles) * lifetime; // age since (re)spawn

        // Calculate velocity and position
        vec2 v0 = initVelocity(id, cycIdx);
        vec2 pos = v0 * t + 0.45 * gravity * t * t;
        vec2 vel = v0 + gravity * t;

        // Shading
        vec2 colorShift = vec2(hash21(vec2(id, 0.123)), 5. + 25. * hash21(vec2(id, 0.456)));

        float d = length(uv - pos);
        float ageFade = 1. - t / lifetime;

        // Hotter color when faster, brightness fades with age
        float sparkVel = clamp(length(vel)/6.0, 0.0, 1.0);
        vec3 color = mix(baseHot, baseCool, sparkVel);
        color = mix(color,getGradientColor(sparkVel),iWow2);

        finalColor += color * sparkColor(d, colorShift) * ageFade;
    }

    fragColor = vec4(finalColor * finalColor,1.0);
}