package titanicsend.pattern.cesar;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEPattern;

import static titanicsend.util.TEColor.TRANSPARENT;

@LXCategory("Panel FG")
public class HandTracker extends TEPattern {
    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.EDGE,
                    "Color of the pattern");

    public final CompoundParameter targetY =
            new CompoundParameter("Target height", 5, 0, 100)
                    .setDescription("Target height from ground");

    public final CompoundParameter targetZ =
            new CompoundParameter("Target position", 5, 0, 100)
                    .setDescription("Target position left and right");

    public final StringParameter indexTip =
            new StringParameter("Index Tip", "10,50")
                    .setDescription("Following a finger tip (X, Z) 0-100 like '(10,50)'");

    public HandTracker(LX lx) {
        super(lx);
        addParameter("targetZ", this.targetZ);
        addParameter("targetY", this.targetY);
        addParameter("indexTip", this.indexTip);
    }

    @Override
    public void run(double deltaMs) {
        float y = this.targetY.getValuef();
        float z = this.targetZ.getValuef();

        int color = this.color.calcColor();

        for (TEPanelModel panel : this.model.panelsById.values()) {
            for (LXPoint point : panel.points) {
                boolean isCloseToZ = Math.abs(Math.floor(point.zn * 100) - z) < 5;
                boolean isCloseToY = Math.abs(Math.floor(point.yn * 100 * 2) - y) < 10;
                if (isCloseToZ && isCloseToY) {
                    colors[point.index] = color;
                } else {
                    colors[point.index] = TRANSPARENT;
                }
            }
        }
    }

    @Override
    public void onParameterChanged(LXParameter p) {
        super.onParameterChanged(p);
        if (p == this.indexTip) {
            String[] points = ((StringParameter) p).getString().split(",");
            float yFromPoint = Float.parseFloat(points[1]);
            float zFromPoint = Float.parseFloat(points[0]);

            this.targetY.setValue(yFromPoint);
            this.targetZ.setValue(zFromPoint);
        }
    }
}
