package titanicsend.app.autopilot;

import heronarts.lx.studio.LXStudio;
import heronarts.p4lx.ui.UI2dContainer;
import heronarts.p4lx.ui.component.UIButton;
import heronarts.p4lx.ui.component.UICollapsibleSection;
import heronarts.p4lx.ui.component.UILabel;
import heronarts.p4lx.ui.component.UISwitch;
import processing.core.PConstants;
import titanicsend.app.TEAutopilot;

/**
 * LX Studio boilerplate around adding UI components to
 * control Autopilot.
 */
public class TEUserInterface {

    public static class AutopilotUISection extends UICollapsibleSection {
        public AutopilotUISection(LXStudio.UI ui, TEAutopilot component) {
            super(ui, 0, 0, ui.leftPane.global.getContentWidth(), 0);
            setTitle("Autopilot");
            setLayout(UI2dContainer.Layout.VERTICAL);
            setChildSpacing(4);

            // Enabled button
            new UISwitch(0, 0)
                .setParameter(component.enabled)
                .addToContainer(this);

            // Options
            UI2dContainer.newHorizontalContainer(16, 4,
                    new UILabel(46, 16, "Options:")
                        .setTextAlignment(PConstants.LEFT, PConstants.CENTER),
                    new UIButton(50, 16, component.includePalette).setLabel("Palette")
                )
                .addToContainer(this);
        }
    }
}
