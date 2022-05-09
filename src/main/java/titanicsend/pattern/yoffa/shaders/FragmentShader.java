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
        double[] colorHsba = getColorForPoint(fragCoordinates, resolution, timeSeconds);
        assert colorHsba.length == 3;
        return new Color(
                (float) TEMath.clamp(colorHsba[0], 0, 1),
                (float) TEMath.clamp(colorHsba[1], 0, 1),
                (float) TEMath.clamp(colorHsba[2], 0, 1),
                1f).getRGB();
    }

    protected abstract double[] getColorForPoint(double[] fragCoordinates, double[] resolution, double timeSeconds);

    public abstract Collection<LXParameter> getParameters();

}
