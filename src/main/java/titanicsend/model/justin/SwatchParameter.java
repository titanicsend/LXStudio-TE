package titanicsend.model.justin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import heronarts.lx.parameter.ObjectParameter;

public class SwatchParameter extends ObjectParameter<SwatchDefinition> {

  public interface Listener {
    public void swatchesChanged(SwatchParameter parameter);
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  public SwatchParameter(String label, SwatchDefinition[] objects) {
    super(label, objects);
    setIncrementMode(IncrementMode.RELATIVE);
    setWrappable(false);
  }

  @Override
  public ObjectParameter<SwatchDefinition> setObjects(SwatchDefinition[] objects) {
    super.setObjects(objects);
    return this;
  }

  // Listener management code sourced from LXPalette

  /**
   * Registers a listener
   *
   * @param listener Parameter listener
   * @return this
   */
  public SwatchParameter addListener(Listener listener) {
    Objects.requireNonNull(listener);
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate SelectedSwatchParameter.Listener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  /**
   * Unregisters a listener
   *
   * @param listener Parameter listener
   * @return this
   */
  public SwatchParameter removeListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("May not remove non-registered SelectedSwatchParameter.Listener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  protected void notifyListeners() {
    for (Listener listener : this.listeners) {
      listener.swatchesChanged(this);
    }
  }
}
