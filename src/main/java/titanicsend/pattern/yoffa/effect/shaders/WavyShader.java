package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TECommonControls;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.Collection;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

//https://www.shadertoy.com/view/4lB3DG
public class WavyShader extends FragmentShaderEffect {

    public WavyShader(PatternTarget target) {
        super(target);

        TECommonControls ctl = pattern.getControls();

        ctl.setRange(TEControlTag.SPEED, 0, -5, 5);
        ctl.setValue(TEControlTag.SPEED, 0.5);

        ctl.setRange(TEControlTag.SIZE, 1, 2, 0.025);  // scale
        ctl.setRange(TEControlTag.QUANTITY, 0, 0, 10); // intensity
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
        fragCoordinates = divideArrays(fragCoordinates, resolution);
        double[] a = addToArray(-.5, fragCoordinates);
        timeSeconds = -timeSeconds;

        double scale = 10 * pattern.getSize();
        double z = atan2(-a[1], -a[0]) * 3;
        double v = cos(z + sin(timeSeconds * .1)) + .5 + sin(fragCoordinates[0] * scale + timeSeconds * 1.3)
            * .4 * pattern.getQuantity();
        double x = 1.2 + cos(z - timeSeconds * .2) + sin(fragCoordinates[1] * scale + timeSeconds * 1.5) * .5;

        // No magic here, just adapting the old hand-tuned color calculation to produce a
        // brightness value that we can use to interpolate our gradient.
        double k = min(x,
            min(sin(v * 4.) * .25 + x * .5, sin(v * 2.) * .3 + x * .5));

        // roughly reproduce the effect of colorize
        double[] col = new double[4];
        colorToRGBArray(pattern.getGradientColor((float) k),col);
        col[3] = clamp(k,0,1);

        return col;
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return null;
    }
}
