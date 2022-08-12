package titanicsend.pattern.yoffa.effect;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.utils.LXUtils;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.TEMath;

import java.util.Collection;
import java.util.List;

import static titanicsend.util.TEMath.wave;

public class PulseEffect extends PatternEffect {

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .1, 0, 1)
                    .setDescription("Amount of motion - Sparkles");

    public Collection<LXParameter> getParameters(){
        return List.of(energy);
    }

    private Double originXn = null;
    private Double originYn = null;
    private Double originZn = null;

    public PulseEffect(PatternTarget target) {
        super(target);
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
        double scaledTrebleRatio = LXUtils.clamp(
                (pattern.getTrebleRatio() - .5) / (1.01 - energy.getValue()) / 6 -
                        .2 + energy.getValue() / 2,
                0, 1);

        for (LXPoint point : getAllPoints()) {
            double distanceFromCenter = originXn == null ? point.rn :
                    TEMath.distance(point.xn, point.yn, point.zn, originXn, originYn, originZn);

            int baseColor = pattern.getPrimaryGradientColor((float) (2 * (distanceFromCenter - pattern.measure())));

            double hue = LXColor.h(baseColor);
            double saturation = LXColor.s(baseColor);
            double brightness = LXColor.b(baseColor);

            saturation = Math.random() < scaledTrebleRatio ? 0 : saturation;

            double alphaWave = wave(2 * distanceFromCenter - scaledTrebleRatio);

            setColor(point, LXColor.hsba(
                    hue,
                    saturation,
                    brightness,
                    alphaWave
            ));
        }
    }

}
