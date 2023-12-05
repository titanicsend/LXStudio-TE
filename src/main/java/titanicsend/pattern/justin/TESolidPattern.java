package titanicsend.pattern.justin;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPerformancePattern;

@LXCategory(LXCategory.COLOR)
public class TESolidPattern extends TEPerformancePattern {

    public TESolidPattern(LX lx) {
        super(lx);

        addCommonControls();
    }

    @Override
    protected void runTEAudioPattern(double deltaMs) {
        int color1 = calcColor();

        for (LXPoint p : getModel().getPoints()) {
            if (this.modelTE.isGapPoint(p)) {
                continue;
            }

            colors[p.index] = color1;
        }
    }
}
