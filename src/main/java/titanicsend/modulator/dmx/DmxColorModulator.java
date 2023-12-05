/**
 * License notes: Expecting to contribute these DMX modulators back to LX upstream
 */
package titanicsend.modulator.dmx;

import heronarts.lx.LXCategory;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.dmx.DmxModulator;
import heronarts.lx.dmx.LXDmxEngine;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.EnumParameter;

/**
 * Extracts a color from three DMX channels starting at a given address.
 *
 * @author Justin K. Belcher
 */
@LXModulator.Global("DMX Color")
@LXModulator.Device("DMX Color")
@LXCategory("DMX")
public class DmxColorModulator extends DmxModulator {

    public enum ColorPosition {
        NONE(-1, "None"),
        ONE(0, "1"),
        TWO(1, "2"),
        THREE(2, "3"),
        FOUR(3, "4"),
        FIVE(4, "5");

        public final int index;
        public final String label;

        private ColorPosition(int index, String label) {
            this.index = index;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public final EnumParameter<LXDmxEngine.ByteOrder> byteOrder =
            new EnumParameter<LXDmxEngine.ByteOrder>("Byte Order", LXDmxEngine.ByteOrder.RGB);

    public final EnumParameter<ColorPosition> colorPosition = new EnumParameter<ColorPosition>(
                    "Color Position", ColorPosition.NONE)
            .setDescription("Destination color position (1-based) in the global palette current swatch");

    public final BooleanParameter fixed = new BooleanParameter("Fixed", true)
            .setDescription("When applying DMX color to the palette, also set the target color mode to Fixed");

    public final ColorParameter color = new ColorParameter("Color", LXColor.BLACK);

    public DmxColorModulator() {
        this("DMX Color");
    }

    public DmxColorModulator(String label) {
        super(label);
        this.channel.setRange(0, LXDmxEngine.MAX_CHANNEL - 2);
        addParameter("byteOrder", this.byteOrder);
        addParameter("colorPosition", this.colorPosition);
    }

    @Override
    public double getNormalized() {
        return getValue();
    }

    @Override
    protected double computeValue(double deltaMs) {
        LXDmxEngine.ByteOrder byteOrder = this.byteOrder.getEnum();

        int color = this.lx.engine.dmx.getColor(this.universe.getValuei(), this.channel.getValuei(), byteOrder);

        // Store color locally for preview
        this.color.setColor(color);

        // Send to target color in global palette
        ColorPosition colorPosition = this.colorPosition.getEnum();
        if (colorPosition != ColorPosition.NONE) {
            while (this.lx.engine.palette.swatch.colors.size() <= colorPosition.index) {
                this.lx.engine.palette.swatch.addColor().primary.setColor(LXColor.BLACK);
            }
            this.lx.engine.palette.swatch.getColor(colorPosition.index).primary.setColor(color);
            if (this.fixed.isOn()) {
                this.lx.engine.palette.swatch.getColor(colorPosition.index).mode.setValue(LXDynamicColor.Mode.FIXED);
            }
        }

        return LXColor.luminosity(color) / 100.;
    }
}
