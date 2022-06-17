package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.*;
import static titanicsend.util.TEMath.floor;
import static titanicsend.util.TEMath.*;

//based on https://www.shadertoy.com/view/3sXSD2
public class ElectricShader extends FragmentShaderEffect {

    private static final double K1 = .366025404;
    private static final double K2 = .211324865;

    private final CompoundParameter energy =
            new CompoundParameter("Energy", 0, 0, 1);

    private final CompoundParameter curve =
            new CompoundParameter("Curve", .2, 0, 5);

    private final CompoundParameter dispersion =
            new CompoundParameter("Disperse", 2, 1, 10);

    private final CompoundParameter thickness =
            new CompoundParameter("Thickness", .25, .25, 5);

    private final CompoundParameter haze =
            new CompoundParameter("Haze", .075, 0.075, .5);

    public ElectricShader(PatternTarget target) {
        super(target);
    }

    private double[] hash(double[] p) {
        p = new double[] {
                dotProduct(p, new double[]{127.1, 311.7}),
                dotProduct (p, new double[]{269.5, 183.3})
        };
        return addToArray(-1, multiplyArray(dispersion.getValue(),
                fract(Arrays.stream(p).map(v -> Math.sin(v) * 43758.5453123).toArray())));
    }

    private double noise (double[] p) {
        double[] i = floor(addToArray((p[0] + p[1]) * K1, p));
        double[] a = addToArray((i[1] + i[1]) * K2, subtractArrays(p, i));
        double[] o = new double[]{
                step(a[1], a[0]),
                step(a[0], a[1])
        };
        double[] b = addToArray(K2, subtractArrays(a, o));
        double[] c = addToArray(-1 + 2 * K2, a);

        double[] hTmp = {dotProduct(a, a), dotProduct(b, b), dotProduct(c, c)};
        double[] h = Arrays.stream(hTmp).map(v -> max(.5 - v, 0)).toArray();

        double[] nTmp = {
                dotProduct(a, hash(i)),
                dotProduct(b, hash(addArrays(i, o))),
                dotProduct(c, hash(addToArray(1, i)))
        };

        double[] n = new double[3];
        for (int j = 0; j < h.length; j++) {
            n[j] = pow(h[j], 4) * nTmp[j];
        }

        return dotProduct(n, new double[]{70, 70, 70});
    }

    private double fbm(double[] p) {
        double[][] rot = rotate2D(27.5);
        double d = noise(p);
        p = multiplyVectorByMatrix(p, rot);
        d += .5 * d;
        p = multiplyVectorByMatrix(p, rot);
        d += .25 * d;
        p = multiplyVectorByMatrix(p, rot);
        d += .125 * d;
        p = multiplyVectorByMatrix(p, rot);
        d += .0625 * d;
        d /= (1. + .5 + .25 + .125 + .0625);
        return .5 + .5 * d;
    }

    private double[] toPolar(double[] p) {
        double r = vectorLength(p);
        double a = atan2(p[1], p[0]);
        return new double[]{r, a};
    }

    private double[] polar2cart(double[] polar) {
        double x = polar[0] * cos(polar[1]);
        double y = polar[0] * sin(polar[1]);
        return new double[]{x, y};
    }

    private double[] mapToScreen (double[] p, double scale, double[] resolution) {
        double[] res = p;
        res = addToArray(-1, multiplyArray(2, res));
        res[0] *= resolution[0] / resolution[1];
        return multiplyArray(scale, res);
    }

    private double[][] rotate2D(double angle)
    {
        angle = Math.toRadians(angle);
        return new double[][]{
                {cos(angle), -sin(angle)},
                {sin(angle), cos(angle)}
        };
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
        double[] uv = mapToScreen(divideArrays(fragCoordinates, resolution), 2.5, resolution);

        uv = multiplyVectorByMatrix(uv, rotate2D(12*timeSeconds));
        double len = vectorLength(uv);

        // distort UVs a bit
        uv = toPolar(uv);
        uv[1] += curve.getValue() * (.5 + .5 * sin(cos (uv[0]) * len));
        uv = polar2cart (uv);

        double d1 = abs (uv[0] * thickness.getValue() / (uv[0] + fbm (addToArray(1.25 * timeSeconds, uv))));
        double d2 = abs (uv[1] * thickness.getValue() / (uv[1] + fbm (addToArray(-1.5 * timeSeconds, uv))));
        double[] col = {0, 0, 0};
        col = addArrays(col, multiplyArray(d1 * haze.getValue(), new double[]{.1, .8, 2}));
        col = addArrays(col, multiplyArray(d2 * haze.getValue(), new double[]{2, .1, .8}));
        return col;
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
        if (parameter == this.energy) {
            this.dispersion.setNormalized(energy.getNormalized());
            this.haze.setNormalized(energy.getNormalized());
        }
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of(energy, curve, dispersion, haze, thickness);
    }
}
