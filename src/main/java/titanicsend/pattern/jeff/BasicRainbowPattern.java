/**
 * @author Jeff Vyduna <j@ngnr.org>
 */

package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SawLFO;
import titanicsend.pattern.TEPattern;

@LXCategory("Combo FG")
public class BasicRainbowPattern extends TEPattern {

    public final SawLFO t1 = new SawLFO(0, 1, 4500);

    public BasicRainbowPattern(LX lx) {
        super(lx);
        addModulator(this.t1).start();
    }

    @Override
    protected void run(double deltaMs) {
        for (LXPoint p : model.points) {
            // t1 is a timer going from 0 to 1 every 4500 ms
            // p.index/(number of total points) is a 0..1 fraction of the total LEDs
            // We wrap colors by adding the two 0..1 values (one a function of time, and one a function of position) and taking the remainder
            // Hue is a 360 degree wheel, so the final 0..1 value is multiplied by 360
            colors[p.index] = LXColor.hsb(360 * ((t1.getValuef() + ((float) p.index) / model.points.length) % 1), 100, 100);
        }
        this.updateVirtualColors(deltaMs);
    }

}