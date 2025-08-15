package com.titanicsend.physics;

import javax.vecmath.Vector3f;

public final class SceneConfig {

  public enum BoundsType {
    INFINITE,
    ROOM
  }

  public enum PathType {
    CIRCLE
  }

  public static class CirclePathConfig {
    /**
     * Center point of the circular path in world coordinates (e.g., (5.0, 3.5, 5.0) for room
     * center)
     */
    public Vector3f center = new Vector3f(0f, 0f, 0f);

    /** Radius of the circular path in world units (distance from center to each orb position) */
    public float radius = 3f;

    /** Fixed Y coordinate (height) where all orbs will be positioned on the circle plane */
    public float y = 3.5f;

    /** Minimum radius for individual orbs spawned on the path (in world units) */
    public float minOrbRadius = 0.2f;

    /** Maximum radius for individual orbs spawned on the path (in world units) */
    public float maxOrbRadius = 0.6f;

    public CirclePathConfig(
        Vector3f center, float radius, float y, float minOrbRadius, float maxOrbRadius) {
      this.center.set(center);
      this.radius = radius;
      this.y = y;
      this.minOrbRadius = minOrbRadius;
      this.maxOrbRadius = maxOrbRadius;
    }
  }

  // --- Gravity Forces (can be combined independently) ---
  // Global gravity (applies to all bodies)
  public boolean globalGravityEnabled = false;
  public Vector3f gravityVector = new Vector3f(0f, -9.81f, 0f);

  // Central mass gravity (inverse square law from a point)
  public boolean centralGravityEnabled = false;
  public Vector3f gravityCenter = new Vector3f(0f, 0f, 0f);
  public float gravityMu = 25f; // GM for central mass
  public float gravitySoftening = 0.25f; // avoids singularity at r≈0

  // --- Central Tether ---
  public boolean centralTetherEnabled = false;
  public Vector3f tetherCenter = new Vector3f(0f, 0f, 0f); // Tether anchor point
  public float tetherRadius = 3.0f; // Rest length (desired distance from center)
  public float tetherStiffness = 50.0f; // Spring stiffness (higher = stiffer)
  public float tetherDamping = 0.1f; // Damping factor (0.0 = no damping, 1.0 = critical)

  // --- Bounds ---
  public BoundsType boundsType = BoundsType.INFINITE;
  public float roomMinX = 0f,
      roomMaxX = 10f,
      roomMinY = 0f,
      roomMaxY = 10f,
      roomMinZ = 0f,
      roomMaxZ = 10f,
      wallThickness = 1f;

  // --- Path sampling ---
  public PathType pathType = null;
  public CirclePathConfig circlePathConfig = null;

  // --- Physics solver knobs ---
  public int solverIterations = 10;
  public float fixedTimeStep = 1f / 120f; // engine can use this if desired

  // --- Rendering/interop knobs (placeholder) ---
  // TODO: per-shader config hook (e.g., SSBO handles, uniforms, color maps)
  public Object shaderConfig = null;

  public SceneConfig withGlobalGravity(Vector3f g) {
    this.globalGravityEnabled = true;
    this.gravityVector.set(g);
    return this;
  }

  public SceneConfig withCentralMass(Vector3f center, float GM, float soft) {
    this.centralGravityEnabled = true;
    this.gravityCenter.set(center);
    this.gravityMu = GM;
    this.gravitySoftening = soft;
    return this;
  }

  public SceneConfig withCentralTether(
      Vector3f center, float radius, float stiffness, float damping) {
    this.centralTetherEnabled = true;
    this.tetherCenter.set(center);
    this.tetherRadius = radius;
    this.tetherStiffness = stiffness;
    this.tetherDamping = damping;
    return this;
  }

  public SceneConfig withRoom(
      float minX, float maxX, float minY, float maxY, float minZ, float maxZ, float thick) {
    this.boundsType = BoundsType.ROOM;
    this.roomMinX = minX;
    this.roomMaxX = maxX;
    this.roomMinY = minY;
    this.roomMaxY = maxY;
    this.roomMinZ = minZ;
    this.roomMaxZ = maxZ;
    this.wallThickness = thick;
    return this;
  }

  public SceneConfig withSolver(int iterations, float fixedDt) {
    this.solverIterations = iterations;
    this.fixedTimeStep = fixedDt;
    return this;
  }

  public SceneConfig withCirclePath(
      Vector3f center, float radius, float y, float minOrbRadius, float maxOrbRadius) {
    this.pathType = PathType.CIRCLE;
    this.circlePathConfig = new CirclePathConfig(center, radius, y, minOrbRadius, maxOrbRadius);
    return this;
  }
}
