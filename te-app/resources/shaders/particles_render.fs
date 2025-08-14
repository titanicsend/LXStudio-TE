// Particle Renderer — reads emitter buffer (stateTex) laid out as:
// y=0: RGBA = pos.xyz, mass
// y=1: RGBA = vel.xyz, age

#pragma LXCategory("Combo FG")
#pragma TEControl.SIZE.Range(0.05,0.01,0.3)

uniform sampler2D stateTex;   // emitter output bound here from Java
uniform ivec2 stateRes;       // = (numParticles, 2)
uniform float iParticleSize;  // radius in model units (0..1)

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    // Model/LED position in 0..1 space
    vec3 ledPos = _getModelCoordinates().xyz;

    vec3 color = vec3(0.0);
    float accum = 0.0;
    
    // Loop over all particles in the state texture's first row
    for (int i = 0; i < stateRes.x; ++i)
    {
        // Exact fetch of pixel (i,0): position+mass
        vec4 posData = texelFetch(stateTex, ivec2(i, 0), 0);
        float mass = posData.a;
        if (mass <= 0.0) continue;

        vec3 p = posData.xyz;               // particle position in 0..1
        float d = length(ledPos.xy - p.xy); // 2D distance, ignore Z

        // soft circle (XY only)
        float r = iParticleSize;
        float w = 1.0 - smoothstep(r * 0.5, r, d);

        // simple per-id tint (two colors for 2 particles)
        vec3 tint = (i == 0) ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 1.0);

        color += tint * w;
        accum += w;
    }

    // normalize a touch
    if (accum > 0.0) color /= max(1.0, accum);

    fragColor = vec4(color, 1.0);
}


