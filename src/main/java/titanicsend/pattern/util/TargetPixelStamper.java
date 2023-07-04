package titanicsend.pattern.util;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;

import java.util.HashMap;
import java.util.Map;

import static titanicsend.util.TEColor.TRANSPARENT;

//TODO make this properly wrap around the 3d model
@LXCategory("Utility Patterns")
public class TargetPixelStamper extends TEPattern {

    public final CompoundParameter xParam =
            new CompoundParameter("X", 61, -100, 100)
                    .setDescription("Target position left and right");

    public final CompoundParameter yParam =
            new CompoundParameter("Y", 25.5, 0, 100)
                    .setDescription("Target height from ground");

    public final CompoundParameter size =
            new CompoundParameter("Size", 1, 0.1, 4)
                    .setDescription("Target size");

    public final BooleanParameter stamp = new BooleanParameter("Stamp").setMode(BooleanParameter.Mode.MOMENTARY);
    public final BooleanParameter clear = new BooleanParameter("Clear").setMode(BooleanParameter.Mode.MOMENTARY);

    private final int[] COLOR_LIST = {LXColor.GREEN, LXColor.BLUE, LXColor.RED, LXColor.WHITE};

    private Map<LXPoint, Integer> currentBullseye;
    private final Map<LXPoint, Integer> savedStamps = new HashMap<>();

    public TargetPixelStamper(LX lx) {
        super(lx);
        addParameter("X", this.xParam);
        addParameter("Y", this.yParam);
        addParameter("Size", this.size);
        addParameter("Stamp", this.stamp);
        addParameter("Clear", this.clear);
    }

    @Override
    public void run(double deltaMs) {
        showSavedStamps();
        showCurrentTarget();
    }

    private void showSavedStamps() {
        for (LXPoint point : savedStamps.keySet()) {
            colors[point.index] = savedStamps.get(point);
        }
    }

    private void showCurrentTarget() {
        float y = this.yParam.getValuef();
        float z = -this.xParam.getValuef();

        float zMax = this.modelTE.boundaryPoints.maxZBoundaryPoint.z;
        float yMax = this.modelTE.boundaryPoints.maxYBoundaryPoint.y;

        Map<LXPoint, Integer> bullseye = new HashMap<>();
        LXPoint targetPoint = null;
        double closestDistance = Float.MAX_VALUE;
        for (LXPoint point : this.modelTE.points) {
            if (this.modelTE.isGapPoint(point)) continue;
            float zPercent = 100.0f * point.z / zMax;
            float yPercent = 100.0f * point.y / yMax;
            float dy = yPercent - y;
            float dz = zPercent - z;
            dz *= 0.85; // Make ellipse into circle
            double distance = Math.sqrt(dy * dy + dz * dz);
            if (distance < closestDistance) {
                closestDistance = distance;
                targetPoint = point;
            }
            if (!savedStamps.containsKey(point)){
                colors[point.index] = TRANSPARENT;
            }
            for (int i = COLOR_LIST.length - 1; i >= 0; i--) {
                if (distance < size.getValue() * i) {
                    colors[point.index] = COLOR_LIST[i];
                    bullseye.put(point, COLOR_LIST[i]);
                }
            }
        }
        if (targetPoint != null) {
            colors[targetPoint.index] = COLOR_LIST[0];
        }
        bullseye.put(targetPoint, COLOR_LIST[0]);
        this.currentBullseye = bullseye;
    }

    @Override
    public void onParameterChanged(LXParameter p) {
        super.onParameterChanged(p);
        if (p == stamp) {
            savedStamps.putAll(currentBullseye);
        } else if (p == clear) {
            savedStamps.clear();
        }
    }
}
