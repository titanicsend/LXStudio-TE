package titanicsend.pattern.will.shaders;

import heronarts.lx.Tempo;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.effect.shaders.FragmentShaderEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static titanicsend.util.TEMath.*;
import static titanicsend.util.TEMath.multiplyArrays;

public class MatrixScrolling extends FragmentShaderEffect {

    public final CompoundParameter radius =
            new CompoundParameter("Blast radius", 2 * 100000, 0, 6 * 100000)
                    .setDescription("");

    public final CompoundParameter centering =
            new CompoundParameter("Centering", 0.5, 0., 1.)
                    .setDescription("");

    public final CompoundParameter blockSize =
            new CompoundParameter("Block size", 0.02, 0.01, 0.05)
                    .setDescription("");

    public final CompoundParameter speed =
            new CompoundParameter("Speed", 1, -2., 2)
                    .setDescription("");

    public final BooleanParameter beatReactive = new BooleanParameter("Beat reactive", true);

//    public final LinkedColorParameter color = new LinkedColorParameter("Color")
//            .setDescription("Color of blocks");

    public MatrixScrolling(PatternTarget target) {
        super(target);
        centering.setPolarity(LXParameter.Polarity.BIPOLAR);
        speed.setPolarity(LXParameter.Polarity.BIPOLAR);

        //protected LinkedColorParameter registerColor(String label, String path, TEPattern.ColorType colorType, String description) {
//            LinkedColorParameter lcp = new LinkedColorParameter(label)
//                    .setDescription(description);
//        this.addParameter(path, lcp);
//        color.mode.setValue(LinkedColorParameter.Mode.PALETTE);
        //color.index.setValue(colorType.index);
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
//        TE.log("Resolution: (%f, %f)", resolution[0], resolution[1]);
        Random random = new Random();

        // randomly displace coordinates on every measure start
        double measureProgress = 1.0 - this.pattern.getLX().engine.tempo.getBasis(Tempo.Division.WHOLE); // 1 when we start measure, 0 when we finish
        measureProgress *= measureProgress; // steeper curve

        if (radius.getValue() > 0) {
            int offset = (int)(measureProgress * random.nextInt( (int)(radius.getValue()) ));
            fragCoordinates = addToArray(offset, fragCoordinates);
        }

        //        vec3 v = vec3(u, 1) / iResolution - 0.5;
        double[] v = new double[]{
                fragCoordinates[0] / resolution[0] - centering.getValue(),
                fragCoordinates[1] / resolution[1] - centering.getValue(),
                1.0
        };

        //        vec3 s = 0.3 / abs(v);
        double[] s = new double[3];
        for (int m = 0; m < 3; m++) {
            s[m] = 0.3 / Math.abs(v[m]);
        }
        //        s.z = min(s.y, s.x);
        s[2] = Math.min(s[0], s[1]);

        //        vec3 t = s.y < s.x ? v.xzz : v.zyz;
        double[] t = new double[3];
        if (s[1] < s[0]) {
            t = new double[]{ v[0], v[2], v[2] };
        } else {
            t = new double[]{ v[2], v[1], v[2] };
        }

        //        vec3 i = ceil(8e2 * s.z * t);
        double[] i = new double[3];
        for (int m = 0; m < 3; m++) {
            i[m] = Math.ceil(8e2 * s[2] * t[m]) * blockSize.getValue();  //        i *= 0.1;
        }

        //        vec3 j = fract(i);
        double[] j = fract(i);

        //        i -= j;
        i = subtractArrays(i, j);

        //        float b = (9. + 8. * sin(i).x);
        double b = (9. + 8. * Math.sin(i[0]));

        //        int tb = int(iTime * b);
        int tb = (int)(timeSeconds * speed.getValue() * b);

        //        vec3 p = vec3(9, tb, 0) + i;
        double[] p = new double[]{ i[0] + 9.0, i[1] + tb, i[2]};

        // #define R fract(1e2 * sin(p.x * 5. + p.y))
        //        color.g = R / s.z;
        double[] color = new double[3];
        double R = fract(1e2 * Math.sin(p[0] * 5. + p[1]));
        color[1] = R / s[2];

        //        p *= j;
        p = multiplyArrays(p, j);

        //        bool oneOrZero = R > 0.5 && j.x < 0.6 && j.y <.8;
        boolean oneOrZero = R > 0.5 && j[0] < 0.6 && j[1] < 0.8;

        //        color *= (oneOrZero ? 1.0 : 0.0);
        color = multiplyArray(oneOrZero ? 1.0 : 0.0, color);

        // apply beat
        if (beatReactive.getValueb()) {
            double oneOnBeatAndLessNearEnd = 1.0 - this.pattern.getLX().engine.tempo.basis();
            color = multiplyArray(oneOnBeatAndLessNearEnd, color);
        }

        return color;
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of(radius, centering, blockSize, speed, beatReactive);
    }
}
