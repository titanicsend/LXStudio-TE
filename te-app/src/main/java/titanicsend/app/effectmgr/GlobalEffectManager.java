package titanicsend.app.effectmgr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.osc.LXOscComponent;
import titanicsend.audio.LOG;
import titanicsend.effect.TEPerformanceEffect;

@LXCategory(LXCategory.OTHER)
@LXComponentName("Effect Manager")
public class GlobalEffectManager extends LXComponent
    implements LXOscComponent, LXBus.Listener, LX.ProjectListener {

//  LXEffect effect1;
//  final BooleanParameter effect1Registered =
//      new BooleanParameter("E2 Registered", false).setDescription("");
//
//  public final TriggerParameter effect1Trigger =
//      new TriggerParameter("E1 Trigger", this::triggerEffect1)
//          .setDescription("Push the managed swatch to the global active swatch");
//
//  public LXListenableNormalizedParameter effect1Param = null;

  private static GlobalEffectManager instance;

  public static GlobalEffectManager get() {
    return instance;
  }

  private final List<TEPerformanceEffect> registeredEffects = new ArrayList<>();

  public GlobalEffectManager(LX lx) {
    super(lx, "effectmgr");

    // When we first open the Project, we need to load up all effects from Master Bus
    this.lx.addProjectListener(this);
    // When effects are added / removed / moved on Master Bus, we gotta listen and update
    this.lx.engine.mixer.masterBus.addListener(this);
  }

  @Override
  public void effectAdded(LXBus channel, LXEffect effect) {
    if (effect instanceof TEPerformanceEffect) {
      attemptToRegister((TEPerformanceEffect) effect);
      LOG.log("TEPerformanceEffect added: " + effect.getLabel());
    }
  }

  @Override
  public void effectRemoved(LXBus channel, LXEffect effect) {
    if (effect instanceof TEPerformanceEffect && registeredEffects.contains((TEPerformanceEffect) effect)) {
      registeredEffects.remove(effect);
      LOG.log("TEPerformanceEffect removed: " + effect.getLabel());
    }
  }

  @Override
  public void effectMoved(LXBus channel, LXEffect effect) {
    LOG.log("TEPerformanceEffect moved: " + effect.getLabel());
  }

  private void attemptToRegister(TEPerformanceEffect effect) {
    LOG.log(
        "Effect[ProjectChanged - TEPerformanceEffect]: '"
            + effect.getLabel()
            + "', enabled: "
            + effect.enabled.isOn());
    if (registeredEffects.contains(effect)) {
      // already exists
      return;
    }

    registeredEffects.add(effect);
  }

  @Override
  public void projectChanged(File file, LX.ProjectListener.Change change) {
    // TODO: is it enough to have the bus listener above / do I also need this proj listener?

//    // Auto-create Director Effect
//    if (change == LX.ProjectListener.Change.OPEN) {
//      // Does effect already exist?
//      for (LXEffect effect : this.lx.engine.mixer.masterBus.effects) {
//        if (effect instanceof TEPerformanceEffect) {
//          attemptToRegister((TEPerformanceEffect) effect);
//        }
//      }
//    }
  }

//  public void registerEffect1(LXEffect effect, LXListenableNormalizedParameter param) {
//    this.effect1 = effect;
//    this.effect1Registered.setValue(true);
//    this.effect1Param = param;
//  }
//
//  public void disposeEffect1(LXEffect effect) {
//    if (this.effect1 != effect) {
//      // for prototyping, I've hardcoded "effect1". Really, this will end up as
//      // an array or list, so we'll find the effect in that list and dispose it.
//      return;
//    }
//  }
//
//  public void triggerEffect1() {
//    // TODO
//  }

  @Override
  public void dispose() {
    this.lx.removeProjectListener(this);
    this.lx.engine.mixer.masterBus.removeListener(this);
    super.dispose();
  }
}
