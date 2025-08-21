package titanicsend.ui;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UISwitch;
import heronarts.lx.LXDeviceComponent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import titanicsend.color.TEColorParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.preset.UIUserPresetSelector;
import titanicsend.preset.UserPresetCollection;

/**
 * A set of UI Knobs matching the layout of a MidiFighterTwister, for use in a device UI. The device
 * is a TEPerformancePattern only to handle our "unused controls" list.
 */
public class UIMFTControls extends UI2dContainer implements LXParameterListener {

  private final LXStudio.UI ui;
  private final LXDeviceComponent device;
  private final boolean isPerformancePattern;
  private final TEPerformancePattern tePerformancePattern;
  private final Map<LXNormalizedParameter, UI2dComponent> controls = new HashMap<>();

  public UIMFTControls(LXStudio.UI ui, LXDeviceComponent device, float height) {
    super(0, 0, 0, height);
    this.ui = ui;

    this.device = device;
    this.isPerformancePattern = device instanceof TEPerformancePattern;
    this.tePerformancePattern = isPerformancePattern ? (TEPerformancePattern) device : null;

    // Refresh UI controls now and after any changes to remote controls
    this.device.remoteControlsChanged.addListener(this, true);
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (parameter == this.device.remoteControlsChanged) {
      refresh();
    }
  }

  /** Rebuild parameter UI controls. Do not touch other child controls. */
  private void refresh() {
    // Remember previous controls so we can reuse them or dispose them at the end
    Map<LXNormalizedParameter, UI2dComponent> oldControls = new HashMap<>(this.controls);
    this.controls.clear();

    // Get new controls from the device
    List<LXNormalizedParameter> params = new ArrayList<>(Arrays.asList(device.getRemoteControls()));

    // Fill in the remaining first MFT UI space with blanks
    while (params.size() < 16) {
      params.add(null);
    }

    if (this.isPerformancePattern) {
      // Add device-specific parameters that are UI-only
      for (LXNormalizedParameter subclassParam : this.tePerformancePattern.subclassParameters) {
        if (!params.contains(subclassParam) && !isUnusedControl(subclassParam)) {
          params.add(subclassParam);
        }
      }

      // Hide [from the UI] TE common controls that are not used but will still be in the remote
      // list
      hideUnusedControls(params);
    }

    // Build MFT-style layout
    int ki = 0;
    int col = 0;
    for (LXNormalizedParameter param : params) {
      if (ki == 16) {
        ki = 0;
        col++;
      }
      float x = (ki % 4) * (UIKnob.WIDTH + 2) + (col * ((4 * (UIKnob.WIDTH + 2) + 15) + 2));
      float y = -3 + (ki / 4) * (UIKnob.HEIGHT);

      // Leave a space for null parameters
      if (param != null) {
        // If a parameter is in the remote list twice, skip entries after the first.
        if (!this.controls.containsKey(param)) {

          // Retrieve existing UI control
          UI2dComponent control = oldControls.remove(param);

          // Or create new UI control
          if (control == null) {
            control = getNewControl(param);
            control.addToContainer(this);
            this.controls.put(param, control);
          }

          // Position control at the current MFT knob
          control.setPosition(x, y);
        }
      }

      ++ki;
    }
    this.setContentWidth((col + 1) * (4 * (UIKnob.WIDTH + 2) + 15) - 15 - 2);

    // Dispose old UI controls that were not reused
    for (Map.Entry<LXNormalizedParameter, UI2dComponent> entry : oldControls.entrySet()) {
      UI2dComponent control = entry.getValue();
      control.removeFromContainer().dispose();
    }
  }

  /**
   * Replace unused controls with null in the UI list. By keeping them in the remote controls list,
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

  /** Use TECommonControls to determine if a common parameter is marked as "unused" */
  private boolean isUnusedControl(LXNormalizedParameter p) {
    return this.tePerformancePattern.getControls().unusedParams.contains(p);
  }

  /** Build a new UI component for a remote control parameter */
  private UI2dComponent getNewControl(LXNormalizedParameter param) {
    if (param instanceof TEColorParameter.TEColorOffsetParameter) {
      return new UITEColorControl(0, 0, (TEColorParameter) param.getParentParameter());
    } else if (param instanceof UserPresetCollection.Selector selector) {
      // Avoid command engine with userPreset selector, it bonks for unknown reasons
      return new UIUserPresetSelector(this.ui, selector);
    } else if (param instanceof BoundedParameter
        || param instanceof DiscreteParameter
        || param instanceof BoundedFunctionalParameter) {
      return new UIKnob(x, y).setParameter(param);
    } else if (param instanceof BooleanParameter) {
      return new UISwitch(x, y).setParameter(param);
    } else {
      throw new RuntimeException(
          "Cannot generate control, unsupported pattern parameter type: " + param.getClass());
    }
  }

  @Override
  public void dispose() {
    this.device.remoteControlsChanged.removeListener(this);
    super.dispose();
  }
}
