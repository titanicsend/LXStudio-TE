/** License notes: Expecting to contribute these DMX modulators back to LX upstream */
package titanicsend.modulator.dmx;

import heronarts.lx.LXCategory;
import heronarts.lx.dmx.DmxModulator;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;

/**
 * Modulator receiving two ranges of input from one DMX channel. Input Output 0 Output1 = 0 Output2
 * = 0 1-127 Output1 = 0-1 Output2 = 0 128-255 Output1 = 0 Output2 = 0-1
 *
 * @author Justin K. Belcher
 */
@LXModulator.Global("DMX Dual Range")
@LXModulator.Device("DMX Dual Range")
@LXCategory("DMX")
public class DmxDualRangeModulator extends DmxModulator {

  public final BooleanParameter isZero =
      new BooleanParameter("Zero", false).setDescription("TRUE when DMX value is zero");

  public final BooleanParameter range1active =
      new BooleanParameter("Active1", false).setDescription("Active1: TRUE when range 1 is active");

  public final BooleanParameter range2active =
      new BooleanParameter("Active2", false).setDescription("Active2: TRUE when range 2 is active");

  public final CompoundParameter output1 =
      new CompoundParameter("Output1", 0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Output for first range, moves 0-1 for DMX values 1-127");

  public final CompoundParameter output2 =
      new CompoundParameter("Output2", 0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Output for second range, moves 0-1 for DMX values 128-255");

  public DmxDualRangeModulator() {
    this("DMX Dual Range");
  }

  public DmxDualRangeModulator(String label) {
    super(label);
    addParameter("isZero", this.isZero);
    addParameter("range1active", this.range1active);
    addParameter("range2active", this.range2active);
    addParameter("output1", this.output1);
    addParameter("output2", this.output2);
  }

  @Override
  public double getNormalized() {
    return getValue();
  }

  @Override
  protected double computeValue(double deltaMs) {
    int dmx = getDmxValuei();

    if (dmx == 0) {
      this.isZero.setValue(true);
      this.range1active.setValue(false);
      this.range2active.setValue(false);
      this.output1.setValue(0);
      this.output2.setValue(0);
      return 0;
    } else if (dmx < 128) {
      double value = (dmx - 1) / 126.;
      this.isZero.setValue(false);
      this.range1active.setValue(true);
      this.range2active.setValue(false);
      this.output1.setValue(value);
      this.output2.setValue(0);
      return value;
    } else {
      double value = (dmx - 128) / 127.;
      this.isZero.setValue(false);
      this.range1active.setValue(false);
      this.range2active.setValue(true);
      this.output1.setValue(0);
      this.output2.setValue(value);
      return 0; // modulator value is output1
    }
  }

  protected int getDmxValuei() {
    return this.lx.engine.dmx.getByte(this.universe.getValuei(), this.channel.getValuei()) & 0xff;
  }
}
