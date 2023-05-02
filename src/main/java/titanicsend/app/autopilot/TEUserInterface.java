package titanicsend.app.autopilot;

import heronarts.lx.studio.LXStudio;
import heronarts.p4lx.ui.component.UICollapsibleSection;
import heronarts.p4lx.ui.component.UISwitch;
import titanicsend.app.TEAutopilot;

/**
 * LX Studio boilerplate around adding UI components to
 * control Autopilot.
 */
public class TEUserInterface {

    public static class AutopilotUISection extends UICollapsibleSection {
        public AutopilotUISection(LXStudio.UI ui, TEAutopilot component) {
            super(ui, 0, 0, ui.leftPane.global.getContentWidth(), 80);
            setTitle("Autopilot");
            new UISwitch(0, 4)
                .setParameter(component.enabled)
                .addToContainer(this);
        }
    }
}
