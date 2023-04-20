package titanicsend.ui;

import java.util.Arrays;
import java.util.List;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedFunctionalParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.p4lx.ui.UI2dContainer.Layout;
import heronarts.p4lx.ui.component.UIKnob;
import heronarts.p4lx.ui.component.UISwitch;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.TEPerformancePattern.TEColorParameter;
import titanicsend.pattern.TEPerformancePattern.TEColorParameter.TEColorOffsetParameter;

/**
 * Device UI for TEPerformancePattern
 * 
 * Adds special UI control for TEColorParameter. 
 * Parameters are arranged in columns of 4 to line up with the MidiFighterTwister.
 * 
 * Based on UIDeviceControl with components from J.Belcher's Rubix project.
 */
public class UITEPerformancePattern implements UIDeviceControls<TEPerformancePattern> {

  @Override
  public void buildDeviceControls(heronarts.lx.studio.LXStudio.UI ui, UIDevice uiDevice, TEPerformancePattern device) {
    uiDevice.setLayout(Layout.NONE);
    uiDevice.setChildSpacing(2);

    List<LXNormalizedParameter> params = Arrays.asList(device.getRemoteControls());

    int ki = 0;
    int col = 0;
    for (LXNormalizedParameter param : params) {
      if (ki == 16) {
        ki = 0;
        col++;
      }          
      float x = (ki % 4) * (UIKnob.WIDTH + 2) + (col * ((4 * (UIKnob.WIDTH + 2) + 15) + 2));
      float y = -3 + (ki / 4) * (UIKnob.HEIGHT);
      if (param instanceof TEColorOffsetParameter) {
        new UITEColorControl(x, y, (TEColorParameter) param.getParentParameter())
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
