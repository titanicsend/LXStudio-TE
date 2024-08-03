package titanicsend.quantize;

import heronarts.lx.LX;
import heronarts.lx.parameter.TriggerParameter;

public class QuantizeTriggerParameter extends TriggerParameter {

  private final Quantizer quantizer;

  public QuantizeTriggerParameter(LX lx, String label) {
    super(label);
    this.quantizer = new Quantizer(lx, this::onQuantizeEvent);
  }

  @Override
  public QuantizeTriggerParameter setValue(boolean value) {
    // Delay trigger on, do not delay trigger off
    setValue(value, !value);
    return this;
  }

  public QuantizeTriggerParameter setValue(boolean value, boolean immediate) {
    if (immediate) {
      super.setValue(value);
    } else {
      this.quantizer.queue();
    }
    return this;
  }

  /**
   * Enable or disable quantized inputs
   * @param on Whether to delay input until the next quantized event
   * @return this
   */
  public QuantizeTriggerParameter setQuantizeEnabled(boolean on) {
    this.quantizer.setEnabled(on);
    return this;
  }

  private void onQuantizeEvent() {
    setValue(true, true);
  }

  @Override
  public void dispose() {
    this.quantizer.dispose();
    super.dispose();
  }

}
