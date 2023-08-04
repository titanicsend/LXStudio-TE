package titanicsend.app.autopilot;

import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UISwitch;
import heronarts.lx.studio.LXStudio;
import titanicsend.app.TEAutopilot;
import titanicsend.lasercontrol.TELaserTask;

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

    // Use this for other random UI buttons we want to add
    public static class TEUISection extends UICollapsibleSection {
      public TEUISection(LXStudio.UI ui, TELaserTask laser) {
          super(ui, 0, 0, ui.leftPane.global.getContentWidth(), 80);
          setTitle("TE General Controls");
          new UISwitch(0, 4)
              .setParameter(laser.enabled)
              .addToContainer(this);
      }
  }
}
