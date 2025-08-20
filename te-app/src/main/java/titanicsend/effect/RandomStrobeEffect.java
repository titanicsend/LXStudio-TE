package titanicsend.effect;

/*
 * Randomized panel/edge strobe effect for Titanic's End
 * Adapted from Chromatik
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
 * <p>
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 * <p>
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 * <p>
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 */

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.parameter.QuantizedTriggerParameter;
import heronarts.lx.utils.LXUtils;
import java.util.ArrayList;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;

@LXCategory("Titanics End")
public class RandomStrobeEffect extends TEEffect implements LX.Listener {

  private abstract class Element {
    protected double offset;

    // set this element's time offset for the strobe basis function
    protected void randomizeOffset() {
      offset = Math.random();
    }

    // return shifted strobe basis time for this element
    protected double shiftBasis(double basis) {
      return (basis + offset) % 1;
    }

    protected abstract LXPoint[] getPoints();
  }

  private class PanelElement extends Element {
    private final TEPanelModel panel;

    private PanelElement(TEPanelModel panel) {
      this.panel = panel;
    }

    @Override
    protected LXPoint[] getPoints() {
      return this.panel.points;
    }
  }

  private class EdgeElement extends Element {
    private final TEEdgeModel edge;

    private EdgeElement(TEEdgeModel edge) {
      this.edge = edge;
    }

    @Override
    protected LXPoint[] getPoints() {
      return this.edge.points;
    }
  }

  private final ArrayList<Element> elements = new ArrayList<>();

  public final QuantizedTriggerParameter launch =
      new QuantizedTriggerParameter.Launch(lx, "Strobe!", this::start)
          .setDescription(
              "Launch the strobe effect. It will run when the next global quantization event is reached.");

  private final String[] runBeatOptions = {"1", "4", "8"};
  public final DiscreteParameter runBeats =
      new DiscreteParameter("Run Beats", runBeatOptions, 1)
          .setDescription("Length of time in beats for the strobe effect to run, in sync mode");

  public final CompoundParameter runTimeMs =
      new CompoundParameter("RunTime", 2000, 50, 5000)
          .setUnits(LXParameter.Units.MILLISECONDS_RAW)
          .setDescription("Length of time in milliseconds for the strobe effect to run");

  public final ObjectParameter<LXWaveshape> waveshape =
      new ObjectParameter<LXWaveshape>(
          "Waveshape",
          new LXWaveshape[] {
            LXWaveshape.SIN, LXWaveshape.TRI, LXWaveshape.SQUARE, LXWaveshape.UP, LXWaveshape.DOWN
          });

  public final BoundedParameter maxFrequency =
      new BoundedParameter("Max Freq", 6, 1, 30)
          .setDescription("Maximum strobing frequency")
          .setUnits(LXParameter.Units.HERTZ);

  public final BoundedParameter minFrequency =
      new BoundedParameter("Min Freq", .5, .1, 1)
          .setDescription("Minimium strobing frequency")
          .setUnits(LXParameter.Units.HERTZ);

  public final CompoundParameter speed =
      new CompoundParameter("Speed", 0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setExponent(2)
          .setDescription("Speed of the strobe effect");

  public final CompoundParameter depth =
      new CompoundParameter("Depth", 0.75)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Depth of the strobe effect");

  public final CompoundParameter bias =
      new CompoundParameter("Bias", 0.5, -1, 1)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setPolarity(CompoundParameter.Polarity.BIPOLAR)
          .setDescription("Bias of the strobe effect");

  public final BooleanParameter tempoSync =
      new BooleanParameter("Sync", true)
          .setDescription("Whether to sync the tempo to a clock division");

  public final EnumParameter<Tempo.Division> tempoDivision =
      new EnumParameter<>("Division", Tempo.Division.EIGHTH)
          .setDescription("Which tempo division to use when in sync mode");

  public final BoundedParameter tempoPhaseOffset =
      new BoundedParameter("Phase Offset", 0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Shifts the phase of the strobe LFO relative to tempo");

  // Non-tempo  basis
  private final SawLFO basis =
      startModulator(
          new SawLFO(
              0,
              1,
              new FunctionalParameter() {
                @Override
                public double getValue() {
                  return 1000
                      / LXUtils.lerp(
                          minFrequency.getValue(), maxFrequency.getValue(), speed.getValue());
                }
              }));

  private boolean needsModelRefresh = true;

  private boolean isRunning = false;
  private boolean isRunningTempo = false;
  private long stopTime;
  private int elapsedBeats = 0;
  private int endBeats = 0;
  private boolean tempoRegistered = false;

  private final Tempo.Listener tempoListener =
      new Tempo.Listener() {
        @Override
        public void onBeat(Tempo tempo, int beat) {
          ++elapsedBeats;
        }
      };

  public RandomStrobeEffect(LX lx) {
    super(lx);
    addParameter("launch", this.launch);
    addParameter("runBeats", this.runBeats);
    addParameter("runTimeMs", this.runTimeMs);
    addParameter("speed", this.speed);
    addParameter("depth", this.depth);
    addParameter("bias", this.bias);
    addParameter("waveshape", this.waveshape);
    addParameter("tempoSync", this.tempoSync);
    addParameter("tempoDivision", this.tempoDivision);
    addParameter("tempoPhaseOffset", this.tempoPhaseOffset);
    addParameter("minFrequency", this.minFrequency);
    addParameter("maxFrequency", this.maxFrequency);

    this.lx.addListener(this);
  }

  @Override
  public void modelGenerationChanged(LX lx, LXModel model) {
    this.needsModelRefresh = true;
  }

  public void buildElementLists() {
    this.elements.clear();

    for (TEPanelModel panel : modelTE.getPanels()) {
      PanelElement pwe = new PanelElement(panel);
      pwe.randomizeOffset();
      this.elements.add(pwe);
    }

    for (TEEdgeModel edge : modelTE.getEdges()) {
      EdgeElement e = new EdgeElement(edge);
      e.randomizeOffset();
      this.elements.add(e);
    }
  }

  @Override
  protected void onEnable() {
    this.basis.setBasis(0).start();
  }

  private long now() {
    return System.currentTimeMillis();
  }

  private void start() {
    // Initialize run variables from parameters
    this.isRunning = true;
    this.isRunningTempo = this.tempoSync.isOn();
    this.stopTime = now() + (long) (this.runTimeMs.getValue());
    this.elapsedBeats = 0;
    this.endBeats = Integer.parseInt(this.runBeats.getOption()) + 1;

    // Register tempo if needed
    if (this.isRunningTempo && !this.tempoRegistered) {
      this.tempoRegistered = true;
      this.lx.engine.tempo.addListener(this.tempoListener);
    }

    // Randomize element offsets
    for (Element element : this.elements) {
      element.randomizeOffset();
    }
  }

  /** Determine if the strobe is finished. Only called while strobe is running. */
  private boolean isFinished() {
    if (this.isRunningTempo) {
      return this.elapsedBeats >= this.endBeats;
    } else {
      return now() > this.stopTime;
    }
  }

  private void stop() {
    this.isRunning = false;
    if (this.tempoRegistered) {
      this.tempoRegistered = false;
      this.lx.engine.tempo.removeListener(this.tempoListener);
    }
  }

  public float compute(double basis, boolean useBaseValue) {
    double strobe = this.waveshape.getObject().compute(basis);
    double bias = useBaseValue ? this.bias.getBaseValue() : this.bias.getValue();
    double expPower = (bias >= 0) ? (1 + 3 * bias) : (1 / (1 - 3 * bias));
    if (expPower != 1) {
      strobe = Math.pow(strobe, expPower);
    }
    return LXUtils.lerpf(
        1, (float) strobe, useBaseValue ? this.depth.getBaseValuef() : this.depth.getValuef());
  }

  private double getTempoBasis() {
    double basis = this.lx.engine.tempo.getBasis(this.tempoDivision.getEnum());
    return (basis + this.tempoPhaseOffset.getValue()) % 1.;
  }

  @Override
  public void run(double deltaMs, double enabledAmount) {
    if (!this.isRunning) {
      return;
    }
    if (isFinished()) {
      stop();
      return;
    }

    if (this.needsModelRefresh) {
      buildElementLists();
      this.needsModelRefresh = false;
    }

    float amt = (float) enabledAmount * this.depth.getValuef();

    if (amt > 0) {
      double strobeBasis = this.tempoSync.isOn() ? getTempoBasis() : this.basis.getValue();

      // Each fixture has its own strobe basis, shifted by a random time offset
      for (Element element : this.elements) {
        double elementBasis = element.shiftBasis(strobeBasis);
        float strobe = LXUtils.lerpf(1, compute(elementBasis, false), (float) enabledAmount);
        if (elementBasis <= 1) {
          if (elementBasis <= 0) {
            setColors(LXColor.BLACK);
            element.randomizeOffset();
          } else {
            int src = LXColor.gray(100 * strobe);
            for (LXPoint p : element.getPoints()) {
              this.colors[p.index] = LXColor.multiply(this.colors[p.index], src, 0xFF);
            }
          }
        }
      }
    }
  }

  @Override
  public void dispose() {
    // Remove tempo listener
    if (this.isRunning) {
      stop();
    }
    // Remove model listener
    this.lx.removeListener(this);

    super.dispose();
  }
}
