package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.awt.*;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

//https://www.shadertoy.com/view/3sc3Df
public class NeonCellsShader extends FragmentShaderEffect {

    private final CompoundParameter energy =
            new CompoundParameter("Energy", 0, 0, 1);

    private final BooleanParameter doubleLayer =
            new BooleanParameter("Double Layer", false);

    private final BooleanParameter doubleSpeed =
            new BooleanParameter("Double Speed", false);

    private final CompoundParameter glow =
            new CompoundParameter("Glow", 0.1, 0.1, .5);

    private final CompoundParameter width =
            new CompoundParameter("Width", .5, .5, 1.5);

    public NeonCellsShader(PatternTarget target) {
        super(target);
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
        double[] uv = multiplyArray(1 / resolution[1],
                subtractArrays(fragCoordinates, multiplyArray(.5, resolution)));

        double phase = PI * pattern.getTempo().getCompositeBasis();
        double speedMultiplier = doubleSpeed.getValueb() ? 2 : 1;

        double d = pat(uv, 5, 2, speedMultiplier, glow.getValue(), phase);
        if (doubleLayer.getValueb()) {
            d *= pat(uv, 3.0, 7.0, 0.25 * speedMultiplier, glow.getValue(), phase);
        }

        float[] color = new Color(pattern.getSwatchColor(TEPattern.ColorType.PRIMARY)).getRGBColorComponents(null);
        return multiplyArray(.5 / d, new double[]{color[0], color[1], color[2]});
    }

    private double pat(double[] uv, double p, double q, double speed, double glow, double phase) {
        q += (0.5 + sin(phase * speed) * width.getValue());
        double z = cos(q * PI * uv[0]) * cos(p * PI * uv[1]) + cos(q * PI * uv[1]) * cos(p * PI * uv[0]);
        return abs(z) * (1 / glow);
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
        if (parameter == this.energy) {
            this.glow.setNormalized(this.energy.getNormalized());
            boolean isHighEnergy = energy.getNormalized() > .7;
            doubleSpeed.setValue(isHighEnergy);
            doubleLayer.setValue(isHighEnergy);
            width.setNormalized(Math.min(energy.getNormalized() / .3, 1));
        }
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of(energy, doubleLayer, doubleSpeed, glow, width);
    }
}
