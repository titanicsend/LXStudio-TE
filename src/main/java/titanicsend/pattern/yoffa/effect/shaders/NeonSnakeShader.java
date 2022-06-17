package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.Collection;
import java.util.List;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

//https://www.shadertoy.com/view/4lB3DG
public class NeonSnakeShader extends FragmentShaderEffect {

    public final CompoundParameter ySquish =
            new CompoundParameter("ySquish", 1.3, 1, 5);

    public final CompoundParameter dispersion =
            new CompoundParameter("Dispersion", .3, .1, 1.5);

    public final CompoundParameter glow =
            new CompoundParameter("Glow", 1, .1, 1);

    public final BooleanParameter trebleGlow =
            new BooleanParameter("TrebleGlow", false);

    public final BooleanParameter beatDisperse =
            new BooleanParameter("BeatDisperse", false);

    public NeonSnakeShader(PatternTarget target) {
        super(target);
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
        if (trebleGlow.getValueb()) {
            double trebleLevel = pattern.getTrebleLevel();
            glow.setNormalized(.25 + .75 * trebleLevel);
        }
        if (beatDisperse.getValueb()) {
            dispersion.setNormalized(pattern.sinePhaseOnBeat() * .25);
        }

        double[] uv = divideArrays(fragCoordinates, resolution);
        double[] waveColor = new double[]{0, 0, 0};

        uv = addToArray(-1.3, multiplyArray(2.4 * ySquish.getValue(), uv));
        uv[1] += .1;
        for (int i = 0; i < 13; i++) {
            uv[1] += (dispersion.getValue() * sin(uv[0] + i / 1.5 + timeSeconds));
            double waveWidth = glow.getValue() * abs(1.0 / (150 * uv[1]));
            waveColor = addArrays(waveColor, new double[] {
                    waveWidth * 1.7 * sin(timeSeconds - 1.5),
                    waveWidth,
                    waveWidth * 1.5 * sin(timeSeconds + 1)
            });
        }
        return waveColor;
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of(ySquish, dispersion, glow, trebleGlow, beatDisperse);
    }
}
