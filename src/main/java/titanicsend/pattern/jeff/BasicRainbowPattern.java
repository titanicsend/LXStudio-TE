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

@LXCategory("TE Examples")
public class BasicRainbowPattern extends TEPattern {

    public final SawLFO ramp = new SawLFO(0, 1, 4500);

    public BasicRainbowPattern(LX lx) {
        super(lx);
        addModulator(this.ramp).start();
    }

    @Override
    protected void run(double deltaMs) {
        for (LXPoint p : model.points) {
            if (this.modelTE.isGapPoint(p)) continue;

            /*
            Notice that when we iterate this way, it turns on the lasers and
            gap points; if that's not desired, iterate through just the edge
            and/or panel points.
             */

            // a 0..1 fraction of the total LEDs
            float indexFrac = (float) (p.index) / model.points.length;

            // ramp is a timer going from 0 to 1 every 4500 ms
            //
            // Wrap around the color wheel by adding the two 0..1 values (one a function of
            // time, and one a function of position) and taking the fractional part.

            float hueFrac = (ramp.getValuef() + indexFrac) % 1.F;

            colors[p.index] = LXColor.hsb(
                    // Hue is a 360 degree wheel, so the 0..1 hueFrac is multiplied by 360
                    360 * hueFrac,
                    100,
                    100);
        }

        // Apply colors to the lasers and solid color panels
        this.updateVirtualColors(deltaMs);
    }

}