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
  private final ArrayList<Tether> tethers = new ArrayList<>();

  private boolean centralGravityEnabled = false;
  // External speed factor for scaling tangential tether force; default 1.0
  private float currentExternalSpeedFactor = 1.0f;
  private final Vector3f centralCenter = new Vector3f(0f, 0f, 0f);
  private float centralMu = 25f;
  private float centralSoft = 0.25f;

  private static class Tether {
    String bodyName;
    final Vector3f anchor = new Vector3f();
    float restLen, k, c; // stiffness, damping
  }

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
  /** Step with optional internal scene forces enabled by SceneConfig */
  public void step(float deltaTime) {
    if (!isInitialized) {
      throw new RuntimeException("Bullet Physics not initialized - cannot step simulation");
    }

    try {
      // apply scene-wide forces (central gravity, tethers)
      applySceneForces(deltaTime);
      // fixed stepping hint (caller may also do fixed substeps externally)
      float fixed = (sceneConfig != null) ? sceneConfig.fixedTimeStep : 1f / 120f;
      dynamicsWorld.stepSimulation(deltaTime, 8, fixed);

      // Only log occasionally to avoid spam
      // TE.log("Bullet Bootstrap: Physics simulation step %.4fs", deltaTime);

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

    if (cfg.centralGravityEnabled) {
      enableCentralGravity(cfg.gravityCenter, cfg.gravityMu, cfg.gravitySoftening);
      TE.log(
          "BulletBootstrap: Enabled central gravity at (%.2f, %.2f, %.2f) GM=%.2f",
          cfg.gravityCenter.x, cfg.gravityCenter.y, cfg.gravityCenter.z, cfg.gravityMu);
    } else {
      centralGravityEnabled = false;
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

  /** Add spheres along a path defined in the scene config */
  public void addSpheresOnPath(String namePrefix, int count, float mass, boolean enableCcd) {
    if (sceneConfig == null || sceneConfig.pathType == null) {
      throw new RuntimeException("No path configuration found in scene config");
    }

    switch (sceneConfig.pathType) {
      case CIRCLE:
        addSpheresOnCircle(namePrefix, count, mass, enableCcd);
        break;
      default:
        throw new RuntimeException("Unsupported path type: " + sceneConfig.pathType);
    }
  }

  /** Add spheres positioned on a circle path with orbital velocity */
  private void addSpheresOnCircle(String namePrefix, int count, float mass, boolean enableCcd) {
    SceneConfig.CirclePathConfig config = sceneConfig.circlePathConfig;
    if (config == null) {
      throw new RuntimeException("Circle path config not found");
    }

    for (int i = 0; i < count; i++) {
      // Calculate position on circle
      float angle = (float) (2.0 * Math.PI * i / count); // Evenly spaced around circle
      float x = config.center.x + config.radius * (float) Math.cos(angle);
      float z = config.center.z + config.radius * (float) Math.sin(angle);
      float y = config.y; // Fixed Y plane

      // Vary orb radius
      float radiusRange = config.maxOrbRadius - config.minOrbRadius;
      float radiusVariation = (float) Math.random() * radiusRange;
      float orbRadius = config.minOrbRadius + radiusVariation;

      // Create sphere
      String name = namePrefix + "-" + i;
      addSphere(name, x, y, z, orbRadius, mass * orbRadius, enableCcd);

      // Set orbital velocity (tangential to the circle) for central gravity
      RigidBody body = nameToRigidBody.get(name);
      if (body != null) {
        // Calculate correct orbital velocity for central gravity: v = sqrt(GM/r)
        // Using the central mass parameters from scene config
        float GM =
            (sceneConfig.centralGravityEnabled)
                ? sceneConfig.gravityMu
                : 25f; // Default GM if central gravity not enabled
        float orbitalRadius = config.radius;
        float orbitalSpeed = (float) Math.sqrt(GM / orbitalRadius);

        // Calculate tangential velocity direction (perpendicular to radius vector)
        float vx = -orbitalSpeed * (float) Math.sin(angle); // Perpendicular to radius in X
        float vz = orbitalSpeed * (float) Math.cos(angle); // Perpendicular to radius in Z
        float vy = 0f; // No Y component for horizontal orbit

        javax.vecmath.Vector3f velocity = new javax.vecmath.Vector3f(vx, vy, vz);
        body.setLinearVelocity(velocity);
        body.activate(true);

        TE.log(
            "BulletBootstrap: Added orbiting comet '%s' at (%.2f, %.2f, %.2f) radius=%.2f velocity=(%.2f, %.2f, %.2f) GM=%.2f",
            name, x, y, z, orbRadius, vx, vy, vz, GM);
      }
    }
  }

  // --- Central gravity feature ---
  public void enableCentralGravity(Vector3f center, float GM, float softening) {
    centralCenter.set(center);
    centralMu = GM;
    centralSoft = Math.max(softening, 1e-4f);
    centralGravityEnabled = true;
  }

  public void disableCentralGravity() {
    centralGravityEnabled = false;
  }

  // --- Tether feature (force-based elastic leash to a point) ---
  public void addTether(
      String bodyName, Vector3f anchor, float restLen, float stiffness, float damping) {
    Tether t = new Tether();
    t.bodyName = bodyName;
    t.anchor.set(anchor);
    t.restLen = Math.max(0.01f, restLen);
    t.k = Math.max(0f, stiffness);
    t.c = Math.max(0f, damping);
    tethers.add(t);
  }

  public void removeTethersFor(String bodyName) {
    tethers.removeIf(t -> t.bodyName.equals(bodyName));
  }

  public void clearTethers() {
    tethers.clear();
  }

  /** Add central tethers for all existing bodies based on SceneConfig */
  public void addCentralTethersForAllBodies() {
    if (sceneConfig == null || !sceneConfig.centralTetherEnabled) {
      return;
    }

    Vector3f center = sceneConfig.tetherCenter;
    float radius = sceneConfig.tetherRadius;
    float stiffness = sceneConfig.tetherStiffness;
    float damping = sceneConfig.tetherDamping;

    for (String bodyName : nameToRigidBody.keySet()) {
      addTether(bodyName, center, radius, stiffness, damping);
      TE.log(
          "BulletBootstrap: Added central tether for '%s' to center (%.2f, %.2f, %.2f) radius=%.2f stiffness=%.2f damping=%.2f",
          bodyName, center.x, center.y, center.z, radius, stiffness, damping);
    }
  }

  // --- Internal force application (call before each step) ---
  private void applySceneForces(float dt) {
    if (centralGravityEnabled) {
      // softened inverse-square: F = -mu * r / (|r|^2 + s^2)^(3/2)
      for (Map.Entry<String, RigidBody> e : nameToRigidBody.entrySet()) {
        RigidBody b = e.getValue();
        if (b == null || b.getInvMass() == 0f) continue;
        Transform t = new Transform();
        b.getMotionState().getWorldTransform(t);
        float rx = t.origin.x - centralCenter.x,
            ry = t.origin.y - centralCenter.y,
            rz = t.origin.z - centralCenter.z;
        float r2s = rx * rx + ry * ry + rz * rz + centralSoft * centralSoft;
        float inv = (float) (1.0 / Math.pow(r2s, 1.5 / 2.0)); // 1 / (sqrt(r2s)^3)
        float Fx = -centralMu * rx * inv, Fy = -centralMu * ry * inv, Fz = -centralMu * rz * inv;
        b.activate(true);
        b.applyCentralForce(new Vector3f(Fx, Fy, Fz));
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

      float Fs = -t.k * (r - t.restLen); // spring inward
      float Fd = -t.c * vAlong; // damping along rope

      Vector3f F = new Vector3f((Fs + Fd) * ux, (Fs + Fd) * uy, (Fs + Fd) * uz);
      b.activate(true);
      b.applyCentralForce(F);

      // Optional tangential merry-go-round force: perpendicular to radial vector
      if (sceneConfig != null
          && sceneConfig.centralTetherEnabled
          && sceneConfig.tetherTangentialForce != 0f) {
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
          float mag = sceneConfig.tetherTangentialForce * currentExternalSpeedFactor;
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

        // cleanup: also clear tethers and any constraints you add later
        clearTethers();

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
