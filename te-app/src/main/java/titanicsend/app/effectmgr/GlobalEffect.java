package titanicsend.app.effectmgr;

import heronarts.lx.effect.LXEffect;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.TriggerParameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A translation layer between the GlobalEffectManager and an LXEffect instance. This will be
 * subclassed for every LXEffect type that can be used as a global effect.
 */
public abstract class GlobalEffect<T extends LXEffect> {

  public enum State {
    EMPTY, // Slot has been allocated, but no matching LXEffect exists in the current project
    DISABLED,
    ENABLED
  }

  // Default to EMPTY, meaning 'this.effect' is null / no LXEffect matching 'this.type' is on
  // the master bus.
  private State state = State.EMPTY;

  public interface Listener {
    void stateChanged(GlobalEffect globalEffect, State state);
  }

  private final List<Listener> listeners = new ArrayList<>();

  // Class of the registered Effect.
  private final Class<T> type;

  // LXEffect matching 'this.type'.
  public T effect;

  @SuppressWarnings("unchecked")
  public GlobalEffect() {
    // Extract the generic type parameter (T) of the subclass
    Type superType = getClass().getGenericSuperclass();
    if (superType instanceof ParameterizedType pType) {
      Type typeArg = pType.getActualTypeArguments()[0];
      if (typeArg instanceof Class<?> cType) {
        this.type = (Class<T>) cType;
      } else {
        throw new IllegalStateException("Cannot resolve generic type for " + getClass().getName());
      }
    } else {
      throw new IllegalStateException("Cannot resolve generic type for " + getClass().getName());
    }
  }

  // Methods that should be overridden to provide a common interface to an Effect

  /**
   * Retrieve the parameter that corresponds to enabling/disabling the effect. By default this will
   * be LXEffect.enabled but it can be overridden.
   *
   * @return A parameter corresponding to enable/disable, or null if it does not apply.
   */
  public BooleanParameter getEnabledParameter() {
    if (this.effect == null) {
      return null;
    }
    return this.effect.enabled;
  }

  /**
   * Retrieve the parameter that corresponds to the level or Amount of the effect that is being
   * applied.
   *
   * @return A parameter that corresponds to Level, or null if it does not apply.
   */
  public abstract LXListenableNormalizedParameter getLevelParameter();

  /**
   * Subclasses can override to indicate a secondary parameter.
   *
   * @return A secondary parameter that can be controlled on the global effect
   */
  public LXListenableNormalizedParameter getSecondaryParameter() {
    return null;
  }

  /**
   * Retrieve a parameter that is the "run now" trigger parameter for the effect, if applicable.
   *
   * @return A parameter that corresponds to Run Now, or null if it does not apply.
   */
  public TriggerParameter getTriggerParameter() {
    return null;
  }

  /**
   * TODO(look): allow custom name when allocating slots, if we had multiple versions of an effect?
   * Maybe not relevant if we have presets and a good way to switch between them.
   */
  public String getName() {
    if (this.effect == null) {
      return String.format("[Class] %s", this.type.getSimpleName());
    } else {
      return String.format("[LXEffect] %s", this.effect.getLabel());
    }
  }

  // Device Management

  public final boolean matches(LXEffect match) {
    return this.type.isInstance(match);
  }

  /** Add this listener to any parameter that contributes to the State calculation */
  private final LXParameterListener stateParameterListener =
      p -> {
        refreshState();
      };

  /** Internal method, package-scoped to be used by GlobalEffectManager */
  final void setEffect(LXEffect lxEffect) {
    if (lxEffect != null && !this.type.isInstance(lxEffect)) {
      throw new IllegalArgumentException(
          "Effect instance "
              + effect.getLabel()
              + " does not match GlobalEffect type "
              + this.type.getName());
    }

    T effect = this.type.cast(lxEffect);
    if (this.effect != effect) {
      if (this.effect != null) {
        unregister();
      }
      this.effect = effect;
      if (this.effect != null) {
        register();
      }
      refreshState();
    }
  }

  private void register() {
    BooleanParameter enabledParam = getEnabledParameter();
    enabledParam.addListener(this.stateParameterListener);
  }

  private void unregister() {
    BooleanParameter enabledParam = getEnabledParameter();
    enabledParam.removeListener(this.stateParameterListener);
  }

  /** A new effect instance has been tied to this slot. Subclasses can override. */
  protected void onRegister() {}

  /** Subclasses can override */
  protected void onUnregister() {}

  // State management

  public State getState() {
    return this.state;
  }

  /** Perform a recalculation of the current state */
  private void refreshState() {
    State state = calcState();
    if (this.state != state) {
      this.state = state;
      for (Listener listener : this.listeners) {
        listener.stateChanged(this, this.state);
      }
    }
  }

  /** Determine the current state. Subclasses can override. */
  protected State calcState() {
    if (this.effect == null) {
      return State.EMPTY;
    }

    return getEnabledParameter().isOn() ? State.ENABLED : State.DISABLED;
  }

  // Listeners

  public GlobalEffect<T> addListener(Listener listener) {
    Objects.requireNonNull(listener, "May not add null Listener");
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate Listener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  public GlobalEffect<T> removeListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot remove non-existent Listener: " + listener);
    }
    this.listeners.remove(listener);
    return this;
  }

  // Shutdown

  public void dispose() {
    setEffect(null);
    this.listeners.clear();
  }
}
