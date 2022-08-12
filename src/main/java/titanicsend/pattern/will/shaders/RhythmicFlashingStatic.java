package titanicsend.pattern.will.shaders;

import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.effect.shaders.FragmentShaderEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.awt.*;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static titanicsend.util.TEMath.*;
import static titanicsend.util.TEMath.multiplyArrays;

import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.Collection;
import java.util.List;

import static titanicsend.util.TEMath.*;
import static java.lang.Math.*;

// taken from:
// https://www.shadertoy.com/view/Xs2BRc
public class RhythmicFlashingStatic extends FragmentShaderEffect {

    public static double STAR_WIDTH = 9.0;
    public static double RESOLUTION = 4096.0;
    public static double STAR_COUNT_INT = 64.0;

    public final CompoundParameter energy =
            new CompoundParameter("Energy", 0.4, 0, 1.0)
                    .setDescription("");

    // TODO(will): use color palette instead of hardcoded blue color
    public final BooleanParameter colorParameter = new BooleanParameter("Color", false);

    public RhythmicFlashingStatic(PatternTarget target) {
        super(target);
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
        return firstTerm * step(trigTerm, energy.getValue());
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
        //        TE.log("coords: (%f, %f)\n", fragCoordinates[0], fragCoordinates[1]); //, fragCoordinates[2]);
        //        vec2 projection = fragCoord.xy / vec2(1024.0,1024.0); // TEXCOORD divided by image resolution
        double[] projection = new double[]{fragCoordinates[0] / RESOLUTION, fragCoordinates[1] / RESOLUTION};

        //        float projX = fract(projection.x) / 4.; // XY direction divided by scale of stars along X
        double projX = fract(projection[0]) / 4.0;

        //        float projY = pow(projection.y, 0.5); // 0.014 :: speed1 of stars
        double projY = pow(projection[1], 0.5);

        //        float time = -0.4 * iTime; //reverse or forward speed
        double time = -0.4 * timeSeconds;

        //        vec3 color;
        double[] color = new double[3];
        //
        //        for(int rgb=0; rgb<3; rgb++)
        for (int i = 0; i < 3; i++) {
            //            time -= 0.01; // color shift distance
            time -= 0.01;

            //            vec2 coord = vec2(projY, projX) * 256.; // *512 :: star count, adjust star-count and scale for scaling along Y axis
            double[] coord = new double[]{projX * STAR_COUNT_INT, projY * STAR_COUNT_INT};

            //            vec2 delta = vec2(time * 16.0, 0); // time*7.0 :: speed2 of stars, adjust both^^ speeds for length of stars
            double[] delta = new double[]{time * 16.0, 0};

            //            float c = Cell(coord -= delta);
            double[] diff = subtractArrays(coord, delta);
            coord = diff;
            double c = cell(coord);

            //            c += Cell(coord -= delta);
            double[] diffAgain = subtractArrays(coord, delta);
            c += cell(diffAgain);

            //            color[rgb]= c * projection.y; // * 16.0; // fade distance at bottom of screen
            color[i] = c * projection[1];
        }

        //        fragColor = vec4(color, 1.);
        double[] newColor = new double[]{1.0 - color[0], 1.0 - color[1], 1.0 - color[2]};

        // now adjust for beat position
        double oneOnBeatAndLessNearEnd = 1.0 - this.getTempo().basis();
        double randFloat = Math.random() + energy.getValue();
        newColor = multiplyArray(oneOnBeatAndLessNearEnd * randFloat, newColor);

        // should we color the pixel a color?
        if (colorParameter.getValueb()) {
            int paletteColor = this.pattern.getSwatchColor(TEPattern.ColorType.PRIMARY);
            float[] c = new Color(paletteColor).getRGBColorComponents(null);
            newColor = multiplyArrays(new double[]{c[0], c[1], c[2]}, newColor);
        }

        return newColor;
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of(energy, colorParameter);
    }
}
