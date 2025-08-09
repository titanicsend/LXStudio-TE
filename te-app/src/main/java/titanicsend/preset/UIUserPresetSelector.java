package titanicsend.preset;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.component.UIKnob;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.ui.device.UIDeviceControls;

public class UIUserPresetSelector extends UIKnob
    implements UIDeviceControls.ParameterControl<UserPresetCollection.Selector> {

  public UIUserPresetSelector(UI ui, UserPresetCollection.Selector selector) {
    this(ui, selector, null);
  }

  public UIUserPresetSelector(UI ui, UserPresetCollection.Selector selector, LXParameter p) {
    super(0, 0, selector);

    // The selector currently bonks if used with the command engine for unknown reasons
    setUseCommandEngine(false);
  }
}
