package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.Collection;
import java.util.List;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

//https://www.shadertoy.com/view/4lB3DG
public class NeonSnakeShader extends FragmentShaderEffect {

    public final BooleanParameter trebleGlow =
            new BooleanParameter("TrebleGlow", false);

    public final BooleanParameter beatDisperse =
            new BooleanParameter("BeatDisperse", false);

    public NeonSnakeShader(PatternTarget target) {
        super(target);

        pattern.controls.setRange(TEControlTag.SIZE, 0.3, 0.2, 0.9);  // dispersion/scale
        pattern.controls.setRange(TEControlTag.QUANTITY, 1, .5, 8);   // snake bend frequency
        pattern.controls.setRange(TEControlTag.WOW1, 1, .1, 2);   // glow
        pattern.controls.setRange(TEControlTag.WOW2, 0, 0, .25);    // beat reactivity
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {

        // Quantity controls the snake's bend frequency
        double bend = pattern.getQuantity();

        // Wow1 controls the base glow level
        double glow = pattern.getWow1();

        // Wow2 makes the snake expand and contract a little with the beat
        double dispersion = pattern.getSize() + pattern.getWow2() * sin(PI * pattern.getTempo().basis());

        // normalize coordinates
        double[] uv = divideArrays(fragCoordinates, resolution);
        uv[1] -= pattern.getYPos() + 0.25;  // offset y to roughly center snake vertically
        uv = multiplyArray(3, uv); // scale pattern properly for car

        // get current calculated palette color (plus alpha, which we'll fill in later)
        double[] waveColor = new double[4];
        colorToRGBArray(pattern.calcColor(), waveColor);

        double brightness = 0;
        for (int i = 0; i < 13; i++) {
            uv[1] += dispersion * sin(uv[0] + i / 1.5 + timeSeconds);
            double waveWidth = glow * abs(1.0 / (150 * uv[1]));
            brightness += waveWidth;
        }

        // gamma correct brightness and use it as alpha
        brightness = clamp(brightness, 0, 1);
        waveColor[3] = brightness * brightness;
        return waveColor;
    }


    @Override
    public Collection<LXParameter> getParameters() {
        return null;
    }
}
