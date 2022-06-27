package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.Dimensions;

import java.awt.*;
import java.util.*;

import static heronarts.lx.utils.LXUtils.clamp;

@Deprecated //we have native support for shaders now. use NativeShaderPatternEffect
public abstract class FragmentShaderEffect extends PatternEffect {

    public FragmentShaderEffect(PatternTarget target) {
        super(target);
    }


    @Override
    public void run(double deltaMS) {
        //multithreading assumes setColor is doing nothing more than updating an array
        double durationSec = getDurationSec();
        pointsToCanvas.entrySet().parallelStream().forEach(entry -> setColor(entry.getKey(),
                getColorForPoint(entry.getKey(), entry.getValue(), durationSec)));
    }


    private int getColorForPoint(LXPoint point, Dimensions canvasDimensions, double timeSec) {
        boolean useZForX = canvasDimensions.getDepth() > canvasDimensions.getWidth();
        double xCoordinate = useZForX ? point.z - canvasDimensions.getMinZ() : point.x - canvasDimensions.getMinX();

        double[] fragCoordinates = new double[] {
                xCoordinate,
                point.y - canvasDimensions.getMinY()
        };

        double widthResolution = useZForX ? canvasDimensions.getDepth() : canvasDimensions.getWidth();
        double[] resolution = new double[] {widthResolution, canvasDimensions.getHeight()};
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


    //similar to an actual fragment shader function
    //inputs arrays of length 2 for x/y
    //output should be of length 3 symbolizing RGB for the input coordinates
    protected abstract double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds);

    public abstract Collection<LXParameter> getParameters();

}
