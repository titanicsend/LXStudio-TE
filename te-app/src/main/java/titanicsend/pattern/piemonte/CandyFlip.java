package titanicsend.pattern.piemonte;

import static titanicsend.util.TEColor.TRANSPARENT;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEVertex;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TEColor;

/**
 * CandyFlip Pattern
 *
 * <p>Dots burst from edge vertices and cascade along connected edges Similar to BouncingDots but
 * with burst and cascade mechanics
 */
@LXCategory("Edge FG")
public class CandyFlip extends GLShaderPattern {

  // Particle system parameters
  private static final int MAX_PARTICLES_PER_VERTEX = 8;
  private static final int MAX_GENERATIONS = 5; // Increased for more chain reactions
  private static final double BURST_DURATION = 1500.0; // Duration for particles to travel edge
  private static final double BURST_CYCLE = 5000.0; // ms for full cycle
  private static final int INITIAL_BURST_COUNT = 4; // Number of initial bursts

  // Particle class
  private class Particle {
    TEVertex sourceVertex;
    TEEdgeModel edge;
    double position; // 0.0 to 1.0 along edge
    double velocity;
    int generation;
    boolean alive;
    double birthTime;
    int color;
    boolean dying; // True when particle has reached destination
    double deathTime; // Time when particle started dying

    Particle() {
      this.alive = false;
    }

    void spawn(TEVertex vertex, TEEdgeModel edge, double currentTime, int generation) {
      this.sourceVertex = vertex;
      this.edge = edge;

      // Determine if we need to travel forward or backward along the edge
      // to move away from the source vertex
      if (edge.v0 == vertex) {
        // Source is v0, so move forward (0.0 to 1.0)
        this.position = 0.0;
        this.velocity = 0.4 + Math.random() * 0.2; // Increased speed
      } else {
        // Source is v1, so move backward (1.0 to 0.0)
        this.position = 1.0;
        this.velocity = -(0.4 + Math.random() * 0.2); // Increased speed
      }

      this.generation = generation;
      this.alive = true;
      this.dying = false;
      this.birthTime = currentTime;
      this.deathTime = 0;
      this.color = getGradientColor((float) (generation * 0.2 + currentTime * 0.0001));
    }

    void update(double deltaMs, double currentTime) {
      if (!alive) return;

      if (!dying) {
        position += velocity * deltaMs / BURST_DURATION;

        // Check if particle reached end of edge
        boolean reachedEnd = false;
        TEVertex destVertex = null;

        if (velocity > 0 && position >= 1.0) {
          // Moving forward and reached v1
          reachedEnd = true;
          position = 1.0;
          destVertex = edge.v1;
        } else if (velocity < 0 && position <= 0.0) {
          // Moving backward and reached v0
          reachedEnd = true;
          position = 0.0;
          destVertex = edge.v0;
        }

        if (reachedEnd) {
          // Start dying phase - velocity stops
          dying = true;
          deathTime = currentTime;
          velocity = 0;

          // Trigger burst at destination vertex if under generation limit
          if (generation < MAX_GENERATIONS - 1 && destVertex != null) {
            triggerBurst(destVertex, generation + 1);
          }
        }
      } else {
        // Particle is dying - check if fade complete
        double deathDuration = 300.0; // 300ms fade out
        if (currentTime - deathTime > deathDuration) {
          alive = false;
        }
      }
    }
  }

  private List<Particle> particles;
  private Map<TEVertex, Double> lastBurstTime;
  private double currentTime = 0;

  public CandyFlip(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    particles = new ArrayList<>();
    lastBurstTime = new HashMap<>();

    // Initialize particle pool (increased for better chain reactions)
    for (int i = 0; i < 1000; i++) {
      particles.add(new Particle());
    }

    // Configure controls
    controls.setRange(TEControlTag.SPEED, 1.0, 0.1, 3.0);
    controls
        .setRange(TEControlTag.SIZE, 5, 1, 20)
        .setUnits(TEControlTag.SIZE, LXParameter.Units.INTEGER);
    controls.setRange(TEControlTag.QUANTITY, 0.5, 0.0, 1.0); // Burst probability

    // Beat reactivity
    controls.setRange(TEControlTag.WOW1, 1.0, 0, 2);

    // Color temperature
    controls.setRange(TEControlTag.WOW2, 0.5, 0, 1);

    // Disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();
    addShader("candy_flip.fs");
  }

  private void triggerBurst(TEVertex vertex, int generation) {
    // Skip if we've exceeded max generations
    if (generation >= MAX_GENERATIONS) return;

    // For chain reactions, we want to allow the same vertex to burst again
    // Only check cooldown for initial bursts (generation 0)
    Double lastBurst = generation == 0 ? lastBurstTime.get(vertex) : null;

    // Only check cooldown for initial bursts (generation 0)
    if (generation == 0 && lastBurst != null && currentTime - lastBurst < 500) {
      return; // Too soon for initial burst
    }

    if (generation == 0) {
      lastBurstTime.put(vertex, currentTime);
    }

    // Spawn particles along ALL connected edges
    int spawnCount = 0;
    for (TEEdgeModel edge : vertex.edges) {
      // Skip the edge we came from to avoid immediate backtracking
      boolean shouldSpawn = true;

      // Find available particle
      for (Particle p : particles) {
        if (!p.alive && shouldSpawn) {
          p.spawn(vertex, edge, currentTime, generation);
          spawnCount++;
          shouldSpawn = false;
          break;
        }
      }
    }

    // Debug: Track if burst actually spawned particles
    if (spawnCount == 0 && vertex.edges.size() > 0) {
      // No available particles - might need to increase pool size
    }
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    currentTime += deltaMs * getSpeed();

    // Clear display
    for (LXPoint point : model.points) {
      colors[point.index] = TRANSPARENT;
    }

    // Periodic burst triggers based on cycle time
    double cyclePhase = (currentTime % BURST_CYCLE) / BURST_CYCLE;
    if (cyclePhase < 0.02) { // Trigger at beginning of each cycle
      // Check if we haven't triggered recently
      boolean canTrigger = true;
      for (Double lastTime : lastBurstTime.values()) {
        if (currentTime - lastTime < BURST_CYCLE * 0.8) {
          canTrigger = false;
          break;
        }
      }

      if (canTrigger) {
        // Trigger exactly 4 initial bursts
        List<TEVertex> vertices = new ArrayList<>(modelTE.getVertexes());
        Collections.shuffle(vertices);

        int burstCount = Math.min(INITIAL_BURST_COUNT, vertices.size());
        for (int i = 0; i < burstCount; i++) {
          triggerBurst(vertices.get(i), 0);
        }
      }
    }

    // Update and render particles
    double beatEffect = 1.0 + getBassLevel() * getWow1();
    int particleSize = (int) (getSize() * beatEffect);

    for (Particle p : particles) {
      if (!p.alive) continue;

      p.update(deltaMs, currentTime);

      if (p.alive && p.edge != null) {
        // Calculate position along edge
        int edgeLength = p.edge.points.length;
        int centerPoint = (int) (p.position * edgeLength);

        // Draw particle with EdgeRunner-style white dot and fading trail
        double headSize = 0.02; // Small white head
        double tailSize = getSize() * 0.15; // Trail length based on size control

        for (int i = 0; i < edgeLength; i++) {
          double pointPosition = (double) i / edgeLength;
          double distanceFromHead = 0.0;

          // Calculate distance considering direction of movement
          if (p.velocity > 0) {
            distanceFromHead = p.position - pointPosition;
          } else {
            distanceFromHead = pointPosition - p.position;
          }

          // Skip points ahead of the particle
          if (distanceFromHead < 0) continue;

          LXPoint point = p.edge.points[i];
          int pointColor = TRANSPARENT;

          if (distanceFromHead <= headSize) {
            // White dot at the head
            pointColor = LXColor.WHITE;
          } else if (distanceFromHead <= tailSize) {
            // Fading trail behind the head
            double trailProgress = (distanceFromHead - headSize) / (tailSize - headSize);
            double brightness = 1.0 - trailProgress;

            // Apply gamma correction for smoother fade
            brightness = brightness * brightness;

            // Apply generation fade
            brightness *= 1.0 - (p.generation / (float) MAX_GENERATIONS) * 0.3;

            // Use gradient color for trail with fading alpha
            int alpha = (int) (brightness * 255);
            pointColor = TEColor.reAlpha(p.color, alpha);
          }

          // Apply overall fade based on particle state
          if (pointColor != TRANSPARENT) {
            double fade = 1.0;

            if (p.dying) {
              // When dying, only show the tail fading out
              if (distanceFromHead <= headSize) {
                continue; // Skip the head when dying
              }
              double deathDuration = 300.0;
              double deathProgress = (currentTime - p.deathTime) / deathDuration;
              fade = 1.0 - deathProgress;
            } else {
              // Normal aging fade while traveling
              double age = currentTime - p.birthTime;
              fade = 1.0 - (age / BURST_DURATION * 0.5);
            }

            if (fade > 0) {
              pointColor = LXColor.scaleBrightness(pointColor, (float) fade);
              colors[point.index] = LXColor.add(colors[point.index], pointColor);
            }
          }
        }
      }
    }
  }
}
