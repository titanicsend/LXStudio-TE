package titanicsend.pattern.yoffa.effect;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.utils.LXUtils;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.TEMath;

import java.util.Collection;
import java.util.List;

import static titanicsend.util.TEMath.clamp;
import static titanicsend.util.TEMath.wave;

public class PulseEffect extends PatternEffect {

    public Collection<LXParameter> getParameters() {
        return null;
    }

    private double originXn = 0;
    private double originYn = 0;
    private double originZn = 0.5;

    public PulseEffect(PatternTarget target) {
        super(target);
        TEPerformancePattern.TECommonControls ctl = pattern.getControls();

        ctl.setRange(TEControlTag.QUANTITY, 2, 2, 10);

    }

    public PulseEffect setOrigin(double xn, double yn, double zn) {
        this.originXn = xn;
        this.originYn = yn;
        this.originZn = zn;
        return this;
    }

    // This is shamelessly stolen from Jeff's ArtStandards class with some tweaked inputs/parameters
    // We could abstract it out, but I didn't want to disrupt his wonderful template
    // I also expect them to diverge as they are tweaked more, so I don't hate the duplication as of now
    @Override
    public void run(double deltaMs) {
        double energy = pattern.getWow1();
        double zOrigin = originZn + pattern.getXPos();
        double yOrigin = originYn + pattern.getYPos();
        double beat = pattern.getTempo().basis();
        double measure = pattern.measure();

        // set us up to respond to large treble events
        double scaledTrebleRatio = clamp((pattern.getTrebleRatio() - 1) / 0.15,0,1);
        scaledTrebleRatio *= scaledTrebleRatio;

        for (LXPoint point : getAllPoints()) {
            double distanceFromCenter = TEMath.distance(point.xn, point.yn, point.zn, originXn, yOrigin, zOrigin);

            int color = pattern.getGradientColor((float)
                (pattern.getQuantity() * distanceFromCenter - measure) % 1);

            double hue = LXColor.h(color);
            double saturation = LXColor.s(color);
            double brightness = LXColor.b(color);

            double dist = 2.0 * distanceFromCenter - scaledTrebleRatio;
            double alphaWave = wave(dist);
            double satWave = (0.86 - dist) * scaledTrebleRatio * Math.random();

            saturation = Math.max(0.0, saturation - (energy * 100 * satWave * satWave));

            setColor(point, LXColor.hsba(
                hue,
                saturation,
                (brightness + (35 * (1.0 - (2 * distanceFromCenter - beat)))) % 100,
                alphaWave * 255
            ));
        }
    }
}
