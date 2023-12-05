package titanicsend.modulator.dmx;

import heronarts.lx.LXCategory;
import heronarts.lx.dmx.DmxModulator;
import heronarts.lx.dmx.LXDmxEngine;
import heronarts.lx.modulator.LXModulator;

/**
 * A DMX modulator consuming two bytes of DMX data for high resolution
 *
 * @author Justin K. Belcher
 */
@LXModulator.Global("DMX 16-bit")
@LXModulator.Device("DMX 16-bit")
@LXCategory("DMX")
public class Dmx16bitModulator extends DmxModulator {

    public Dmx16bitModulator() {
        this("DMX 16-bit");
    }

    public Dmx16bitModulator(String label) {
        super(label);
        this.channel.setRange(0, LXDmxEngine.MAX_CHANNEL - 1);
    }

    @Override
    protected double computeValue(double deltaMs) {
        int universe = this.universe.getValuei();
        int channel = this.channel.getValuei();

        byte byte1 = this.lx.engine.dmx.getByte(universe, channel);
        byte byte2 = this.lx.engine.dmx.getByte(universe, channel + 1);

        return (((byte1 & 0xff) << 8) | (byte2 & 0xff)) / 65535.;
    }
}
