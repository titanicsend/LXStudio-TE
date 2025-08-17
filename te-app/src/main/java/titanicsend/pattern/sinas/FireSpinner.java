package titanicsend.pattern.sinas;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.titanicsend.physics.BulletBootstrap;
import com.titanicsend.physics.LoopPath;
import com.titanicsend.physics.SceneConfig;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.glengine.TEShader;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;

@LXCategory("Combo FG")
public class FireSpinner extends GLShaderPattern {
  private TEShader renderShader;

  // --- Particle Emitter Demo ---
  private static final int MAX_PARTICLES = 100; // Maximum particles in the system
  private static final float PARTICLE_LIFETIME = 3.0f; // Particles live for 3 seconds
  private static final float EMIT_RATE = 10.0f; // Particles per second

  // Room constants - same normalization as OrbitingComets
  private static final float NORM_MIN_X = 0.0f;
  private static final float NORM_MAX_X = 10.0f;
  private static final float NORM_MIN_Y = 0.0f;
  private static final float NORM_MAX_Y = 10.0f;
  private static final float NORM_MIN_Z = 0.0f;
  private static final float NORM_MAX_Z = 10.0f;

  // Unified scaling: physical world 0-10 maps to normalized 0-1
  private static final float WORLD_TO_NORMALIZED_SCALE = 1.0f / (NORM_MAX_X - NORM_MIN_X); // = 0.1

  private BulletBootstrap bulletPhysics;
  private java.util.List<ActiveParticle> activeParticles = new java.util.ArrayList<>();
  private boolean bulletPhysicsInitialized = false;

  // Texture buffers for GPU upload
  private java.nio.FloatBuffer physicsPositionBuffer =
      Buffers.newDirectFloatBuffer(MAX_PARTICLES * 3); // x,y,z
  private java.nio.FloatBuffer physicsSizeBuffer = Buffers.newDirectFloatBuffer(MAX_PARTICLES);

  // Enhanced particle tracking with age
  private static class ActiveParticle {
    String name;
    float radius;
    float age; // Age in seconds
    float maxAge; // Maximum age before removal

    // Transform data
    javax.vecmath.Vector3f prevPosition = new javax.vecmath.Vector3f();
    javax.vecmath.Vector3f currPosition = new javax.vecmath.Vector3f();

    ActiveParticle(String name, float radius, float maxAge) {
      this.name = name;
      this.radius = radius;
      this.age = 0f;
      this.maxAge = maxAge;
    }
  }

  private final float particleMass = 0.1f; // Light particles
  private int particleCounter = 0;

  // Emitter properties
  private LoopPath.CirclePath emitterPath;
  private long emitterStartTime;
  private float timeSinceLastEmit = 0f;
  private javax.vecmath.Vector3f emitterPrevPos = new javax.vecmath.Vector3f();
  private javax.vecmath.Vector3f emitterCurrPos = new javax.vecmath.Vector3f();
  private javax.vecmath.Vector3f emitterVelocity = new javax.vecmath.Vector3f();

  // Logging control
  private float logFPS = 10.0f; // Default 10 logs per second
  private long lastLogTime = System.nanoTime();
  private float logInterval = 1.0f / logFPS; // Seconds between logs

  public FireSpinner(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // Controls
    controls.setRange(TEControlTag.SIZE, 0.1, 0.05, 0.3); // Particle size
    controls.setRange(TEControlTag.SPEED, 2.0, 0.5, 5.0); // Spinning speed (faster range)
    controls.setRange(TEControlTag.WOW1, 5.0, 0.0, 20.0); // Initial particle velocity
    controls.setRange(TEControlTag.WOW2, 10.0, 1.0, 60.0); // Log FPS (logs per second)
    controls.setRange(
        TEControlTag.QUANTITY, 15.0, 5.0, 50.0); // Emit rate (more particles for fire effect)

    // Disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));

    addCommonControls();

    // Render particles using physics fireflies shader
    this.renderShader =
        addShader(
            GLShader.config(lx)
                .withFilename("fireflies_physics_3d.fs")
                .withUniformSource(this::setCustomUniforms));

    // Initialize physics scene
    initializeBulletPhysics();
  }

  // Custom uniform source for physics-based rendering
  private void setCustomUniforms(GLShader shader) {
    if (shader == this.renderShader) {
      // Update physics positions and upload to textures
      updatePhysicsPositions();

      // Set particle count and room bounds
      shader.setUniform("posCount", activeParticles.size());

      // Pass normalized room bounds to shader
      float roomMinX = 0.5f * WORLD_TO_NORMALIZED_SCALE;
      float roomMaxX = 9.5f * WORLD_TO_NORMALIZED_SCALE;
      float roomMinY = 0.5f * WORLD_TO_NORMALIZED_SCALE;
      float roomMaxY = 9.5f * WORLD_TO_NORMALIZED_SCALE;
      float roomMinZ = 0.5f * WORLD_TO_NORMALIZED_SCALE;
      float roomMaxZ = 9.5f * WORLD_TO_NORMALIZED_SCALE;
      shader.setUniform("roomMinX", roomMinX);
      shader.setUniform("roomMinY", roomMinY);
      shader.setUniform("roomMaxX", roomMaxX);
      shader.setUniform("roomMaxY", roomMaxY);
      shader.setUniform("roomMinZ", roomMinZ);
      shader.setUniform("roomMaxZ", roomMaxZ);

      // Add depth scale factor for Z-based perspective
      shader.setUniform("depthScaleFactor", 2.0f);

      // Upload texture data to GPU
      uploadPhysicsTextures(shader);
    }
  }

  // Update current transforms from physics engine
  private void updateCurrentTransforms() {
    if (!bulletPhysicsInitialized || bulletPhysics == null) {
      return;
    }

    for (ActiveParticle particle : activeParticles) {
      float[] pos = bulletPhysics.getBodyPosition(particle.name);
      particle.currPosition.set(pos[0], pos[1], pos[2]);
    }
  }

  private void updatePhysicsPositions() {
    if (!bulletPhysicsInitialized || bulletPhysics == null) {
      return;
    }

    physicsPositionBuffer.clear();
    physicsSizeBuffer.clear();

    for (int i = 0; i < activeParticles.size() && i < MAX_PARTICLES; i++) {
      ActiveParticle particle = activeParticles.get(i);
      float[] pos = bulletPhysics.getBodyPosition(particle.name);

      // Map world coordinates to normalized space
      float nx = (pos[0] - NORM_MIN_X) / (NORM_MAX_X - NORM_MIN_X);
      float ny = (pos[1] - NORM_MIN_Y) / (NORM_MAX_Y - NORM_MIN_Y);
      float nz = (pos[2] - NORM_MIN_Z) / (NORM_MAX_Z - NORM_MIN_Z);
      physicsPositionBuffer.put(nx).put(ny).put(nz);

      // Scale radius based on age (fade out as particle ages)
      float ageRatio = 1.0f - (particle.age / particle.maxAge);
      float normalizedRadius = particle.radius * WORLD_TO_NORMALIZED_SCALE * ageRatio;
      physicsSizeBuffer.put(normalizedRadius);
    }

    physicsPositionBuffer.rewind();
    physicsSizeBuffer.rewind();
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    if (this.shaders.isEmpty()) {
      return;
    }

    // Update model coordinates if needed
    if (getModel() != null && getModel().points != null) {
      for (TEShader shader : this.shaders) {
        shader.setModelCoordinates(getModel());
      }
    }

    // Set up shader chaining
    for (int i = 0; i < (this.shaders.size() - 1); i++) {
      this.shaders.get(i).setCpuBuffer(null);
    }
    this.shaders.getLast().setCpuBuffer(this.lx.engine.renderMode.cpu ? this.colors : null);

    // Tick physics and particle system
    tickBulletPhysics((float) (deltaMs / 1000.0));

    // Run shader
    this.shaders.get(0).run();
  }

  // Check if it's time to log based on the log FPS parameter
  private boolean shouldLog() {
    float newLogFPS = (float) controls.getLXControl(TEControlTag.WOW2).getValuef();
    if (newLogFPS != logFPS) {
      logFPS = newLogFPS;
      logInterval = 1.0f / logFPS;
    }

    long currentTime = System.nanoTime();
    float timeSinceLastLog = (currentTime - lastLogTime) / 1_000_000_000.0f;

    if (timeSinceLastLog >= logInterval) {
      lastLogTime = currentTime;
      return true;
    }
    return false;
  }

  private void initializeBulletPhysics() {
    try {
      TE.log("FireSpinner: Initializing particle emitter demo...");

      // Ball radius (for calculating thin Z room)
      float ballRadius = 0.1f; // Small particles
      float zThickness = 1.5f * (2.0f * ballRadius); // 1.5 times the ball diameter
      float zCenter = 5.0f; // Center of the thin Z slab
      float zMin = zCenter - zThickness / 2.0f;
      float zMax = zCenter + zThickness / 2.0f;

      // Create scene with thin room bounds and gravity
      SceneConfig config =
          new SceneConfig()
              .withGlobalGravity(new javax.vecmath.Vector3f(0f, -6.0f, 0f)) // Moderate gravity
              .withRoom(
                  0.5f, 9.5f, 0.5f, 9.5f, zMin, zMax, 1.0f) // Thin room bounds like ParticlesTest
              .withSolver(10, 1.0f / 120.0f); // Standard solver settings

      bulletPhysics = new BulletBootstrap();
      bulletPhysics.initializeScene(config);

      // Create fast horizontal circular path for the fire spinner
      emitterPath =
          new LoopPath.CirclePath(
              new javax.vecmath.Vector3f(5f, 5f, zCenter), // Center of room at thin Z
              3.0f, // Circle radius (larger for dramatic effect)
              5.0f, // Y plane (mid-height)
              3.0f); // Fast angular speed (radians/sec) for spinning fire effect

      emitterStartTime = System.nanoTime();

      // Initialize emitter position
      emitterCurrPos = emitterPath.getPosition(0, 1, 0);
      emitterPrevPos.set(emitterCurrPos);

      // Create particle emitter in BulletBootstrap
      bulletPhysics.addEmitter(
          "fire-emitter",
          MAX_PARTICLES,
          PARTICLE_LIFETIME,
          BulletBootstrap.EmitterInitialSpeed.EMITTER_SPEED,
          emitterPath);

      // Configure emitter properties for fire effect
      float particleSize = (float) controls.getLXControl(TEControlTag.SIZE).getValuef();
      float emitRate = (float) controls.getLXControl(TEControlTag.QUANTITY).getValuef();
      bulletPhysics.setEmitterProperties(
          "fire-emitter", particleSize, particleMass * 0.5f, emitRate); // Lighter particles

      bulletPhysicsInitialized = true;
      TE.log("FireSpinner: Particle emitter initialized");

    } catch (Exception e) {
      TE.error(e, "FireSpinner: Failed to initialize Bullet Physics");
      bulletPhysicsInitialized = false;
    }
  }

  private void tickBulletPhysics(float dtSeconds) {
    if (!bulletPhysicsInitialized || bulletPhysics == null) {
      return;
    }

    try {
      // Update emitter properties from controls
      float speed = (float) controls.getLXControl(TEControlTag.SPEED).getValuef();
      float particleSize = (float) controls.getLXControl(TEControlTag.SIZE).getValuef();
      float emitRate = (float) controls.getLXControl(TEControlTag.QUANTITY).getValuef();

      // Update emitter properties
      bulletPhysics.setEmitterProperties("fire-emitter", particleSize, particleMass, emitRate);
      bulletPhysics.setEmitterSpeed("fire-emitter", speed);

      // Step physics (emitters are now updated automatically)
      bulletPhysics.step(dtSeconds);

      // Get active particles from emitter
      java.util.List<String> particleNames = bulletPhysics.getEmitterParticles("fire-emitter");

      // Sync our particle list with physics and restrict to Z plane
      activeParticles.clear();
      for (String name : particleNames) {
        float[] particleData = bulletPhysics.getEmitterParticleData("fire-emitter", name);
        if (particleData != null) {
          ActiveParticle particle =
              new ActiveParticle(
                  name,
                  particleData[0], // radius
                  particleData[1]); // maxAge
          particle.age = particleData[2]; // current age
          activeParticles.add(particle);

          // Restrict particles to XY plane (like ParticlesTest)
          bulletPhysics.setLinearFactor(name, new javax.vecmath.Vector3f(1, 1, 0));
        }
      }

      // Update positions
      updateCurrentTransforms();

      // Log status
      if (shouldLog()) {
        TE.log("FireSpinner: Active particles: %d", activeParticles.size());
      }

    } catch (Exception e) {
      TE.error(e, "FireSpinner: Error in physics simulation");
    }
  }

  @Override
  public void dispose() {
    if (bulletPhysics != null) {
      bulletPhysics.cleanup();
      bulletPhysics = null;
    }
    super.dispose();
  }

  // GPU texture upload methods (same as OrbitingComets)
  private void uploadPhysicsTextures(GLShader shader) {
    GL4 gl4 = shader.getGL4();

    ensurePosTexture(gl4);
    gl4.glActiveTexture(GL4.GL_TEXTURE0 + 4);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, posTexHandle);
    gl4.glTexSubImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        0,
        0,
        Math.min(activeParticles.size(), MAX_PARTICLES),
        1,
        GL4.GL_RGB,
        GL4.GL_FLOAT,
        physicsPositionBuffer);
    shader.setUniform("physicsPosTex", 4);

    ensureSizeTexture(gl4);
    gl4.glActiveTexture(GL4.GL_TEXTURE0 + 5);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, sizeTexHandle);
    gl4.glTexSubImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        0,
        0,
        Math.min(activeParticles.size(), MAX_PARTICLES),
        1,
        GL4.GL_RED,
        GL4.GL_FLOAT,
        physicsSizeBuffer);
    shader.setUniform("physicsSizeTex", 5);
  }

  private int posTexHandle = -1;
  private boolean posTexInitialized = false;

  private void ensurePosTexture(GL4 gl4) {
    if (posTexInitialized) return;
    int[] tex = new int[1];
    gl4.glGenTextures(1, tex, 0);
    posTexHandle = tex[0];
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, posTexHandle);
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D, 0, GL4.GL_RGB32F, MAX_PARTICLES, 1, 0, GL4.GL_RGB, GL4.GL_FLOAT, null);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
    posTexInitialized = true;
  }

  private int sizeTexHandle = -1;
  private boolean sizeTexInitialized = false;

  private void ensureSizeTexture(GL4 gl4) {
    if (sizeTexInitialized) return;
    int[] tex = new int[1];
    gl4.glGenTextures(1, tex, 0);
    sizeTexHandle = tex[0];
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, sizeTexHandle);
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D, 0, GL4.GL_R32F, MAX_PARTICLES, 1, 0, GL4.GL_RED, GL4.GL_FLOAT, null);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
    sizeTexInitialized = true;
  }

  @Override
  protected void onWowTrigger(boolean on) {
    super.onWowTrigger(on);

    if (on && bulletPhysicsInitialized) {
      // Burst emit particles when WOW triggered
      TE.log("FireSpinner: WOW TRIGGER! Burst emitting particles");

      // Tell the emitter to burst
      if (bulletPhysics != null) {
        bulletPhysics.burstEmitter("fire-emitter", 20); // Emit 20 particles instantly
      }
    }
  }
}
