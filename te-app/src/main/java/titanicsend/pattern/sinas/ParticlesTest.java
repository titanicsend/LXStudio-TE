package titanicsend.pattern.sinas;

import com.bulletphysics.dynamics.RigidBody;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.titanicsend.physics.BulletBootstrap;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.glengine.TEShader;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;

@LXCategory("Combo FG")
public class ParticlesTest extends GLShaderPattern {
  private TEShader renderShader;

  // --- Dynamic Sphere Emitter Demo ---
  private static final int MAX_SPHERES = 250; // Increased for 20,000+ balls
  // Room constants
  // Legacy room constants (unused in current normalization test)
  // private static final float ROOM_HALF_X = 6.0f;
  // private static final float ROOM_HEIGHT = 4.0f;
  // Fixed normalization space (no clamping): world 0..10 maps to 0..1 on x and y
  private static final float NORM_MIN_X = 0.0f;
  private static final float NORM_MAX_X = 10.0f;
  private static final float NORM_MIN_Y = 0.0f;
  private static final float NORM_MAX_Y = 10.0f;

  // Unified scaling: physical world 0-10 maps to normalized 0-1
  private static final float WORLD_TO_NORMALIZED_SCALE = 1.0f / (NORM_MAX_X - NORM_MIN_X); // = 0.1
  private BulletBootstrap bulletPhysics;
  private java.util.List<ActiveSphere> activeSpheres = new java.util.ArrayList<>();
  private boolean disableCollisions = true; // user flag
  private boolean bulletPhysicsInitialized = false;

  // Buffers for OpenGL rendering
  private java.nio.FloatBuffer physicsPositionBuffer =
      Buffers.newDirectFloatBuffer(MAX_SPHERES * 3); // x,y,z
  private java.nio.FloatBuffer physicsSizeBuffer = Buffers.newDirectFloatBuffer(MAX_SPHERES);

  // Simple physics parameters
  private static class ActiveSphere {
    String name;
    float radius;
    float ageSeconds;

    ActiveSphere(String name, float radius) {
      this.name = name;
      this.radius = radius;
      this.ageSeconds = 0f;
    }
  }

  private final float sphereMass = 0.5f; // Standard sphere mass
  private int spawnCounter = 0;
  private boolean spawnedStaticSet = false;

  // Kick timing
  private float timeSinceLastKick = 0f;
  private final float kickInterval = 5.0f; // 5 seconds between kicks

  public ParticlesTest(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // Simplified controls for physics demo
    controls.setRange(TEControlTag.SIZE, 0.2, 0.1, 0.8); // Sphere size multiplier
    controls.setRange(TEControlTag.SPEED, 1.0, 0.1, 3.0); // Physics simulation speed
    controls.setRange(TEControlTag.WOW1, 1.0, 0.0, 5.0); // Force strength multiplier

    // Disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    // WOWTRIGGER will apply upward forces to all spheres

    addCommonControls();

    // Render spheres from Bullet Physics simulation
    this.renderShader =
        addShader(
            GLShader.config(lx)
                .withFilename("fireflies_physics.fs")
                .withUniformSource(this::setCustomUniforms));

    // Initialize simple Bullet Physics scene
    initializeBulletPhysics();
  }

  // GPU textures for physics data
  private int posTexHandle = -1;
  private int sizeTexHandle = -1;
  private boolean posTexInitialized = false;
  private boolean sizeTexInitialized = false;

  // Custom uniform source to set particle buffer and sizes
  private void setCustomUniforms(GLShader shader) {
    // For the render shader, bind CPU positions and sizes textures
    if (shader == this.renderShader) {
      GL4 gl4 = shader.getGL4();
      // Refresh physics buffers from Bullet before upload
      updatePhysicsPositions();
      ensurePosTexture(gl4);
      ensureSizeTexture(gl4);

      // Upload latest positions
      physicsPositionBuffer.rewind();
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, posTexHandle);
      gl4.glTexSubImage2D(
          GL4.GL_TEXTURE_2D,
          0,
          0,
          0,
          Math.max(1, activeSpheres.size()),
          1,
          GL4.GL_RGB, // RGB for x,y,z positions
          GL4.GL_FLOAT,
          physicsPositionBuffer);

      // Upload latest sizes (normalized radii)
      physicsSizeBuffer.rewind();
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, sizeTexHandle);
      gl4.glTexSubImage2D(
          GL4.GL_TEXTURE_2D,
          0,
          0,
          0,
          Math.max(1, activeSpheres.size()),
          1,
          GL4.GL_RED,
          GL4.GL_FLOAT,
          physicsSizeBuffer);

      // Bind textures to shader uniforms
      shader.bindTextureUnit(GLShader.FIRST_UNRESERVED_TEXTURE_UNIT, posTexHandle);
      shader.setUniform("physicsPosTex", GLShader.FIRST_UNRESERVED_TEXTURE_UNIT);
      shader.bindTextureUnit(GLShader.FIRST_UNRESERVED_TEXTURE_UNIT + 1, sizeTexHandle);
      shader.setUniform("physicsSizeTex", GLShader.FIRST_UNRESERVED_TEXTURE_UNIT + 1);
      shader.setUniform("posCount", activeSpheres.size());
      // Compute and pass normalized inner wall bounds (physics walls at 2..8 → 0.2..0.8)
      // Pass normalized room bounds to shader using unified scaling
      // Walls at x,y 0-10 in world -> 0.0-1.0 normalized (full scene)
      float roomMinX = 0f * WORLD_TO_NORMALIZED_SCALE;
      float roomMaxX = 10f * WORLD_TO_NORMALIZED_SCALE;
      float roomMinY = 0f * WORLD_TO_NORMALIZED_SCALE;
      float roomMaxY = 10f * WORLD_TO_NORMALIZED_SCALE;
      shader.setUniform("roomMinX", roomMinX);
      shader.setUniform("roomMinY", roomMinY);
      shader.setUniform("roomMaxX", roomMaxX);
      shader.setUniform("roomMaxY", roomMaxY);
    }
  }

  // Normalize world coords to 0..1 and fill GPU buffers
  private void updatePhysicsPositions() {
    if (!bulletPhysicsInitialized || bulletPhysics == null) {
      return;
    }
    physicsPositionBuffer.clear();
    physicsSizeBuffer.clear();
    for (int i = 0; i < activeSpheres.size(); i++) {
      ActiveSphere as = activeSpheres.get(i);
      float[] pos = bulletPhysics.getBodyPosition(as.name);
      // Map world x,y (0..10) -> normalized 0..1 without clamping
      float nx = (pos[0] - NORM_MIN_X) / (NORM_MAX_X - NORM_MIN_X);
      float ny = (pos[1] - NORM_MIN_Y) / (NORM_MAX_Y - NORM_MIN_Y);
      // Keep 2D view with z centered
      float nz = 0.5f;
      physicsPositionBuffer.put(nx).put(ny).put(nz);
      // Use unified scaling: physical radius -> normalized radius
      // e.g., radius 2.0 -> 0.2 normalized
      float normalizedRadius = as.radius * WORLD_TO_NORMALIZED_SCALE;
      physicsSizeBuffer.put(normalizedRadius);
    }
    physicsPositionBuffer.rewind();
    physicsSizeBuffer.rewind();
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    // Safety check: bail if the pattern contains no shaders
    if (this.shaders.isEmpty()) {
      return;
    }

    // No separate particle buffer; emitter output is used as state texture

    // Update the model coords texture only when changed (and the first run)
    if (getModel() != null && getModel().points != null) {
      for (TEShader shader : this.shaders) {
        shader.setModelCoordinates(getModel());
      }
    }

    // Set the CPU buffer for any non-last shader to be null. These will be chained.
    for (int i = 0; i < (this.shaders.size() - 1); i++) {
      this.shaders.get(i).setCpuBuffer(null);
    }
    // Set the CPU buffer for the last shader, if using CPU mixer
    this.shaders.getLast().setCpuBuffer(this.lx.engine.renderMode.cpu ? this.colors : null);

    // Tick Bullet Physics before rendering
    tickBulletPhysics((float) (deltaMs / 1000.0));
    // Single shader
    this.shaders.get(0).run();
  }

  // --- Bullet Physics helpers ---

  private void initializeBulletPhysics() {
    try {
      TE.log("ParticlesTest: Initializing dynamic sphere emitter demo...");

      bulletPhysics = new BulletBootstrap();
      bulletPhysics.initialize();
      // Gravity set to (0, -1, 0) per request
      bulletPhysics.setGravity(0f, -10f, 0f);
      // Create walls matching full scene borders: X:[0,10], Y:[0,10]
      bulletPhysics.createRoom(
          0.5f, 9.5f, 0.5f, 9.5f, -0.1f, 0.1f, 1.0f); // Full scene with thin Z containment
      // Enable collision response for this test
      disableCollisions = false;

      bulletPhysicsInitialized = true;
      TE.log("ParticlesTest: Dynamic sphere emitter initialized");

    } catch (Exception e) {
      TE.error(e, "ParticlesTest: Failed to initialize Bullet Physics");
      bulletPhysicsInitialized = false;
    }
  }

  private void tickBulletPhysics(float dtSeconds) {
    if (!bulletPhysicsInitialized || bulletPhysics == null) {
      return;
    }

    try {
      float speed = (float) controls.getLXControl(TEControlTag.SPEED).getValuef();
      // Step the simulation
      bulletPhysics.step(dtSeconds * speed);

      // One-time static spawn of smaller spheres on the ground
      if (!spawnedStaticSet) {
        spawnedStaticSet = true;
        int numBalls = 200;
        float ballRadius = 0.1f; // Much smaller balls
        float groundY = 0.2f; // Just above the ground (floor is at Y=0)

        // Spawn balls in a grid pattern across the scene
        int gridSize = (int) Math.ceil(Math.sqrt(numBalls));
        float spacing = 9.5f / gridSize; // Space evenly within 0.25 to 9.75 range

        for (int i = 0; i < numBalls && i < MAX_SPHERES; i++) {
          int row = i / gridSize;
          int col = i % gridSize;

          // Add some randomness to avoid perfect grid
          float x = 0.25f + col * spacing + (float) (Math.random() - 0.5) * spacing * 0.3f;
          float y = groundY + row * ballRadius * 2.5f; // Stack them slightly
          float z = 0f + (float) (Math.random() - 0.5) * 0.1f; // Small Z randomness

          String name = "sphere-" + (spawnCounter++);
          bulletPhysics.addSphere(
              name, x, y, z, ballRadius, sphereMass * 0.1f, true); // Lighter balls
          RigidBody body = bulletPhysics.getBody(name);
          if (body != null) {
            body.setLinearVelocity(new javax.vecmath.Vector3f(0, 0, 0));
            body.setAngularVelocity(new javax.vecmath.Vector3f(0, 0, 0));
            bulletPhysics.setLinearFactor(
                name, new javax.vecmath.Vector3f(1, 1, 0)); // Restrict to XY plane
            body.activate(true);
          }
          if (disableCollisions) {
            bulletPhysics.setCollisionResponseEnabled(name, false);
          }
          activeSpheres.add(new ActiveSphere(name, ballRadius));
        }
      }
      // No aging/removal; keep static set

      // Periodic kick to a random ball every 5 seconds
      timeSinceLastKick += dtSeconds * speed;
      if (timeSinceLastKick >= kickInterval && !activeSpheres.isEmpty()) {
        // Pick a random ball to kick
        int randomIndex = (int) (Math.random() * activeSpheres.size());
        ActiveSphere targetSphere = activeSpheres.get(randomIndex);
        RigidBody body = bulletPhysics.getBody(targetSphere.name);
        if (body != null) {
          // Apply a random impulse in XY plane
          float angle = (float) (Math.random() * Math.PI * 2.0);
          float mag = 10.0f + (float) Math.random() * 10.0f;
          javax.vecmath.Vector3f impulse =
              new javax.vecmath.Vector3f(
                  (float) Math.cos(angle) * mag, (float) Math.sin(angle) * mag, 0f);
          body.applyCentralImpulse(impulse);
          body.activate(true);
        }
        timeSinceLastKick = 0f;
      }

    } catch (Exception e) {
      TE.error(e, "ParticlesTest: Error in Bullet Physics simulation");
    }
  }

  // Override to clean up Bullet Physics when pattern is disposed
  @Override
  public void dispose() {
    if (bulletPhysics != null) {
      bulletPhysics.cleanup();
      bulletPhysics = null;
    }
    super.dispose();
  }

  private void ensurePosTexture(GL4 gl4) {
    if (posTexInitialized) return;
    int[] tex = new int[1];
    gl4.glGenTextures(1, tex, 0);
    posTexHandle = tex[0];
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, posTexHandle);
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        GL4.GL_RGB32F, // RGB for 3D positions
        MAX_SPHERES,
        1,
        0,
        GL4.GL_RGB, // Changed to RGB
        GL4.GL_FLOAT,
        null);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, 0);
    posTexInitialized = true;
  }

  private void ensureSizeTexture(GL4 gl4) {
    if (sizeTexInitialized) return;
    int[] tex = new int[1];
    gl4.glGenTextures(1, tex, 0);
    sizeTexHandle = tex[0];
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, sizeTexHandle);
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        GL4.GL_R32F, // Single channel for radius
        MAX_SPHERES,
        1,
        0,
        GL4.GL_RED,
        GL4.GL_FLOAT,
        null);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, 0);
    sizeTexInitialized = true;
  }

  // Handle wow trigger button press - explosive force to scatter all spheres
  @Override
  protected void onWowTrigger(boolean on) {
    super.onWowTrigger(on);

    if (on && bulletPhysicsInitialized && !activeSpheres.isEmpty()) {
      // Wow trigger activated! Apply gentle wave force to all spheres
      float waveStrength = 3.0f + (float) getWow1() * 7.0f; // Much gentler force
      TE.log(
          "ParticlesTest: WOW TRIGGER! Gentle wave affecting %d spheres with strength %.1f",
          activeSpheres.size(), waveStrength);

      for (ActiveSphere sphere : activeSpheres) {
        RigidBody body = bulletPhysics.getBody(sphere.name);
        if (body != null) {
          // Random direction in XY plane
          float angle = (float) (Math.random() * Math.PI * 2.0);
          // Minimal upward component for subtle effect
          float upwardComponent = 0.1f + (float) Math.random() * 0.2f;
          float horizontalComponent = 0.6f + (float) Math.random() * 0.4f;

          javax.vecmath.Vector3f waveImpulse =
              new javax.vecmath.Vector3f(
                  (float) Math.cos(angle) * waveStrength * horizontalComponent,
                  waveStrength * upwardComponent, // Gentle upward push
                  0f // Keep Z at 0
                  );

          body.applyCentralImpulse(waveImpulse);
          body.activate(true);
        }
      }
    }
  }
}
