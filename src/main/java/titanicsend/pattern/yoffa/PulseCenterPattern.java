package titanicsend.pattern.yoffa;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPattern;

@LXCategory("Panel FG")
public class PulseCenterPattern extends TEAudioPattern {

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .1, 0, 1)
                    .setDescription("Amount of motion - Sparkles");

    private final PanelPulser panelPulser;

    public PulseCenterPattern(LX lx) {
        super(lx);
        addParameter("energy", energy);
        panelPulser = new PanelPulser();
    }

    public void runTEAudioPattern(double deltaMs) {
        updateGradients();
        panelPulser.pulsePanels(this, model.getAllPanels(), energy.getNormalized());
    }

}
