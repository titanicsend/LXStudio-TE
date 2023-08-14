package titanicsend.ui;

import heronarts.glx.ui.component.UIColorControl;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UISwitch;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedFunctionalParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import titanicsend.pattern.TEPerformancePattern.TEColorParameter;
import titanicsend.pattern.TEPerformancePattern.TEColorParameter.TEColorOffsetParameter;

public class UIUtils {

  /**
   * Constructs device controls in 4x4 grids, matching the layout of the MidiFighterTwisters
   */
  static public void buildMftStyleDeviceControls(UI ui, UIDevice uiDevice, LXDeviceComponent device) {    
    int ki = 0;
    int col = 0;
    for (LXNormalizedParameter param : device.getRemoteControls()) {
      if (ki == 16) {
        ki = 0;
        col++;
      }          
      float x = (ki % 4) * (UIKnob.WIDTH + 2) + (col * ((4 * (UIKnob.WIDTH + 2) + 15) + 2));
      float y = -3 + (ki / 4) * (UIKnob.HEIGHT);
      if (param instanceof TEColorOffsetParameter) {
        new UITEColorControl(x, y, (TEColorParameter) param.getParentParameter())
        .addToContainer(uiDevice);
      } else if (param instanceof LinkedColorParameter) {
        // TODO: find correct UI control
        new UIColorControl(x, y, (LinkedColorParameter) param)
        .addToContainer(uiDevice);
      } else if (param instanceof BoundedParameter || param instanceof DiscreteParameter || param instanceof BoundedFunctionalParameter) {
        new UIKnob(x, y)
        .setParameter(param)
        .addToContainer(uiDevice);
      } else if (param instanceof BooleanParameter) {
        new UISwitch(x, y)
        .setParameter(param)
        .addToContainer(uiDevice);
      } else if (param == null) {
        // Leave a space
      } else {
        // Hey developer: probably added a type in isEligibleControlParameter() that wasn't handled down here.
        throw new RuntimeException("Cannot generate control, unsupported pattern parameter type: " + param.getClass());
      }

      ++ki;
    }
    uiDevice.setContentWidth((col + 1) * (4 * (UIKnob.WIDTH + 2) + 15) - 15 - 2);
  }

}
