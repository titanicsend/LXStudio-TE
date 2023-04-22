package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.Dimensions;

import java.awt.*;
import java.util.*;

import static heronarts.lx.utils.LXUtils.clamp;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static titanicsend.util.TEMath.multiplyVectorByMatrix;

@Deprecated //we have native support for shaders now. use NativeShaderPatternEffect
public abstract class FragmentShaderEffect extends PatternEffect {
    public FragmentShaderEffect(PatternTarget target) {
        super(target);
    }


    @Override
    public void run(double deltaMS) {
        //multithreading assumes setColor is doing nothing more than updating an array
        double durationSec = pattern.getTime();

        pointsToCanvas.entrySet().parallelStream().forEach(entry -> setColor(entry.getKey(),
                getColorForPoint(entry.getKey(), entry.getValue(), durationSec)));
    }


    private int getColorForPoint(LXPoint point, Dimensions canvasDimensions, double timeSec) {
        boolean useZForX = canvasDimensions.getDepth() > canvasDimensions.getWidth();
        double xCoordinate = useZForX ? point.z - canvasDimensions.getMinZ() : point.x - canvasDimensions.getMinX();

        double[] fragCoordinates = new double[]{
                xCoordinate,
                point.y - canvasDimensions.getMinY()
        };

        double widthResolution = useZForX ? canvasDimensions.getDepth() : canvasDimensions.getWidth();
        double[] resolution = new double[]{widthResolution, canvasDimensions.getHeight()};
        double[] colorRgb = getColorForPoint(fragCoordinates, resolution, timeSec);
        //most shaders ignore alpha but optionally plumbing it through is helpful,
        // esp if we want to layer underneath it. can change black background to transparent, etc.
        float alpha = colorRgb.length > 3 ? (float) colorRgb[3] : 1f;
        return new Color(
                (float) clamp(colorRgb[0], 0, 1),
                (float) clamp(colorRgb[1], 0, 1),
                (float) clamp(colorRgb[2], 0, 1),
                alpha).getRGB();
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
