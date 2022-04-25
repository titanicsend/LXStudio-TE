package titanicsend.pattern.yoffa;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.model.TEPanelSection;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.Dimensions;

@LXCategory("Panel FG")
public class PulseDoublePattern extends TEAudioPattern {

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .1, 0, 1)
                    .setDescription("Amount of motion - Sparkles");

    private final PanelPulser panelPulser;

    public PulseDoublePattern(LX lx) {
        super(lx);
        addParameter("energy", energy);
        panelPulser = new PanelPulser();
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        updateGradients();

        float znLeft = calcCenterZn(TEPanelSection.FRONT_LEFT);
        float znRight = calcCenterZn(TEPanelSection.FRONT_RIGHT);

        panelPulser.pulsePanels(this, model.getLeftPanels(), energy.getNormalized(), 0, 0, znLeft);
        panelPulser.pulsePanels(this, model.getRightPanels(), energy.getNormalized(), 0, 0, znRight);
    }

    private float calcCenterZn(TEPanelSection section) {
        Dimensions dimensions = Dimensions.fromPanels(model.getPanelsBySection(section));
        return (dimensions.getDepthNormalized() / 2) + dimensions.getMinZn();
    }

}
