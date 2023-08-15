package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.awt.*;
import java.util.*;

import static heronarts.lx.utils.LXUtils.clamp;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static titanicsend.util.TEMath.*;


@Deprecated //we have native support for shaders now. use NativeShaderPatternEffect
public abstract class FragmentShaderEffect extends PatternEffect {
    double[][] rotationMatrix;
    double[] translationFromControls = new double[2];
    int color1 = 0;
    int color2 = 0;

    public FragmentShaderEffect(PatternTarget target) {
        super(target);
    }

    @Override
    public void run(double deltaMS) {
        //multithreading assumes setColor is doing nothing more than updating an array

        // calculate per-frame control derived variables
        double durationSec = pattern.getTime();

        color1 = pattern.calcColor();
        color2 = pattern.calcColor2();

        double angle = -pattern.getRotationAngleFromSpin();
        translationFromControls[0] = pattern.getXPos();
        translationFromControls[1] = pattern.getYPos();

        rotationMatrix = new double[][]{
            {cos(angle), -sin(angle)},
            {sin(angle), cos(angle)}
        };

        getPoints().parallelStream().forEach(p -> setColor(p, getColorForPoint(p, durationSec)));
    }

    private int getColorForPoint(LXPoint point, double timeSec) {
        float alpha;
        double[] fragCoordinates = new double[] { point.zn, point.yn };
        double[] resolution = new double[] { 1,1 };
        double[] colorRgb = getColorForPoint(fragCoordinates, resolution, timeSec);

        for (int i = 0; i < colorRgb.length; i++) {
            colorRgb[i] = clamp(colorRgb[i], 0, 1);
        }

        if (colorRgb.length > 3) {
            // if we're given an alpha value, use it
            alpha = (float) colorRgb[3];
        }
        else {
            // otherwise, set things up to use alpha as brightness for best possible
            // blending.  First, calculate alpha, based on the brightest color component.
            alpha = (float) Math.max(colorRgb[0],Math.max(colorRgb[1],colorRgb[2]));

            // if fully transparent, we're done
            if (alpha <= 0f) return 0;

            // set color to its brightest possible level.
            colorRgb[0] /= alpha;
            colorRgb[1] /= alpha;
            colorRgb[2] /= alpha;
        }

        return new Color(
            (float) colorRgb[0],
            (float) colorRgb[1],
            (float) colorRgb[2],
            alpha).getRGB();
    }

    // Rotate point in 2D around specified origin, using the
    // current precalculated matrix
    public double[] rotate2D(double[] point, double[] origin) {
        double[] p1 = subtractArrays(point, origin);
        p1 = multiplyVectorByMatrix(p1, rotationMatrix);
        return addArrays(p1, origin);
    }

    public double[] translate(double[] point) {
        return addArrays(point,translationFromControls);
    }

    public int calcColor() {
        return color1;
    }

    public int calcColor2() {
        return color2;
    }

    /**
     * @param color    - packed LX color
     * @param rgbArray - a 3 or 4 element array to receive the normalized RGB or RGBA color.
     */
    public static void colorToRGBArray(int color, double[] rgbArray) {
        rgbArray[0] = (double) (0xff & LXColor.red(color)) / 255;
        rgbArray[1] = (double) (0xff & LXColor.green(color)) / 255;
        rgbArray[2] = (double) (0xff & LXColor.blue(color)) / 255;
        if (rgbArray.length > 3) rgbArray[3] = (double) (0xff & LXColor.alpha(color)) / 255;
    }


    //similar to an actual fragment shader function
    //inputs arrays of length 2 for x/y
    //output should be of length 3 symbolizing RGB for the input coordinates
    protected abstract double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds);

    public abstract Collection<LXParameter> getParameters();

}
