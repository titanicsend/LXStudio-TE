package titanicsend.ui;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UI2dContainer.Layout;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UISwitch;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedFunctionalParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import titanicsend.color.TEColorParameter;
import titanicsend.ndi.NDIPattern;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.preset.UIUserPresetCollection;

/**
 * Device UI for TEPerformancePattern
 *
 * Adds special UI control for TEColorParameter. Parameters are arranged in columns of 4 to line
 * up with the MidiFighterTwister.
 *
 * Based on UIDeviceControl with components from J.Belcher's Rubix project.
 */
public class UITEPerformancePattern
    implements UIDeviceControls<TEPerformancePattern>, LXParameterListener {

  private heronarts.lx.studio.LXStudio.UI ui;
  private UIDevice uiDevice;
  private TEPerformancePattern device;
  private final List<UI2dComponent> controls = new ArrayList<UI2dComponent>();

  @Override
  public void buildDeviceControls(
      heronarts.lx.studio.LXStudio.UI ui, UIDevice uiDevice, TEPerformancePattern device) {
    uiDevice.setLayout(Layout.HORIZONTAL, 2);

    this.ui = ui;
    this.uiDevice = uiDevice;
    this.device = device;
    this.device.remoteControlsChanged.addListener(this);

    refresh();
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (parameter == this.device.remoteControlsChanged) {
      refresh();
    }
  }

  protected void refresh() {
    clearControls();
    this.uiDevice.addChildren(
      createControls(),
      createUserPresets()
    );

  }

  /*
   * Remote Controls
   */

  private void clearControls() {
    this.uiDevice.removeAllChildren(true);
  }

  private UI2dContainer createControls() {
    UI2dContainer container = new UI2dContainer(0,0,0, this.uiDevice.getContentHeight());

    List<LXNormalizedParameter> params =
        new ArrayList<LXNormalizedParameter>(Arrays.asList(device.getRemoteControls()));

    // Extra controls, displayed only in design mode
    params.add(device.getControls().getControl(TEControlTag.BRIGHTNESS).control);
    params.add(device.getControls().getControl(TEControlTag.TWIST).control);
    // add two spaces so we wind up with two columns of these extra controls
    // in the design UI - this makes them more visible without expanding
    // the Chromatik window.
    params.add(null);
    params.add(null);
    params.add(null);
    params.add(null);

    // slight hack to add source select and gain controls for NDI patterns without
    // replicating all this UI code...
    if (device instanceof NDIPattern) {
      params.add(((NDIPattern) device).getSourceControl());
      params.add(((NDIPattern) device).getGainControl());
    }

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
      if (param instanceof TEColorParameter.TEColorOffsetParameter) {
        this.controls.add(
            new UITEColorControl(x, y, (TEColorParameter) param.getParentParameter())
                .addToContainer(container));
      } else if (param instanceof BoundedParameter
          || param instanceof DiscreteParameter
          || param instanceof BoundedFunctionalParameter) {
        this.controls.add(new UIKnob(x, y).setParameter(param).addToContainer(container));
      } else if (param instanceof BooleanParameter) {
        this.controls.add(new UISwitch(x, y).setParameter(param).addToContainer(container));
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
    container.setWidth((col + 1) * (4 * (UIKnob.WIDTH + 2) + 15) - 15 - 2);

    return container;
  }

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

  /*
   * User Presets
   */

  private UI2dContainer createUserPresets() {
    return new UIUserPresetCollection(this.ui, this.device, uiDevice.getContentHeight());
  }

  @Override
  public void disposeDeviceControls(
      heronarts.lx.studio.LXStudio.UI ui, UIDevice uiDevice, TEPerformancePattern device) {
    if (this.device != null) {
      clearControls();
      this.device.remoteControlsChanged.removeListener(this);
      this.device = null;
      this.uiDevice = null;
    }
  }
}
