package titanicsend.pattern.piemonte;

import static heronarts.lx.utils.Noise.stb_perlin_fbm_noise3;
import static heronarts.lx.utils.Noise.stb_perlin_turbulence_noise3;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * ComeUp
 *
 * <p>The come-up, rendered in light. A tide of liquid color rises from the bottom of the vehicle,
 * its surface churning with multi-octave fBm swells and chaotic whitewater chop — dial in Wow1 to
 * push the sea from a dreamy roll to a full-on storm. When the wave crests over the top and the
 * whole car is awash in one color, a fresh hue surges up from below, flooding the previous and
 * starting the cycle again. Endless. Best viewed, deep playa.
 */
@LXCategory("Mothership")
public class ComeUp extends TEPerformancePattern {

  private enum Phase {
    RISING,
    HOLD
  }

  private static final double RISE_RATE_PER_MS = 1.0 / 6000.0;
  private static final double EDGE_BAND = 0.04;
  private static final float GRADIENT_STEP = 0.137f;
  private static final double MAX_HOLD_MS = 2500.0;
  private static final float TEMPORAL_HZ = 0.5f;

  private Phase phase = Phase.RISING;
  private double fillLevel = 0.0;
  private double holdElapsedMs = 0.0;
  private float gradientPhase = 0f;
  private int currColor;
  private int prevColor;
  private double currentTimeMs = 0.0;

  public ComeUp(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.SPEED, 1.0, 0.1, 4.0);
    controls.setRange(TEControlTag.SIZE, 0.10, 0.0, 0.30);
    controls
        .setRange(TEControlTag.QUANTITY, 3, 1, 10)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);
    controls.setRange(TEControlTag.WOW1, 0.35, 0.0, 1.0);
    controls.setRange(TEControlTag.WOW2, 0.3, 0.0, 1.0);

    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));

    addCommonControls();

    this.currColor = getGradientColor(this.gradientPhase);
    this.prevColor = getGradientColor(this.gradientPhase - GRADIENT_STEP);
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {
    this.currentTimeMs += deltaMs;

    if (this.phase == Phase.RISING) {
      this.fillLevel += deltaMs * getSpeed() * RISE_RATE_PER_MS;
      if (this.fillLevel >= 1.0) {
        this.fillLevel = 1.0;
        this.phase = Phase.HOLD;
        this.holdElapsedMs = 0.0;
      }
    } else {
      this.holdElapsedMs += deltaMs;
      if (this.holdElapsedMs >= getWow2() * MAX_HOLD_MS) {
        this.gradientPhase += GRADIENT_STEP;
        this.prevColor = this.currColor;
        this.currColor = getGradientColor(this.gradientPhase);
        this.fillLevel = 0.0;
        this.phase = Phase.RISING;
      }
    }

    final double level = this.fillLevel;
    final float amp = (float) getSize();
    final float freq = (float) Math.max(1, getQuantity());
    final float t = (float) (this.currentTimeMs * 0.001 * TEMPORAL_HZ);
    final float turb = (float) getWow1();
    final double invBand = 1.0 / EDGE_BAND;
    final float bright = (float) getBrightness();
    final int curr = this.currColor;
    final int prev = this.prevColor;

    // directional drift so waves travel across the surface
    final float driftX = t * 0.3f;
    final float driftZ = t * 0.17f;

    for (LXPoint p : model.points) {
      float nx = (p.xn + driftX) * freq;
      float nz = (p.zn + driftZ) * freq;

      // base swells: smooth 3-octave fBm in -1..1
      float base = stb_perlin_fbm_noise3(nx, nz, t, 2.0f, 0.5f, 3);

      // chop: turbulence noise at higher frequency, centered to -1..1
      // turbulence returns |fBm| so it's 0..~1; subtract 0.5 to roughly center
      float chopRaw = stb_perlin_turbulence_noise3(nx * 2.3f, nz * 2.3f, t * 1.7f, 2.2f, 0.5f, 4);
      float chop = (chopRaw - 0.5f) * 2.0f;

      float n = base + chop * turb * 1.2f;

      double localLevel = level + n * amp;
      double mix = Math.max(0, Math.min(1, 0.5 + 0.5 * (localLevel - p.yn) * invBand));

      int c;
      if (mix <= 0.0) {
        c = prev;
      } else if (mix >= 1.0) {
        c = curr;
      } else {
        c = LXColor.lerp(prev, curr, (float) mix);
      }
      colors[p.index] = LXColor.scaleBrightness(c, bright);
    }
  }
}
