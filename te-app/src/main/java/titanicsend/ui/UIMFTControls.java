package titanicsend.ui;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UISwitch;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedFunctionalParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.studio.LXStudio;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import titanicsend.color.TEColorParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;

/**
 * A set of UI Knobs matching the layout of a MidiFighterTwister, for use in a device UI. The device
 * is a TEPerformancePattern only to handle our "unused controls" list.
 */
public class UIMFTControls extends UI2dContainer implements LXParameterListener {

  private final TEPerformancePattern device;
  private final List<UI2dComponent> controls = new ArrayList<>();

  public UIMFTControls(LXStudio.UI ui, TEPerformancePattern device, float height) {
    super(0, 0, 0, height);

    this.device = device;
    this.device.remoteControlsChanged.addListener(this, true);
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (parameter == this.device.remoteControlsChanged) {
      refresh();
    }
  }

  protected void refresh() {
    clearControls();
    buildControls();
  }

  private void clearControls() {
    // Remove only the parameter controls, not all children.
    for (UI2dComponent control : this.controls) {
      control.removeFromContainer();
    }
    this.controls.clear();
  }

  private void buildControls() {
    List<LXNormalizedParameter> params = new ArrayList<>(Arrays.asList(device.getRemoteControls()));

    // For the UI, replace unused remote controls with null
    hideUnusedControls(params);

    // Fill in the remaining first MFT UI space with blanks
    while (params.size() < 16) {
      params.add(null);
    }

    // Extra controls, displayed only in design mode
    params.add(device.getControls().getControl(TEControlTag.BRIGHTNESS).control);
    // TODO - twist disabled for now.  We may add it again later.
    // params.add(device.getControls().getControl(TEControlTag.TWIST).control);

    int ki = 0;
    int col = 0;
    for (LXNormalizedParameter param : params) {
      if (ki == 16) {
        ki = 0;
        col++;
      }
      float x = (ki % 4) * (UIKnob.WIDTH + 2) + (col * ((4 * (UIKnob.WIDTH + 2) + 15) + 2));
      float y = -3 + (ki / 4) * (UIKnob.HEIGHT);
      if (param instanceof TEColorParameter.TEColorOffsetParameter) {
        this.controls.add(
            new UITEColorControl(x, y, (TEColorParameter) param.getParentParameter())
                .addToContainer(this));
      } else if (param instanceof BoundedParameter
          || param instanceof DiscreteParameter
          || param instanceof BoundedFunctionalParameter) {
        this.controls.add(new UIKnob(x, y).setParameter(param).addToContainer(this));
      } else if (param instanceof BooleanParameter) {
        this.controls.add(new UISwitch(x, y).setParameter(param).addToContainer(this));
      } else if (param == null) {
        // Leave a space
      } else {
        // Hey developer: probably added a type in isEligibleControlParameter() that wasn't handled
        // down here.
        throw new RuntimeException(
            "Cannot generate control, unsupported pattern parameter type: " + param.getClass());
      }

      ++ki;
    }
    this.setContentWidth((col + 1) * (4 * (UIKnob.WIDTH + 2) + 15) - 15 - 2);
  }

  /**
   * Replace unused controls with null in the UI list. By keeping them in the remote controls list
   * we prevent MFT reboots on pattern change.
   */
  private void hideUnusedControls(List<LXNormalizedParameter> params) {
    for (int i = 0; i < params.size(); i++) {
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
  public void dispose() {
    this.device.remoteControlsChanged.removeListener(this);
    super.dispose();
  }
}
