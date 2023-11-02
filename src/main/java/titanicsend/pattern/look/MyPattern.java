package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPerformancePattern;

@LXCategory(LXCategory.COLOR)
public class MyPattern extends TEPerformancePattern {

    public MyPattern(LX lx) {
        super(lx);

        addCommonControls();
    }

    @Override
    protected void runTEAudioPattern(double deltaMs) {
        int color1 = calcColor();
        int color2 = calcColor2();

        for (LXPoint p : this.modelTE.edgePoints) {
            if (this.modelTE.isGapPoint(p)) {
                continue;
            }

            colors[p.index] = color1;
        }
        for (LXPoint p : this.modelTE.panelPoints) {
            if (this.modelTE.isGapPoint(p)) {
                continue;
            }

            colors[p.index] = color2;
        }
    }

}
