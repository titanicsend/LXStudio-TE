package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.TEMath;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.*;
import static titanicsend.util.TEMath.*;

//https://www.shadertoy.com/view/wstGz4
//Noncommercial license
public class OutrunGridShader extends FragmentShaderEffect {

    public static final double HORIZON_Y = 0.7;

    private static final double DEG2RAD = PI/180.;
    private static final int SUPERSAMP = 8;

    private final CompoundParameter forwardSpeed =
            new CompoundParameter("Forward Speed", 4, 0, 10)
                    .setDescription("Forward speed");

    private final CompoundParameter sidewaysSpeed =
            new CompoundParameter("Sideways Speed", 2, 0, 5)
                    .setDescription("Speed at which the camera waves side to side");

    private final CompoundParameter colorChangeSpeed =
            new CompoundParameter("Color Change Speed", 1, 0, 5)
                    .setDescription("Speed at which the grid changes colors");

    private final BooleanParameter alphaWaves =
            new BooleanParameter("Alpha Waves", true)
                    .setDescription("Toggle the horizontal waves of black");

    private final BooleanParameter glowing =
            new BooleanParameter("Glowing", true)
                    .setDescription("Toggle whether the grid should glow brightly");

    public OutrunGridShader(PatternTarget target) {
        super(target);
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of(forwardSpeed, sidewaysSpeed, colorChangeSpeed, alphaWaves, glowing);
    }

    @Override
    protected double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds) {
        double[] p = new double[]{
                (fragCoordinates[0] - .5 * resolution[0]) / resolution[1],
                (fragCoordinates[1] - .5 * resolution[1]) / resolution[1]
        };

        double fragSize = 1. / resolution[1];
        double superSize = fragSize / SUPERSAMP;

        //operates on a scale of (-0.5, .5)
        double horizonY = HORIZON_Y - 0.5;

        double[] fragColor;
        if (p[1] > horizonY) {
            fragColor = new double[]{0, 0, 0, 0};
        } else {
            double intensity = 0.;

            //responsible for waving the camera around
            double[] displace = new double[]{3 * sin(sidewaysSpeed.getValue() * PI * 0.1 * timeSeconds),
                    forwardSpeed.getValue() * timeSeconds, 1.5};

            double fov = 90.0;

            for (int i = 0; i < SUPERSAMP; i++) {
                for (int j = 0; j < SUPERSAMP; j++) {
                    double[] superOffset = multiplyArray(superSize, new double[]{i, j});
                    double[] gridPosLong = revProject(
                            subtractArrays(addArrays(superOffset, p), new double[]{0, horizonY}), displace[2], fov);

                    double[] gridPos = new double[]{gridPosLong[0], gridPosLong[2]};
                    double[] grid = Arrays.stream(
                            subtractArrays(gridPos, Arrays.copyOfRange(displace, 0, 2)))
                            .map(v -> TEMath.fract(v) - .5).toArray();

                    //responsible for the dark traveling waves
                    double pattern = alphaWaves.getValueb() ? 0.7 + 0.6 * sin(gridPos[1] - 6 * timeSeconds) : 1;

                    double dist = max(grid[0] * grid[0], grid[1] * grid[1]);
                    double fade = min(1.5, pow(1.2, -vectorLength(gridPos) + 15.0));
                    double bright = 0.015 / (0.26 - dist);
                    intensity += fade * pattern * bright;
                }
            }

            double[] baseColor = new double[]{0, 10, 20};

            //responsible for the color changing
            double[] pTmp = new double[]{p[1], p[0], p[1]};
            double[] col = Arrays.stream(addArrays(baseColor, pTmp))
                    .map(v -> .5 + .5 * cos(timeSeconds * colorChangeSpeed.getValue() + v))
                    .toArray();


            intensity /= SUPERSAMP*SUPERSAMP;

            fragColor = new double[]{intensity * col[0], intensity * col[1],
                    intensity * col[2], 1f};
        }

        //responsible for the glowing
        if (glowing.getValueb()) {
            fragColor = Arrays.stream(fragColor).map(v -> pow(v, .4545)).toArray();
        }
        return fragColor;
    }

    private double[] revProject(double[] camPos, double worldY, double fov) {
        double worldZ = worldY / (camPos[1] * tan(fov * DEG2RAD * .5));
        double worldX = worldY * camPos[0] / camPos[1];
        return new double[]{worldX, worldY, worldZ};
    }

}
