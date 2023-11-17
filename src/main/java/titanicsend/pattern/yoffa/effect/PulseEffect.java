package titanicsend.pattern.yoffa.effect;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TECommonControls;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.TEMath;

import java.util.Collection;

public class PulseEffect extends PatternEffect {

    public Collection<LXParameter> getParameters() {
        return null;
    }

    private double originXn = 0;
    private double originYn = 0;
    private double originZn = 0.5;

    private static final double pulseWidthBase = 0.2;

    public PulseEffect(PatternTarget target) {
        super(target);
        TECommonControls ctl = pattern.getControls();

        ctl.setRange(TEControlTag.SPEED, 0, -2, 2);
        ctl.setValue(TEControlTag.SPEED, 0.25);

        ctl.setRange(TEControlTag.SIZE, 3, 12, 0.7);

        ctl.setRange(TEControlTag.WOW2, 0, 0, 15);
        ctl.setUnits(TEControlTag.WOW2, LXParameter.Units.INTEGER);
    }

    public PulseEffect setOrigin(double xn, double yn, double zn) {
        this.originXn = xn;
        this.originYn = yn;
        this.originZn = zn;
        return this;
    }

    // quantize an input value in the range [0,1] into one of the
    // specified number of level bins.  If number of bins is zero,
    // return the value untouched.
    double posterize(double n, double levels) {
        double result;
        if (levels == 0) {
            result = n;
        } else {
            levels += 1; // helps limit black in palettes that have a lot of it
            result = Math.floor(n * levels) / levels;
            // average w/original color to blend the band edges a little
            result = (result + result + n) / 3;
        }
        return result;
    }

    @Override
    public void run(double deltaMs) {
        double zOrigin = originZn + pattern.getXPos();
        double yOrigin = originYn + pattern.getYPos();

        double energy = pattern.getWow1();
        double levels = Math.floor(pattern.getWow2());

        double scale = pattern.getSize();
        double pulseWidth = pulseWidthBase / scale;

        // getGradientColor does not yet support brightness from the control
        double masterBri = pattern.getBrightness();

        // make sure beat pulse follows the direction of the waves.
        double beat = pattern.getTempo().basis();
        beat = (Math.signum(pattern.getSpeed()) >= 0) ? beat : 1 - beat;

        double beatPulse = energy * 40 * (1 - beat);

        double t1 = pattern.getTime();

        for (LXPoint point : getPoints()) {
            double d1 = TEMath.distance(point.xn, point.yn, point.zn, originXn, yOrigin, zOrigin);
            double dist = d1 * scale;

            // dark sparkles - moving beat wave controlled by Wow1
            double beatWave = Math.abs(d1 - beat);
            beatWave = (beatWave > pulseWidth) ? 0 :
                energy * 40 * (1.0 - beatWave);

            // straightforward radial gradient, with color posterization
            double index = posterize(Math.abs(t1 - dist) % 1, levels);
            int color = pattern.getGradientColor((float) index);

            double hue = LXColor.h(color);
            double saturation = TEMath.clamp(LXColor.s(color) - beatWave, 0, 100);
            double brightness = masterBri * TEMath.clamp(LXColor.b(color) - beatPulse, 0, 100);

            setColor(point, LXColor.hsb(
                hue,
                saturation,
                brightness
            ));
        }
    }
}
