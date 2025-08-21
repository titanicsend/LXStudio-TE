package titanicsend.app.effectmgr;

import heronarts.lx.LXDeviceComponent;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.utils.LXUtils;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import titanicsend.preset.PresetEngine;
import titanicsend.preset.UserPreset;
import titanicsend.preset.UserPresetCollection;

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

  // Referencing a preset
  private final boolean isPreset;
  private final boolean hasPresetName;
  private final String presetName;
  private final int presetIndex;

  /** Create a new slot */
  @SuppressWarnings("unchecked")
  public Slot() {
    this(false, false, null, 0);
  }

  /** Create new slot that looks for a preset by name */
  public Slot(String presetName) {
    this(true, true, presetName, 0);
  }

  /** Create a new slot that looks for a preset by index */
  public Slot(int presetIndex) {
    this(true, false, null, presetIndex);
  }

  private Slot(boolean isPreset, boolean hasPresetName, String presetName, int presetIndex) {
    this.isPreset = isPreset;
    this.hasPresetName = hasPresetName;
    this.presetName = presetName;
    this.presetIndex = presetIndex;

    // Safety checks
    if (isPreset) {
      if (hasPresetName) {
        if (LXUtils.isEmpty(presetName)) {
          throw new IllegalArgumentException("PresetName cannot be null or empty");
        }
      } else {
        if (presetIndex < 0) {
          throw new IllegalArgumentException("PresetIndex must be >= 0");
        }
      }
    }

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
  public final BooleanParameter getTriggerParameter() {
    if (this.device == null) {
      return null;
    }
    return _getTriggerParameter();
  }

  /**
   * Subclasses can provide a "run now" parameter for the device, if applicable. In an inconvenient
   * naming alignment it does not have to be of type "TriggerParameter"; it can be of type
   * "BooleanParameter", either Momentary or Toggle.
   */
  protected BooleanParameter _getTriggerParameter() {
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

  // Presets

  public boolean isPreset() {
    return this.isPreset;
  }

  // Helper methods that apply the preset before running
  // TODO: make this slot look empty if the matching preset is not available

  /** Apply preset if used, then trigger the device */
  public boolean trigger(boolean on) {
    BooleanParameter parameter = getTriggerParameter();

    if (on) {
      // Apply preset on trigger ON but not on trigger OFF
      if (applyPreset()) {
        if (parameter != null) {
          if (parameter instanceof TriggerParameter t) {
            t.trigger();
          } else {
            if (parameter.getMode() == BooleanParameter.Mode.TOGGLE) {
              // Toggle Mode
              parameter.toggle();
            } else {
              // Momentary Mode
              parameter.setValue(true);
            }
          }
          return true;
        }
      }
      // Preset or Parameter not found
      return false;
    } else {
      // Turning the trigger off, i.e. button release
      if (parameter != null) {
        // No-op for TriggerParameters, they have already turned themselves off.
        if (!(parameter instanceof TriggerParameter)) {
          if (parameter.getMode() == BooleanParameter.Mode.MOMENTARY) {
            // Only Momentary Mode will turn off with button/key release
            parameter.setValue(false);
          }
        }
        return true;
      } else {
        // Parameter not found
        return false;
      }
    }
  }

  /** Apply preset if possible. Returns true if the correct preset was found and applied. */
  protected final boolean applyPreset() {
    if (this.device == null) {
      return false;
    }
    if (!this.isPreset) {
      // Slot does not use a preset
      return true;
    } else {
      UserPreset foundPreset = null;
      // Pull the current list of presets for this device
      UserPresetCollection presetCollection = PresetEngine.get().getLibrary().get(this.device);
      if (this.hasPresetName) {
        // Find preset by name
        for (UserPreset preset : presetCollection.presets) {
          if (this.presetName.equalsIgnoreCase(preset.getLabel())) {
            foundPreset = preset;
            break;
          }
        }
      } else {
        // Find preset by index
        if (this.presetIndex < presetCollection.presets.size()) {
          foundPreset = presetCollection.presets.get(this.presetIndex);
        }
      }

      // If preset was found, apply it!
      if (foundPreset != null) {
        PresetEngine.get().applyPreset(foundPreset, this.device);
        return true;
      }

      // Preset was not found
      return false;
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
