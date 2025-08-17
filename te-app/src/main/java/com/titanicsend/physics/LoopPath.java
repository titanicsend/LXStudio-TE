package com.titanicsend.physics;

import javax.vecmath.Vector3f;

/** Abstract base class for loop paths that can be sampled for positions */
public abstract class LoopPath {

  /**
   * Get the position on this loop path for a given object index and time
   *
   * @param index The index of the object (0 to N-1)
   * @param totalCount Total number of objects on the path
   * @param timeOffsetMicros Time offset in microseconds from start
   * @return The position in world coordinates
   */
  public abstract Vector3f getPosition(int index, int totalCount, long timeOffsetMicros);

  /**
   * Get the initial velocity for proper orbital motion (if applicable)
   *
   * @param index The index of the object (0 to N-1)
   * @param totalCount Total number of objects on the path
   * @param centerGravity The center of gravity (may be null)
   * @param GM The gravitational parameter (GM) for orbital velocity calculation
   * @return The initial velocity vector, or zero if not applicable
   */
  public Vector3f getOrbitalVelocity(int index, int totalCount, Vector3f centerGravity, float GM) {
    return new Vector3f(0f, 0f, 0f); // Default: no initial velocity
  }

  /** Circle path implementation */
  public static class CirclePath extends LoopPath {
    private final Vector3f center = new Vector3f();
    private final float radius;
    private final float y; // Fixed Y plane
    private final float angularSpeed; // radians per second (0 for static positions)

    public CirclePath(Vector3f center, float radius, float y) {
      this(center, radius, y, 0f);
    }

    public CirclePath(Vector3f center, float radius, float y, float angularSpeed) {
      this.center.set(center);
      this.radius = radius;
      this.y = y;
      this.angularSpeed = angularSpeed;
    }

    @Override
    public Vector3f getPosition(int index, int totalCount, long timeOffsetMicros) {
      float baseAngle = (float) (2.0 * Math.PI * index / totalCount);
      float timeAngle = angularSpeed * (timeOffsetMicros / 1_000_000.0f);
      float angle = baseAngle + timeAngle;

      float x = center.x + radius * (float) Math.cos(angle);
      float z = center.z + radius * (float) Math.sin(angle);

      return new Vector3f(x, y, z);
    }

    @Override
    public Vector3f getOrbitalVelocity(
        int index, int totalCount, Vector3f centerGravity, float GM) {
      if (centerGravity == null || GM <= 0) return new Vector3f(0f, 0f, 0f);

      // Get position
      Vector3f pos = getPosition(index, totalCount, 0);

      // Calculate orbital velocity: v = sqrt(GM/r)
      float dx = pos.x - centerGravity.x;
      float dy = pos.y - centerGravity.y;
      float dz = pos.z - centerGravity.z;
      float r = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

      if (r < 0.001f) return new Vector3f(0f, 0f, 0f);

      float orbitalSpeed = (float) Math.sqrt(GM / r);

      // Tangential direction (perpendicular to radius in the orbital plane)
      // For circular orbit in XZ plane: tangent = (-dz, 0, dx) normalized
      float tangentLength = (float) Math.sqrt(dz * dz + dx * dx);
      if (tangentLength < 0.001f) return new Vector3f(0f, 0f, 0f);

      float vx = -orbitalSpeed * dz / tangentLength;
      float vy = 0f; // No Y component for horizontal orbit
      float vz = orbitalSpeed * dx / tangentLength;

      return new Vector3f(vx, vy, vz);
    }
  }

  /** Line path implementation */
  public static class LinePath extends LoopPath {
    private final Vector3f start = new Vector3f();
    private final Vector3f end = new Vector3f();

    public LinePath(Vector3f start, Vector3f end) {
      this.start.set(start);
      this.end.set(end);
    }

    @Override
    public Vector3f getPosition(int index, int totalCount, long timeOffsetMicros) {
      // Distribute objects evenly along the line
      float t = totalCount > 1 ? (float) index / (totalCount - 1) : 0.5f;

      float x = start.x + t * (end.x - start.x);
      float y = start.y + t * (end.y - start.y);
      float z = start.z + t * (end.z - start.z);

      return new Vector3f(x, y, z);
    }
  }
}
