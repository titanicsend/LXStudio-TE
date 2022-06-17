package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.Collection;
import java.util.List;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

//based on https://www.shadertoy.com/view/WsK3D3
public class NeonBarsShader extends FragmentShaderEffect {

    private final CompoundParameter energy =
            new CompoundParameter("Energy", 0, 0, 10);

    private final CompoundParameter speed =
            new CompoundParameter("Speed", 0, 0, 10);
    private final CompoundParameter glow =
            new CompoundParameter("Glow", 1.5, 1.5, 4);

    private final CompoundParameter glow2 =
            new CompoundParameter("Glow2", 9, 0, 15);
    private final CompoundParameter thickness =
            new CompoundParameter("Thickness", .3, 0, 1);
    private final CompoundParameter wibbleSize =
            new CompoundParameter("Wibble Size", .15, .15, .3);

    public NeonBarsShader(PatternTarget target) {
        super(target);
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {

        double[] uv = multiplyArray(1 / resolution[1], subtractArrays(fragCoordinates, multiplyArray(0.5, resolution)));
        uv = multiplyVectorByMatrix(uv, rotate2D(270));

        uv[0] -= 1.0;
        uv[0] = dotProduct(uv,uv);
        uv = multiplyArray(2, uv);
        uv[1] += 0.1;			// vscroll
        
        // lots of silly parameters, have fun :)
        double grad = 0;
        double nlines = 2.0;
        double thickness = this.thickness.getValue() * pattern.sinePhaseOnBeat();
        double wibblesize = this.wibbleSize.getValue(); //pattern.sinePhaseOnBeat()
        double glowintensity = this.glow.getValue();
        double wibblespeed = speed.getValue();
        double glowclamp = 1.0;
        double extraglow = glow2.getValue();
        double[] col = hsv2rgb(new double[]{timeSeconds * 0.025, 0.5, 0.5});
        col = SSTLines(uv, col, nlines, grad, thickness, wibblesize, wibblespeed, glowintensity, glowclamp, extraglow, timeSeconds);

        // vignette
        double[] q = divideArrays(fragCoordinates, resolution);
        return multiplyArray(0.5 + 0.5 * pow(16.0 * q[0] * q[1] * (1.0-q[0]) * (1.0-q[1]), 0.35), col);
    }

    private double[] hsv2rgb(double[] c)
    {
        double[] rgbTmp = abs(addToArray(-3, mod(addToArray(c[0] * 6.0, new double[]{0.0,4.0,2.0}), 6)));
        double[] rgb = clamp(addToArray(-1, rgbTmp), 0.0, 1.0);
        double[] mixed = new double[] {
                mix(1, rgb[0], c[1]),
                mix(1, rgb[1], c[1]),
                mix(1, rgb[2], c[1])
        };
        return multiplyArray(c[2], mixed);
    }

    private double[] SSTLines(double[] duv, double[] col2, double nl, double grad, double thickness, double wibblesize,
                              double wibblespeed, double glowintensity, double glowclamp, double extraglow, double timeSeconds)
    {
        duv[1] -= (floor(duv[0])*grad) + (duv[0]*grad);
        duv = fract(duv);
        double l1 = abs(fract((duv[0] * grad - duv[1])*nl) -0.5);
            double dd = sin(-timeSeconds * wibblespeed + duv[0] * 6.28) * wibblesize;
        l1 = min(glowclamp, (thickness + dd) / l1);
        double[] col = multiplyArray(l1 * glowintensity + (dd * extraglow), col2);
        return new double[]{
                mix(col2[0], col[0], l1),
                mix(col2[1], col[1], l1),
                mix(col2[2], col[2], l1)
        };
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
    public void onParameterChanged(LXParameter parameter) {
        if (parameter == this.energy) {
            this.wibbleSize.setNormalized(this.energy.getNormalized());
            this.glow.setNormalized(this.energy.getNormalized());
            boolean isHighEnergy = energy.getNormalized() > .7;
            if (isHighEnergy) {
                speed.setValue(10);
            } else {
                speed.setValue(0);
            }
        }
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of(energy, thickness, glow, glow2, wibbleSize, speed);
    }

}
