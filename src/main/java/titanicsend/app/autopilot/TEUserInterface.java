package titanicsend.app.autopilot;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.p4lx.ui.component.UICheckbox;
import heronarts.p4lx.ui.component.UICollapsibleSection;

/**
 * LX Studio boilerplate around adding UI components to
 * control Autopilot.
 */
public class TEUserInterface {

    public static class AutopilotComponent extends LXComponent implements LXOscComponent {
        public final BooleanParameter autopilotEnabledToggle =
                new BooleanParameter("Autopilot Enabled")
                        .setDescription("Toggle to turn on VJ autopilot mode")
                        .setValue(false);
        public AutopilotComponent(LX lx) {
            super(lx);
            addParameter("autopilotEnabledToggle", this.autopilotEnabledToggle);
        }
    }

    public static class AutopilotUISection extends UICollapsibleSection {
        public AutopilotUISection(LXStudio.UI ui, AutopilotComponent myComponent) {
            super(ui, 0, 0, ui.leftPane.global.getContentWidth(), 80);
            setTitle("Autopilot: enable?");
            new UICheckbox(0, 0, myComponent.autopilotEnabledToggle).addToContainer(this);
        }
    }
}
