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
    // TODO(look): I'm taking EMPTY to mean GlobalEffectManager has "allocated" a lot, but
    //             no LXEffect has been "registered" (actually added to the master bus).
    //             Do I need a concept of "unregistered"? Or is that just 'null' (or maybe even
    //             the GlobalEffect just wouldn't exist...?
    EMPTY,
    DISABLED,
    ENABLED
  }

  // Default to EMPTY, meaning 'this.effect' is null / no LXEffect matching 'this.type' is on
  // the master bus.
  private State state = State.EMPTY;

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
        // TODO: fix dispose
        /*
                [LX 2025/08/17 13:51:39] WARNING / SHOULDFIX: Stranded listener on parameter: /lx/mixer/master/effect/0/enabled - titanicsend.app.effectmgr.GlobalEffect$1
        java.lang.Exception
        	at heronarts.lx.parameter.LXListenableParameter.dispose(LXListenableParameter.java:175)
        	at heronarts.lx.LXComponent.removeParameter(LXComponent.java:1313)
        	at heronarts.lx.LXComponent.removeParameter(LXComponent.java:1280)
        	at heronarts.lx.LXComponent.dispose(LXComponent.java:1093)
        	at heronarts.lx.LXModulatorComponent.dispose(LXModulatorComponent.java:153)
        	at heronarts.lx.LXLayeredComponent.dispose(LXLayeredComponent.java:206)
        	at heronarts.lx.LXDeviceComponent.dispose(LXDeviceComponent.java:443)
        	at heronarts.lx.effect.LXEffect.dispose(LXEffect.java:402)
        	at heronarts.lx.LX.dispose(LX.java:727)
        	at heronarts.lx.mixer.LXBus.removeEffect(LXBus.java:313)
        	at heronarts.lx.mixer.LXBus.clear(LXBus.java:564)
        	at heronarts.lx.mixer.LXMixerEngine.clear(LXMixerEngine.java:1380)
        	at heronarts.lx.LXEngine.dispose(LXEngine.java:1477)
        	at heronarts.lx.LX.dispose(LX.java:727)
        	at heronarts.lx.LX.dispose(LX.java:735)
        	at heronarts.glx.GLX.dispose(GLX.java:319)
        	at heronarts.glx.GLX.run(GLX.java:298)
        	at heronarts.lx.studio.TEApp.applicationThread(TEApp.java:1226)
        	at heronarts.lx.studio.TEApp.lambda$main$1(TEApp.java:1184)
        	at java.base/java.lang.Thread.run(Thread.java:1583)
        [LX 2025/08/17 13:51:39] java.lang.IllegalStateException:Trying to remove unregistered LXParameterListener /effect/0/enabled titanicsend.app.effectmgr.GlobalEffect$1
        java.lang.IllegalStateException: Trying to remove unregistered LXParameterListener /effect/0/enabled titanicsend.app.effectmgr.GlobalEffect$1
        	at heronarts.lx.parameter.LXListenableParameter.removeListener(LXListenableParameter.java:120)
        	at titanicsend.app.effectmgr.GlobalEffect.onUnregister(GlobalEffect.java:174)
        	at titanicsend.app.effectmgr.GlobalEffect.registerEffect(GlobalEffect.java:82)
        	at titanicsend.app.effectmgr.GlobalEffect.dispose(GlobalEffect.java:219)
        	at titanicsend.app.effectmgr.GlobalEffectManager.dispose(GlobalEffectManager.java:278)
        	at heronarts.lx.studio.TEApp$Plugin.dispose(TEApp.java:983)
        	at heronarts.lx.LXRegistry$Plugin.dispose(LXRegistry.java:527)
        	at heronarts.lx.LXRegistry.disposePlugins(LXRegistry.java:1476)
                 */
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
