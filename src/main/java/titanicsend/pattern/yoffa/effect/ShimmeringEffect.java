package titanicsend.pattern.yoffa.effect;

import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.TEColor;

import java.util.Collection;
import java.util.List;

@LXCategory("Edge FG")
public class ShimmeringEffect extends PatternEffect {


    public enum Mode {
        FOREGROUND("Foreground color"),
        GRADIENT("Current gradient color");
        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    public final EnumParameter<Mode> mode =
        (EnumParameter<Mode>) new EnumParameter<Mode>("Mode", Mode.FOREGROUND)
            .setDescription("Color Mode")
            .setWrappable(false);
    private int lastBeat;

    public ShimmeringEffect(PatternTarget target) {
        super(target);

        TEPerformancePattern.TECommonControls ctl = pattern.getControls();
        ctl.setRange(TEControlTag.SIZE, 0.1, 0.01, 1.0);  // pulse length
        ctl.setRange(TEControlTag.WOW2, 0.5, 0, 1.0);  //background brightness

        // color mode (in Wow1 control position)
        ctl.setControl(TEControlTag.WOW1,mode);
    }

    @Override
    public void onPatternActive() {
        lastBeat = 0;
    }

    @Override
    public void run(double deltaMs) {

        boolean useGradient = mode.getEnum() == Mode.GRADIENT;
        int baseColor = pattern.calcColor();
        double pulseLength = pattern.getSize();
        double bgBrightness = pattern.getWow2();
        double sysBri = pattern.getBrightness();

        double basis = getTempo().basis();
        int beatCount = getTempo().beatCount();
        int beatsPerMeasure = getTempo().beatsPerMeasure.getValuei();

        if (beatCount > lastBeat && beatCount % beatsPerMeasure == 0) {
            lastBeat = getTempo().beatCount();
        }

        for (LXPoint point : getPoints()) {
            double distanceFromTarget = getDistanceFromTarget(point, basis);
            if (useGradient) baseColor = pattern.getGradientColor((float) distanceFromTarget);

            int alpha = 255;
            if ((distanceFromTarget > 0 && beatCount % beatsPerMeasure == 0) ||
                    (distanceFromTarget < 0 && beatCount % beatsPerMeasure == beatsPerMeasure - 1)) {
                alpha = 0;
            }

            double brightness = Math.abs(distanceFromTarget) > pulseLength ? bgBrightness :
                0.5 + 0.5 * (1 - (Math.abs(distanceFromTarget) / pulseLength));

            int color = TEColor.setBrightness(baseColor,(float) (brightness * sysBri));
            color = TEColor.reAlpha(color,alpha);
            setColor(point, color);
        }
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of();
    }

    private double getDistanceFromTarget(LXPoint point, double basis) {
        double current;
        double target;
        int direction = (int) (Math.abs(pattern.getStaticRotationAngle()) / (.5 * Math.PI));
        target = basis;
        current = direction == 0 || direction == 2 ? point.yn : point.zn;
        if (direction > 1) {
            current = 1 - current;
        }
        return current - target;
    }

}
