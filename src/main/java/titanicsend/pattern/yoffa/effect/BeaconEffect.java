package titanicsend.pattern.yoffa.effect;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.effect.LXModelEffect;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.model.TEEdgeSection;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEPanelSection;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.*;

public class BeaconEffect extends LXModelEffect<TEWholeModel> {

    public final CompoundParameter brightness =
        new CompoundParameter("Brightness", 100, 0, 100);

    public BeaconEffect(LX lx) {
        super(lx);
        addParameter("Brightness", brightness);
    }

    public void run(double deltaMs, double enabledAmount) {
        // light all port side edges and panels in a highly visible color
        for (LXPoint point : model.getEdgePointsBySection(TEEdgeSection.PORT)) {
            int baseColor = lx.engine.palette.getSwatchColor(TEPattern.ColorType.PRIMARY.swatchIndex()).getColor();

            setColor(point, LXColor.hsba(
                LXColor.h(baseColor),
                LXColor.s(baseColor),
                brightness.getValue(),
                100
            ));
        }

        Set<LXPoint> points = new HashSet<>();
        points.addAll(model.getPointsBySection(TEPanelSection.PORT_AFT));
        points.addAll(model.getPointsBySection(TEPanelSection.PORT_AFT_SINGLE));
        points.addAll(model.getPointsBySection(TEPanelSection.PORT_FORE));
        points.addAll(model.getPointsBySection(TEPanelSection.PORT_FORE_SINGLE));

        for (LXPoint point : points) {
            int baseColor = LXColor.BLUE;

            setColor(point, LXColor.hsba(
                LXColor.h(baseColor),
                LXColor.s(baseColor),
                brightness.getValue(),
                100
            ));
        }
    }

}
