package titanicsend.pattern.yoffa;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPattern;

import static titanicsend.util.TEMath.wave;

@LXCategory("Panel FG")
public class PulseSidePattern extends TEAudioPattern {

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .1, 0, 1)
                    .setDescription("Amount of motion - Sparkles");

    private final PanelPulser panelPulser;

    public PulseSidePattern(LX lx) {
        super(lx);
        addParameter("energy", energy);
        panelPulser = new PanelPulser();
    }

    public void runTEAudioPattern(double deltaMs) {
        updateGradients();
        panelPulser.pulsePanels(this, model.getAllPanels(), energy.getNormalized(), 0, 0, 0);
    }

}
