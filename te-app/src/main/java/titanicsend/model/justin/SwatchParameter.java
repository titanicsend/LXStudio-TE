package titanicsend.model.justin;

import heronarts.lx.LX;
import heronarts.lx.parameter.ObjectParameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SwatchParameter extends ObjectParameter<SwatchDefinition>
    implements DisposableParameter {

  public interface Listener {
    void swatchesChanged(SwatchParameter parameter);
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
  public SwatchParameter addSwwatchParamListener(Listener listener) {
    Objects.requireNonNull(listener);
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException(
          "Cannot add duplicate SelectedSwatchParameter.Listener: " + listener);
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
  public SwatchParameter removeSwatchParamListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException(
          "May not remove non-registered SelectedSwatchParameter.Listener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  protected void notifyListeners() {
    for (Listener listener : this.listeners) {
      listener.swatchesChanged(this);
    }
  }

  private final List<DisposeListener> disposeListeners = new ArrayList<DisposeListener>();

  public void listenDispose(DisposeListener listener) {
    disposeListeners.add(listener);
  }

  public void unlistenDispose(DisposeListener listener) {
    if (!disposeListeners.remove(listener)) {
      LX.warning("Tried to remove unregistered DisposeListener");
    }
  }

  private void notifyDisposing() {
    for (int i = disposeListeners.size() - 1; i >= 0; --i) {
      disposeListeners.remove(i).disposing(this);
    }
  }

  @Override
  public void dispose() {
    notifyDisposing();
    super.dispose();
  }
}
