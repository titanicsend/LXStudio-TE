package com.titanicsend.physics;

import javax.vecmath.Vector3f;

public final class SceneConfig {

  public enum BoundsType {
    INFINITE,
    ROOM
  }

  // --- Gravity Forces (can be combined independently) ---
  // Global gravity (applies to all bodies)
  public boolean globalGravityEnabled = false;
  public Vector3f gravityVector = new Vector3f(0f, -9.81f, 0f);

  // Support multiple centers of gravity
  public static class CentralGravityConfig {
    public final Vector3f center = new Vector3f();
    public float mu;
    public float soft;

    public CentralGravityConfig(Vector3f c, float mu, float soft) {
      this.center.set(c);
      this.mu = mu;
      this.soft = soft;
    }
  }

  public java.util.List<CentralGravityConfig> centralGravities = new java.util.ArrayList<>();

  // --- Central Tether ---
  // Removed scene-level tether in favor of per-body tether configuration

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

  public SceneConfig withGlobalGravity(Vector3f g) {
    this.globalGravityEnabled = true;
    this.gravityVector.set(g);
    return this;
  }

  // Add one central gravity to the list (preferred API; can be called multiple times)
  public SceneConfig addCentralGravity(Vector3f center, float GM, float soft) {
    this.centralGravities.add(new CentralGravityConfig(center, GM, soft));
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
