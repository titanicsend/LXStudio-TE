package titanicsend.pattern.yoffa;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.LXRangeModulator;
import heronarts.lx.modulator.TriangleLFO;
import titanicsend.pattern.TEPattern;

import java.util.*;

@LXCategory("Edge FG")
public class AlternatingDots extends TEPattern {

    private final int maxPoints = model.panelPoints.size() / 25;

    private Set<LXPoint> breathingPointsPrev = new HashSet<>();
    private Set<LXPoint> breathingPointsNext = new HashSet<>();
    private final Set<LXPoint> extraShinyPoints = new HashSet<>();
    private final List<LXPoint> panelPointsList = new ArrayList<>(model.panelPoints);

    private int lastBeat = 0;

    public AlternatingDots(LX lx) {
        super(lx);
    }

    @Override
    public void onActive() {
        breathingPointsPrev.clear();
        breathingPointsNext.clear();
        extraShinyPoints.clear();
        lastBeat = 0;
    }

    public void run(double deltaMs) {
        int baseColor = getSwatchColor(ColorType.PANEL);
        double basis = lx.engine.tempo.basis();
        int beatCount = lx.engine.tempo.beatCount();

        if (beatCount > lastBeat && basis > .5) {
            lastBeat = beatCount;
            breathingPointsPrev = breathingPointsNext;
            breathingPointsNext = new HashSet<>();
            Collections.shuffle(panelPointsList);
            for (int i = 0; breathingPointsNext.size() < maxPoints && i < panelPointsList.size(); i++) {
                if (!breathingPointsPrev.contains(panelPointsList.get(i))) {
                    breathingPointsNext.addAll(panelPointsList.subList(0, maxPoints));
                    if (extraShinyPoints.size() < maxPoints / 2) {
                        extraShinyPoints.add(panelPointsList.get(i));
                    }
                }
            }
        }

        double breathStatusNext = basis;
        double breathStatusPrev = 1 - basis;
        for (LXPoint point : model.panelPoints) {
            double alpha = 100;
            double breathStatus;
            if (breathingPointsPrev.contains(point)) {
                breathStatus = breathStatusPrev;
            } else if (breathingPointsNext.contains(point)) {
                breathStatus = breathStatusNext;
            } else {
                breathStatus = 0;
                alpha = 0;
            }
            double brightness = extraShinyPoints.contains(point) ? 100 : 50;
            colors[point.index] = LXColor.hsba(
                    LXColor.h(baseColor),
                    LXColor.s(baseColor),
                    brightness * breathStatus,
                    alpha
            );
        }

    }

}
