package com.titanicsend.physics;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for Bullet Physics integration
 *
 * <p>Validates that Bullet Physics can be initialized, run simulation steps, and clean up properly.
 * Uses JBullet for cross-platform compatibility.
 */
public class BulletSmokeTest {

  private BulletBootstrap bullet;

  @BeforeEach
  void setUp() {
    bullet = new BulletBootstrap();
  }

  @AfterEach
  void tearDown() {
    if (bullet != null) {
      bullet.cleanup();
    }
  }

  @Test
  void testBulletPhysicsInitialization() {
    System.out.println("=== Bullet Physics Smoke Test: Initialization ===");

    assertDoesNotThrow(
        () -> {
          // Test Bullet Physics initialization
          bullet.initialize();

          // Verify successful initialization
          assertTrue(bullet.isInitialized(), "Bullet Physics should be initialized");
          assertTrue(bullet.isGpuEnabled(), "Physics processing should be enabled");

          System.out.println("✓ Bullet Physics initialized successfully");
          System.out.println("✓ JBullet pure Java physics engine enabled");
        });
  }

  @Test
  void testPhysicsSimulationSteps() {
    System.out.println("=== Bullet Physics Smoke Test: Simulation Steps ===");

    assertDoesNotThrow(
        () -> {
          // Initialize Bullet Physics
          bullet.initialize();
          assertTrue(bullet.isInitialized(), "Bullet Physics should be initialized");
          assertTrue(bullet.isGpuEnabled(), "Physics processing should be enabled");

          // Add a test body and get its initial position
          bullet.addSphere("test", 0f, 5f, 0f, 0.5f, 1.0f);
          float[] initialPos = bullet.getBodyPosition("test");
          System.out.printf(
              "Initial box position: [%.2f, %.2f, %.2f]%n",
              initialPos[0], initialPos[1], initialPos[2]);

          // Run 10 physics simulation steps
          float deltaTime = 1.0f / 60.0f; // 60 FPS
          for (int i = 0; i < 10; i++) {
            bullet.step(deltaTime);

            float[] currentPos = bullet.getBodyPosition("test");
            System.out.printf(
                "Physics Step %d - Box position: [%.2f, %.2f, %.2f]%n",
                i + 1, currentPos[0], currentPos[1], currentPos[2]);

            // Validate position is changing (box should be falling with gravity)
            if (i > 0) {
              assertTrue(
                  currentPos[1] <= initialPos[1],
                  "Box should be falling (Y position should decrease or stay same)");
            }
          }

          // Final position should be different from initial
          float[] finalPos = bullet.getBodyPosition("test");
          assertNotEquals(
              initialPos[1],
              finalPos[1],
              0.1f,
              "Box position should have changed significantly after physics simulation");

          System.out.println("✓ Physics simulation steps completed successfully");
        });
  }

  @Test
  void testPhysicsCleanup() {
    System.out.println("=== Bullet Physics Smoke Test: Cleanup ===");

    assertDoesNotThrow(
        () -> {
          // Initialize Bullet Physics and then cleanup
          bullet.initialize();
          assertTrue(bullet.isInitialized(), "Bullet Physics should be initialized");
          assertTrue(bullet.isGpuEnabled(), "Physics processing should be enabled");

          bullet.cleanup();
          assertFalse(
              bullet.isInitialized(), "Bullet Physics should not be initialized after cleanup");
          assertFalse(
              bullet.isGpuEnabled(), "Physics processing should not be enabled after cleanup");

          System.out.println("✓ Physics cleanup completed successfully");
        });
  }

  @Test
  void testMultipleInitializeCleanupCycles() {
    System.out.println("=== Bullet Physics Smoke Test: Multiple Init/Cleanup Cycles ===");

    assertDoesNotThrow(
        () -> {
          // Test multiple initialize/cleanup cycles to check for resource leaks
          for (int cycle = 0; cycle < 3; cycle++) {
            System.out.printf("Physics Cycle %d:%n", cycle + 1);

            // Initialize
            bullet.initialize();
            assertTrue(
                bullet.isInitialized(),
                "Bullet Physics should be initialized in cycle " + (cycle + 1));
            assertTrue(
                bullet.isGpuEnabled(),
                "Physics processing should be enabled in cycle " + (cycle + 1));

            // Run a few physics simulation steps
            for (int step = 0; step < 3; step++) {
              bullet.step(1.0f / 60.0f);
            }

            float[] pos = bullet.getBodyPosition("test");
            System.out.printf("  Final position: [%.2f, %.2f, %.2f]%n", pos[0], pos[1], pos[2]);

            // Cleanup
            bullet.cleanup();
            assertFalse(
                bullet.isInitialized(),
                "Bullet Physics should be cleaned up in cycle " + (cycle + 1));
            assertFalse(
                bullet.isGpuEnabled(),
                "Physics processing should be disabled in cycle " + (cycle + 1));
          }

          System.out.println("✓ Multiple physics cycles completed successfully");
        });
  }

  @Test
  void testMultipleSpheres() {
    System.out.println("=== Bullet Physics Smoke Test: Multiple Spheres ===");

    assertDoesNotThrow(
        () -> {
          // Initialize Bullet Physics
          bullet.initialize();
          assertTrue(bullet.isInitialized(), "Bullet Physics should be initialized");

          // Add multiple spheres to the simulation
          bullet.addSphere("s1", -2.0f, 8.0f, 0.0f, 0.5f, 1.0f);
          bullet.addSphere("s2", 0.0f, 10.0f, 0.0f, 0.7f, 1.5f);
          bullet.addSphere("s3", 2.0f, 12.0f, 0.0f, 0.3f, 0.8f);

          System.out.printf("Created spheres: s1, s2, s3%n");

          // Apply some forces
          // Apply some forces
          com.bulletphysics.dynamics.RigidBody b1 = bullet.getBody("s1");
          com.bulletphysics.dynamics.RigidBody b2 = bullet.getBody("s2");
          com.bulletphysics.dynamics.RigidBody b3 = bullet.getBody("s3");
          b1.applyCentralForce(new javax.vecmath.Vector3f(5f, 0f, 0f));
          b2.applyCentralForce(new javax.vecmath.Vector3f(0f, 10f, 0f));
          b3.applyCentralForce(new javax.vecmath.Vector3f(-3f, 0f, 0f));

          // Simulate physics for multiple steps
          for (int i = 0; i < 20; i++) {
            bullet.step(1.0f / 60.0f);

            if (i % 5 == 0) { // Print every 5th step
              float[] pos1 = bullet.getBodyPosition("s1");
              float[] pos2 = bullet.getBodyPosition("s2");
              float[] pos3 = bullet.getBodyPosition("s3");

              System.out.printf("Step %d - Sphere positions:%n", i);
              System.out.printf("  Sphere 1: [%.2f, %.2f, %.2f]%n", pos1[0], pos1[1], pos1[2]);
              System.out.printf("  Sphere 2: [%.2f, %.2f, %.2f]%n", pos2[0], pos2[1], pos2[2]);
              System.out.printf("  Sphere 3: [%.2f, %.2f, %.2f]%n", pos3[0], pos3[1], pos3[2]);
            }
          }

          System.out.println("✓ Multiple spheres simulation completed successfully");
        });
  }
}
