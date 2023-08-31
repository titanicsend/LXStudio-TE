package titanicsend.pattern.util;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.model.TEPanelSection;
import titanicsend.pattern.TEPattern;

import java.util.*;

import static titanicsend.util.TEColor.TRANSPARENT;

@LXCategory("Utility")
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

    public final EnumParameter<Side> side =
            new EnumParameter<>("Side", Side.STARBOARD)
                    .setDescription("Side of model");

    public enum Side {
        STARBOARD, FORE, PORT, AFT
    }

    public final BooleanParameter stamp = new BooleanParameter("Stamp").setMode(BooleanParameter.Mode.MOMENTARY);
    public final BooleanParameter clear = new BooleanParameter("Clear").setMode(BooleanParameter.Mode.MOMENTARY);

    // A special value in savedStamps that means this pixel shouldn't be a static
    // color, but rather an animated bullseye that keeps changing color
    public static final int ANIMATED_BULLSEYE = 123;
    private final int[] ANIMATED_BULLSEYE_COLORS = {
            LXColor.RED, LXColor.GREEN, LXColor.BLUE, LXColor.WHITE};

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
        addParameter("Side", side);
        this.totalMsec = 0.0;
    }

    @Override
    public void run(double deltaMs) {
        this.totalMsec += deltaMs;
        int phase = (int)(this.totalMsec / MSEC_PER_COLOR) % 4;
        int bullseyeColor = ANIMATED_BULLSEYE_COLORS[phase];
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
        float w = side.getEnum() == Side.FORE ? this.xParam.getValuef() : -this.xParam.getValuef();
        float y = this.yParam.getValuef();

        float xMax = this.modelTE.boundaryPoints.maxXBoundaryPoint.x;
        float yMax = this.modelTE.boundaryPoints.maxYBoundaryPoint.y;
        float zMax = this.modelTE.boundaryPoints.maxZBoundaryPoint.z;

        Set<LXPoint> pointsForSide = getPointsForSide(side.getEnum());

        Map<LXPoint, Integer> bullseye = new HashMap<>();
        LXPoint targetPoint = null;
        double closestDistance = Float.MAX_VALUE;
        for (LXPoint point : this.modelTE.points) {
            if (this.modelTE.isGapPoint(point)) continue;
            if (!pointsForSide.contains(point)) {
                if (!savedStamps.containsKey(point)) {
                    colors[point.index] = TRANSPARENT;
                }
                continue;
            }

            float pointW;
            float wMax;
            float dwMultiplier;
            if (side.getEnum() == Side.PORT || side.getEnum() == Side.STARBOARD) {
                pointW = point.z;
                wMax = zMax;
                dwMultiplier = .85f;
            } else {
                pointW = point.x;
                wMax = xMax;
                dwMultiplier = .2f;
            }

            float wPercent = 100.0f * pointW / wMax;
            float yPercent = 100.0f * point.y / yMax;
            float dy = yPercent - y;
            float dw = wPercent - w;
            dw *= dwMultiplier; // Make ellipse into circle
            double distance = Math.sqrt(dy * dy + dw * dw);
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

    private Set<LXPoint> getPointsForSide(Side side) {
        Set<LXPoint> points = new HashSet<>();
        switch (side) {
            case PORT -> {
                points.addAll(modelTE.getPointsBySection(TEPanelSection.PORT_AFT));
                points.addAll(modelTE.getPointsBySection(TEPanelSection.PORT_AFT_SINGLE));
                points.addAll(modelTE.getPointsBySection(TEPanelSection.PORT_FORE));
                points.addAll(modelTE.getPointsBySection(TEPanelSection.PORT_FORE_SINGLE));
            }
            case FORE -> {
                points.addAll(modelTE.getPointsBySection(TEPanelSection.FORE));
            }
            case AFT -> {
                points.addAll(modelTE.getPointsBySection(TEPanelSection.AFT));
            }
            case STARBOARD -> {
                points.addAll(modelTE.getPointsBySection(TEPanelSection.STARBOARD_AFT));
                points.addAll(modelTE.getPointsBySection(TEPanelSection.STARBOARD_AFT_SINGLE));
                points.addAll(modelTE.getPointsBySection(TEPanelSection.STARBOARD_FORE));
                points.addAll(modelTE.getPointsBySection(TEPanelSection.STARBOARD_FORE_SINGLE));
            }
        }
        return points;
    }
}
