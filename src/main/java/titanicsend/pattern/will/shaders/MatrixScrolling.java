package titanicsend.pattern.will.shaders;

import heronarts.lx.LX;
import heronarts.lx.Tempo;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.shaders.FragmentShaderEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.TE;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static titanicsend.util.TEMath.*;
import static titanicsend.util.TEMath.multiplyArrays;

public class MatrixScrolling extends FragmentShaderEffect {

    SplittableRandom random;

    double[] origin;

    public MatrixScrolling(PatternTarget target) {
        super(target);

        TEPerformancePattern.TECommonControls ctl = pattern.getControls();

        ctl.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
        ctl.setValue(TEControlTag.SPEED, 0.5);

        ctl.setRange(TEControlTag.SIZE, 0.02, 0.05, 0.01);  // block size
        ctl.setRange(TEControlTag.WOW1, 0.0, 0, 1);         // blast radius
        ctl.setRange(TEControlTag.WOW2, 0.0, 0, 1);         // beat reactivity

        origin = new double[] {0.5, 0.5};

        // about twice as fast as the java.util.Random class
        random = new SplittableRandom();
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {

        // rotate
        fragCoordinates = rotate2D(fragCoordinates, origin);

        // randomly displace coordinate on every measure start
        if (pattern.getWow1() > 0) {
            double measureProgress = 1.0 - this.pattern.getLX().engine.tempo.getBasis(Tempo.Division.WHOLE); // 1 when we start measure, 0 when we finish
            measureProgress *= measureProgress * measureProgress;

            double k = 0.0005 + pattern.getWow1();

            fragCoordinates[0] += measureProgress * random.nextDouble(k);
            fragCoordinates[1] += measureProgress * random.nextDouble(k);
        }

        //        vec3 v = vec3(u, 1) / iResolution - 0.5;
        double[] v = new double[]{
            fragCoordinates[0] / resolution[0] - 0.5 + pattern.getXPos(),
            fragCoordinates[1] / resolution[1] - 0.5 + pattern.getYPos(),
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
            t = new double[]{v[0], v[2], v[2]};
        } else {
            t = new double[]{v[2], v[1], v[2]};
        }

        //        vec3 i = ceil(8e2 * s.z * t);
        double[] i = new double[3];
        for (int m = 0; m < 3; m++) {
            i[m] = Math.ceil(8e2 * s[2] * t[m]) * pattern.getSize();  //        i *= 0.1;
        }

        //        vec3 j = fract(i);
        double[] j = fract(i);

        //        i -= j;
        i = subtractArrays(i, j);

        //        float b = (9. + 8. * sin(i).x);
        double b = (9. + 8. * Math.sin(i[0]));

        //        int tb = int(iTime * b);
        int tb = (int) (timeSeconds * b);

        //        vec3 p = vec3(9, tb, 0) + i;
        double[] p = new double[]{i[0] + 9.0 , i[1] + tb, i[2]};

        // #define R fract(1e2 * sin(p.x * 5. + p.y))
        //        color.g = R / s.z;
        double[] color = new double[3];
        double R = fract(100 * Math.sin(p[0] * 5. + p[1]));

        //color[2] = R / s[2];
        float  k = (float) (R / s[2]);
        int swatchRGB = pattern.getGradientColor(k);
        float rn = k * (float) (0xff & LXColor.red(swatchRGB)) / 255f;
        float gn = k * (float) (0xff & LXColor.green(swatchRGB)) / 255f;
        float bn = k * (float) (0xff & LXColor.blue(swatchRGB)) / 255f;
        color[0] = rn;
        color[1] = gn;
        color[2] = bn;

        //TE.log("green val: %f, my green: %f", R / s[2], color[1]);

        //        p *= j;
        p = multiplyArrays(p, j);

        //        bool oneOrZero = R > 0.5 && j.x < 0.6 && j.y <.8;
        boolean oneOrZero = R > 0.5 && j[0] < 0.6 && j[1] < 0.8;

        //        color *= (oneOrZero ? 1.0 : 0.0);
        // slight bump to brightness b/c original calculation came out too dark
        // after switch to view normalized coordinates
        color = multiplyArray(oneOrZero ? 1.25 : 0.0, color);

        // apply beat reactivity, amount controlled by getWow2
        double oneOnBeatAndLessNearEnd = 1.0 - (pattern.getWow2() * this.pattern.getLX().engine.tempo.basis());
        color = multiplyArray(oneOnBeatAndLessNearEnd, color);

        return color;
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return null;
    }
}
