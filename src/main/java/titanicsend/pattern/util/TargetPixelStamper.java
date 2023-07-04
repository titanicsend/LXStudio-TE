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
    public static final int MSEC_PER_COLOR = 1000;

    double totalMsec;

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

    // A special value in savedStamps that means this pixel shouldn't be a static
    // color, but rather an animated bullseye that keeps changing color
    public static final int ANIMATED_BULLSEYE = 123;

    // The concentric colors for a saved target
    private final int[] ACTIVE_TARGET_COLORS = {LXColor.BLACK, LXColor.WHITE, LXColor.GREEN};

    // The concentric colors for a saved target
    private final int[] SAVED_TARGET_COLORS = {LXColor.BLACK, LXColor.WHITE, LXColor.RED};

    private Map<LXPoint, Integer> currentBullseye;
    private final Map<LXPoint, Integer> savedStamps = new HashMap<>();

    public TargetPixelStamper(LX lx) {
        super(lx);
        addParameter("X", this.xParam);
        addParameter("Y", this.yParam);
        addParameter("Size", this.size);
        addParameter("Stamp", this.stamp);
        addParameter("Clear", this.clear);
        this.totalMsec = 0.0;
    }

    @Override
    public void run(double deltaMs) {
        this.totalMsec += deltaMs;
        int phase = (int)(this.totalMsec / MSEC_PER_COLOR) % 4;
        int bullseyeColor;
        if (phase == 0) {
            bullseyeColor = LXColor.RED;
        } else if (phase == 1) {
            bullseyeColor = LXColor.GREEN;
        } else if (phase == 2) {
            bullseyeColor = LXColor.BLUE;
        } else {
            bullseyeColor = LXColor.WHITE;
        }
        showSavedStamps(bullseyeColor);
        showCurrentTarget(bullseyeColor);
    }

    private void showSavedStamps(int bullseyeColor) {
        for (LXPoint point : savedStamps.keySet()) {
            int color = savedStamps.get(point);
            if (color == ANIMATED_BULLSEYE) color = bullseyeColor;
            colors[point.index] = color;
        }
    }

    private void showCurrentTarget(int bullseyeColor) {
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
            for (int i = SAVED_TARGET_COLORS.length - 1; i >= 0; i--) {
                if (distance < size.getValue() * (i+1)) {
                    colors[point.index] = ACTIVE_TARGET_COLORS[i];
                    bullseye.put(point, SAVED_TARGET_COLORS[i]);
                }
            }
        }
        if (targetPoint != null) {
            colors[targetPoint.index] = bullseyeColor;
        }
        bullseye.put(targetPoint, ANIMATED_BULLSEYE);
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
