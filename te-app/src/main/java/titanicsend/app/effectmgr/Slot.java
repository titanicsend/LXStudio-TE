package titanicsend.app.effectmgr;

import heronarts.lx.LXDeviceComponent;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.pattern.LXPattern;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A translation layer between the GlobalEffectManager and an effect or pattern instance. This will
 * be subclassed for every LXDeviceComponent type that can be used as a global effect.
 */
public abstract class Slot<T extends LXDeviceComponent> {

  public enum State {
    EMPTY, // Slot has been allocated, but no matching device exists in the current project
    DISABLED,
    ENABLED
  }

  // Default to EMPTY, meaning 'this.device' is null
  private State state = State.EMPTY;

  public interface Listener {
    void stateChanged(Slot slot, State state);
  }

  private final List<Listener> listeners = new ArrayList<>();

  // Class of the registered Device
  private final Class<T> type;

  // LXDeviceComponent matching 'this.type'.
  public T device;

  @SuppressWarnings("unchecked")
  public Slot() {
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

  // Methods that should be overridden to provide a common interface to a Device

  /**
   * Retrieve the parameter that corresponds to enabling/disabling the device. By default this will
   * be device.enabled but it can be modified by a child class.
   *
   * @return A parameter corresponding to enable/disable, or null if it does not apply.
   */
  public final BooleanParameter getEnabledParameter() {
    if (this.device == null) {
      return null;
    }
    return _getEnabledParameter();
  }

  /** Subclasses can override. Will only get called if device is non-null */
  protected BooleanParameter _getEnabledParameter() {
    // Note: Both LXEffect and LXPattern have an enabled parameter, but it is not on the parent
    if (this.device instanceof LXEffect) {
      return ((LXEffect) device).enabled;
    } else if (this.device instanceof LXPattern) {
      return ((LXPattern) device).enabled;
    }
    // Shouldn't be reached. This method is only called if device is not null.
    return null;
  }

  /**
   * Retrieve the parameter that corresponds to the level or Amount of the device that is being
   * applied.
   *
   * @return A parameter that corresponds to Level, or null if it does not apply.
   */
  public final LXListenableNormalizedParameter getLevelParameter() {
    if (this.device == null) {
      return null;
    }
    return _getLevelParameter();
  }

  protected abstract LXListenableNormalizedParameter _getLevelParameter();

  /**
   * Retrieve an optional secondary parameter for the device
   *
   * @return A secondary parameter that can be controlled on the global device
   */
  public final LXListenableNormalizedParameter getSecondaryParameter() {
    if (this.device == null) {
      return null;
    }
    return _getSecondaryParameter();
  }

  /**
   * Subclasses can override to indicate a secondary parameter. Only called if device is non-null.
   *
   * @return A secondary parameter that can be controlled on the global device
   */
  protected LXListenableNormalizedParameter _getSecondaryParameter() {
    return null;
  }

  /**
   * Retrieve a parameter that is the "run now" trigger parameter for the device, if applicable.
   *
   * @return A parameter that corresponds to Run Now, or null if it does not apply.
   */
  public final TriggerParameter getTriggerParameter() {
    if (this.device == null) {
      return null;
    }
    return _getTriggerParameter();
  }

  protected TriggerParameter _getTriggerParameter() {
    return null;
  }

  /**
   * TODO(look): allow custom name when allocating slots, if we had multiple versions of a device?
   * Maybe not relevant if we have presets and a good way to switch between them.
   */
  public String getName() {
    if (this.device == null) {
      return String.format("[Class] %s", this.type.getSimpleName());
    } else {
      return String.format("[LXDeviceComponent] %s", this.device.getLabel());
    }
  }

  // Device Management

  public final boolean matches(LXDeviceComponent match) {
    return this.type.isInstance(match);
  }

  /** Add this listener to any parameter that contributes to the State calculation */
  private final LXParameterListener stateParameterListener =
      p -> {
        refreshState();
      };

  /** Internal method, package-scoped to be used by GlobalEffectManager */
  final void setDevice(LXDeviceComponent lxDevice) {
    if (lxDevice != null && !this.type.isInstance(lxDevice)) {
      throw new IllegalArgumentException(
          "Device instance "
              + device.getLabel()
              + " does not match Slot type "
              + this.type.getName());
    }

    T device = this.type.cast(lxDevice);
    if (this.device != device) {
      if (this.device != null) {
        unregister();
      }
      this.device = device;
      if (this.device != null) {
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

  /** A new device instance has been tied to this slot. Subclasses can override. */
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
    if (this.device == null) {
      return State.EMPTY;
    }

    return getEnabledParameter().isOn() ? State.ENABLED : State.DISABLED;
  }

  // Listeners

  public Slot<T> addListener(Listener listener) {
    Objects.requireNonNull(listener, "May not add null Listener");
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate Listener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  public Slot<T> removeListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot remove non-existent Listener: " + listener);
    }
    this.listeners.remove(listener);
    return this;
  }

  // Shutdown

  public void dispose() {
    setDevice(null);
    this.listeners.clear();
  }
}
