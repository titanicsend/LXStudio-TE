package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.Arrays;
import java.util.Collection;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

//https://www.shadertoy.com/view/7l3GDS
//Non-commercial license
public class NeonHeartShader extends FragmentShaderEffect {

    private static final int POINT_COUNT = 8;

    private final CompoundParameter speed =
            new CompoundParameter("Speed", -0.5, -0.1, -2.5)
                    .setDescription("Speed");

    private final CompoundParameter scale =
            new CompoundParameter("Scale", 0.012, 0.006, 0.024)
                    .setDescription("Scale");;

    public final CompoundParameter radius =
            new CompoundParameter("Radius", 0.012, 0.006, 0.024)
                    .setDescription("Radius");

    public final CompoundParameter thickness =
            new CompoundParameter("Thickness", 0.0035, 0.001, 0.01)
                    .setDescription("Thickness");

    public final CompoundParameter intensity =
            new CompoundParameter("Intensity", 1.3, 0.1, 5)
                    .setDescription("Intensity");

    public final CompoundParameter length =
            new CompoundParameter("Length", 0.25, 0.01, 0.5)
                    .setDescription("Length");

    public NeonHeartShader(PatternTarget target) {
        super(target);
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return Arrays.asList(speed, scale, radius, thickness, intensity, length);
    }

    @Override
    public double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
        double[] uv = divideArrays(fragCoordinates, resolution);
        double widthHeightRatio = resolution[0] / resolution[1];
        double[] centre = new double[]{0.5, 0.5};
        double[] pos = subtractArrays(centre, uv);
        pos[1] /= widthHeightRatio;
        pos[1] += 0.03;

        double dist = getSegment(timeSeconds, pos, 0.0);
        double glow = getGlow(dist, radius.getValue(), intensity.getValue());

        double[] col = multiplyArray(glow, new double[]{1.0, 0.05, 0.3});

        dist = getSegment(timeSeconds, pos, 3.4);
        glow = getGlow(dist, radius.getValue(), intensity.getValue());

        col = addArrays(col, multiplyArray(glow, new double[]{0.1, 0.4, 1.0}));

        col = Arrays.stream(col)
                .map(v -> 1 - exp(-v))
                .toArray();

        col = Arrays.stream(col)
                .map(v -> pow(v, 0.4545))
                .toArray();

        return col;
    }

    private double sdBezier(double[] pos, double[] A, double[] B, double[] C) {
        double[] a = subtractArrays(B, A);
        double[] b = addArrays((subtractArrays(A, multiplyArray(2, B))), C);
        double[] c = multiplyArray(2, a);
        double[] d = subtractArrays(A, pos);

        double kk = 1.0 / dotProduct(b, b);
        double kx = kk * dotProduct(a, b);
        double ky = kk * (2 * dotProduct(a, a) + dotProduct(d, b)) / 3.0;
        double kz = kk * dotProduct(d, a);

        double res;

        double p = ky - kx*kx;
        double p3 = p*p*p;
        double q = kx*(2.0*kx*kx - 3.0*ky) + kz;
        double h = q*q + 4.0*p3;

        if(h >= 0.0){
            h = sqrt(h);

            double[] x = new double[]{(h - q)/2, (-h - q)/2};
            double[] uv1 = new double[]{signum(x[0]), signum(x[1])};
            double[] uv2 = new double[]{pow(abs(x[0]), 1/3.), pow(abs(x[1]), 1/3.)};
            double[] uv = multiplyArrays(uv1, uv2);
            double t = uv[0] + uv[1] - kx;
            t = clamp(t, 0.0, 1.0);

            double[] qos = addArrays(d, multiplyArray(t, addArrays(c, multiplyArray(t, b))));
            res = vectorLength(qos);
        } else {
            double z = sqrt(-p);
            double v = acos(q / (p * z * 2.0) ) / 3.0;
            double m = cos(v);
            double n = sin(v)*1.732050808;
            double[] t = addToArray(-kx, multiplyArray(z, new double[]{m + m, -n - m, n - m}));
            t = Arrays.stream(t)
                    .map(value -> clamp(value, 0.0, 1.0))
                    .toArray();

            double[] qos = addArrays(d, multiplyArray(t[0], addArrays(multiplyArray(t[0], b), c)));
            double dis = dotProduct(qos,qos);

            res = dis;

            qos = addArrays(d, multiplyArray(t[1], addArrays(multiplyArray(t[1], b), c)));
            dis = dotProduct(qos,qos);
            res = min(res,dis);

            qos = addArrays(d, multiplyArray(t[2], addArrays(multiplyArray(t[2], b), c)));
            dis = dotProduct(qos,qos);
            res = min(res,dis);

            res = sqrt(res);
        }
        return res;
    }

    private double[] getHeartPosition(double t){
        return new double[] {16.0 * sin(t) * sin(t) * sin(t),
                -(13.0 * cos(t) - 5.0 * cos(2.0*t) - 2.0 * cos(3.0*t) - cos(4.0*t))};
    }

    private double getGlow(double dist, double radius, double intensity){
        return pow(radius * dist, intensity);
    }

    private double getSegment(double t, double[] pos, double offset){
        double[][] points = new double[POINT_COUNT][2];
        for(int i = 0; i < POINT_COUNT; i++){
            points[i] = getHeartPosition(offset + i * length.getValue() + fract(speed.getValue() * t) * 6.28);
        }

        double[] c = multiplyArray(.5, addArrays(points[0], points[1]));

        double[] c_prev;
        double light = 0.;
        double eps = 1e-10;

        for (int i = 0; i < POINT_COUNT - 1; i++) {
            c_prev = c;
            c = multiplyArray(.5, addArrays(points[i], points[i+1]));
            double d = sdBezier(pos, multiplyArray(scale.getValue(), c_prev), multiplyArray(scale.getValue(), points[i]), multiplyArray(scale.getValue(), c));
            double e = i > 0 ? vectorDistance(pos, multiplyArray(scale.getValue(), c_prev)) : 1000.;
            light += 1. / max(d - thickness.getValue(), eps);
            light -= 1. / max(e - thickness.getValue(), eps);
        }

        return max(0.0, light);
    }

}
