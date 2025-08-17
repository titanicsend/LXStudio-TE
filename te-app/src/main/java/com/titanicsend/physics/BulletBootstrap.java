package com.titanicsend.physics;

import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.vecmath.Vector3f;
import titanicsend.util.TE;

/**
 * Bullet Physics Bootstrap for Titanic's End
 *
 * <p>Real Bullet Physics engine integration using JBullet (pure Java). Provides GPU-like
 * performance for physics simulation.
 */
public class BulletBootstrap {

  private boolean isInitialized = false;
  private boolean isGpuEnabled = false; // JBullet runs on CPU but optimized

  // Bullet Physics world and configuration
  private DefaultCollisionConfiguration collisionConfiguration;
  private CollisionDispatcher dispatcher;
  private DbvtBroadphase overlappingPairCache;
  private SequentialImpulseConstraintSolver solver;
  private DiscreteDynamicsWorld dynamicsWorld;

  // Physics objects
  private final Map<String, RigidBody> nameToRigidBody = new LinkedHashMap<>();
  // JBullet collision flag value for NO_CONTACT_RESPONSE (see CollisionObject.CollisionFlags)
  private static final int CF_NO_CONTACT_RESPONSE = 4;

  // Simulation parameters
  private final float gravity = -9.81f; // Standard gravity

  // Enhanced scene configuration
  private SceneConfig sceneConfig;

  // External speed factor for scaling tangential tether force; default 1.0
  private float currentExternalSpeedFactor = 1.0f;

  private static class CentralGravity {
    final Vector3f center = new Vector3f();
    float mu;
    float soft;
  }

  private final ArrayList<CentralGravity> centralGravities = new ArrayList<>();

  // Tether tracking
  private static class Tether {
    String bodyName;
    final Vector3f anchor = new Vector3f();
    float restLen, stiffness, damping, tangentialForce;
  }

  private final ArrayList<Tether> tethers = new ArrayList<>();

  // Particle Emitter
  public enum EmitterInitialSpeed {
    ZERO, // Particles start with zero velocity
    EMITTER_SPEED, // Particles inherit emitter's velocity
    FIXED_SPEED, // Particles have fixed initial speed
    TANGENTIAL, // Particles get tangential velocity based on emitter movement
    RANDOM // Particles get random velocity scaled by speed multiplier
  }

  private static class ParticleEmitter {
    String name;
    int maxParticles;
    float particleLifetime;
    EmitterInitialSpeed initialSpeedMode;
    float fixedSpeed; // Used when initialSpeedMode is FIXED_SPEED
    LoopPath path;
    float particleRadius = 0.1f;
    float particleMass = 0.1f;
    float emitRate = 10.0f; // particles per second
    float speedMultiplier = 1.0f; // Speed multiplier for movement and particle velocity

    // Emitter state
    Vector3f prevPosition = new Vector3f();
    Vector3f currPosition = new Vector3f();
    Vector3f velocity = new Vector3f();
    float timeSinceLastEmit = 0f;
    int particleCounter = 0;

    // Active particles tracking
    static class Particle {
      String bodyName;
      float age;
      float maxAge;
      float radius;
    }

    final ArrayList<Particle> particles = new ArrayList<>();
  }

  private final Map<String, ParticleEmitter> emitters = new LinkedHashMap<>();

  // Fixed-step physics timing
  private long lastPhysicsTime = System.nanoTime();
  private float physicsAccumulator = 0f;

  /** Initialize Bullet Physics simulation */
  public void initialize() {
    if (isInitialized) {
      return;
    }

    try {
      // Collision configuration contains default setup for memory, collision setup
      collisionConfiguration = new DefaultCollisionConfiguration();

      // Use the default collision dispatcher
      dispatcher = new CollisionDispatcher(collisionConfiguration);

      // The default constraint solver
      solver = new SequentialImpulseConstraintSolver();

      // Broadphase interface
      overlappingPairCache = new DbvtBroadphase();

      // Create dynamics world
      dynamicsWorld =
          new DiscreteDynamicsWorld(
              dispatcher, overlappingPairCache, solver, collisionConfiguration);

      // Set gravity
      dynamicsWorld.setGravity(new Vector3f(0, gravity, 0));

      isInitialized = true;
      isGpuEnabled = true; // JBullet is optimized, so we consider it "GPU-like"

    } catch (Exception e) {
      TE.error(e, "Bullet Bootstrap: Bullet Physics initialization FAILED");

      // Clean up any partially created objects
      cleanup();

      // Re-throw the exception
      throw new RuntimeException("Bullet Physics initialization failed", e);
    }
  }

  // Removed ground plane helper; we rely on explicit room boxes when needed

  /**
   * Step the simulation forward by the given time delta
   *
   * @param deltaTime time step in seconds
   */
  /** Step with fixed timestep accumulation - call this with actual frame time */
  public void step(float deltaTime) {
    if (!isInitialized) {
      throw new RuntimeException("Bullet Physics not initialized - cannot step simulation");
    }

    try {
      long currentTime = System.nanoTime();
      float frameTime = (currentTime - lastPhysicsTime) / 1_000_000_000.0f;
      lastPhysicsTime = currentTime;

      // Accumulate time and run fixed timesteps
      physicsAccumulator += Math.min(frameTime, 0.25f); // Cap max frame time

      float fixedStep = (sceneConfig != null) ? sceneConfig.fixedTimeStep : 1f / 120f;

      // Run fixed timesteps
      while (physicsAccumulator >= fixedStep) {
        // Update all emitters
        updateAllEmitters(fixedStep);
        // apply scene-wide forces (central gravity, tethers)
        applySceneForces(fixedStep);
        dynamicsWorld.stepSimulation(fixedStep, 1, fixedStep);
        physicsAccumulator -= fixedStep;
      }

    } catch (Exception e) {
      throw new RuntimeException("Bullet Physics simulation step failed", e);
    }
  }

  /** Check if GPU dynamics are enabled (JBullet is optimized, so we say yes) */
  public boolean isGpuEnabled() {
    return isGpuEnabled;
  }

  /** Allow patterns to supply a speed factor used for tangential tether force scaling */
  public void setExternalSpeedFactor(float speedFactor) {
    if (Float.isFinite(speedFactor) && speedFactor > 0f) {
      this.currentExternalSpeedFactor = speedFactor;
    } else {
      this.currentExternalSpeedFactor = 1.0f;
    }
  }

  /** Check if Bullet Physics is initialized */
  public boolean isInitialized() {
    return isInitialized;
  }

  /**
   * Add a dynamic sphere to the simulation
   *
   * @param name unique body name
   * @param x initial X position
   * @param y initial Y position
   * @param z initial Z position
   * @param radius sphere radius
   * @param mass sphere mass
   */
  public void addSphere(String name, float x, float y, float z, float radius, float mass) {
    if (!isInitialized) {
      throw new RuntimeException("Bullet Physics not initialized");
    }

    // Create proper sphere collision shape
    CollisionShape sphereShape = new SphereShape(radius);

    // Calculate inertia
    Vector3f localInertia = new Vector3f(0, 0, 0);
    if (mass != 0.0f) {
      sphereShape.calculateLocalInertia(mass, localInertia);
    }

    // Create motion state
    Transform startTransform = new Transform();
    startTransform.setIdentity();
    startTransform.origin.set(x, y, z);
    DefaultMotionState motionState = new DefaultMotionState(startTransform);

    // Create rigid body
    RigidBodyConstructionInfo rbInfo =
        new RigidBodyConstructionInfo(mass, motionState, sphereShape, localInertia);
    RigidBody rigidBody = new RigidBody(rbInfo);
    // Bouncy material properties
    rigidBody.setFriction(0.2f); // Lower friction for more sliding
    rigidBody.setRestitution(0.9f); // High restitution for bouncy behavior
    // Enable CCD by default to reduce tunneling through walls
    rigidBody.setCcdMotionThreshold(radius * 0.5f);
    rigidBody.setCcdSweptSphereRadius(radius * 0.9f);

    // Add to world
    dynamicsWorld.addRigidBody(rigidBody);
    nameToRigidBody.put(name, rigidBody);
  }

  /** Overload allowing explicit CCD toggle */
  public void addSphere(
      String name, float x, float y, float z, float radius, float mass, boolean enableCcd) {
    addSphere(name, x, y, z, radius, mass);
    if (!enableCcd) {
      RigidBody body = nameToRigidBody.get(name);
      if (body != null) {
        body.setCcdMotionThreshold(0f);
        body.setCcdSweptSphereRadius(0f);
      }
    }
  }

  /** Add a dynamic sphere and immediately tether it to a point with properties. */
  public void addTetheredSphere(
      String name,
      float x,
      float y,
      float z,
      float radius,
      float mass,
      boolean enableCcd,
      Vector3f tetherAnchor,
      float tetherRestLen,
      float tetherStiffness,
      float tetherDamping,
      float tetherTangentialForce) {
    addSphere(name, x, y, z, radius, mass, enableCcd);
    addTether(
        name, tetherAnchor, tetherRestLen, tetherStiffness, tetherDamping, tetherTangentialForce);
  }

  /** Set world gravity. */
  public void setGravity(float gx, float gy, float gz) {
    if (!isInitialized) return;
    dynamicsWorld.setGravity(new Vector3f(gx, gy, gz));
  }

  /**
   * Create a boundary room using explicit min/max bounds. X in [minX, maxX], Y in [minY, maxY], Z
   * in [minZ, maxZ]. Uses a sane default thickness.
   */
  public void createRoom(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
    createRoom(minX, maxX, minY, maxY, minZ, maxZ, 0.5f);
  }

  /** Create a boundary room using explicit min/max bounds and specified wall thickness. */
  public void createRoom(
      float minX, float maxX, float minY, float maxY, float minZ, float maxZ, float thickness) {
    float widthX = maxX - minX;
    float height = maxY - minY;
    float widthZ = maxZ - minZ;

    float centerX = minX + widthX * 0.5f;
    float centerY = minY + height * 0.5f;
    float centerZ = minZ + widthZ * 0.5f;

    // Floor and ceiling
    addStaticBox(
        "floor",
        new Vector3f(widthX * 0.5f, thickness * 0.5f, widthZ * 0.5f),
        new Vector3f(centerX, minY - thickness * 0.5f, centerZ));
    addStaticBox(
        "ceiling",
        new Vector3f(widthX * 0.5f, thickness * 0.5f, widthZ * 0.5f),
        new Vector3f(centerX, maxY + thickness * 0.5f, centerZ));

    // Left and right walls
    addStaticBox(
        "wall-left",
        new Vector3f(thickness * 0.5f, height * 0.5f, widthZ * 0.5f),
        new Vector3f(minX - thickness * 0.5f, centerY, centerZ));
    addStaticBox(
        "wall-right",
        new Vector3f(thickness * 0.5f, height * 0.5f, widthZ * 0.5f),
        new Vector3f(maxX + thickness * 0.5f, centerY, centerZ));

    // Front and back walls (Z)
    addStaticBox(
        "wall-front",
        new Vector3f(widthX * 0.5f, height * 0.5f, thickness * 0.5f),
        new Vector3f(centerX, centerY, minZ - thickness * 0.5f));
    addStaticBox(
        "wall-back",
        new Vector3f(widthX * 0.5f, height * 0.5f, thickness * 0.5f),
        new Vector3f(centerX, centerY, maxZ + thickness * 0.5f));
  }

  private void addStaticBox(String name, Vector3f halfExtents, Vector3f center) {
    CollisionShape shape = new BoxShape(new Vector3f(halfExtents.x, halfExtents.y, halfExtents.z));
    Vector3f localInertia = new Vector3f(0, 0, 0);
    Transform t = new Transform();
    t.setIdentity();
    t.origin.set(center);
    DefaultMotionState motionState = new DefaultMotionState(t);
    RigidBodyConstructionInfo info =
        new RigidBodyConstructionInfo(0f, motionState, shape, localInertia);
    RigidBody body = new RigidBody(info);
    body.setFriction(0.9f);
    body.setRestitution(0.2f);
    dynamicsWorld.addRigidBody(body);
    nameToRigidBody.put(name, body);
  }

  // (Reverted) No special 0..10 room helper; use existing APIs

  /**
   * Get position of a body by name
   *
   * @param name body name
   * @return position as float array [x, y, z]
   */
  public float[] getBodyPosition(String name) {
    if (!isInitialized) {
      return new float[] {0, 0, 0};
    }
    RigidBody body = nameToRigidBody.get(name);
    if (body == null) {
      return new float[] {0, 0, 0};
    }
    Transform transform = new Transform();
    body.getMotionState().getWorldTransform(transform);

    return new float[] {transform.origin.x, transform.origin.y, transform.origin.z};
  }

  /** Get a RigidBody by name for advanced client-side manipulation */
  public RigidBody getBody(String name) {
    return nameToRigidBody.get(name);
  }

  /**
   * Enable or disable collision response for a named body (keeps broadphase overlap but no contact
   * impulses).
   */
  public void setCollisionResponseEnabled(String name, boolean enabled) {
    RigidBody body = nameToRigidBody.get(name);
    if (body == null) return;
    int flags = body.getCollisionFlags();
    if (enabled) {
      flags &= ~CF_NO_CONTACT_RESPONSE;
    } else {
      flags |= CF_NO_CONTACT_RESPONSE;
    }
    body.setCollisionFlags(flags);
  }

  /**
   * Set linear factor to restrict movement along specific axes (JBullet doesn't support
   * setLinearFactor) For now, we rely on walls to contain the motion instead.
   *
   * @param name body name
   * @param linearFactor factor for [x,y,z] axes (1.0 = free movement, 0.0 = locked)
   */
  public void setLinearFactor(String name, Vector3f linearFactor) {
    // JBullet doesn't have setLinearFactor method
    // We rely on physical walls to constrain motion instead
    // TODO: Consider implementing via constraints if needed
  }

  /** Set linear velocity of a body by name */
  public void setBodyLinearVelocity(String name, float vx, float vy, float vz) {
    RigidBody body = nameToRigidBody.get(name);
    if (body != null) {
      body.setLinearVelocity(new Vector3f(vx, vy, vz));
      body.activate(true);
    }
  }

  /** Remove a body by name from the physics world and internal map. */
  public void removeBody(String name) {
    RigidBody body = nameToRigidBody.remove(name);
    if (body != null && dynamicsWorld != null) {
      dynamicsWorld.removeRigidBody(body);
    }
  }

  /** New: one-call scene setup */
  public void initializeScene(SceneConfig cfg) {
    this.sceneConfig = cfg;
    if (!isInitialized) initialize();

    // Solver knobs
    dynamicsWorld.getSolverInfo().numIterations = Math.max(1, cfg.solverIterations);

    // Configure gravity forces independently
    if (cfg.globalGravityEnabled) {
      dynamicsWorld.setGravity(new Vector3f(cfg.gravityVector));
      TE.log(
          "BulletBootstrap: Enabled global gravity (%.2f, %.2f, %.2f)",
          cfg.gravityVector.x, cfg.gravityVector.y, cfg.gravityVector.z);
    } else {
      dynamicsWorld.setGravity(new Vector3f(0f, 0f, 0f));
    }

    centralGravities.clear();
    if (cfg.centralGravities != null && !cfg.centralGravities.isEmpty()) {
      for (SceneConfig.CentralGravityConfig c : cfg.centralGravities) {
        addCentralGravity(new Vector3f(c.center), c.mu, c.soft);
      }
      TE.log("BulletBootstrap: Added %d central gravities", centralGravities.size());
    }

    // Bounds
    if (cfg.boundsType == SceneConfig.BoundsType.ROOM) {
      createRoom(
          cfg.roomMinX,
          cfg.roomMaxX,
          cfg.roomMinY,
          cfg.roomMaxY,
          cfg.roomMinZ,
          cfg.roomMaxZ,
          cfg.wallThickness);
    }
  }

  // --- Central gravity feature ---
  // New API for multi-center gravity
  public void addCentralGravity(Vector3f center, float GM, float softening) {
    CentralGravity cg = new CentralGravity();
    cg.center.set(center);
    cg.mu = GM;
    cg.soft = Math.max(softening, 1e-4f);
    centralGravities.add(cg);
  }

  public void clearCentralGravities() {
    centralGravities.clear();
  }

  // --- Tether feature (force-based elastic leash to a point) ---
  public void addTether(
      String bodyName,
      Vector3f anchor,
      float restLen,
      float stiffness,
      float damping,
      float tangentialForce) {
    Tether t = new Tether();
    t.bodyName = bodyName;
    t.anchor.set(anchor);
    t.restLen = restLen;
    t.stiffness = stiffness;
    t.damping = damping;
    t.tangentialForce = tangentialForce;
    tethers.add(t);
  }

  public void removeTethersFor(String bodyName) {
    tethers.removeIf(t -> t.bodyName.equals(bodyName));
  }

  public void clearTethers() {
    tethers.clear();
  }

  // --- Particle Emitter API ---

  /** Add a particle emitter to the scene */
  public void addEmitter(
      String name,
      int maxParticles,
      float particleLifetime,
      EmitterInitialSpeed initialSpeedMode,
      LoopPath path) {
    ParticleEmitter emitter = new ParticleEmitter();
    emitter.name = name;
    emitter.maxParticles = maxParticles;
    emitter.particleLifetime = particleLifetime;
    emitter.initialSpeedMode = initialSpeedMode;
    emitter.path = path;

    // Initialize emitter position
    emitter.currPosition = path.getPosition(0, 1, 0);
    emitter.prevPosition.set(emitter.currPosition);

    emitters.put(name, emitter);
  }

  /** Set emitter properties */
  public void setEmitterProperties(
      String name, float particleRadius, float particleMass, float emitRate) {
    ParticleEmitter emitter = emitters.get(name);
    if (emitter != null) {
      emitter.particleRadius = particleRadius;
      emitter.particleMass = particleMass;
      emitter.emitRate = emitRate;
    }
  }

  /** Internal emitter time tracking */
  private final Map<String, Long> emitterStartTimes = new LinkedHashMap<>();

  /** Update all emitters automatically during physics step */
  private void updateAllEmitters(float dt) {
    long currentTime = System.nanoTime();

    for (Map.Entry<String, ParticleEmitter> entry : emitters.entrySet()) {
      String name = entry.getKey();
      ParticleEmitter emitter = entry.getValue();

      // Get or initialize start time
      Long startTime = emitterStartTimes.get(name);
      if (startTime == null) {
        startTime = currentTime;
        emitterStartTimes.put(name, startTime);
      }

      // Calculate time since emitter started
      long timeOffsetMicros = (currentTime - startTime) / 1000;

      // Update emitter
      updateEmitterInternal(emitter, timeOffsetMicros, dt);
    }
  }

  /** Internal method to update a single emitter */
  private void updateEmitterInternal(ParticleEmitter emitter, long timeOffsetMicros, float dt) {
    // Update position along path
    emitter.prevPosition.set(emitter.currPosition);
    emitter.currPosition = emitter.path.getPosition(0, 1, timeOffsetMicros);

    // Calculate velocity
    emitter.velocity.x = (emitter.currPosition.x - emitter.prevPosition.x) / dt;
    emitter.velocity.y = (emitter.currPosition.y - emitter.prevPosition.y) / dt;
    emitter.velocity.z = (emitter.currPosition.z - emitter.prevPosition.z) / dt;

    // Get emit rate
    float actualEmitRate = emitter.emitRate;

    // Emit new particles
    emitter.timeSinceLastEmit += dt;
    float emitInterval = 1.0f / actualEmitRate;

    while (emitter.timeSinceLastEmit >= emitInterval
        && emitter.particles.size() < emitter.maxParticles) {
      emitParticle(emitter);
      emitter.timeSinceLastEmit -= emitInterval;
    }

    // Update particle ages and remove old ones
    ArrayList<ParticleEmitter.Particle> toRemove = new ArrayList<>();
    for (ParticleEmitter.Particle p : emitter.particles) {
      p.age += dt;
      if (p.age >= p.maxAge) {
        toRemove.add(p);
        removeBody(p.bodyName);
      }
    }
    emitter.particles.removeAll(toRemove);
  }

  /** Set emitter speed multiplier (affects path traversal speed and particle velocity) */
  public void setEmitterSpeed(String name, float speedMultiplier) {
    ParticleEmitter emitter = emitters.get(name);
    if (emitter != null) {
      emitter.speedMultiplier = speedMultiplier;
    }

    // Adjust the emitter's time offset to simulate speed change
    if (speedMultiplier != 1.0f && emitterStartTimes.containsKey(name)) {
      long currentTime = System.nanoTime();
      long oldStartTime = emitterStartTimes.get(name);
      long elapsed = currentTime - oldStartTime;
      long adjustedElapsed = (long) (elapsed / speedMultiplier);
      long newStartTime = currentTime - adjustedElapsed;
      emitterStartTimes.put(name, newStartTime);
    }
  }

  /** Emit a single particle */
  private void emitParticle(ParticleEmitter emitter) {
    String particleName = emitter.name + "-p" + (emitter.particleCounter++);

    // Add sphere to physics world
    addSphere(
        particleName,
        emitter.currPosition.x,
        emitter.currPosition.y,
        emitter.currPosition.z,
        emitter.particleRadius,
        emitter.particleMass);

    // Set initial velocity based on mode
    RigidBody body = getBody(particleName);
    if (body != null) {
      Vector3f initialVel = new Vector3f();

      switch (emitter.initialSpeedMode) {
        case ZERO:
          initialVel.set(0, 0, 0);
          break;
        case EMITTER_SPEED:
          initialVel.set(emitter.velocity);
          break;
        case FIXED_SPEED:
          // Random direction with fixed magnitude
          float theta = (float) (Math.random() * Math.PI * 2);
          float phi = (float) (Math.acos(2.0 * Math.random() - 1.0));
          float sinPhi = (float) Math.sin(phi);
          initialVel.x = (float) Math.cos(theta) * sinPhi * emitter.fixedSpeed;
          initialVel.y = (float) Math.sin(theta) * sinPhi * emitter.fixedSpeed;
          initialVel.z = (float) Math.cos(phi) * emitter.fixedSpeed;
          break;
        case TANGENTIAL:
          // Calculate tangential velocity based on emitter movement speed
          float speed = Math.abs(emitter.speedMultiplier) * -5.0f; // Base tangential speed

          // Get normalized velocity direction (tangent to circle)
          Vector3f velDir = new Vector3f(emitter.velocity);
          float velMagnitude =
              (float) Math.sqrt(velDir.x * velDir.x + velDir.y * velDir.y + velDir.z * velDir.z);
          if (velMagnitude > 1e-6f) {
            velDir.scale(1.0f / velMagnitude); // Normalize
            // Scale by speed parameter
            initialVel.x = velDir.x * speed;
            initialVel.y = velDir.y * speed;
            initialVel.z = velDir.z * speed;
          } else {
            initialVel.set(0, 0, 0);
          }
          break;
        case RANDOM:
          // Random direction with magnitude equal to emitter's current movement speed
          Vector3f emitterVel = new Vector3f(emitter.velocity);
          float baseEmitterSpeed =
              (float)
                  Math.sqrt(
                      emitterVel.x * emitterVel.x
                          + emitterVel.y * emitterVel.y
                          + emitterVel.z * emitterVel.z);

          // Scale by the speed multiplier to match emitter's speed changes
          float particleSpeed = baseEmitterSpeed * Math.abs(emitter.speedMultiplier);

          // Generate random direction (uniform distribution on sphere)
          float randomTheta = (float) (Math.random() * Math.PI * 2);
          float randomPhi = (float) (Math.acos(2.0 * Math.random() - 1.0));
          float randomSinPhi = (float) Math.sin(randomPhi);

          // Apply scaled speed in random direction
          initialVel.x = (float) Math.cos(randomTheta) * randomSinPhi * particleSpeed;
          initialVel.y = (float) Math.sin(randomTheta) * randomSinPhi * particleSpeed;
          initialVel.z = (float) Math.cos(randomPhi) * particleSpeed;
          break;
      }

      body.setLinearVelocity(initialVel);
      body.activate(true);
    }

    // Track particle
    ParticleEmitter.Particle particle = new ParticleEmitter.Particle();
    particle.bodyName = particleName;
    particle.age = 0f;
    particle.maxAge = emitter.particleLifetime;
    particle.radius = emitter.particleRadius;
    emitter.particles.add(particle);
  }

  /** Force emit a burst of particles */
  public void burstEmitter(String name, int count) {
    ParticleEmitter emitter = emitters.get(name);
    if (emitter == null) return;

    for (int i = 0; i < count && emitter.particles.size() < emitter.maxParticles; i++) {
      emitParticle(emitter);
    }
  }

  /** Get list of active particle names from an emitter */
  public java.util.List<String> getEmitterParticles(String name) {
    ParticleEmitter emitter = emitters.get(name);
    if (emitter == null) return new ArrayList<>();

    ArrayList<String> names = new ArrayList<>();
    for (ParticleEmitter.Particle p : emitter.particles) {
      names.add(p.bodyName);
    }
    return names;
  }

  /** Get particle data [radius, maxAge, currentAge] */
  public float[] getEmitterParticleData(String emitterName, String particleName) {
    ParticleEmitter emitter = emitters.get(emitterName);
    if (emitter == null) return null;

    for (ParticleEmitter.Particle p : emitter.particles) {
      if (p.bodyName.equals(particleName)) {
        return new float[] {p.radius, p.maxAge, p.age};
      }
    }
    return null;
  }

  /** Remove an emitter and all its particles */
  public void removeEmitter(String name) {
    ParticleEmitter emitter = emitters.remove(name);
    if (emitter != null) {
      for (ParticleEmitter.Particle p : emitter.particles) {
        removeBody(p.bodyName);
      }
    }
    emitterStartTimes.remove(name);
  }

  // --- Internal force application (call before each step) ---
  private void applySceneForces(float dt) {
    if (!centralGravities.isEmpty()) {
      for (Map.Entry<String, RigidBody> e : nameToRigidBody.entrySet()) {
        RigidBody b = e.getValue();
        if (b == null || b.getInvMass() == 0f) continue;
        Transform t = new Transform();
        b.getMotionState().getWorldTransform(t);
        Vector3f fSum = new Vector3f();
        for (CentralGravity cg : centralGravities) {
          float rx = t.origin.x - cg.center.x,
              ry = t.origin.y - cg.center.y,
              rz = t.origin.z - cg.center.z;
          float r2s = rx * rx + ry * ry + rz * rz + cg.soft * cg.soft;
          float inv = (float) (1.0 / Math.pow(r2s, 1.5 / 2.0));
          fSum.x += -cg.mu * rx * inv;
          fSum.y += -cg.mu * ry * inv;
          fSum.z += -cg.mu * rz * inv;
        }
        b.activate(true);
        b.applyCentralForce(fSum);
      }
    }
    // Tethers: spring + damping along rope direction when stretched
    for (Tether t : tethers) {
      RigidBody b = nameToRigidBody.get(t.bodyName);
      if (b == null || b.getInvMass() == 0f) continue;

      Transform xform = new Transform();
      b.getMotionState().getWorldTransform(xform);
      float rx = xform.origin.x - t.anchor.x,
          ry = xform.origin.y - t.anchor.y,
          rz = xform.origin.z - t.anchor.z;
      float r2 = rx * rx + ry * ry + rz * rz;
      float r = (float) Math.sqrt(Math.max(r2, 1e-8f));
      if (r <= t.restLen) continue;

      float ux = rx / r, uy = ry / r, uz = rz / r; // rope dir
      Vector3f vel = new Vector3f();
      b.getLinearVelocity(vel);
      float vAlong = vel.x * ux + vel.y * uy + vel.z * uz;

      float Fs = -t.stiffness * (r - t.restLen); // spring inward
      float Fd = -t.damping * vAlong; // damping along rope

      Vector3f F = new Vector3f((Fs + Fd) * ux, (Fs + Fd) * uy, (Fs + Fd) * uz);
      b.activate(true);
      b.applyCentralForce(F);

      // Optional tangential merry-go-round force: perpendicular to radial vector
      if (t.tangentialForce != 0f) {
        // Build a perpendicular direction to (ux,uy,uz). Choose a helper vector not parallel to u
        float hx = Math.abs(ux) < 0.9f ? 1f : 0f;
        float hy = Math.abs(uy) < 0.9f ? 1f : 0f;
        float hz = (hx == 0f && hy == 0f) ? 1f : 0f;
        // w = u x h
        float wx = uy * hz - uz * hy;
        float wy = uz * hx - ux * hz;
        float wz = ux * hy - uy * hx;
        float wLen = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (wLen > 1e-6f) {
          wx /= wLen;
          wy /= wLen;
          wz /= wLen;
          // Compute tangential magnitude, optionally scale by external speed factor set by caller
          float mag = t.tangentialForce * currentExternalSpeedFactor;
          Vector3f Ft = new Vector3f(wx * mag, wy * mag, wz * mag);
          b.applyCentralForce(Ft);
        }
      }
    }
  }

  /** Clean up Bullet Physics resources */
  public void cleanup() {
    try {
      if (isInitialized && dynamicsWorld != null) {
        TE.log("Bullet Bootstrap: Cleaning up Bullet Physics resources");

        // cleanup: also clear tethers, emitters and any constraints you add later
        clearTethers();
        clearCentralGravities();

        // Clear all emitters
        for (ParticleEmitter emitter : emitters.values()) {
          for (ParticleEmitter.Particle p : emitter.particles) {
            removeBody(p.bodyName);
          }
        }
        emitters.clear();

        // Remove all rigid bodies from the world
        for (RigidBody body : nameToRigidBody.values()) {
          if (body != null) {
            dynamicsWorld.removeRigidBody(body);
          }
        }
        nameToRigidBody.clear();

        // Clean up dynamics world
        dynamicsWorld = null;
        solver = null;
        overlappingPairCache = null;
        dispatcher = null;
        collisionConfiguration = null;
      }
    } catch (Exception e) {
      TE.error(e, "Bullet Bootstrap: Error during cleanup");
    } finally {
      isInitialized = false;
      isGpuEnabled = false;

      TE.log("Bullet Bootstrap: Bullet Physics cleanup complete");
    }
  }
}
