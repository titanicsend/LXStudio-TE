package titanicsend.pattern.yoffa;

import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.utils.LXUtils;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPattern;
import titanicsend.util.TEMath;

import java.util.Collection;
import java.util.function.Function;

import static titanicsend.util.TEMath.wave;

public class PanelPulser {

    public void pulsePanels(TEAudioPattern pattern, Collection<TEPanelModel> panels, double energyNormalized) {
        pulsePanels(pattern, panels, energyNormalized, (point) -> point.rn);
    }

    public void pulsePanels(TEAudioPattern pattern, Collection<TEPanelModel> panels, double energyNormalized, float xn, float yn, float zn) {
        pulsePanels(pattern, panels, energyNormalized, (point) -> (float) TEMath.distance(point.xn, point.yn, point.zn, xn, yn, zn));
    }

    // This is shamelessly stolen from Jeff's ArtStandards class with some tweaked inputs/parameters
    // We could abstract it out, but I didn't want to disrupt his wonderful template
    // I also expect them to diverge as they are tweaked more, so I don't hate the duplication as of now
    private void pulsePanels(TEAudioPattern pattern, Collection<TEPanelModel> panels, double energyNormalized,
                             Function<LXPoint, Float> distanceFunction) {
        double scaledTrebleRatio = LXUtils.clamp(
                (pattern.getTrebleRatio() - .5) / (1.01 - energyNormalized) / 6 -
                        .2 + energyNormalized / 2,
                0, 1);

        for (TEPanelModel panel : panels) {
            for (LXPoint point : panel.points) {
                float distanceFromCenter = distanceFunction.apply(point);

                int baseColor = pattern.getEdgeGradientColor(2 * (distanceFromCenter - (float) pattern.measure()));

                float hue = LXColor.h(baseColor);
                float saturation = LXColor.s(baseColor);
                float brightness = LXColor.b(baseColor);

                saturation = Math.random() < scaledTrebleRatio ? 0 : saturation;

                double alphaWave = wave(2 * distanceFromCenter - scaledTrebleRatio);

                pattern.getColors()[point.index] = LXColor.hsba(
                        hue,
                        saturation,
                        brightness,
                        alphaWave
                );
            }
        }
    }

}
