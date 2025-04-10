/**
 * Copyright 2022- Justin Belcher, Mark C. Slee, Heron Arts LLC
 *
 * <p>This file is part of the LX Studio software library. By using LX, you agree to the terms of
 * the LX Studio Software License and Distribution Agreement, available at: http://lx.studio/license
 *
 * <p>Please note that the LX license is not open-source. The license allows for free,
 * non-commercial use.
 *
 * <p>HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE,
 * WITH RESPECT TO THE SOFTWARE.
 *
 * @author Justin K Belcher <justin@jkb.studio>
 */
package titanicsend.app.autopilot.justin;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.blend.DissolveBlend;
import heronarts.lx.blend.LXBlend;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXChannel.AutoCycleMode;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.modulation.LXModulationEngine;
import heronarts.lx.modulation.LXParameterModulation.ModulationException;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.modulator.LXPeriodicModulator;
import heronarts.lx.modulator.LXVariablePeriodModulator.ClockMode;
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.modulator.VariableLFO;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BooleanParameter.Mode;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.snapshot.LXGlobalSnapshot;
import heronarts.lx.utils.LXUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import titanicsend.app.autopilot.justin.AutoParameter.Scale;

/**
 * Autopilot system creates modulators for current patterns and enables autocycle and transitions.
 * Disabling the autopilot restores the animated components to their prior state with a snapshot.
 *
 * <p>Origin Joule 2022
 *
 * @author Justin K. Belcher <jkb.studio>
 */
public abstract class Autopilot extends LXComponent implements LX.ProjectListener {

  public static final String SNAPSHOTNAME_BEFORE = "Before Autopilot";
  public static final String SNAPSHOTNAME_RUNNING = "Autopilot";

  /**
   * Prefix for all modulator and modulation labels created by the Autopilot. Will be used to clear
   * objects from previous runs.
   */
  public static final String MOD_PREFIX = "A_";

  public final BooleanParameter enabled =
      new BooleanParameter("Enabled", false)
          .setMode(Mode.TOGGLE)
          .setDescription("Set to TRUE to start Autopilot");

  private final LXParameterListener enabledListener =
      (p) -> {
        enabledChanged(this.enabled.isOn());
      };

  public final TriggerParameter reset =
      new TriggerParameter(
              "Reset",
              () -> {
                if (!this.enabled.isOn()) {
                  this.onReset();
                }
              })
          .setDescription(
              "Clear objects created by Autopilot.  Will result in new modulators being created on the next run.  Only works when Autopilot is disabled.");

  private final List<LXNormalizedParameter> mutableVisibleParameters =
      new ArrayList<LXNormalizedParameter>();
  public final List<LXNormalizedParameter> visibleParameters =
      Collections.unmodifiableList(this.mutableVisibleParameters);

  private LXGlobalSnapshot before;
  private LXGlobalSnapshot running;

  public final AutopilotLibrary library = new AutopilotLibrary();

  public Autopilot(LX lx) {
    super(lx);

    lx.addProjectListener(this);

    addVisibleParameter("enabled", this.enabled);
    addVisibleParameter("reset", this.reset);

    this.enabled.addListener(this.enabledListener);

    LX.error("Initializing JKB autopilot");
    // TODO: Monitor for our snapshots getting manually deleted.
    // It works now but would be cleaner with a listener.
    // lx.engine.snapshots.addListener(this);
  }

  /**
   * Subclasses can register a parameter with this method instead of addParameter() to include it in
   * the UI.
   */
  protected Autopilot addVisibleParameter(String path, LXNormalizedParameter parameter) {
    addParameter(path, parameter);
    setParameterVisible(parameter);
    return this;
  }

  /** Adding a parameter here will make it show up in the Autopilot UI */
  protected Autopilot setParameterVisible(LXNormalizedParameter parameter) {
    this.mutableVisibleParameters.add(parameter);
    return this;
  }

  private void enabledChanged(boolean on) {
    if (on) {
      // Turning on
      onWillEnable();

      refreshSnapshotReferences();

      if (this.before == null) {
        this.before = lx.engine.snapshots.addSnapshot();
        this.before.label.setValue(SNAPSHOTNAME_BEFORE);
      }
      this.before.update();

      if (this.running == null) {
        // First run
        LX.log("Initializing first autopilot state...");
        firstStart();
      } else {
        // Back to previous Autopilot mode
        LX.log("Recalling previous autopilot state...");
        running.recall.bang();

        // Turn on prior modulators
        enableAutopilotModulators();
        enableChannelTransitions();
      }

      LX.log("Autopilot ON");
      onDidEnable();

    } else {
      // Turning off
      onWillDisable();
      refreshSnapshotReferences();

      LX.log("Saving autopilot state...");
      if (this.running == null) {
        this.running = lx.engine.snapshots.addSnapshot();
        this.running.label.setValue(SNAPSHOTNAME_RUNNING);
      }
      this.running.update();

      if (this.before != null) {
        this.before.recall.bang();
      } else {
        // Oops, we lost it!
        LX.error("Could not find pre-autopilot snapshot.");
      }

      disableChannelTransitions();
      disableAutopilotModulators();
      LX.log("Autopilot OFF");
      onDidDisable();
    }
  }

  private void refreshSnapshotReferences() {
    if (this.before == null) {
      // Look for existing by name - could have been a file save/load
      for (LXGlobalSnapshot snapshot : lx.engine.snapshots.snapshots) {
        if (snapshot.getLabel().equals(SNAPSHOTNAME_BEFORE)) {
          this.before = snapshot;
          break;
        }
      }
    } else {
      // Until we implement the snapshot engine listener, just check for stale snapshot reference
      if (!lx.engine.snapshots.contains(this.before)) {
        this.before = null;
      }
    }
    if (this.running == null) {
      // Look for existing by name - could have been a file save/load
      for (LXGlobalSnapshot snapshot : lx.engine.snapshots.snapshots) {
        if (snapshot.getLabel().equals(SNAPSHOTNAME_RUNNING)) {
          this.running = snapshot;
          break;
        }
      }
    } else {
      // Until we implement the snapshot engine listener, just check for stale snapshot reference
      if (!lx.engine.snapshots.contains(this.running)) {
        this.running = null;
      }
    }
  }

  /**
   * First start of the Autopilot. Will not be called if restarting (applying a snapshot). This will
   * get called if the "running" snapshot is not found.
   */
  private void firstStart() {
    // Delete any modulators from previous runs
    clearAutopilotModulators();

    // Notify child classes
    onStarting();

    int newMods = 0;

    for (LXAbstractChannel aChannel : lx.engine.mixer.channels) {
      if (aChannel instanceof LXChannel && checkChannelQualifies((LXChannel) aChannel)) {
        LXChannel channel = (LXChannel) aChannel;

        // Channel transition and autocycle
        enableTransitions(channel);

        // Pattern
        for (LXPattern pattern : channel.getPatterns()) {

          // Allowed by subclasses?
          if (checkPatternQualifies(channel, pattern)) {

            // Is it in the library?
            AutoPattern autoPattern = this.library.getPattern(pattern);
            if (autoPattern != null) {
              for (AutoParameter ap : autoPattern.parameters) {

                LXParameter p = pattern.getParameter(ap.path);
                if (p == null) {
                  LX.warning(
                      "Autopilot failed to find parameter "
                          + ap.path
                          + " on pattern "
                          + pattern.getCanonicalLabel());
                  continue;
                }
                if (!(p instanceof CompoundParameter)) {
                  LX.warning(
                      "Autopilot cannot modulate parameter, is invalid type: "
                          + p.getCanonicalLabel());
                  continue;
                }

                // Modulate the parameter
                CompoundParameter cp = (CompoundParameter) p;
                mod(pattern, cp, ap, getModulationLabel(pattern, cp, ap));
                newMods++;
              }
            }
          }
        }
      }
    }

    LX.log("Autopilot created " + newMods + " modulators");
    onStarted();
  }

  /** Create modulator on a parameter */
  protected void mod(
      LXDeviceComponent device, CompoundParameter parameter, AutoParameter ap, String label) {

    // Modulation can be global or local to device
    LXModulationEngine modulationEngine = device != null ? device.modulation : lx.engine.modulation;

    double min = ap.min;
    double max = ap.max;
    double rangeActive = ap.range;
    if (ap.scale == Scale.ABSOLUTE) {
      // Convert to normalized
      min = parameter.range.getNormalized(min);
      max = parameter.range.getNormalized(max);
      rangeActive = rangeActive / parameter.range.range;
    }
    double rangeTotal = max - min;
    double period = getModulationPeriod(parameter, ap) * 1000;

    // Set parameter base value, randomly placing the active range within the total range
    // If active range is zero, this effectively randomizes the parameter
    parameter.setNormalized(min + (Math.random() * (rangeTotal - rangeActive)));

    // If this was just a randomizer, don't create modulator
    if (rangeActive == 0) {
      return;
    }

    // Add Modulator (LFO source)
    VariableLFO modulator = new VariableLFO(label);
    if (period < modulator.periodSlow.range.min) {
      // Fast
      modulator.periodFast.setValue(period);
      modulator.clockMode.setValue(ClockMode.FAST);
    } else if (period < modulator.periodSlow.range.max) {
      // Slow.  Range 1-900 seconds.
      modulator.periodSlow.setValue(period);
      modulator.clockMode.setValue(ClockMode.SLOW);
    } else {
      // setPeriod(ms) sets a custom period which is not picked up by UI. Avoid if possible.
      modulator.setPeriod(period);
    }
    modulator.setEndValue(.5);
    modulator.waveshape.setValue(LXWaveshape.SIN);
    modulator.running.setValue(true);
    modulationEngine.addModulator(modulator);

    try {
      // Add Modulation (links source -> target)
      LXCompoundModulation modulation =
          new LXCompoundModulation(modulationEngine, modulator, parameter);
      modulation.range.setValue(rangeActive);
      modulationEngine.addModulation(modulation);
      onModAdded(parameter, modulator, modulation);
    } catch (ModulationException e) {
      e.printStackTrace();
      LX.error(e);
      return;
    }
  }

  protected double getModulationPeriod(CompoundParameter parameter, AutoParameter ap) {
    return LXUtils.random(ap.minPeriodSec, ap.maxPeriodSec);
  }

  protected String getModulationLabel(
      LXPattern pattern, CompoundParameter parameter, AutoParameter ap) {
    return MOD_PREFIX + parameter.getLabel();
  }

  /** Called every time Autopilot is enabled, prior to snapshot and modulators created. */
  protected void onWillEnable() {}

  /** Called the first time Autopilot is started, prior to modulators created. */
  protected void onStarting() {}

  /** Called the first time Autopilot is started, after modulators created */
  protected void onStarted() {}

  /** Called every time after Autopilot is enabled */
  protected void onDidEnable() {}

  /** Called when Autopilot is stopping, before Running snapshot is taken */
  protected void onWillDisable() {}

  /** Called when Autopilot is stopped, after Before snapshot restored */
  protected void onDidDisable() {}

  /** Notification to subclasses a new modulator was created. This only happens on first start. */
  protected void onModAdded(
      CompoundParameter parameter, VariableLFO modulator, LXCompoundModulation modulation) {}

  protected void enableChannelTransitions() {
    for (LXAbstractChannel aChannel : lx.engine.mixer.channels) {
      if (aChannel instanceof LXChannel) {
        enableTransitions((LXChannel) aChannel);
      }
    }
  }

  protected void enableTransitions(LXChannel channel) {
    for (LXBlend blend : channel.transitionBlendMode.getObjects()) {
      if (blend instanceof DissolveBlend) {
        // Set to dissolve blend
        channel.transitionBlendMode.setValue(blend);
        break;
      }
    }

    channel.transitionTimeSecs.setValue(LXUtils.random(2, 15));
    channel.transitionEnabled.setValue(true);

    channel.autoCycleTimeSecs.setValue(LXUtils.random(30, 75));
    channel.autoCycleMode.setValue(AutoCycleMode.RANDOM);
    channel.autoCycleEnabled.setValue(true);
  }

  protected void disableChannelTransitions() {
    for (LXAbstractChannel aChannel : lx.engine.mixer.channels) {
      if (aChannel instanceof LXChannel) {
        LXChannel channel = (LXChannel) aChannel;
        channel.transitionEnabled.setValue(false);
        channel.autoCycleEnabled.setValue(false);
      }
    }
  }

  /** Clear modulators from past autopilot runs */
  private void clearAutopilotModulators() {
    // Globals
    clearAutopilotModulators(lx.engine.modulation);

    // Loop over channels
    for (LXAbstractChannel aChannel : lx.engine.mixer.channels) {
      if (aChannel instanceof LXChannel && checkChannelQualifies((LXChannel) aChannel)) {
        LXChannel channel = (LXChannel) aChannel;

        // Patterns
        for (LXPattern pattern : channel.getPatterns()) {
          clearAutopilotModulators(pattern.modulation);
        }

        // Effects
        for (LXEffect effect : channel.getEffects()) {
          clearAutopilotModulators(effect.modulation);
        }
      }
    }
  }

  protected final void clearAutopilotModulators(LXModulationEngine scope) {
    // Remove modulations
    LXCompoundModulation modulation;
    for (int i = scope.modulations.size() - 1; i >= 0; i--) {
      modulation = scope.modulations.get(i);
      if (isAutopilotModulation(modulation)) {
        scope.removeModulation(modulation);
      }
    }

    // Remove modulators
    LXModulator modulator;
    for (int i = scope.modulators.size() - 1; i >= 0; i--) {
      modulator = scope.modulators.get(i);
      if (isAutopilotModulator(modulator)) {
        scope.removeModulator(modulator);
      }
    }
  }

  protected void disableAutopilotModulators() {
    // Globals
    disableAutopilotModulators(lx.engine.modulation);

    // Loop over channels
    for (LXAbstractChannel aChannel : lx.engine.mixer.channels) {
      if (aChannel instanceof LXChannel && checkChannelQualifies((LXChannel) aChannel)) {
        LXChannel channel = (LXChannel) aChannel;

        // Patterns
        for (LXPattern pattern : channel.getPatterns()) {
          disableAutopilotModulators(pattern.modulation);
        }

        // Effects
        for (LXEffect effect : channel.getEffects()) {
          disableAutopilotModulators(effect.modulation);
        }
      }
    }
  }

  /** Stop modulators and set their basis to zero */
  protected final void disableAutopilotModulators(LXModulationEngine scope) {
    for (LXModulator modulator : scope.modulators) {
      if (isAutopilotModulator(modulator)) {
        modulator.stop();
        if (modulator instanceof LXPeriodicModulator) {
          LXPeriodicModulator modulatorP = (LXPeriodicModulator) modulator;
          // Funny trick, have to disable tempo lock to reset basis.
          // We accept the immediate value jump to regain full parameter range.
          boolean wasTempoLock = modulatorP.tempoLock.getValueb();
          if (wasTempoLock) {
            modulatorP.tempoLock.setValue(false);
          }
          // Reset to start, else parameter will be stuck partially modulated
          modulatorP.setBasis(0);
          if (wasTempoLock) {
            modulatorP.tempoLock.setValue(true);
          }
        }
      }
    }
  }

  /** Turn modulators back on */
  protected void enableAutopilotModulators() {
    // Globals
    enableAutopilotModulators(lx.engine.modulation);

    // Loop over channels
    for (LXAbstractChannel aChannel : lx.engine.mixer.channels) {
      if (aChannel instanceof LXChannel && checkChannelQualifies((LXChannel) aChannel)) {
        LXChannel channel = (LXChannel) aChannel;

        // Patterns
        for (LXPattern pattern : channel.getPatterns()) {
          enableAutopilotModulators(pattern.modulation);
        }

        // Effects
        for (LXEffect effect : channel.getEffects()) {
          enableAutopilotModulators(effect.modulation);
        }
      }
    }
  }

  protected final void enableAutopilotModulators(LXModulationEngine scope) {
    for (LXModulator modulator : scope.modulators) {
      if (isAutopilotModulator(modulator)) {
        modulator.start();
      }
    }
  }

  protected boolean isAutopilotModulation(LXCompoundModulation modulation) {
    return modulation.getLabel().startsWith(MOD_PREFIX);
  }

  protected boolean isAutopilotModulator(LXModulator modulation) {
    return modulation.getLabel().startsWith(MOD_PREFIX);
  }

  /** Subclasses can override to exclude certain channels. */
  protected boolean checkChannelQualifies(LXChannel channel) {
    return true;
  }

  /** Subclasses can override to exclude certain patterns. */
  protected boolean checkPatternQualifies(LXChannel channel, LXPattern pattern) {
    return true;
  }

  protected void onReset() {
    // Delete any snapshots we created
    // By using the label this will capture ones that were saved to file
    clearSnapshots();

    // Delete any stray modulations+modulators we created
    clearAutopilotModulators();
  }

  /** Delete any snapshots that may have been created by Autopilot */
  protected void clearSnapshots() {
    releaseSnapshots();

    LXGlobalSnapshot snapshot;
    for (int i = lx.engine.snapshots.snapshots.size() - 1; i >= 0; i--) {
      snapshot = lx.engine.snapshots.snapshots.get(i);
      if (snapshot.getLabel().equals(SNAPSHOTNAME_BEFORE)
          || snapshot.getLabel().equals(SNAPSHOTNAME_RUNNING)) {
        lx.engine.snapshots.removeSnapshot(snapshot);
      }
    }
  }

  @Override
  public void projectChanged(File file, Change change) {
    if (change != Change.SAVE) {
      releaseSnapshots();
    }
  }

  public void releaseSnapshots() {
    this.before = null;
    this.running = null;
  }

  @Override
  public void dispose() {
    this.enabled.removeListener(this.enabledListener);
    super.dispose();
  }
}
