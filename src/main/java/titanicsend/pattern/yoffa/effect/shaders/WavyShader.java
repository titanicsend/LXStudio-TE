package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.Collection;
import java.util.List;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

//https://www.shadertoy.com/view/4lB3DG
public class WavyShader extends FragmentShaderEffect {

    public final CompoundParameter intensity =
            new CompoundParameter("Intensity", 0, 0, 10)
                    .setDescription("");

    public final CompoundParameter speed =
            new CompoundParameter("Speed", 1, 1, 10)
                    .setDescription("");

    public final CompoundParameter colors =
            new CompoundParameter("Colors", 1, 1, 20)
                    .setDescription("");

    public WavyShader(PatternTarget target) {
        super(target);
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
        fragCoordinates = divideArrays(fragCoordinates, resolution);
        double[] a = addToArray(.5, multiplyArray(-1, fragCoordinates));
        double z  = atan2(a[1], a[0]) * 3;
        double v = cos(z + sin(timeSeconds * .1)) + .5 + sin(fragCoordinates[0] * 10 + timeSeconds * 1.3) * .4 * intensity.getValue();

        double x = 1.2 + cos(z - timeSeconds * .2) + sin(fragCoordinates[1] * 10. + timeSeconds * 1.5 * speed.getValue()) * .5;
        return new double[] {
                x,
                sin(v * 4.) * .25 * colors.getValue() + x * .5,
                sin(v * 2.) * .3 + x * .5
        };
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of(intensity, speed, colors);
    }
}
