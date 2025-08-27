package titanicsend.parameter;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.component.UIKnob;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.ui.device.UIDeviceControls;

public class UIOffairDiscreteParameter extends UIKnob
    implements UIDeviceControls.ParameterControl<OffairDiscreteParameter> {

  public UIOffairDiscreteParameter(UI ui, OffairDiscreteParameter parameter) {
    this(ui, parameter, null);
  }

  public UIOffairDiscreteParameter(
      UI ui, OffairDiscreteParameter parameter, LXParameter childParameter) {
    super(0, 0, parameter);

    // There's an issue when this wraps a preset selector. It needs to not use the command engine.
    setUseCommandEngine(false);
  }
}
