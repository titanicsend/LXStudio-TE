package titanicsend.app.dev;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXSerializable;
import heronarts.lx.Tempo.ClockSource;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.BooleanParameter.Mode;
import titanicsend.app.autopilot.TEOscMessage;
import titanicsend.dmx.DmxEngine;
import titanicsend.dmx.model.BeaconModel;
import titanicsend.dmx.model.AdjStealthModel;
import titanicsend.lasercontrol.TELaserTask;
import titanicsend.midi.MidiNames;
import titanicsend.osc.CrutchOSC;
import titanicsend.output.ChromatechSocket;
import titanicsend.util.TE;
import heronarts.lx.parameter.LXListenableNormalizedParameter;

/**
 * Central control for switching between developer and 
 * production environments.
 * 
 * @author Justin Belcher
 */
public class DevSwitch extends LXComponent implements LXSerializable, LX.ProjectListener {

  private static final String DEVSWITCH_FILE_NAME = ".devSwitch";
  private static final String INDENT = "     ";

  private static final boolean SAVE_STATE_WHEN_DEFAULT = false;

  public enum State {
    DEFAULT,
    DEVELOPER,
    PRODUCTION
  }

  public class DetailParameter {
    public LXListenableNormalizedParameter parameter;
    public String label;

    public DetailParameter(LXListenableNormalizedParameter parameter, String label) {
      this.parameter = parameter;
      this.label = label;
    }
  }

  private final File file;
  private boolean inLoad = false;

  /*
   * Top-level options
   */

  private State state = State.DEFAULT;
  private State stateBeforeFileOpen = State.DEFAULT;

  // Hidden for now but could be made visible.
  // Would be nice as a lock icon in the UI.
  // If made visible, be sure to clear the beforeFileOpenState
  public final BooleanParameter lock =
      new BooleanParameter("Lock", true)
      .setMode(Mode.TOGGLE)   
      .setDescription("When TRUE, if switch is set before a file is opened, switch will be re-applied after file open operation completes.");

  public final BooleanParameter isDev =
      new BooleanParameter("Developer", false)
      .setMode(Mode.TOGGLE)   
      .setDescription("Developer mode: disable outputs");

  public final BooleanParameter isProduction =
      new BooleanParameter("Production", false)
      .setMode(Mode.TOGGLE)   
      .setDescription("Production mode: enable outputs, midi controllers, etc");

  /*
   * Application-specific controls (Detail Parameters)
   * 
   * These are parameters that don't exist elsewhere (ie don't need to duplicate lx.engine.output.enabled)
   */

  // LX engine output (already exists)

  // LEDs

  public final BooleanParameter engineLEDs =
      new BooleanParameter(INDENT + "LEDs", true)
      .setMode(Mode.TOGGLE)   
      .setDescription("Output to LEDs ie all panel+edge pixels (requires LIVE to be enabled)");

  // Beacons

  public final BooleanParameter engineBeacons =
      new BooleanParameter(INDENT + "Beacons", true)
      .setMode(Mode.TOGGLE)   
      .setDescription("Beacons output");

  // DJ Lights

  public final BooleanParameter engineDJlights =
      new BooleanParameter(INDENT + "DJ lights", true)
      .setMode(Mode.TOGGLE)   
      .setDescription("DJ Lights output");

  // OSC engine input (exists)

  // OSC engine output (exists)

  // OSC to lasers (exists)

  // CrutchOSC to iPads (exists)

  // Audio input (exists)

  // Midi surfaces enabled (MFT + APC40)

  public final BooleanParameter midiSurfaces =
      new BooleanParameter("Midi Surfaces", false)
      .setMode(Mode.TOGGLE)   
      .setDescription("Enable all the standard midi surfaces");

  // Tempo enabled (exists)

  // TempoSource = OSC (*change this to direct engine parameter?)

  public final BooleanParameter tempoSourceOSC =
      new BooleanParameter("Tempo is OSC", false)
      .setMode(Mode.TOGGLE)   
      .setDescription("Tempo clockSource is OSC");


  private final LXParameterListener detailParameterListener = (p) -> {
    onDetailParameterChanged(p);
  };

  private final LXParameterListener tempoSourceListener = (p) -> {
    onTempoSourceChanged(p);
  };

  // All parameters that are visible in the UI
  private final List<LXNormalizedParameter> mutableVisibleParameters = new ArrayList<LXNormalizedParameter>();
  public final List<LXNormalizedParameter> visibleParameters = Collections.unmodifiableList(this.mutableVisibleParameters);

  // All detail parameters that affect the state
  final List<DetailParameter> detailParameters = new ArrayList<DetailParameter>();

  public static DevSwitch current;

  public DevSwitch(LX lx) {
    super(lx, "devSwitch");
    current = this;

    this.file = lx.getMediaFile(DEVSWITCH_FILE_NAME);

    this.tempoSourceOSC.setValue(this.lx.engine.tempo.clockSource.getEnum() == ClockSource.OSC);

    addParameter("lock", this.lock);
    addParameter("dev", this.isDev);
    addParameter("prod", this.isProduction);

    addParameter("engineLEDs", this.engineLEDs);
    addParameter("engineBeacons", this.engineBeacons);
    addParameter("engineDJlights", this.engineDJlights);
    addParameter("midiSurfaces", this.midiSurfaces);
    addParameter("tempoSourceOSC", this.tempoSourceOSC);

    addDetailParameter(this.lx.engine.output.enabled, "LIVE Output");
    addDetailParameter(this.engineLEDs);
    addDetailParameter(this.engineBeacons);
    addDetailParameter(this.engineDJlights);
    addDetailParameter(this.lx.engine.osc.receiveActive, "OSC Input");
    addDetailParameter(this.lx.engine.osc.transmitActive, "OSC Output");
    addDetailParameter(TELaserTask.get().enabled, INDENT + "OSC to lasers");
    addDetailParameter(CrutchOSC.get().transmitActive, INDENT + "OSC to iPads");
    addDetailParameter(this.lx.engine.audio.enabled, "Audio Input");
    addDetailParameter(this.midiSurfaces);
    addDetailParameter(this.lx.engine.tempo.enabled, "Tempo Enabled");
    addDetailParameter(this.tempoSourceOSC);

    listenDetailParameters();
    lx.engine.tempo.clockSource.addListener(this.tempoSourceListener);

    refreshState();

    lx.addProjectListener(this);

    load();
  }

  private void addDetailParameter(LXListenableNormalizedParameter parameter) {
    addDetailParameter(parameter, parameter.getLabel());
  }

  private void addDetailParameter(LXListenableNormalizedParameter parameter, String label) {
    this.detailParameters.add(new DetailParameter(parameter, label));
  }

  private void listenDetailParameters() {
    for (DetailParameter p : this.detailParameters) {
      p.parameter.addListener(this.detailParameterListener);
    }
  }

  private void unListenDetailParameters() {
    for (DetailParameter p : this.detailParameters) {
      p.parameter.removeListener(this.detailParameterListener);
    }
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.isDev) {
      onDevChanged(this.isDev.getValueb());
    } else if (p == this.isProduction) {
      onProdChanged(this.isProduction.getValueb());
    } else if (p == this.engineLEDs) {
      onEngineLEDsChanged(this.engineLEDs.getValueb());
    } else if (p == this.engineBeacons) {
      onEngineBeaconsChanged(this.engineBeacons.getValueb());
    } else if (p == this.engineDJlights) {
      onEngineDJlightsChanged(this.engineDJlights.getValueb());
    } else if (p == this.midiSurfaces) {
      onMidiSurfacesChanged(this.midiSurfaces.getValueb());
    } else if (p == this.tempoSourceOSC) {
      onTempoSourceOSCChanged(this.tempoSourceOSC.getValueb());
    }
  }

  private void onDevChanged(boolean isOn) {
    if (!this.interalChange) {
      setState(isOn ? State.DEVELOPER : State.DEFAULT);
      save();
    }
  }

  private void onProdChanged(boolean isOn) {
    if (!this.interalChange) {
      setState(isOn ? State.PRODUCTION : State.DEFAULT);
      save();
    }
  }

  boolean interalChange = false;

  protected void onDetailParameterChanged(LXParameter p) {
    if (!this.interalChange) {
      refreshState();
      save();
    }
  }

  /**
   * Set Dev/Production UI indicators switches based on current detail parameters
   */
  private void refreshState() {
    this.interalChange = true;

    this.state = getState();

    switch (this.state) {
    case DEVELOPER:
      this.isDev.setValue(true);
      this.isProduction.setValue(false);
      break;
    case PRODUCTION:
      this.isProduction.setValue(true);
      this.isDev.setValue(false);
      break;
    case DEFAULT:
    default:
      this.isDev.setValue(false);
      this.isProduction.setValue(false);
      break;
    }

    this.interalChange = false;
  }

  /**
   * Go to a new state.
   */
  private void setState(State state) {
    this.interalChange = true;

    this.state = state;

    switch (this.state) {
    case DEVELOPER:
      this.isDev.setValue(true);
      this.isProduction.setValue(false);
      setStateDev();
      break;
    case PRODUCTION:
      this.isProduction.setValue(true);
      this.isDev.setValue(false);
      setStateProduction();
      break;
    case DEFAULT:
    default:
      this.isDev.setValue(false);
      this.isProduction.setValue(false);
      setStateDefault();
      break;
    }

    this.interalChange = false;
  }

  /*
   * Project-specific implementations.  Edit these as needed.
   */

  /**
   * Project-specific implementation: Query
   * 
   * Interpret the state from the detail parameters.
   */
  protected State getState() {
    if (!this.lx.engine.output.enabled.isOn() &&
        !this.engineLEDs.isOn() &&
        !this.engineBeacons.isOn() &&
        !this.engineDJlights.isOn() &&
        this.lx.engine.osc.receiveActive.isOn() &&
        !this.lx.engine.osc.transmitActive.isOn() &&
        !CrutchOSC.get().transmitActive.isOn() &&
        !TELaserTask.get().enabled.isOn() &&
        this.lx.engine.audio.enabled.isOn() &&
        this.midiSurfaces.isOn() &&
        this.lx.engine.tempo.enabled.isOn() &&
        this.lx.engine.tempo.clockSource.getEnum() == ClockSource.INTERNAL
        ) {
      return State.DEVELOPER;
    } else if (
        this.lx.engine.output.enabled.isOn() &&
        this.engineLEDs.isOn() &&
        this.engineBeacons.isOn() &&
        this.engineDJlights.isOn() &&
        this.lx.engine.osc.receiveActive.isOn() &&
        this.lx.engine.osc.transmitActive.isOn() &&
        CrutchOSC.get().transmitActive.isOn() &&
        TELaserTask.get().enabled.isOn() == TELaserTask.DEFAULT_ENABLE_IN_PRODUCTION &&
        this.lx.engine.audio.enabled.isOn() &&
        this.midiSurfaces.isOn() &&
        this.lx.engine.tempo.enabled.isOn() &&
        this.lx.engine.tempo.clockSource.getEnum() == ClockSource.OSC
        ) {
      return State.PRODUCTION;
    }

    return State.DEFAULT;
  }

  /**
   * Project-specific implementation.
   * 
   * Start "Developer" state
   */
  protected void setStateDev() {
    TE.log("DevSwitch setting state to DEVELOPER...");

    this.lx.engine.output.enabled.setValue(false);
    this.lx.engine.output.enabled.setValue(false);
    this.engineLEDs.setValue(false);
    this.engineBeacons.setValue(false);
    this.engineDJlights.setValue(false);
    this.lx.engine.osc.receiveActive.setValue(true);
    this.lx.engine.osc.transmitActive.setValue(false);
    CrutchOSC.get().transmitActive.setValue(false);
    TELaserTask.get().enabled.setValue(false);
    this.lx.engine.audio.enabled.setValue(true);
    if (!this.midiSurfaces.getValueb()) {
      this.midiSurfaces.setValue(true);
    } else {
      // Confirm they're on since we're not tracking them individually for now
      setMidiSurfacesEnabled(true);
    }
    this.lx.engine.tempo.enabled.setValue(true);
    this.lx.engine.tempo.clockSource.setValue(ClockSource.INTERNAL);
  }

  /**
   * Project-specific implementation.
   * 
   * Start "Production" state
   */
  protected void setStateProduction() {
    TE.log("DevSwitch setting state to PRODUCTION...");

    this.lx.engine.output.enabled.setValue(true);    
    this.engineLEDs.setValue(true);
    this.engineBeacons.setValue(true);
    this.engineDJlights.setValue(true);
    this.lx.engine.osc.receiveActive.setValue(true);
    this.lx.engine.osc.transmitActive.setValue(true);
    CrutchOSC.get().transmitActive.setValue(true);
    TELaserTask.get().enabled.setValue(TELaserTask.DEFAULT_ENABLE_IN_PRODUCTION);
    this.lx.engine.audio.enabled.setValue(true);
    if (!this.midiSurfaces.getValueb()) {
      this.midiSurfaces.setValue(true);
    } else {
      // Confirm they're on since we're not tracking them individually for now
      setMidiSurfacesEnabled(true);
    }
    this.lx.engine.tempo.enabled.setValue(true);
    this.lx.engine.tempo.clockSource.setValue(ClockSource.OSC);

    // Set OSC output port & IP
    TEOscMessage.applyTEOscOutputSettings(lx);
  }

  /**
   * Project-specific implementation.
   * 
   * Start "Default" state.
   */
  protected void setStateDefault() {
    TE.log("DevSwitch setting state to DEFAULT...");
    // Generally this will not change any detail parameters.
  }

  private void onEngineLEDsChanged(boolean isOn) {
    ChromatechSocket.setEnabled(isOn);
  }

  public void applyDmxOutputsEnabled() {
    onEngineBeaconsChanged(this.engineBeacons.isOn());
    onEngineDJlightsChanged(this.engineDJlights.isOn());
  }

  private void onEngineBeaconsChanged(boolean isOn) {
    DmxEngine.get().setOutputsEnabledByType(BeaconModel.class, isOn);
  }

  private void onEngineDJlightsChanged(boolean isOn) {
    DmxEngine.get().setOutputsEnabledByType(AdjStealthModel.class, isOn);
  }

  /**
   * this.midiSurfaces changed
   */
  private void onMidiSurfacesChanged(boolean isOn) {
    setMidiSurfacesEnabled(isOn);
  }

  /**
   * Enable all TE-relevant surfaces
   */
  private void setMidiSurfacesEnabled(boolean enabled) {
    for (LXMidiSurface surface : this.lx.engine.midi.surfaces) {
      if (isTESurface(surface)) {
        surface.enabled.setValue(enabled);
      }
    }

    for (LXMidiInput lmi : lx.engine.midi.inputs) {
      if (lmi.getName().equals(MidiNames.MF64) ||
          lmi.getName().equals(MidiNames.BOMEBOX_MF64)) {
        // MF64
        lmi.channelEnabled.setValue(enabled);
      } else if (
          lmi.getName().equals(MidiNames.APC40MK2) ||
          lmi.getName().equals(MidiNames.BOMEBOX_APC40MK2)) {
        // APC40mk2
        lmi.controlEnabled.setValue(enabled);
      }
    }
  }

  /**
   * Returns TRUE for normal surfaces that should be enabled for TE production
   */
  private boolean isTESurface(LXMidiSurface surface) {
    return
        surface instanceof titanicsend.lx.APC40Mk2 ||
        surface instanceof titanicsend.lx.MidiFighterTwister ||
        surface instanceof heronarts.lx.midi.surface.APC40Mk2 ||
        surface instanceof heronarts.lx.midi.surface.MidiFighterTwister ||
        surface instanceof heronarts.lx.midi.surface.DJM900nxs2 ||
        surface instanceof heronarts.lx.midi.surface.DJMV10;
  }

  /*
   * Wrap the LX engine tempo clockSource enum parameter in a convenient boolean parameter
   */

  private boolean internalTempoSourceChange = false;

  /**
   * this.tempoSourceOSC changed
   */
  private void onTempoSourceOSCChanged(boolean isOn) {
    if (!this.internalTempoSourceChange) {
      this.internalTempoSourceChange = true;
      this.lx.engine.tempo.clockSource.setValue(isOn ? ClockSource.OSC : ClockSource.INTERNAL);
      this.internalTempoSourceChange = false;
    }
  }

  /**
   * lx.engine.tempo.clockSource changed
   */
  private void onTempoSourceChanged(LXParameter p) {
    if (!this.internalTempoSourceChange) {
      this.internalTempoSourceChange = true;
      this.tempoSourceOSC.setValue(this.lx.engine.tempo.clockSource.getEnum() == ClockSource.OSC);
      this.internalTempoSourceChange = false;
      // onDetailParameterChanged(this.tempoSourceOSC);
    }
  }

  /*
   * LX File Opened
   */

  @Override
  public void projectChanged(File file, Change change) {
    if (change == Change.TRY || change == Change.NEW) {
      // About to do an Open File
      this.stateBeforeFileOpen = this.state;
      TE.log("DevSwitch detected potential file change, remembering state " + this.stateBeforeFileOpen);
    } else if (change == Change.OPEN) {
      // Completion of project file open
      if (this.lock.isOn()) {
        // Force relevant parameters back into Dev or Production mode,
        // regardless of how they were saved in the file.
        TE.log("DevSwitch restoring the state " + this.stateBeforeFileOpen);
        setState(this.stateBeforeFileOpen);
        save();
      } else {
        // Passive mode, letting file settings persist.
        // Parameter listeners will handle this one.
      }
    }
  }

  /*
   * Save DevSwitch state to local disk
   *
   * Save/load methods are from LXPreferences
   */

  private static final String KEY_DEVSWITCH = "devSwitch";

  @Override
  public void save(LX lx, JsonObject object) {
    object.addProperty(KEY_DEVSWITCH, this.state.toString());
  }

  @Override
  public void load(LX lx, JsonObject object) {
    if (object.has(KEY_DEVSWITCH)) {
      State state = State.valueOf(object.get(KEY_DEVSWITCH).getAsString());
      setState(state);
    }
  }

  private void save() {
    // Don't re-save the file on updates caused by loading it...
    if (this.inLoad) {
      return;
    }

    // The important computers are strictly Dev or Production.
    // We don't want to save the state of DEFAULT just because someone
    // turned off the beacons, for example.
    if (this.state == State.DEFAULT && !SAVE_STATE_WHEN_DEFAULT) {
      return;
    }

    JsonObject obj = new JsonObject();
    save(this.lx, obj);
    try (JsonWriter writer = new JsonWriter(new FileWriter(this.file))) {
      writer.setIndent("  ");
      new GsonBuilder().create().toJson(obj, writer);
    } catch (IOException iox) {
      LX.error(iox, "Exception writing the DevSwitch file: " + this.file);
    }
  }

  public void load() {
    this.inLoad = true;
    if (this.file.exists()) {
      try (FileReader fr = new FileReader(this.file)) {
        // Load parameters and settings from file
        load(this.lx, new Gson().fromJson(fr, JsonObject.class));
      } catch (Exception x) {
        LX.error(x, "Exception loading DevSwitch file: " + this.file);
      }
    }
    this.inLoad = false;
  }

  @Override
  public void dispose() {
    unListenDetailParameters();
    lx.engine.tempo.clockSource.removeListener(tempoSourceListener);
    super.dispose();
  }
}
