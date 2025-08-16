package titanicsend.app.effectmgr;

import heronarts.lx.effect.LXEffect;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.TriggerParameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A translation layer between the GlobalEffectManager and an LXEffect instance. This will be
 * subclassed for every LXEffect type that can be used as a global effect.
 */
public abstract class GlobalEffect<T extends LXEffect> {
  public enum State {
    EMPTY,
    DISABLED,
    ENABLED
  }

  private final Class<T> type;
  public T effect;
  private State state;
  // Subscribe to updates on the "enabled" param
  private LXParameterListener enabledListener;
  // Keep a handle to the effect manager, so we can notify it for any state updates:
  // - Effect registered / unregistered
  // - Effect enabled / disabled
  // Since these affect button colors, we want a way to notify the Minilab3 driver
  // to refresh button colors. If ObservableList had an "itemUpdated" option, that would
  // work - but for now, the simplest thing feels like adding a hook to notify the manager,
  // and then Minilab3 can have a GlobalEffectManager.Listener which will get triggered
  // and redraw the pad LED's.
  private GlobalEffectManager manager;

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

  public final boolean matches(LXEffect match) {
    return this.type.isInstance(match);
  }

  public final void registerEffect(LXEffect lxEffect, GlobalEffectManager manager) {
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
        onUnregister();
      }
      this.effect = effect;
      // important: register(null, null) is effectively unregister.
      // we'll need manager handle to trigger callback for onRegister()
      // and onUnregister().
      if (manager != null) {
        this.manager = manager;
      }
      if (this.effect != null) {
        onRegister();
      }
    }
  }

  // TODO(look): allow custom name when allocating slots, if we had multiple versions of an effect?
  //             Maybe not relevant if we have presets and a good way to switch between them.
  public String getName() {
    if (this.effect == null) {
      return String.format("[Class] %s", this.type.getSimpleName());
    } else {
      return String.format("[LXEffect] %s", this.effect.getLabel());
    }
  }

  public State getState() {
    return state;
  }

  /**
   * State is only updated internally, so we can validate the state transition (e.g. so if state is
   * ENABLED/DISABLED we never need to check if LXEffect is null), and also so we can notify
   * GlobalEffectManager listeners.
   *
   * @param targetState, the new state that the underlying LXEffect is already updated to.
   */
  private void updateState(State targetState) {
    if (this.effect == null) {
      if (targetState == State.ENABLED
          || targetState == State.DISABLED
          || targetState == State.EMPTY) {
        throw new IllegalStateException(
            "Effect "
                + this.type.getName()
                + " has null LXEffect, cannot transition to "
                + targetState.toString());
      }
    } else if (targetState == State.ENABLED && !this.effect.isEnabled()) {
      throw new IllegalStateException(
          "Effect " + this.type.getName() + " is disabled, cannot transition to ENABLED");
    } else if (targetState == State.DISABLED && this.effect.isEnabled()) {
      throw new IllegalStateException(
          "Effect " + this.type.getName() + " is enabled, cannot transition to DISABLED");
    }
    this.state = targetState;
    // Notify manager's listeners (to refresh MIDI controller pad LEDs)
    if (this.manager != null) {
      this.manager.effectStateUpdated(this);
    }
  }

  /**
   * Called when a new effect instance is tied to this GlobalEffect slot. Subclasses can override.
   */
  protected void onRegister() {
    BooleanParameter enabledParam = getEnabledParameter();
    if (enabledParam != null) {
      updateState(enabledParam.isOn() ? State.ENABLED : State.DISABLED);
      enabledListener =
          new LXParameterListener() {
            @Override
            public void onParameterChanged(LXParameter parameter) {
              if (parameter instanceof BooleanParameter) {
                boolean isEnabled = ((BooleanParameter) parameter).getValueb();
                updateState(isEnabled ? State.ENABLED : State.DISABLED);
              } else {
                throw new IllegalStateException("Invalid parameter " + parameter);
              }
            }
          };
      enabledParam.addListener(enabledListener);
    } else {
      throw new IllegalStateException(
          "enabledParameter cannot be null when GlobalEffect is registered");
    }
  }

  protected void onUnregister() {
    state = State.EMPTY;
    if (enabledListener != null) {
      BooleanParameter enabledParam = getEnabledParameter();
      if (enabledParam != null) {
        enabledParam.removeListener(enabledListener);
      }
    }
  }

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

  public void dispose() {
    registerEffect(null, null);
  }
}
