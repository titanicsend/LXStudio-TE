// Particle Gravity Simulation - updates particle physics based on previous state
// State format: R=pos.x, G=pos.y, B=enc(vel.x), A=enc(vel.y)

#pragma LXCategory("Combo FG")

// Gravity and physics parameters
uniform float iGravityStrength;    // gravity acceleration
uniform float iDamping;           // velocity damping factor

// Encode/decode helpers
float enc01(float v) { return v * 0.5 + 0.5; }
float dec11(float e) { return e * 2.0 - 1.0; }

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // Read previous particle state
    vec4 prevState = _getBackbufferPixel();
    
    // If no particle at this pixel, pass through
    if (prevState.a == 0.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }
    
    // Decode particle state
    vec2 pos = prevState.xy;  // position in 2D space [0,1]
    vec2 velocity = vec2(dec11(prevState.b), dec11(prevState.a));
    
    // Physics simulation
    float dt = 1.0 / 60.0;  // Assume 60fps for consistent physics
    
    // Apply gravity (downward in Y direction)
    float gravityY = iGravityStrength * dt;
    velocity.y -= gravityY;
    
    // Apply damping
    velocity *= iDamping;
    
    // Update position
    pos += velocity * dt;
    
    // Boundary collisions with bounce
    float bounce = 0.7;  // energy retained after bounce
    
    // Floor collision (Y=0)
    if (pos.y < 0.0) {
        pos.y = 0.0;
        velocity.y = abs(velocity.y) * bounce;
    }
    
    // Ceiling collision (Y=1)
    if (pos.y > 1.0) {
        pos.y = 1.0;
        velocity.y = -abs(velocity.y) * bounce;
    }
    
    // Side walls (X boundaries)
    if (pos.x < 0.0) {
        pos.x = 0.0;
        velocity.x = abs(velocity.x) * bounce;
    }
    if (pos.x > 1.0) {
        pos.x = 1.0;
        velocity.x = -abs(velocity.x) * bounce;
    }
    
    // Pack updated state and output: RG=pos, B=enc(vx), A=enc(vy)
    fragColor = vec4(pos, enc01(velocity.x), enc01(velocity.y));
}


