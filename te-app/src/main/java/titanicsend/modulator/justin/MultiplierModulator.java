/**
 * Copyright 2023- Justin K. Belcher, Mark C. Slee, Heron Arts LLC
 *
 * <p>This file is part of the LX Studio software library. By using LX, you agree to the terms of
 * the LX Studio Software License and Distribution Agreement, available at: http://lx.studio/license
 *
 * <p>Please note that the LX license is not open-source. The license allows for free,
 * non-commercial use.
 *
 * <p>HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE,
 * WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */
package titanicsend.modulator.justin;

// JKB note: going to circle back around and add divide/add/subtract as options.

import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.CompoundParameter;

@LXModulator.Global("Multiplier")
@LXCategory(LXCategory.MACRO)
public class MultiplierModulator extends LXModulator implements LXOscComponent {

  public final CompoundParameter inputA =
      new CompoundParameter("Input A")
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Map something to Input A or set manually");

  public final CompoundParameter inputB =
      new CompoundParameter("Input B")
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Map something to Input B or set manually");

  public final CompoundParameter output =
      new CompoundParameter("Output")
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription(
              "Output = Multiplied A and B normalized values.  Map output to a destination.");

  public MultiplierModulator() {
    this("Multiplier");
  }

  public MultiplierModulator(String label) {
    super(label);
    addParameter("inputA", this.inputA);
    addParameter("inputB", this.inputB);
    addParameter("output", this.output);
  }

  @Override
  protected double computeValue(double deltaMs) {
    // TODO: Right now, mapping the modulator requires selection the output knob as a source. Fix
    // that.
    this.output.setNormalized(this.inputA.getNormalized() * this.inputB.getNormalized());
    return this.output.getNormalized();
  }
}
