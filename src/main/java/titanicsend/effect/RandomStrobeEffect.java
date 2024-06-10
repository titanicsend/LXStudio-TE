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
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.utils.LXUtils;
import java.util.ArrayList;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;

@LXCategory("Titanics End")
public class RandomStrobeEffect extends TEEffect {

  private class Element {
    protected double offset;

    // set this element's time offset for the strobe basis function
    protected void randomizeOffset() {
      offset = Math.random();
    }

    // return shifted strobe basis time for this element
    protected double shiftBasis(double basis) {
      return (basis + offset) % 1;
    }
  }

  private class PanelElement extends Element {
    public TEPanelModel panel = null;
  }

  private class EdgeElement extends Element {
    public TEEdgeModel edge = null;
  }

  private final ArrayList<PanelElement> panelElements = new ArrayList<>();
  private final ArrayList<EdgeElement> edgeElements = new ArrayList<>();

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
      new BooleanParameter("Sync", false)
          .setDescription("Whether to sync the tempo to a clock division");

  public final EnumParameter<Tempo.Division> tempoDivision =
      new EnumParameter<Tempo.Division>("Division", Tempo.Division.QUARTER)
          .setDescription("Which tempo division to use when in sync mode");

  public final BoundedParameter tempoPhaseOffset =
      new BoundedParameter("Phase Offset", 0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Shifts the phase of the strobe LFO relative to tempo");

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

  public RandomStrobeEffect(LX lx) {
    super(lx);
    addParameter("speed", this.speed);
    addParameter("depth", this.depth);
    addParameter("bias", this.bias);
    addParameter("waveshape", this.waveshape);
    addParameter("tempoSync", this.tempoSync);
    addParameter("tempoDivision", this.tempoDivision);
    addParameter("tempoPhaseOffset", this.tempoPhaseOffset);
    addParameter("minFrequency", this.minFrequency);
    addParameter("maxFrequency", this.maxFrequency);

    buildElementLists();
  }

  public void buildElementLists() {
    for (TEPanelModel panel : modelTE.getPanels()) {
      PanelElement pwe = new PanelElement();
      pwe.panel = panel;
      pwe.randomizeOffset();
      this.panelElements.add(pwe);
    }

    for (TEEdgeModel edge : modelTE.getEdges()) {
      EdgeElement e = new EdgeElement();
      e.edge = edge;
      e.randomizeOffset();
      this.edgeElements.add(e);
    }
  }

  @Override
  protected void onEnable() {
    this.basis.setBasis(0).start();
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
    float amt = (float) enabledAmount * this.depth.getValuef();

    if (amt > 0 && this.speed.getValue() > 0) {
      double strobeBasis = this.tempoSync.isOn() ? getTempoBasis() : this.basis.getValue();

      // each panel and edge has its own strobe basis, shifted by a random time offset
      // draw the panels
      for (PanelElement pe : this.panelElements) {
        double elementBasis = pe.shiftBasis(strobeBasis);
        float strobe = LXUtils.lerpf(1, compute(elementBasis, false), (float) enabledAmount);
        if (elementBasis <= 1) {
          if (elementBasis <= 0) {
            setColors(LXColor.BLACK);
            pe.randomizeOffset();
          } else {
            int src = LXColor.gray(100 * strobe);
            for (TEPanelModel.LitPointData p : pe.panel.litPointData) {
              this.colors[p.point.index] = LXColor.multiply(this.colors[p.point.index], src, 0xFF);
            }
          }
        }
      }

      // draw the edges
      for (EdgeElement e : this.edgeElements) {
        double elementBasis = e.shiftBasis(strobeBasis);
        float strobe = LXUtils.lerpf(1, compute(elementBasis, false), (float) enabledAmount);
        if (elementBasis <= 1) {
          if (elementBasis <= 0) {
            setColors(LXColor.BLACK);
            e.randomizeOffset();
          } else {
            int src = LXColor.gray(100 * strobe);
            for (TEEdgeModel.Point p : e.edge.points) {
              this.colors[p.index] = LXColor.multiply(this.colors[p.index], src, 0xFF);
            }
          }
        }
      }
    }
  }

  /*
   * To be called when the model changes. Not working for now b/c
   * the Chromatik view system doesn't seem to want to return a working
   * partial TEWholeModel with panel and edge information.
   * (Works fine if you're just using model.points though.)
   * TODO - figure out how to get this working for real.

  @Override
  public void onModelChanged(LXModel model) {
      super.onModelChanged(model);
      this.modelTE = (TEWholeModel) this.getModelView();
      this.panelElements.clear();
      this.edgeElements.clear();
      buildElementLists();
  }
  */

}
