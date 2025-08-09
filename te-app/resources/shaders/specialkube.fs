// SpecialKube - dimensional cubes from tomorrow, today on a trip through space–best viewed in
// futuristic dystopian dreamscapes or at 10 & F
// warning: flashing imagery

#include <include/constants.fs>
#include <include/colorspace.fs>

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

// 3D rotation matrices
mat3 rotateX(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat3(1.0, 0.0, 0.0,
                0.0, c, -s,
                0.0, s, c);
}

mat3 rotateY(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat3(c, 0.0, s,
                0.0, 1.0, 0.0,
                -s, 0.0, c);
}

mat3 rotateZ(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat3(c, -s, 0.0,
                s, c, 0.0,
                0.0, 0.0, 1.0);
}

// project 3D point to 2D screen
vec2 project(vec3 p) {
    float perspective = 1.0 / (p.z + 3.0);
    return p.xy * perspective * 2.0;
}

float distToSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

float drawCube(vec2 uv, vec3 pos, mat3 rot, float size) {
    vec3 vertices[8];
    vertices[0] = vec3(-size, -size, -size);
    vertices[1] = vec3( size, -size, -size);
    vertices[2] = vec3( size,  size, -size);
    vertices[3] = vec3(-size,  size, -size);
    vertices[4] = vec3(-size, -size,  size);
    vertices[5] = vec3( size, -size,  size);
    vertices[6] = vec3( size,  size,  size);
    vertices[7] = vec3(-size,  size,  size);
    
    vec2 proj[8];
    for (int i = 0; i < 8; i++) {
        vec3 v = rot * vertices[i] + pos;
        proj[i] = project(v);
    }
    
    float minDist = 1000.0;
    
    // back face
    minDist = min(minDist, distToSegment(uv, proj[0], proj[1]));
    minDist = min(minDist, distToSegment(uv, proj[1], proj[2]));
    minDist = min(minDist, distToSegment(uv, proj[2], proj[3]));
    minDist = min(minDist, distToSegment(uv, proj[3], proj[0]));
    
    // front face
    minDist = min(minDist, distToSegment(uv, proj[4], proj[5]));
    minDist = min(minDist, distToSegment(uv, proj[5], proj[6]));
    minDist = min(minDist, distToSegment(uv, proj[6], proj[7]));
    minDist = min(minDist, distToSegment(uv, proj[7], proj[4]));
    
    // connecting edges
    minDist = min(minDist, distToSegment(uv, proj[0], proj[4]));
    minDist = min(minDist, distToSegment(uv, proj[1], proj[5]));
    minDist = min(minDist, distToSegment(uv, proj[2], proj[6]));
    minDist = min(minDist, distToSegment(uv, proj[3], proj[7]));
    
    return minDist;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // model coordinates for proper mapping
    vec3 modelCoords = _getModelCoordinates().xyz;
    vec2 uv = modelCoords.xy * 2.0 - 1.0;
    
    uv -= iTranslate;
    uv.x *= 1.5;
    
    vec3 col = vec3(0.0);
    
    int numCubes = int(iQuantity);
    for (int i = 0; i < 30; i++) {
        if (i >= numCubes) break;
        
        float id = float(i);

        // variation for spawning
        float rand1 = hash(id * 7.1);
        float rand2 = hash(id * 13.7);
        float rand3 = hash(id * 23.3);
        float rand4 = hash(id * 31.1);
        float rand5 = hash(id * 41.3);
        float slowTime = iTime * 0.05;
        float variation1 = sin(slowTime + rand1 * 12.56) * 0.3;
        float variation2 = cos(slowTime * 0.7 + rand2 * 12.56) * 0.3;
        
        // position with continuous variation
        vec3 pos;
        pos.x = (rand1 - 0.5) * 3.0 + variation1;
        pos.y = (rand2 - 0.5) * 2.0 + variation2;
        
        float cycleTime = mod(iTime * iSpeed + rand3 * 10.0, 10.0);
        pos.z = 7.0 - cycleTime * 1.5 * iSpeed;
        
        // skip if too close or too far
        if (pos.z < -1.0 || pos.z > 6.0) continue;
        
        // rotation angles with base angle offset
        float rotX = iRotationAngle + iTime * iSpin * (0.5 + rand4 * 0.5);
        float rotY = iRotationAngle * 0.7 + iTime * iSpin * (0.3 + rand5 * 0.5);
        float rotZ = iRotationAngle * 0.4 + iTime * iSpin * (0.4 + rand1 * 0.5);
        mat3 rotation = rotateX(rotX) * rotateY(rotY) * rotateZ(rotZ);
        
        float size = 0.15 * iScale * (0.7 + rand2 * 0.3);
        float dist = drawCube(uv, pos, rotation, size);
        
        // determine if this cube is "special" - use a stable random value
        float specialSeed = hash(id * 53.7 + floor(pos.z * 0.5) * 17.3);
        bool isSpecial = specialSeed < iWow2;
        
        float glow = 0.0;
        vec3 cubeColor = getGradientColor(rand3);
        
        if (isSpecial) {
            // special glitchy cube with intense brightness
            // glitch effect - rapid brightness oscillation
            float glitchTime = iTime * 20.0;
            float glitch = step(0.5, fract(glitchTime + rand1 * 10.0));
            glitch *= step(0.7, fract(glitchTime * 3.7 + rand2 * 5.0));
            
            // glitch effect on some frames
            float strobe = step(0.9, fract(glitchTime * 0.13 + rand3 * 7.0));
            
            // super bright glow with glitch modulation
            glow += exp(-dist * 150.0) * 8.0 * (1.0 + glitch * 2.0); // Very bright core
            glow += exp(-dist * 50.0) * 4.0;   // Intense inner glow
            glow += exp(-dist * 20.0) * 2.0;   // Strong outer glow
            glow += exp(-dist * 5.0) * 0.5;    // Extended halo
            
            // warning: flashing imagery
            if (strobe > 0.0) {
                cubeColor = vec3(1.0);
                glow *= 2.0;
            }
            
            // color shift for special cubes
            cubeColor = mix(cubeColor, vec3(0.0, 1.0, 1.0), glitch * 0.5);
        } else {
            // Normal cube glow
            glow += exp(-dist * 100.0) * 3.0; // Bright core
            glow += exp(-dist * 30.0) * 1.0;  // Inner glow
            glow += exp(-dist * 10.0) * 0.3;  // Outer glow
        }
        
        // fade based on Z distance
        float fade = smoothstep(7.0, 4.0, pos.z) * smoothstep(-2.0, 0.0, pos.z);
        
        // brightness boost for closer cubes
        fade *= (1.0 + 0.5 * (1.0 - pos.z / 6.0));
        if (isSpecial) {
            fade *= 1.5; // extra
        }
        
        col += cubeColor * glow * fade * iWow1;
    }

    // add a gradient
    col += 0.01 * vec3(0.05, 0.1, 0.15) * (1.0 + uv.y * 0.5);
    col = min(col, vec3(1.0));
    
    fragColor = vec4(col, 1.0);
}