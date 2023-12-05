package titanicsend.ui.modulator;

import heronarts.glx.ui.component.UIIndicator;
import heronarts.glx.ui.component.UIIntegerBox;
import heronarts.glx.ui.component.UIMeter;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import titanicsend.modulator.dmx.DmxRangeModulator;

public class UIDmxRangeModulator implements UIModulatorControls<DmxRangeModulator> {

    private static final int HEIGHT = 50;
    private static final int COL_WIDTH = 42;

    @Override
    public void buildModulatorControls(UI ui, UIModulator uiModulator, DmxRangeModulator modulator) {
        uiModulator.setContentHeight(HEIGHT);

        uiModulator.addChildren(
                new UIIntegerBox(0, 2, COL_WIDTH, 16).setParameter(modulator.universe),
                controlLabel(ui, "Univ", COL_WIDTH).setPosition(0, 21).setTextAlignment(VGraphics.Align.CENTER),
                new UIIntegerBox(46, 2, COL_WIDTH, 16).setParameter(modulator.channel),
                controlLabel(ui, "Chan", COL_WIDTH).setPosition(46, 21).setTextAlignment(VGraphics.Align.CENTER),
                new UIIntegerBox(96, 2, COL_WIDTH, 16).setParameter(modulator.min),
                controlLabel(ui, "Min", COL_WIDTH).setPosition(96, 21).setTextAlignment(VGraphics.Align.CENTER),
                new UIIntegerBox(142, 2, COL_WIDTH, 16).setParameter(modulator.max),
                controlLabel(ui, "Max", COL_WIDTH).setPosition(142, 21).setTextAlignment(VGraphics.Align.CENTER),
                new UIIndicator(ui, 12, 12, modulator.active)
                        .setTriggerable(true)
                        .setPosition(uiModulator.getContentWidth() - 12, 2),
                UIMeter.newVerticalMeter(ui, modulator, 12, 32).setPosition(uiModulator.getContentWidth() - 12, 16));
    }
}
