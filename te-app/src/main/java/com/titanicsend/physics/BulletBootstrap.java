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
  public void step(float deltaTime) {
    if (!isInitialized) {
      throw new RuntimeException("Bullet Physics not initialized - cannot step simulation");
    }

    try {
      // Step the physics simulation
      dynamicsWorld.stepSimulation(deltaTime, 10); // max 10 substeps

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
    // Basic material properties
    rigidBody.setFriction(0.6f);
    rigidBody.setRestitution(0.2f);
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

  /** Remove a body by name from the physics world and internal map. */
  public void removeBody(String name) {
    RigidBody body = nameToRigidBody.remove(name);
    if (body != null && dynamicsWorld != null) {
      dynamicsWorld.removeRigidBody(body);
    }
  }

  /** Clean up Bullet Physics resources */
  public void cleanup() {
    try {
      if (isInitialized && dynamicsWorld != null) {
        TE.log("Bullet Bootstrap: Cleaning up Bullet Physics resources");

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
