// Candy Flip - dots cascade out from edge joints and cause
// a scattering butterfly effect, moving from one to the next

#include <include/colorspace.fs>

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)),
             dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

vec2 getJointPosition(float jointId) {
    float cols = ceil(sqrt(iQuantity));
    float row = floor(jointId / cols);
    float col = mod(jointId, cols);
    
    float xOffset = mod(row, 2.0) * 0.5;
    
    vec2 pos = vec2(
        (col + xOffset) / cols,
        row / cols
    );
    
    // scale and center
    pos = pos * 0.7 + 0.15;
    return pos;
}

float getNearestJoint(vec2 pos, float excludeJoint) {
    float numJoints = floor(iQuantity);
    float nearestDist = 999.0;
    float nearestJoint = -1.0;
    
    for (float i = 0.0; i < 30.0; i++) {
        if (i >= numJoints || i == excludeJoint) continue;
        
        vec2 jointPos = getJointPosition(i);
        float dist = distance(pos, jointPos);
        
        if (dist < nearestDist) {
            nearestDist = dist;
            nearestJoint = i;
        }
    }
    
    return nearestDist < 0.2 ? nearestJoint : -1.0;
}

vec2 getParticlePosition(float particleId, float time) {
    float burstDuration = 1.5;
    float burstCycle = 5.0;
    float maxGenerations = 3.0;
    
    // current cycle timing
    float cycleTime = mod(time, burstCycle);
    float currentCycle = floor(time / burstCycle);
    
    vec2 seed = vec2(particleId, currentCycle);
    float startJoint = floor(hash(seed) * iQuantity);
    float angle = hash(seed + vec2(1.0, 0.0)) * 6.28318;
    
    vec2 pos = getJointPosition(startJoint);
    vec2 vel = vec2(cos(angle), sin(angle)) * 0.15;
    
    float generation = 0.0;
    float genTime = cycleTime;
    
    // particle movement through generations
    for (float gen = 0.0; gen < maxGenerations; gen++) {
        if (genTime <= 0.0) break;
        
        float dt = min(genTime, burstDuration);
        
        vec2 newPos = pos + vel * dt * (1.0 + gen * 0.3); // Accelerate with each generation
        
        // check if we hit a joint
        if (dt >= burstDuration * 0.8) { // Near end of burst
            float nextJoint = getNearestJoint(newPos, startJoint);
            
            if (nextJoint >= 0.0 && generation < maxGenerations - 1.0) {
                // burst!
                generation += 1.0;
                startJoint = nextJoint;
                pos = getJointPosition(nextJoint);
                
                // new direction - scatter pattern
                float scatterAngle = (particleId / 8.0) * 6.28318;
                angle = scatterAngle + hash(vec2(particleId, nextJoint)) * 1.0 - 0.5;
                vel = vec2(cos(angle), sin(angle)) * 0.15 * (1.0 - generation * 0.2);
                
                genTime -= burstDuration;
            } else {
                pos = newPos;
                break;
            }
        } else {
            pos = newPos;
            break;
        }
    }
    
    return pos;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    float time = iTime * iSpeed;
    
    vec3 color = vec3(0.0);
    float totalAlpha = 0.0;
    
    // beat effects
    float beatPulse = beat * iWow1;
    
    // draw joints
    float numJoints = floor(iQuantity);
    for (float i = 0.0; i < 30.0; i++) {
        if (i >= numJoints) break;
        
        vec2 jointPos = getJointPosition(i);
        float dist = distance(uv, jointPos);
        
        // pulsing joints with burst indication
        float burstCycle = 5.0;
        float cyclePhase = mod(time + i * 0.2, burstCycle) / burstCycle;
        float burstPulse = smoothstep(0.0, 0.1, cyclePhase) * smoothstep(0.2, 0.1, cyclePhase);
        
        float jointSize = 0.02 * iSize * (1.0 + burstPulse * 2.0 + beatPulse * 0.3);
        
        if (dist < jointSize) {
            vec3 jointColor = getGradientColor(i / numJoints + time * 0.05);
            float alpha = smoothstep(jointSize, jointSize * 0.5, dist);
            alpha *= (1.0 + burstPulse * 3.0);
            
            color += jointColor * alpha;
            totalAlpha = max(totalAlpha, alpha);
        }
    }
    
    float particlesPerJoint = 8.0;
    float numParticles = numJoints * particlesPerJoint;
    
    for (float i = 0.0; i < 240.0; i++) {
        if (i >= numParticles) break;
        
        vec2 particlePos = getParticlePosition(i, time);
        
        // check if particle is visible
        if (particlePos.x >= -0.1 && particlePos.x <= 1.1 && 
            particlePos.y >= -0.1 && particlePos.y <= 1.1) {
            
            float dist = distance(uv, particlePos);
            
            // particle size decreases with generation
            float burstCycle = 5.0;
            float cycleTime = mod(time, burstCycle);
            float generation = floor(cycleTime / 1.5); // Approximate generation
            
            float particleSize = 0.008 * iSize * (1.0 - generation * 0.25);
            
            if (dist < particleSize) {
                float colorShift = generation * 0.3 + time * 0.1;
                vec3 particleColor = getGradientColor(colorShift);
                
                // trail effect (like edge runner)
                float alpha = smoothstep(particleSize, 0.0, dist);
                
                // fade out near end of cycle
                float fadeFactor = smoothstep(burstCycle * 0.9, burstCycle, cycleTime);
                alpha *= (1.0 - fadeFactor);
                
                // beat sync
                alpha *= (1.0 + beatPulse * 0.5);
                
                color += particleColor * alpha * 0.8;
                totalAlpha = max(totalAlpha, alpha);
            }
        }
    }
    
    float brightness = iBrightness * (1.0 + beatPulse * 0.2);
    color *= brightness;
    totalAlpha = min(totalAlpha * brightness, 1.0);
    
    vec3 warmTint = vec3(1.2, 1.0, 0.8);
    vec3 coolTint = vec3(0.8, 1.0, 1.2);
    vec3 tint = mix(coolTint, warmTint, iWow2);
    color *= tint;
    
    color = max(color, vec3(totalAlpha * 0.05));
    
    fragColor = vec4(color, totalAlpha);
}
