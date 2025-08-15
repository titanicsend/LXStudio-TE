package com.titanicsend.physics;

import javax.vecmath.Vector3f;

public final class SceneConfig {

  public enum GravityType {
    NONE,
    GLOBAL_VECTOR,
    CENTRAL_MASS
  }

  public enum BoundsType {
    INFINITE,
    ROOM
  }

  // --- Gravity ---
  public GravityType gravityType = GravityType.NONE;
  public Vector3f gravityVector = new Vector3f(0f, -9.81f, 0f); // GLOBAL_VECTOR
  public Vector3f gravityCenter = new Vector3f(0f, 0f, 0f); // CENTRAL_MASS
  public float gravityMu = 25f; // GM for central mass
  public float gravitySoftening = 0.25f; // avoids singularity at r≈0

  // --- Bounds ---
  public BoundsType boundsType = BoundsType.INFINITE;
  public float roomMinX = 0f,
      roomMaxX = 10f,
      roomMinY = 0f,
      roomMaxY = 10f,
      roomMinZ = 0f,
      roomMaxZ = 10f,
      wallThickness = 1f;

  // --- Physics solver knobs ---
  public int solverIterations = 10;
  public float fixedTimeStep = 1f / 120f; // engine can use this if desired

  // --- Rendering/interop knobs (placeholder) ---
  // TODO: per-shader config hook (e.g., SSBO handles, uniforms, color maps)
  public Object shaderConfig = null;

  public SceneConfig withGravity(GravityType t) {
    this.gravityType = t;
    return this;
  }

  public SceneConfig withGlobalGravity(Vector3f g) {
    this.gravityVector.set(g);
    return this;
  }

  public SceneConfig withCentralMass(Vector3f center, float GM, float soft) {
    this.gravityCenter.set(center);
    this.gravityMu = GM;
    this.gravitySoftening = soft;
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
}
