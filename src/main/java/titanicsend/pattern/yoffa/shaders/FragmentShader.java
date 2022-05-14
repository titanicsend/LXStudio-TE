package titanicsend.pattern.yoffa.shaders;

import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.util.Dimensions;
import titanicsend.util.TEMath;

import java.awt.*;
import java.util.Collection;

public abstract class FragmentShader {

    //TODO @yoffa currently set up to work for stage/starboard panels. will expand on this in a follow up change.
    public int getColorForPoint(LXPoint point, Dimensions canvasDimensions, long timeMillis) {
        double[] fragCoordinates = new double[] {
                point.z - canvasDimensions.getMinZ(),
                point.y - canvasDimensions.getMinY()
        };
        double[] resolution = new double[] {canvasDimensions.getDepth(), canvasDimensions.getHeight()};
        double timeSeconds = timeMillis / 1000.;
        double[] colorRgb = getColorForPoint(fragCoordinates, resolution, timeSeconds);
        assert colorRgb.length == 3;
        return new Color(
                (float) TEMath.clamp(colorRgb[0], 0, 1),
                (float) TEMath.clamp(colorRgb[1], 0, 1),
                (float) TEMath.clamp(colorRgb[2], 0, 1),
                1f).getRGB();
    }

    //similar to an actual fragment shader function
    //inputs arrays of length 2 for x/y
    //output should be of length 3 symbolizing RGB for the input coordinates
    protected abstract double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds);

    public abstract Collection<LXParameter> getParameters();

}
