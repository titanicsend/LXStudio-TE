package titanicsend.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedFunctionalParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer.Layout;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UISwitch;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.TEPerformancePattern.TEColorParameter;
import titanicsend.pattern.TEPerformancePattern.TEColorParameter.TEColorOffsetParameter;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.util.TE;

/**
 * Device UI for TEPerformancePattern
 * 
 * Adds special UI control for TEColorParameter. 
 * Parameters are arranged in columns of 4 to line up with the MidiFighterTwister.
 * 
 * Based on UIDeviceControl with components from J.Belcher's Rubix project.
 */
public class UITEPerformancePattern implements UIDeviceControls<TEPerformancePattern>, LXParameterListener {

  private UIDevice uiDevice;
  private TEPerformancePattern device;
  private final List<UI2dComponent> controls = new ArrayList<UI2dComponent>();

  @Override
  public void buildDeviceControls(heronarts.lx.studio.LXStudio.UI ui, UIDevice uiDevice, TEPerformancePattern device) {
    uiDevice.setLayout(Layout.NONE);
    uiDevice.setChildSpacing(2);

    this.uiDevice = uiDevice;
    this.device = device;
    this.device.remoteControlsChanged.addListener(this);

    addControls();
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (parameter == this.device.remoteControlsChanged) {
      refresh();
    }
  }

  protected void refresh() {
    clearControls();
    addControls();
  }

  private void clearControls() {
    for (UI2dComponent control : this.controls) {
      try {
        control.removeFromContainer();
        control.dispose();
      } catch (Exception ex) {
        TE.log("Warning in UITEPerformancePattern: error removing control from container: " + ex.toString());
      }
    }
    this.controls.clear();
  }

  private void addControls() {
    List<LXNormalizedParameter> params = new ArrayList<LXNormalizedParameter>(Arrays.asList(device.getRemoteControls()));

    // For design mode, append Brightness.  Useful for AutoVJ especially.
    params.add(device.getControls().getControl(TEControlTag.BRIGHTNESS).control);

    // For the UI, replace unused controls with null
    hideUnusedControls(params);

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
        this.controls.add(
          new UITEColorControl(x, y, (TEColorParameter) param.getParentParameter())
          .addToContainer(uiDevice));
      } else if (param instanceof BoundedParameter || param instanceof DiscreteParameter || param instanceof BoundedFunctionalParameter) {
        this.controls.add(
          new UIKnob(x, y)
          .setParameter(param)
          .addToContainer(uiDevice));
      } else if (param instanceof BooleanParameter) {
        this.controls.add(
          new UISwitch(x, y)
          .setParameter(param)
          .addToContainer(uiDevice));
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

  private void hideUnusedControls(List<LXNormalizedParameter> params) {
    for (int i=0; i < params.size(); i++) {
      LXNormalizedParameter p = params.get(i);
      if (p != null && isUnusedControl(p)) {
        params.set(i, null);
      }
    }
  }

  private boolean isUnusedControl(LXNormalizedParameter p) {
    return this.device.getControls().unusedParams.contains(p);
  }

  @Override
  public void disposeDeviceControls(heronarts.lx.studio.LXStudio.UI ui, UIDevice uiDevice, TEPerformancePattern device) {
    if (this.device != null) {
      clearControls();
      this.device.remoteControlsChanged.removeListener(this);
      this.device = null;
      this.uiDevice = null;
    }
  }
}
