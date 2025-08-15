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

  // GPU-optimized SSBO for instanced rendering
  private static final int INSTANCE_DATA_SIZE = 16 * 4 + 16 * 4 + 4 + 4; // 2 mat4 + radius + padding = 136 bytes
  private java.nio.ByteBuffer instanceDataBuffer = Buffers.newDirectByteBuffer(MAX_SPHERES * INSTANCE_DATA_SIZE);
  private int instanceSSBO = -1;
  private boolean ssboInitialized = false;
  
  // Sphere mesh for instanced rendering
  private int sphereVAO = -1;
  private int sphereVBO = -1;
  private int sphereEBO = -1;
  private int sphereIndexCount = 0;
  private boolean sphereMeshInitialized = false;
  
  // Legacy texture buffers (will be removed)
  private java.nio.FloatBuffer physicsPositionBuffer =
      Buffers.newDirectFloatBuffer(MAX_SPHERES * 3); // x,y,z
  private java.nio.FloatBuffer physicsSizeBuffer = Buffers.newDirectFloatBuffer(MAX_SPHERES);

  // GPU-optimized physics parameters with interpolation data
  private static class ActiveSphere {
    String name;
    float radius;
    float ageSeconds;
    
    // Transform interpolation data for smooth rendering
    javax.vecmath.Vector3f prevPosition = new javax.vecmath.Vector3f();
    javax.vecmath.Vector3f currPosition = new javax.vecmath.Vector3f();
    javax.vecmath.Quat4f prevRotation = new javax.vecmath.Quat4f(0, 0, 0, 1);
    javax.vecmath.Quat4f currRotation = new javax.vecmath.Quat4f(0, 0, 0, 1);

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
  
  // Fixed-step physics timing for smooth interpolation
  private static final float PHYSICS_TIMESTEP = 1.0f / 120.0f; // 120 Hz fixed timestep
  private float physicsAccumulator = 0f;
  private long lastPhysicsTime = System.nanoTime();
  private float interpolationAlpha = 0f;

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

    // Render spheres using optimized texture-based fragment shader
    this.renderShader =
        addShader(
            GLShader.config(lx)
                .withFilename("fireflies_physics.fs")
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
      // Walls at x,y 0.5-9.5 in world -> 0.05-0.95 normalized (slightly inset scene)
      float roomMinX = 0.5f * WORLD_TO_NORMALIZED_SCALE;
      float roomMaxX = 9.5f * WORLD_TO_NORMALIZED_SCALE;
      float roomMinY = 0.5f * WORLD_TO_NORMALIZED_SCALE;
      float roomMaxY = 9.5f * WORLD_TO_NORMALIZED_SCALE;
      shader.setUniform("roomMinX", roomMinX);
      shader.setUniform("roomMinY", roomMinY);
      shader.setUniform("roomMaxX", roomMaxX);
      shader.setUniform("roomMaxY", roomMaxY);
      
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
      
      // For now, spheres don't rotate meaningfully, but we'll track it for completeness
      // sphere.currRotation could be updated from RigidBody rotation if needed
    }
  }
  
  // Legacy method - will be replaced by SSBO approach
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
      
      // Fixed-step physics with interpolation
      long currentTime = System.nanoTime();
      float frameTime = (currentTime - lastPhysicsTime) / 1_000_000_000.0f * speed;
      lastPhysicsTime = currentTime;
      
      // Accumulate time and run fixed timesteps
      physicsAccumulator += Math.min(frameTime, 0.25f); // Cap max frame time
      
      // Store previous positions before stepping
      for (ActiveSphere sphere : activeSpheres) {
        sphere.prevPosition.set(sphere.currPosition);
        sphere.prevRotation.set(sphere.currRotation);
      }
      
      // Run fixed timesteps
      while (physicsAccumulator >= PHYSICS_TIMESTEP) {
        bulletPhysics.step(PHYSICS_TIMESTEP);
        physicsAccumulator -= PHYSICS_TIMESTEP;
        
        // Update current positions after each physics step
        updateCurrentTransforms();
      }
      
      // Calculate interpolation alpha for smooth rendering
      interpolationAlpha = physicsAccumulator / PHYSICS_TIMESTEP;

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
          
          // Initialize sphere with current position
          ActiveSphere sphere = new ActiveSphere(name, ballRadius);
          sphere.currPosition.set(x, y, z);
          sphere.prevPosition.set(x, y, z); // Start with no movement
          activeSpheres.add(sphere);
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

  // Upload physics data to GPU textures (manual texture management for compatibility)
  private void uploadPhysicsTextures(GLShader shader) {
    GL4 gl4 = shader.getGL4();
    
    // Create position texture (RGB32F for x,y,z positions)
    ensurePosTexture(gl4);
    gl4.glActiveTexture(GL4.GL_TEXTURE0 + 4); // Use texture unit 4
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, posTexHandle);
    gl4.glTexSubImage2D(GL4.GL_TEXTURE_2D, 0, 0, 0, activeSpheres.size(), 1, 
                        GL4.GL_RGB, GL4.GL_FLOAT, physicsPositionBuffer);
    shader.setUniform("physicsPosTex", 4);
    
    // Create size texture (R32F for radius)
    ensureSizeTexture(gl4);
    gl4.glActiveTexture(GL4.GL_TEXTURE0 + 5); // Use texture unit 5  
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, sizeTexHandle);
    gl4.glTexSubImage2D(GL4.GL_TEXTURE_2D, 0, 0, 0, activeSpheres.size(), 1,
                        GL4.GL_RED, GL4.GL_FLOAT, physicsSizeBuffer);
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
    gl4.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_RGB32F, MAX_SPHERES, 1, 0,
                     GL4.GL_RGB, GL4.GL_FLOAT, null);
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
    gl4.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_R32F, MAX_SPHERES, 1, 0,
                     GL4.GL_RED, GL4.GL_FLOAT, null);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
    sizeTexInitialized = true;
  }
  
  // Initialize SSBO for instanced rendering
  private void ensureInstanceSSBO(GL4 gl4) {
    if (ssboInitialized) return;
    
    int[] ssbo = new int[1];
    gl4.glGenBuffers(1, ssbo, 0);
    instanceSSBO = ssbo[0];
    
    // Bind and allocate buffer (using glBufferData for compatibility)
    gl4.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, instanceSSBO);
    gl4.glBufferData(
        GL4.GL_SHADER_STORAGE_BUFFER,
        MAX_SPHERES * INSTANCE_DATA_SIZE,
        null, // Initial data (null for now)
        GL4.GL_DYNAMIC_DRAW // Use dynamic draw for regular updates
    );
    
    // Bind to binding point 0 (matches shader layout)
    gl4.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 0, instanceSSBO);
    gl4.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, 0);
    
    ssboInitialized = true;
  }
  
  // Update SSBO with interpolated instance data
  private void updateInstanceSSBO(GL4 gl4, float alpha) {
    if (!ssboInitialized) return;
    
    instanceDataBuffer.clear();
    
    for (int i = 0; i < activeSpheres.size(); i++) {
      ActiveSphere sphere = activeSpheres.get(i);
      
      // Interpolated position
      float x = sphere.prevPosition.x + alpha * (sphere.currPosition.x - sphere.prevPosition.x);
      float y = sphere.prevPosition.y + alpha * (sphere.currPosition.y - sphere.prevPosition.y);
      float z = sphere.prevPosition.z + alpha * (sphere.currPosition.z - sphere.prevPosition.z);
      
      // Create interpolated transform matrix (simple translation for spheres)
      // Previous transform matrix (4x4)
      writeMatrix4f(instanceDataBuffer, 
          1.0f, 0.0f, 0.0f, sphere.prevPosition.x,
          0.0f, 1.0f, 0.0f, sphere.prevPosition.y,
          0.0f, 0.0f, 1.0f, sphere.prevPosition.z,
          0.0f, 0.0f, 0.0f, 1.0f);
      
      // Current transform matrix (4x4)
      writeMatrix4f(instanceDataBuffer,
          1.0f, 0.0f, 0.0f, sphere.currPosition.x,
          0.0f, 1.0f, 0.0f, sphere.currPosition.y,
          0.0f, 0.0f, 1.0f, sphere.currPosition.z,
          0.0f, 0.0f, 0.0f, 1.0f);
      
      // Radius
      instanceDataBuffer.putFloat(sphere.radius);
      
      // Padding to align to 16 bytes
      instanceDataBuffer.putFloat(0.0f);
    }
    
    instanceDataBuffer.flip();
    
    // Upload to GPU
    gl4.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, instanceSSBO);
    gl4.glBufferSubData(GL4.GL_SHADER_STORAGE_BUFFER, 0, instanceDataBuffer.remaining(), instanceDataBuffer);
    gl4.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 0, instanceSSBO);
  }
  
  // Helper to write a 4x4 matrix to ByteBuffer
  private void writeMatrix4f(java.nio.ByteBuffer buffer, 
      float m00, float m01, float m02, float m03,
      float m10, float m11, float m12, float m13,
      float m20, float m21, float m22, float m23,
      float m30, float m31, float m32, float m33) {
    // OpenGL uses column-major order
    buffer.putFloat(m00); buffer.putFloat(m10); buffer.putFloat(m20); buffer.putFloat(m30);
    buffer.putFloat(m01); buffer.putFloat(m11); buffer.putFloat(m21); buffer.putFloat(m31);
    buffer.putFloat(m02); buffer.putFloat(m12); buffer.putFloat(m22); buffer.putFloat(m32);
    buffer.putFloat(m03); buffer.putFloat(m13); buffer.putFloat(m23); buffer.putFloat(m33);
  }
  
  // Create sphere mesh for instanced rendering
  private void ensureSphereMesh(GL4 gl4) {
    if (sphereMeshInitialized) return;
    
    // Generate low-poly sphere (icosphere with 2 subdivisions)
    int subdivisions = 2;
    SphereData sphereData = generateIcosphere(subdivisions);
    
    // Create VAO
    int[] vao = new int[1];
    gl4.glGenVertexArrays(1, vao, 0);
    sphereVAO = vao[0];
    gl4.glBindVertexArray(sphereVAO);
    
    // Create VBO for vertices
    int[] vbo = new int[1];
    gl4.glGenBuffers(1, vbo, 0);
    sphereVBO = vbo[0];
    gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, sphereVBO);
    gl4.glBufferData(GL4.GL_ARRAY_BUFFER, sphereData.vertices.length * 4, 
                     java.nio.FloatBuffer.wrap(sphereData.vertices), GL4.GL_STATIC_DRAW);
    
    // Position attribute (location 0)
    gl4.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 6 * 4, 0);
    gl4.glEnableVertexAttribArray(0);
    
    // Normal attribute (location 1)
    gl4.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 6 * 4, 3 * 4);
    gl4.glEnableVertexAttribArray(1);
    
    // Create EBO for indices
    int[] ebo = new int[1];
    gl4.glGenBuffers(1, ebo, 0);
    sphereEBO = ebo[0];
    gl4.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, sphereEBO);
    gl4.glBufferData(GL4.GL_ELEMENT_ARRAY_BUFFER, sphereData.indices.length * 4,
                     java.nio.IntBuffer.wrap(sphereData.indices), GL4.GL_STATIC_DRAW);
    
    sphereIndexCount = sphereData.indices.length;
    sphereMeshInitialized = true;
    
    gl4.glBindVertexArray(0);
  }
  
  // Simple sphere data structure
  private static class SphereData {
    float[] vertices; // [x,y,z, nx,ny,nz, x,y,z, nx,ny,nz, ...]
    int[] indices;
  }
  
  // Generate icosphere mesh (efficient for low-poly spheres)
  private SphereData generateIcosphere(int subdivisions) {
    // For simplicity, generate a UV sphere instead (good enough for particles)
    int rings = 8;
    int sectors = 16;
    
    java.util.List<Float> vertices = new java.util.ArrayList<>();
    java.util.List<Integer> indices = new java.util.ArrayList<>();
    
    // Generate vertices
    for (int r = 0; r <= rings; r++) {
      float phi = (float) Math.PI * r / rings;
      for (int s = 0; s <= sectors; s++) {
        float theta = 2.0f * (float) Math.PI * s / sectors;
        
        float x = (float) (Math.sin(phi) * Math.cos(theta));
        float y = (float) Math.cos(phi);
        float z = (float) (Math.sin(phi) * Math.sin(theta));
        
        // Position
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        
        // Normal (same as position for unit sphere)
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
      }
    }
    
    // Generate indices
    for (int r = 0; r < rings; r++) {
      for (int s = 0; s < sectors; s++) {
        int current = r * (sectors + 1) + s;
        int next = current + sectors + 1;
        
        // First triangle
        indices.add(current);
        indices.add(next);
        indices.add(current + 1);
        
        // Second triangle
        indices.add(current + 1);
        indices.add(next);
        indices.add(next + 1);
      }
    }
    
    SphereData data = new SphereData();
    data.vertices = new float[vertices.size()];
    data.indices = new int[indices.size()];
    
    for (int i = 0; i < vertices.size(); i++) {
      data.vertices[i] = vertices.get(i);
    }
    for (int i = 0; i < indices.size(); i++) {
      data.indices[i] = indices.get(i);
    }
    
    return data;
  }
  
  // Helper method to create orthographic projection matrix
  private float[] createOrthographicMatrix(float left, float right, float bottom, float top, float near, float far) {
    float[] matrix = new float[16];
    matrix[0] = 2.0f / (right - left);
    matrix[1] = 0.0f;
    matrix[2] = 0.0f;
    matrix[3] = 0.0f;
    
    matrix[4] = 0.0f;
    matrix[5] = 2.0f / (top - bottom);
    matrix[6] = 0.0f;
    matrix[7] = 0.0f;
    
    matrix[8] = 0.0f;
    matrix[9] = 0.0f;
    matrix[10] = -2.0f / (far - near);
    matrix[11] = 0.0f;
    
    matrix[12] = -(right + left) / (right - left);
    matrix[13] = -(top + bottom) / (top - bottom);
    matrix[14] = -(far + near) / (far - near);
    matrix[15] = 1.0f;
    
    return matrix;
  }
  
  // Helper method to create identity matrix
  private float[] createIdentityMatrix() {
    return new float[] {
      1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 1.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 1.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 1.0f
    };
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
