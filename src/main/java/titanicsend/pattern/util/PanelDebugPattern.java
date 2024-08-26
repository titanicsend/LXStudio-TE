package titanicsend.pattern.util;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.DiscreteParameter;
import titanicsend.pattern.TEPattern;

import java.util.List;

@LXCategory("Utility")
public class PanelDebugPattern extends TEPattern {

    public final DiscreteParameter start =
        new DiscreteParameter("Start", 0, 0, 60);

    public final DiscreteParameter spacing =
        new DiscreteParameter("Spacing", 3, 1, 30);

    public PanelDebugPattern(LX lx) {
        super(lx);
        addParameter("start", this.start);
        addParameter("spacing", this.spacing);
    }

    @Override
    protected void run(double v) {
        int start = this.start.getValuei();
        int spacing = this.spacing.getValuei();

        for (LXModel panel : this.model.sub("panel")) {
            List<LXModel> rows = panel.sub("row");
            for (int r = start; r < rows.size(); r+=spacing) {
                LXModel row = rows.get(r);
                setColor(row, LXColor.RED);
                int length = row.points.length;
                if (length > 2) {
                    colors[row.points[0].index] = LXColor.WHITE;
                    colors[row.points[1].index] = LXColor.WHITE;
                    colors[row.points[length-1].index] = LXColor.BLUE;
                }
            }
        }
    }
}
