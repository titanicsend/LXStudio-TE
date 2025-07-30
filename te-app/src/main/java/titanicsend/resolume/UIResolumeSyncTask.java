package titanicsend.resolume;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UITextBox;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIControls;

public class UIResolumeSyncTask extends UICollapsibleSection implements UIControls {

  private static final float VERTICAL_SPACING = 4;
  private static final int CHECKBOX_LABEL_WIDTH = 80;

  public UIResolumeSyncTask(LXStudio.UI ui, ResolumeSyncTask resolumeTask, float w) {
    super(ui, 0, 0, w, 50); // Reduced height for simplified UI
    setTitle("OSC MONITOR");
    setLayout(Layout.VERTICAL, VERTICAL_SPACING);

    // Single row with monitoring checkbox
    UI2dContainer row1 = newHorizontalContainer(getContentWidth(), 22,
        newCheckbox(ui, resolumeTask.enabled, "Monitor OSC"),
        new UILabel(0, 0, 150, 20, "Log incoming OSC messages").setMargin(10, 0, 0, 0)
    );

    addChildren(row1);
  }
    
  private UI2dContainer newCheckbox(LXStudio.UI ui, BooleanParameter p, String label) {
    return newHorizontalContainer(
        CHECKBOX_LABEL_WIDTH + 18, 20,
        new UIButton.Toggle(p),
        new UILabel(CHECKBOX_LABEL_WIDTH, label)
            .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE));
  }
}


// package titanicsend.resolume;

// import heronarts.glx.ui.UI2dContainer;
// import heronarts.glx.ui.component.UIButton;
// import heronarts.glx.ui.component.UICollapsibleSection;
// import heronarts.glx.ui.component.UILabel;
// import heronarts.glx.ui.component.UITextBox;
// import heronarts.glx.ui.vg.VGraphics;
// import heronarts.lx.parameter.BooleanParameter;
// import heronarts.lx.studio.LXStudio;
// import heronarts.lx.studio.ui.device.UIControls;

// public class UIResolumeSyncTask extends UICollapsibleSection implements UIControls {

//   private static final float VERTICAL_SPACING = 4;
//   private static final int CHECKBOX_LABEL_WIDTH = 60;

//   public UIResolumeSyncTask(LXStudio.UI ui, ResolumeSyncTask resolumeTask, float w) {
//     super(ui, 0, 0, w, 85); // Fixed height
//     setTitle("RESOLUME SYNC");
//     setLayout(Layout.VERTICAL, VERTICAL_SPACING);

//     // Row 1: Status & Network
//     UI2dContainer row1 = newHorizontalContainer(getContentWidth(), 22,
//         newCheckbox(ui, resolumeTask.enabled, "Enabled"),
//         new UILabel(0, 0, 25, 20, "IP:"),
//         new UITextBox(0, 0, 100, 20, resolumeTask.resolumeIP),
//         new UILabel(0, 0, 35, 20, "Port:").setMargin(5, 0, 0, 0),
//         new UITextBox(0, 0, 50, 20, resolumeTask.resolumePort)
//     );
    
//     // Row 2: Actions
//     UI2dContainer row2 = newHorizontalContainer(getContentWidth(), 22,
//         new UIButton(0, 0, 70, 20, resolumeTask.setUpOsc).setLabel("Set Up"),
//         new UIButton(0, 0, 60, 20, resolumeTask.reloadConfig).setLabel("Reload")
//     );

//     addChildren(row1, row2);
//   }
    
//   private UI2dContainer newCheckbox(LXStudio.UI ui, BooleanParameter p, String label) {
//     return newHorizontalContainer(
//         CHECKBOX_LABEL_WIDTH + 18, 20,
//         new UIButton.Toggle(p),
//         new UILabel(CHECKBOX_LABEL_WIDTH, label)
//             .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE));
//   }
// }
