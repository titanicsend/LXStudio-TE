//  Ok... here's what we do with the legendary Unicorn Vomit shader!
//  Add some sound reactivity and mirror it in radial sections about the origin and...
//  Kaleidoscope!

// short term moving average volume from TEAudioPattern
uniform float avgVolume;

const float PI = 3.141592653589793;
const float halfpi = PI / 2.;
const float TAU = PI * 2.;

// gives particle movement a little acceleration over time due to "gravity".
// physics it ain't.
const vec2 gravity = vec2(0.3, -.3);

// random number between 0 and 1
float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233)))*43758.5453);
}

// random number in specified range
float rand(float from, float to, vec2 co) {
    return from + rand(co)*(to - from);
}

// particles fade out over time
float getParticleAlpha(float particle_time, float threshold, float period) {
    return 0.35 * ((particle_time > threshold) ? 1.0 - (particle_time - threshold)/(period - threshold) : 1.0);
}

// cotton candy colors, to reflect what the unicorn ate.  We're still unicorn vomit at heart!
vec4 getParticleColor(float i, float index) {
    return vec4(rand(vec2(i*index, 2.0)), rand(vec2(i*index, 1.5)), rand(vec2(i*index, 1.2)), 1.0);
}

// create a number of radial reflections around the specified origin
vec2 Kaleidoscope( vec2 uv, float n, float bias ) {
    float angle = PI / n;

    float r = length( uv*.5 );
    float a = atan( uv.y, uv.x ) / angle;
    a -= iRotationAngle;

    a = mix( fract( a ), 1.0 - fract( a ), mod( floor( a ), 2.0 ) ) * angle;
    return vec2( cos( a ), sin( a ) ) * r;
}


float counter = 0.0;
void mainImage( out vec4 fragColor, in vec2 fragCoord ) {

    // normalize our coordinates
    vec2 coord = gl_FragCoord.xy / max(iResolution.x, iResolution.y);

    // then reflect them into the kaleidoscope
    coord = Kaleidoscope(vec2(-0.5, -0.25) + coord, 7., iTime );

    // read the audio values for the current pixel
    float freq = texture(iChannel0, vec2(coord.x,0)).r;
    float wave = texture(iChannel1, vec2(coord.x,0)).r;
    float volume = (avgVolume > 0.0) ? avgVolume : 0.512 + 0.5 * sin(20. * iTime);

    fragColor = vec4(0.);

    // create a number of particles
    for (float i = 1.; i < 20.; i++) {

        // generate cell index and lifetime for this particle
        float period = rand(1.0, 2.0, vec2(i, 0.));
        float t = iTime / 2. - period*rand(vec2(i, 1.));

        float particle_time = mod(t, period * 1.414);
        float index = ceil(t/period);

        //vec2 speed = vec2(rand(0.35, .75, vec2(index*i, 3.)), rand(.21, .76, vec2(index*i, 4.)));

        // speed is based on to be the difference between eq level at the current frequency and average volume
        // plus a little randomization
        // TODO - we may need to subdivide this into bass and treble bands to keep the ranges sane.
        vec2 speed = abs(avgVolume - freq)/avgVolume + vec2(rand(0.05, .25, vec2(index*i, 3.)), rand(.014, .26, vec2(index*i, 4.)));
        vec2 pos = particle_time*speed + gravity*particle_time*particle_time;

        float threshold = .7*period;

        float alpha = getParticleAlpha(particle_time, threshold, period);
        vec4 particle_color = getParticleColor(i, index);

        float angle_speed = 3.1415; // rand(-1.0,0.0, vec2(index*i, 5.0));
        float angle = atan(pos.y - coord.y, pos.x - coord.x) + angle_speed*iTime;

        //float radius = rand(.09, .05, vec2(index*i, 2.));
        float radius = abs(wave) / 100.;

        float dist_1 = sin(angle)*radius;
        float dist_2 = -1.0 * radius + sin(angle)*radius;
        float dist_3 = 0.002 + sin(angle)*radius;

        // draw particles in three discrete size groups
        counter++;
        float particleSize = mod(counter, 3.0);

        if (particleSize == 0.0) {
            fragColor += alpha * ((1.0 - smoothstep(dist_1, dist_1 + .25, distance(coord, pos))) +
            (1.0 - smoothstep(0.01, 0.02, distance(coord, pos))) +
            (1.0 - smoothstep(radius * 0.1, radius * 0.9, distance(coord, pos)))) * particle_color;
        }

        else if (particleSize  == 1.0) {
            fragColor += alpha * ((1.0 - smoothstep(dist_2, dist_2 + .05, distance(coord, pos))) +
            (1.0 - smoothstep(0.01, 0.02, distance(coord, pos))) +
            (1.0 - smoothstep(radius * 0.1, radius * 0.11, distance(coord, pos)))) * particle_color;
        }

        else if (particleSize == 2.0) {
            fragColor += alpha * ((1.0 - smoothstep(dist_3, dist_3 + .105, distance(coord, pos))) +
            (1.0 - smoothstep(0.01, 0.102, distance(coord, pos))) +
            (1.0 - smoothstep(radius * 0.1, radius * 0.9, distance(coord, pos)))) * particle_color;
        }

        if (counter > 100.0) counter = 0.0;
    }
}

