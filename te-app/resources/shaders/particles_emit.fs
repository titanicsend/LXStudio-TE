// Particle Emitter - Write particle state to a dedicated state texture

#pragma LXCategory("Combo FG")

// We render to a small framebuffer sized (numParticles x 2)
// y=0: RGBA = pos.xyz, mass
// y=1: RGBA = vel.xyz, age

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
	int particleIndex = int(gl_FragCoord.x);
	int dataRow = int(gl_FragCoord.y);

	// Initialize two particles for testing
	if (dataRow == 0) {
		if (particleIndex == 0) {
			// Particle 0 at XY=(0.25,0.25)
			fragColor = vec4(0.0, 0.0, 3.0, 1.0);
		} else if (particleIndex == 1) {
			// Particle 1 shares same XY so it overlaps; different Z
			fragColor = vec4(1.0, 1.0, 3.0, 1.0);
		} else {
			fragColor = vec4(0.0);
		}
	} else if (dataRow == 1) {
		if (particleIndex == 0) {
			fragColor = vec4(0.0, 0.0, 0.0, 0.0);
		} else if (particleIndex == 1) {
			fragColor = vec4(0.0, 0.0, 0.0, 0.0);
		} else {
			fragColor = vec4(0.0);
		}
	} else {
		fragColor = vec4(0.0);
	}
}
