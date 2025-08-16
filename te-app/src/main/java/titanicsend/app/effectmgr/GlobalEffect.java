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
  private LXParameterListener enabledListener;
  private State state;

  //  private int slotIndex = -1;

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

  public final void registerEffect(LXEffect lxEffect) {
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
      if (this.effect != null) {
        onRegister();
      }
    }
  }

  //  public void setSlotIndex(int slotIndex) {
  //    this.slotIndex = slotIndex;
  //  }
  //
  //  public final int getSlotIndex() {
  //    return this.slotIndex;
  //  }

  public State getState() {
    return state;
  }

  /**
   * Called when a new effect instance is tied to this GlobalEffect slot. Subclasses can override.
   */
  protected void onRegister() {
    state = State.DISABLED;
    BooleanParameter enabledParam = getEnabledParameter();
    if (enabledParam != null) {
      enabledListener =
          new LXParameterListener() {
            @Override
            public void onParameterChanged(LXParameter parameter) {
              if (parameter instanceof BooleanParameter) {
                boolean isEnabled = ((BooleanParameter) parameter).getValueb();
                state = isEnabled ? State.ENABLED : State.DISABLED;
              } else {
                throw new IllegalStateException("Invalid parameter " + parameter);
              }
            }
          };
      enabledParam.addListener(enabledListener);
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
    registerEffect(null);
  }
}
