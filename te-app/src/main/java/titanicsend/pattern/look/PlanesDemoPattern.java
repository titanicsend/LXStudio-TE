package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.transform.LXVector;
import titanicsend.color.TEColorType;
import titanicsend.pattern.TEAudioPattern;

@LXCategory("TE Examples")
public class PlanesDemoPattern extends TEAudioPattern {
    public final LinkedColorParameter color =
            registerColor("Color", "color", TEColorType.PRIMARY, "Primary color of the field");

    public final CompoundParameter xCutoff =
            new CompoundParameter("xCutoff", .0, -1, 1)
                    .setDescription("xCutoff");
    public final CompoundParameter yCutoff =
            new CompoundParameter("yCutoff", .85, 0, 1)
                    .setDescription("yCutoff");
    public final CompoundParameter zCutoff =
            new CompoundParameter("zCutoff", .2, -1, 1)
                    .setDescription("zCutoff");

    // Memoizing these boosts FPS significantly due to the many iterations
    protected float scaleValue, frequency;
    static final float PI = (float) Math.PI;

    public PlanesDemoPattern(LX lx) {
        super(lx);
        addParameter("xCutoff", xCutoff);
        addParameter("yCutoff", yCutoff);
        addParameter("zCutoff", zCutoff);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        float yRange = model.yMax - model.yMin;
        float xRange = model.xMax - model.xMin;
        float zRange = model.zMax - model.zMin;

        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);

            float normY = point.y / yRange;
            float normX = point.x / xRange;
            float normZ = point.z / zRange;

            if (normX > xCutoff.getValuef()) {
                colors[point.index] += LXColor.RED;
            }
            if (normY > yCutoff.getValuef()) {
                colors[point.index] += LXColor.GREEN;
            }
            if (normZ > zCutoff.getValuef()) {
                colors[point.index] += LXColor.BLUE;
            }
        }
    }
}
