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

  /** Circle path implementation - supports arbitrary 3D circles */
  public static class CirclePath extends LoopPath {
    private final Vector3f center = new Vector3f();
    private final float radius;
    private final Vector3f u = new Vector3f(); // First basis vector for circle plane
    private final Vector3f v = new Vector3f(); // Second basis vector for circle plane
    private final float angularSpeed; // radians per second (0 for static positions)

    /** Create circle in XZ plane at fixed Y (legacy constructor) */
    public CirclePath(Vector3f center, float radius, float y) {
      this(center, radius, y, 0f);
    }

    /** Create circle in XZ plane at fixed Y with angular speed (legacy constructor) */
    public CirclePath(Vector3f center, float radius, float y, float angularSpeed) {
      this.center.set(center);
      this.radius = radius;
      this.angularSpeed = angularSpeed;
      // Set up XZ plane at fixed Y
      this.u.set(1f, 0f, 0f); // X direction
      this.v.set(0f, 0f, 1f); // Z direction
      this.center.y = y; // Override center Y
    }

    /** Create circle in arbitrary 3D plane defined by two orthogonal vectors */
    public CirclePath(
        Vector3f center,
        float radius,
        Vector3f uDirection,
        Vector3f vDirection,
        float angularSpeed) {
      this.center.set(center);
      this.radius = radius;
      this.angularSpeed = angularSpeed;

      // Normalize the basis vectors
      this.u.set(uDirection);
      this.u.normalize();
      this.v.set(vDirection);
      this.v.normalize();

      // Ensure orthogonality by using Gram-Schmidt process
      float dot = this.u.dot(this.v);
      this.v.x -= dot * this.u.x;
      this.v.y -= dot * this.u.y;
      this.v.z -= dot * this.u.z;
      this.v.normalize();
    }

    /** Create horizontal circle in XY plane at fixed Z */
    public static CirclePath createXYCircle(
        Vector3f center, float radius, float z, float angularSpeed) {
      Vector3f centerAtZ = new Vector3f(center.x, center.y, z);
      Vector3f uDirection = new Vector3f(1f, 0f, 0f); // X direction
      Vector3f vDirection = new Vector3f(0f, 1f, 0f); // Y direction
      return new CirclePath(centerAtZ, radius, uDirection, vDirection, angularSpeed);
    }

    @Override
    public Vector3f getPosition(int index, int totalCount, long timeOffsetMicros) {
      float baseAngle = (float) (2.0 * Math.PI * index / totalCount);
      float timeAngle = angularSpeed * (timeOffsetMicros / 1_000_000.0f);
      float angle = baseAngle + timeAngle;

      // Calculate position using parametric circle equation: center + radius * (cos(θ) * u + sin(θ)
      // * v)
      float cosAngle = (float) Math.cos(angle);
      float sinAngle = (float) Math.sin(angle);

      float x = center.x + radius * (cosAngle * u.x + sinAngle * v.x);
      float y = center.y + radius * (cosAngle * u.y + sinAngle * v.y);
      float z = center.z + radius * (cosAngle * u.z + sinAngle * v.z);

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

      // Calculate tangent vector in the circle plane
      // Tangent = cross product of radius vector with circle normal
      Vector3f radius = new Vector3f(dx, dy, dz);
      Vector3f normal = new Vector3f();
      normal.cross(u, v); // Circle normal = u × v

      Vector3f tangent = new Vector3f();
      tangent.cross(normal, radius); // Tangent = normal × radius

      float tangentLength = tangent.length();
      if (tangentLength < 0.001f) return new Vector3f(0f, 0f, 0f);

      tangent.scale(orbitalSpeed / tangentLength);
      return tangent;
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
