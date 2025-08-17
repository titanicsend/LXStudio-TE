package titanicsend.pattern.sinas;

import com.bulletphysics.dynamics.RigidBody;
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
public class OrbitingComets extends GLShaderPattern {
  private TEShader renderShader;

  // --- Multi-Ball 3D Demo ---
  private static final int MAX_SPHERES = 10; // 10 balls with varied radii
  // Room constants
  // Legacy room constants (unused in current normalization test)
  // private static final float ROOM_HALF_X = 6.0f;
  // private static final float ROOM_HEIGHT = 4.0f;
  // Fixed normalization space (no clamping): world 0..10 maps to 0..1 on x and y
  private static final float NORM_MIN_X = 0.0f;
  private static final float NORM_MAX_X = 10.0f;
  private static final float NORM_MIN_Y = 0.0f;
  private static final float NORM_MAX_Y = 10.0f;
  private static final float NORM_MIN_Z = 0.0f;
  private static final float NORM_MAX_Z = 10.0f;

  // Unified scaling: physical world 0-10 maps to normalized 0-1
  private static final float WORLD_TO_NORMALIZED_SCALE = 1.0f / (NORM_MAX_X - NORM_MIN_X); // = 0.1
  private BulletBootstrap bulletPhysics;
  private java.util.List<ActiveSphere> activeSpheres = new java.util.ArrayList<>();
  private boolean bulletPhysicsInitialized = false;

  // Legacy texture buffers (will be removed)
  private java.nio.FloatBuffer physicsPositionBuffer =
      Buffers.newDirectFloatBuffer(MAX_SPHERES * 3); // x,y,z
  private java.nio.FloatBuffer physicsSizeBuffer = Buffers.newDirectFloatBuffer(MAX_SPHERES);

  // GPU-optimized physics parameters with interpolation data
  private static class ActiveSphere {
    String name;
    float radius;

    // Transform interpolation data for smooth rendering
    javax.vecmath.Vector3f prevPosition = new javax.vecmath.Vector3f();
    javax.vecmath.Vector3f currPosition = new javax.vecmath.Vector3f();
    javax.vecmath.Quat4f prevRotation = new javax.vecmath.Quat4f(0, 0, 0, 1);
    javax.vecmath.Quat4f currRotation = new javax.vecmath.Quat4f(0, 0, 0, 1);

    ActiveSphere(String name, float radius) {
      this.name = name;
      this.radius = radius;
    }
  }

  private final float sphereMass = 0.5f; // Standard sphere mass
  private LoopPath.CirclePath orbitPath;

  // Logging control
  private float logFPS = 10.0f; // Default 10 logs per second
  private long lastLogTime = System.nanoTime();
  private float logInterval = 1.0f / logFPS; // Seconds between logs

  public OrbitingComets(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // Simplified controls for physics demo
    controls.setRange(TEControlTag.SIZE, 0.2, 0.1, 0.8); // Sphere size multiplier
    // SPEED now controls tangential force multiplier (-1..1), not physics step rate
    controls.setRange(TEControlTag.SPEED, 0.05, -1.0, 1.0);
    controls.setRange(TEControlTag.WOW1, 1.0, 0.0, 5.0); // Force strength multiplier
    controls.setRange(TEControlTag.WOW2, 10.0, 1.0, 60.0); // Log FPS (logs per second)

    // Disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    // WOWTRIGGER will apply upward forces to all spheres

    addCommonControls();

    // Render spheres using optimized texture-based fragment shader
    this.renderShader =
        addShader(
            GLShader.config(lx)
                .withFilename("fireflies_physics_3d.fs")
                .withUniformSource(this::setCustomUniforms));

    // Initialize simple Bullet Physics scene
    initializeBulletPhysics();
  }

  // Texture buffers for physics position data (already declared above)

  // Custom uniform source for physics-based rendering (texture approach for compatibility)
  private void setCustomUniforms(GLShader shader) {
    if (shader == this.renderShader) {
      // Update physics positions and upload to textures
      updatePhysicsPositions();

      // Set particle count and room bounds
      shader.setUniform("posCount", activeSpheres.size());

      // Pass normalized room bounds to shader using unified scaling
      // Walls at x,y,z 0.5-9.5 in world -> 0.05-0.95 normalized (slightly inset scene)
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

      // Add depth scale factor for Z-based perspective (adjust sphere size based on Z distance)
      shader.setUniform("depthScaleFactor", 2.0f); // Scale factor for perspective effect

      // Upload texture data to GPU
      uploadPhysicsTextures(shader);
    }
  }

  // Update current transforms from physics engine
  private void updateCurrentTransforms() {
    if (!bulletPhysicsInitialized || bulletPhysics == null) {
      return;
    }

    for (ActiveSphere sphere : activeSpheres) {
      float[] pos = bulletPhysics.getBodyPosition(sphere.name);
      sphere.currPosition.set(pos[0], pos[1], pos[2]);

      // Log sphere position and size at controlled FPS rate
      if (shouldLog()) {
        float normalizedX = (pos[0] - NORM_MIN_X) / (NORM_MAX_X - NORM_MIN_X);
        float normalizedY = (pos[1] - NORM_MIN_Y) / (NORM_MAX_Y - NORM_MIN_Y);
        float normalizedZ = (pos[2] - NORM_MIN_Z) / (NORM_MAX_Z - NORM_MIN_Z);
        float normalizedRadius = sphere.radius / (NORM_MAX_X - NORM_MIN_X);

        TE.log(
            "OrbitingComets: Sphere '%s' - World(%.2f, %.2f, %.2f) Norm(%.3f, %.3f, %.3f) Radius=%.3f NormRadius=%.3f LogFPS=%.1f",
            sphere.name,
            pos[0],
            pos[1],
            pos[2],
            normalizedX,
            normalizedY,
            normalizedZ,
            sphere.radius,
            normalizedRadius,
            logFPS);
      }

      // For now, spheres don't rotate meaningfully, but we'll track it for completeness
      // sphere.currRotation could be updated from RigidBody rotation if needed
    }
  }

  private void updatePhysicsPositions() {
    if (!bulletPhysicsInitialized || bulletPhysics == null) {
      return;
    }

    // Log sphere count at controlled FPS rate
    boolean shouldLogNow = shouldLog();
    if (shouldLogNow) {
      TE.log(
          "OrbitingComets: updatePhysicsPositions - Processing %d spheres", activeSpheres.size());
    }

    physicsPositionBuffer.clear();
    physicsSizeBuffer.clear();
    for (int i = 0; i < activeSpheres.size(); i++) {
      ActiveSphere as = activeSpheres.get(i);
      float[] pos = bulletPhysics.getBodyPosition(as.name);
      // Map world x,y,z (0..10) -> normalized 0..1 without clamping
      float nx = (pos[0] - NORM_MIN_X) / (NORM_MAX_X - NORM_MIN_X);
      float ny = (pos[1] - NORM_MIN_Y) / (NORM_MAX_Y - NORM_MIN_Y);
      float nz = (pos[2] - NORM_MIN_Z) / (NORM_MAX_Z - NORM_MIN_Z); // Use actual Z coordinate
      physicsPositionBuffer.put(nx).put(ny).put(nz);
      // Use unified scaling: physical radius -> normalized radius
      // e.g., radius 2.0 -> 0.2 normalized
      float normalizedRadius = as.radius * WORLD_TO_NORMALIZED_SCALE;
      physicsSizeBuffer.put(normalizedRadius);

      // Log first sphere details at controlled FPS rate
      if (i == 0 && shouldLogNow) {
        TE.log(
            "OrbitingComets: Sphere 0 '%s' - Shader gets Norm(%.3f, %.3f, %.3f) NormRadius=%.3f",
            as.name, nx, ny, nz, normalizedRadius);
      }
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

  // Check if it's time to log based on the log FPS parameter
  private boolean shouldLog() {
    // Update log FPS from control
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
      TE.log("OrbitingComets: Initializing orbiting comets demo with circle path...");

      // Create SceneConfig for orbiting comets with combined forces
      SceneConfig config =
          new SceneConfig()
              .addCentralGravity(
                  new javax.vecmath.Vector3f(5f, 3.5f, 5f), /*GM*/
                  1.0f, /*soft*/
                  0.25f) // Provides orbital dynamics
              .withRoom(
                  0.5f, 9.5f, 0.5f, 9.5f, 0.5f, 20f,
                  1.0f) // Room bounds: minX, maxX, minY, maxY, minZ, maxZ, thickness
              .withSolver(10, 1.0f / 120.0f); // Standard solver settings

      bulletPhysics = new BulletBootstrap();
      bulletPhysics.initializeScene(config);

      // Create the circular path for orbiting comets
      orbitPath =
          new LoopPath.CirclePath(
              new javax.vecmath.Vector3f(5f, 3.5f, 5f), // Center at (5, 3.5, 5) in world space
              3.0f, // Circle radius = 3
              3.5f); // Y plane = 3.5

      // Create 10 tethered comets on the circle path
      int numComets = 10;
      javax.vecmath.Vector3f centerGravity = new javax.vecmath.Vector3f(5f, 3.5f, 5f);
      float GM = 1.0f; // Match the central gravity GM

      for (int i = 0; i < numComets; i++) {
        String name = "comet-" + i;

        // Get position from path
        javax.vecmath.Vector3f pos = orbitPath.getPosition(i, numComets, 0);

        // Vary orb radius
        float minRadius = 0.1f;
        float maxRadius = 0.2f;
        float radiusRange = maxRadius - minRadius;
        float radiusVariation = (float) Math.random() * radiusRange;
        float orbRadius = minRadius + radiusVariation;

        // Create tethered sphere
        bulletPhysics.addTetheredSphere(
            name,
            pos.x,
            pos.y,
            pos.z,
            orbRadius,
            sphereMass * orbRadius,
            true, // enableCcd
            centerGravity, // tether anchor = center of gravity
            3.0f, // rest length = circle radius
            10.0f, // stiffness
            0.3f, // damping
            1.0f); // tangential force for merry-go-round effect

        // Set orbital velocity
        RigidBody body = bulletPhysics.getBody(name);
        if (body != null) {
          javax.vecmath.Vector3f velocity =
              orbitPath.getOrbitalVelocity(i, numComets, centerGravity, GM);
          body.setLinearVelocity(velocity);
          body.activate(true);

          // Set collision properties
          bulletPhysics.setCollisionResponseEnabled(name, true);
          bulletPhysics.setLinearFactor(name, new javax.vecmath.Vector3f(1, 1, 1));

          // Add to our tracking system
          ActiveSphere sphere = new ActiveSphere(name, orbRadius);
          sphere.currPosition.set(pos.x, pos.y, pos.z);
          sphere.prevPosition.set(pos.x, pos.y, pos.z);
          activeSpheres.add(sphere);

          TE.log(
              "OrbitingComets: Added tethered comet '%s' at (%.2f, %.2f, %.2f) radius=%.2f velocity=(%.2f, %.2f, %.2f)",
              name, pos.x, pos.y, pos.z, orbRadius, velocity.x, velocity.y, velocity.z);
        }
      }

      bulletPhysicsInitialized = true;
      TE.log("OrbitingComets: Orbiting comets physics demo initialized with %d comets", numComets);

    } catch (Exception e) {
      TE.error(e, "OrbitingComets: Failed to initialize Bullet Physics");
      bulletPhysicsInitialized = false;
    }
  }

  private void tickBulletPhysics(float dtSeconds) {
    if (!bulletPhysicsInitialized || bulletPhysics == null) {
      return;
    }

    try {
      // SPEED is a pure multiplier for tangential force
      float speed = (float) controls.getLXControl(TEControlTag.SPEED).getValuef();
      if (bulletPhysics != null) bulletPhysics.setExternalSpeedFactor(speed);

      // Just step the physics - BulletBootstrap handles fixed timestep internally
      bulletPhysics.step(dtSeconds);

      // Update current positions after physics step
      updateCurrentTransforms();

    } catch (Exception e) {
      TE.error(e, "OrbitingComets: Error in Bullet Physics simulation");
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

  // Upload physics data to GPU textures (manual texture management for compatibility)
  private void uploadPhysicsTextures(GLShader shader) {
    GL4 gl4 = shader.getGL4();

    // Create position texture (RGB32F for x,y,z positions)
    ensurePosTexture(gl4);
    gl4.glActiveTexture(GL4.GL_TEXTURE0 + 4); // Use texture unit 4
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, posTexHandle);
    gl4.glTexSubImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        0,
        0,
        activeSpheres.size(),
        1,
        GL4.GL_RGB,
        GL4.GL_FLOAT,
        physicsPositionBuffer);
    shader.setUniform("physicsPosTex", 4);

    // Create size texture (R32F for radius)
    ensureSizeTexture(gl4);
    gl4.glActiveTexture(GL4.GL_TEXTURE0 + 5); // Use texture unit 5
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, sizeTexHandle);
    gl4.glTexSubImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        0,
        0,
        activeSpheres.size(),
        1,
        GL4.GL_RED,
        GL4.GL_FLOAT,
        physicsSizeBuffer);
    shader.setUniform("physicsSizeTex", 5);
  }

  // Create position texture if needed
  private int posTexHandle = -1;
  private boolean posTexInitialized = false;

  private void ensurePosTexture(GL4 gl4) {
    if (posTexInitialized) return;
    int[] tex = new int[1];
    gl4.glGenTextures(1, tex, 0);
    posTexHandle = tex[0];
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, posTexHandle);
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D, 0, GL4.GL_RGB32F, MAX_SPHERES, 1, 0, GL4.GL_RGB, GL4.GL_FLOAT, null);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
    posTexInitialized = true;
  }

  // Create size texture if needed
  private int sizeTexHandle = -1;
  private boolean sizeTexInitialized = false;

  private void ensureSizeTexture(GL4 gl4) {
    if (sizeTexInitialized) return;
    int[] tex = new int[1];
    gl4.glGenTextures(1, tex, 0);
    sizeTexHandle = tex[0];
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, sizeTexHandle);
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D, 0, GL4.GL_R32F, MAX_SPHERES, 1, 0, GL4.GL_RED, GL4.GL_FLOAT, null);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
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
          "OrbitingComets: WOW TRIGGER! Gentle wave affecting %d spheres with strength %.1f",
          activeSpheres.size(), waveStrength);

      for (ActiveSphere sphere : activeSpheres) {
        RigidBody body = bulletPhysics.getBody(sphere.name);
        if (body != null) {
          // Uniform random direction in 3D space using spherical coordinates
          float theta = (float) (Math.random() * Math.PI * 2.0); // Azimuthal angle (0 to 2π)
          float phi =
              (float)
                  (Math.acos(
                      2.0 * Math.random() - 1.0)); // Polar angle for uniform sphere distribution

          // Use uniform distribution on unit sphere for even 3D scattering
          float radius =
              (float)
                  Math.pow(Math.random(), 1.0 / 3.0); // Cube root for uniform volume distribution

          // Convert spherical to Cartesian coordinates
          float sinPhi = (float) Math.sin(phi);
          float forceX = (float) Math.cos(theta) * sinPhi * radius * waveStrength;
          float forceY = (float) Math.sin(theta) * sinPhi * radius * waveStrength;
          float forceZ = (float) Math.cos(phi) * radius * waveStrength;

          javax.vecmath.Vector3f waveImpulse = new javax.vecmath.Vector3f(forceX, forceY, forceZ);

          TE.log(
              "OrbitingComets: Applying 3D impulse (%.2f, %.2f, %.2f) to sphere '%s'",
              forceX, forceY, forceZ, sphere.name);
          body.applyCentralImpulse(waveImpulse);
          body.activate(true);
        }
      }
    }
  }
}
