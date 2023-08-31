package titanicsend.pattern.will.shaders;

import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.shaders.FragmentShaderEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.awt.*;
import java.util.Collection;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

// taken from:
// https://www.shadertoy.com/view/Xs2BRc
public class RhythmicFlashingStatic extends FragmentShaderEffect {

    public static double STAR_WIDTH = 9.0;
    public static double RESOLUTION = 0.2;
    public static double STAR_COUNT_INT = 64.0;

    public RhythmicFlashingStatic(PatternTarget target) {
        super(target);

        // set reasonable initial control values
        pattern.getControls().setValue(TEControlTag.WOW1, 0.75);   // beat reactive pulse
    }

    private double cell(double[] c) {
        //        vec2 uv = fract(c);
        double[] uv = fract(c);
        //        c -= uv;
        double[] newC = subtractArrays(c, uv);

        //        return (3.0 - length(uv)) * step(fract( sin(c.x + c.y*50.0) *1000.0), 0.04); // 3.0 :: star width and fade strength, 0.04 :: star count
        double cx = newC[0];
        double cy = newC[1];
        double firstTerm = STAR_WIDTH - vectorLength(uv);
        double trigTerm = fract(sin(cx + cy * 50.0) * 1000.0);
        return firstTerm * step(trigTerm, min(0.9995, 1.0 - pattern.getQuantity()));
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {

        double[] projection = new double[]{fragCoordinates[0] / RESOLUTION, fragCoordinates[1] / RESOLUTION};

        double projX = fract(projection[0]) / 4.0;
        double projY = pow(projection[1], 0.5);

        //        float time = -0.4 * iTime; //reverse or forward speed
        double time = 0.05 * timeSeconds;

        //            vec2 coord = vec2(projY, projX) * 256.; // *512 :: star count, adjust star-count and scale for scaling along Y axis
        double[] coord = new double[]{projX * STAR_COUNT_INT, projY * STAR_COUNT_INT};

        //            vec2 delta = vec2(time * 16.0, 0); // time*7.0 :: speed2 of stars, adjust both^^ speeds for length of stars
        double[] delta = new double[]{time, 0};

        //            float c = Cell(coord -= delta);
        double[] diff = subtractArrays(coord, delta);
        coord = diff;
        double c = cell(coord);

        //            c += Cell(coord -= delta);
        double[] diffAgain = subtractArrays(coord, delta);
        c += cell(diffAgain);

        //            color[rgb]= c * projection.y; // * 16.0; // fade distance at bottom of screen
        double color = fract(c * projection[1]);

        // get a color from the current gradient
        int paletteColor = this.pattern.getGradientColor((float) color);

        // calculate and scale beat reactive brightness
        double brightness = (1.0 - color) * (1.0 - (random() * this.getTempo().basis() * pattern.getWow1()));
        brightness = clamp(8 * brightness * brightness, 0, 1);

        // set final output color and alpha
        float[] newColor = new Color(paletteColor).getRGBColorComponents(null);
        double[] outColor = new double[3];
        outColor[0] = newColor[0] * brightness;
        outColor[1] = newColor[1] * brightness;
        outColor[2] = newColor[2] * brightness;

        return outColor;
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return null;
    }
}
