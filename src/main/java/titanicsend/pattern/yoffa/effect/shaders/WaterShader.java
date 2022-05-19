package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.Collection;
import java.util.List;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

//https://www.shadertoy.com/view/MdlXz8
public class WaterShader extends FragmentShaderEffect {

    public final CompoundParameter tile =
            new CompoundParameter("Tiling", 0, 1, 4)
                    .setDescription("");

    public final CompoundParameter speed =
            new CompoundParameter("Speed", 1, 1, 10)
                    .setDescription("");

    public final CompoundParameter intensity =
            new CompoundParameter("Inten1", 5, 1, 20)
                    .setDescription("Intensity");

    public final CompoundParameter intensity2 =
            new CompoundParameter("Inten2", .005, .001, .010)
                    .setDescription("Intensity but like different though");
    private static final double TAU = 6.28318530718;

    public WaterShader(PatternTarget target) {
        super(target);
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
        double time = timeSeconds * .5+23.0;
        // uv should be the 0-1 uv of texture...
        double[] uv = divideArrays(fragCoordinates, resolution);

        double[] p = addToArray(-250, mod(multiplyArray(tile.getValue() * TAU, uv), tile.getValue() * TAU));
        double[] i = new double[]{p[0], p[1]};
        double c = 1.0;
        double inten = intensity2.getValue();

        for (int n = 0; n < intensity.getValue(); n++)
        {
            double t = time * (speed.getValue() - (3.5 / (n+1)));
            i = addArrays(p, new double[]{cos(t - i[0]) + sin(t + i[1]), sin(t - i[1]) + cos(t + i[0])});
            c += 1.0/vectorLength(new double[]{p[0] / (sin(i[0]+t)/inten),p[1] / (cos(i[1]+t)/inten)});
        }
        c /= intensity.getValue();
        c = 1.17-pow(c, 1.4);
        double colourValue = pow(abs(c), 8.0);
        double[] colour = new double[]{colourValue, colourValue, colourValue};
        colour = clamp(addArrays(colour, new double[]{0.0, 0.35, 0.5}), 0.0, 1.0);

        return colour;
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of(tile, speed, intensity, intensity2);
    }
}
