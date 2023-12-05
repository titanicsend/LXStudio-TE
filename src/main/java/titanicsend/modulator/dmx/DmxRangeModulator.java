/**
 * License notes: Expecting to contribute these DMX modulators back to LX upstream
 */
package titanicsend.modulator.dmx;

import heronarts.lx.LXCategory;
import heronarts.lx.dmx.DmxModulator;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;

/**
 * Modulator receiving a range from [min] to [max] within a DMX channel.
 *
 * @author Justin K. Belcher
 */
@LXModulator.Global("DMX Range")
@LXModulator.Device("DMX Range")
@LXCategory("DMX")
public class DmxRangeModulator extends DmxModulator {

    public final DiscreteParameter min =
            new DiscreteParameter("Min", 0, 256).setDescription("Minimum input value for range");

    public final DiscreteParameter max =
            new DiscreteParameter("Max", 255, 0, 256).setDescription("Maximum input value for range");

    public final BooleanParameter active = new BooleanParameter("Active", false)
            .setDescription("Active: TRUE when DMX value is within the range [min, max], inclusive");

    public DmxRangeModulator() {
        this("DMX Range");
    }

    public DmxRangeModulator(String label) {
        super(label);
        addParameter("min", this.min);
        addParameter("max", this.max);
        addParameter("active", this.active);
    }

    private boolean internal = false;

    @Override
    public void onParameterChanged(LXParameter p) {
        if (this.internal) {
            return;
        }

        this.internal = true;
        if (p == this.min) {
            final int min = this.min.getValuei();
            if (this.max.getValuei() < min) {
                this.max.setValue(min);
            }
        } else if (p == this.max) {
            final int max = this.max.getValuei();
            if (this.min.getValuei() > max) {
                this.min.setValue(max);
            }
        }
        this.internal = false;
    }

    @Override
    protected double computeValue(double deltaMs) {
        final int min = this.min.getValuei();
        final int max = this.max.getValuei();

        final int dmx = getDmxValuei(this.universe.getValuei(), this.channel.getValuei());

        if (dmx >= min && dmx <= max) {
            this.active.setValue(true);
            if (max == min) {
                return 1;
            }
            return ((double) dmx - min) / (max - min);
        } else {
            this.active.setValue(false);
            return 0;
        }
    }

    protected int getDmxValuei(int universe, int channel) {
        return this.lx.engine.dmx.getByte(universe, channel) & 0xff;
    }
}
