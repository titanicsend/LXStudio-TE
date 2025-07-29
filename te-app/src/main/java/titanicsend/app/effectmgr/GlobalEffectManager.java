package titanicsend.app.effectmgr;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.osc.LXOscComponent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import titanicsend.audio.LOG;
import titanicsend.effect.TEPerformanceEffect;

@LXCategory(LXCategory.OTHER)
@LXComponentName("Effect Manager")
public class GlobalEffectManager extends LXComponent
    implements LXOscComponent, LXBus.Listener, LX.ProjectListener {

  private static GlobalEffectManager instance;

  private final List<TEPerformanceEffect> registeredEffects = new ArrayList<>();

  public GlobalEffectManager(LX lx) {
    super(lx, "effectmgr");
    GlobalEffectManager.instance = this;

    // When we first open the Project, we need to load up all effects from Master Bus
    this.lx.addProjectListener(this);
    // When effects are added / removed / moved on Master Bus, we gotta listen and update
    this.lx.engine.mixer.masterBus.addListener(this);
  }

  public static GlobalEffectManager get() {
    return instance;
  }

  @Override
  public void effectAdded(LXBus channel, LXEffect effect) {
    if (isPerformanceEffect(effect)) {
      attemptToRegister(effect);
      LOG.log("TEPerformanceEffect added: " + effect.getLabel());
    }
  }

  @Override
  public void effectRemoved(LXBus channel, LXEffect effect) {
    if (isPerformanceEffect(effect) && registeredEffects.contains((TEPerformanceEffect) effect)) {
      registeredEffects.remove(effect);
      LOG.log("TEPerformanceEffect removed: " + effect.getLabel());
    }
  }

  @Override
  public void effectMoved(LXBus channel, LXEffect effect) {
    LOG.log("TEPerformanceEffect moved: " + effect.getLabel());
  }

  private void attemptToRegister(LXEffect effect) {
    LOG.log(
        "Effect[ProjectChanged - TEPerformanceEffect]: '"
            + effect.getLabel()
            + "', enabled: "
            + effect.enabled.isOn());
    if (!isPerformanceEffect(effect)) {
      LOG.log("NOT A PERFORMANCE EFFECT");
      return;
    }
    TEPerformanceEffect performanceEffect = (TEPerformanceEffect) effect;
    if (registeredEffects.contains(performanceEffect)) {
      registeredEffects.add(performanceEffect);
    }
  }

  private static boolean isPerformanceEffect(LXEffect effect) {
    if (TEPerformanceEffect.class.isAssignableFrom(effect.getClass())) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void projectChanged(File file, LX.ProjectListener.Change change) {
    // TODO: is it enough to have the bus listener above / do I also need this proj listener?
  }

  @Override
  public void dispose() {
    this.lx.removeProjectListener(this);
    this.lx.engine.mixer.masterBus.removeListener(this);
    super.dispose();
  }
}
