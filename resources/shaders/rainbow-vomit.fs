//  Someday, it will be a sensibile particle fireworks pattern, but today
//  it's Unicorn Vomit!

const vec2 gravity = vec2(0.3, -.43);

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233)))*43758.5453);
}

// random number in specified range
float rand(float from, float to, vec2 co) {
    return from + rand(co)*(to - from);
}

float getParticleAlpha(float particle_time, float threshold, float period) {
    return 0.35 * ((particle_time > threshold) ? 1.0 - (particle_time - threshold)/(period - threshold) : 1.0);
}

// cotton candy colors, to reflect what the unicorn ate
vec4 getParticleColor(float i, float index) {
    return vec4(rand(vec2(i*index, 2.0)), rand(vec2(i*index, 1.5)), rand(vec2(i*index, 1.2)), 1.0);
}

float counter = 0.0;
void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    float scale = 1.5/max(iResolution.x, iResolution.y);
    vec2 coord = gl_FragCoord.xy*scale;
    //coord.x = mod(2.*coord.x,1.5);
    
    vec2 origin = vec2(0.01, 0.4*iResolution.y*scale);

    fragColor = vec4(0.);

    for (float i = 1.; i < 60.; i++) {
        float period = rand(1.0, 2.0, vec2(i, 0.));

        float t = iTime - period*rand(vec2(i, 1.));

        float particle_time = mod(t, period);
        float index = ceil(t/period);

        vec2 speed = vec2(rand(0.35, .75, vec2(index*i, 3.)), rand(.21, .76, vec2(index*i, 4.)));
        vec2 pos = origin + particle_time*speed + gravity*particle_time*particle_time;

        float threshold = .7*period;

        float alpha = getParticleAlpha(particle_time, threshold, period);
        vec4 particle_color = getParticleColor(i, index);

        float angle_speed = 3.1415; // rand(-1.0,0.0, vec2(index*i, 5.0));
        float angle = atan(pos.y - coord.y, pos.x - coord.x) + angle_speed*iTime;

        float radius = rand(.09, .05, vec2(index*i, 2.));

        float dist_1 = sin(angle)*radius;
        float dist_2 = -2.0 * radius + sin(angle)*radius;
        float dist_3 = 0.002 + sin(angle)*radius;
        
        // particles come evenly distributed in three sizes, chosen more-or-less at random
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

