package titanicsend.resolume;

import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.modulation.LXModulationEngine;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscConnection;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.utils.LXUtils;
import titanicsend.resolume.modulator.ResolumeBrightnessModulator;
import titanicsend.resolume.modulator.ResolumeTempoModulator;
import titanicsend.resolume.ui.UIResolumePlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Plugin for Chromatik that connects to Resolume Arena/Avenue
 * Based on the Beyond plugin architecture
 */
@LXPlugin.Name("Resolume")
public class ResolumePlugin implements LXStudio.Plugin {

  private static final int RESOLUME_OSC_PORT = 7000;
  private static final String RESOLUME_OSC_FILTER = "/composition";

  public final TriggerParameter setUpNow =
    new TriggerParameter("Set Up Now", this::runSetup)
      .setDescription("Add an OSC output for Resolume and add global modulators for brightness and tempo sync");

  private final LX lx;

  public ResolumePlugin(LX lx) {
    this.lx = lx;
    LOG.log("ResolumePlugin(LX) constructor called - version: " + loadVersion());
  }

  @Override
  public void initialize(LX lx) {
    LOG.log("ResolumePlugin.initialize() called");
  }

  @Override
  public void initializeUI(LXStudio lx, LXStudio.UI ui) {
    LOG.log("ResolumePlugin.initializeUI() called");
  }

  @Override
  public void onUIReady(LXStudio lx, LXStudio.UI ui) {
    LOG.log("ResolumePlugin.onUIReady() called - adding UI to leftPane.content");
    new UIResolumePlugin(ui, this, ui.leftPane.content.getContentWidth())
      .addToContainer(ui.leftPane.content, 2);
  }

  // Special for custom builds: register anything that would have been auto-imported from LXPackage.

  /**
   * Projects that import this library and are not an LXPackage (such as a custom build)
   * should call this method from their initialize().
   */
  public static void registerComponents(LX lx) {
    LOG.log("ResolumePlugin.registerComponents() called - registering modulators");
    lx.registry.addModulator(ResolumeBrightnessModulator.class);
    lx.registry.addModulator(ResolumeTempoModulator.class);
    LOG.log("ResolumePlugin.registerComponents() completed");
  }

  /**
   * Projects that import this library and are not an LXPackage (such as a custom build)
   * should call this method from their initializeUI().
   */
  public static void registerUIComponents(LXStudio lx, LXStudio.UI ui) { }

  /**
   * Add the basic common items: OSC output, global brightness modulator, global tempo modulator.
   */
  private void runSetup() {
    confirmOscOutput(this.lx);
    addBrightnessModulator();
    addTempoModulator();
  }

  public static LXOscConnection.Output confirmOscOutput(LX lx) {
    return confirmOscOutput(lx, null, RESOLUME_OSC_PORT);
  }

  public static LXOscConnection.Output confirmOscOutput(LX lx, String host, int port) {
    for (LXOscConnection.Output output : lx.engine.osc.outputs) {
      if (output.hasFilter.isOn() && RESOLUME_OSC_FILTER.equals(output.filter.getString())) {
        return output;
      }
    }

    LXOscConnection.Output oscOutput = lx.engine.osc.addOutput();
    if (!LXUtils.isEmpty(host)) {
      oscOutput.host.setValue(host);
    }
    oscOutput.port.setValue(port);
    oscOutput.filter.setValue(RESOLUME_OSC_FILTER);
    oscOutput.hasFilter.setValue(true);
    try {
      oscOutput.active.setValue(true);
    } catch (Exception e) {
      LOG.error(e, "Failed to activate OSC output for Resolume. Set the correct IP and port.");
      return null;
    }
    return oscOutput;
  }

  public ResolumeBrightnessModulator addBrightnessModulator() {
    return addBrightnessModulator(this.lx.engine.modulation);
  }

  public ResolumeBrightnessModulator addBrightnessModulator(LXModulationEngine modulationEngine) {
    LXModulator brightnessModulator = findModulator(ResolumeBrightnessModulator.class);
    if (brightnessModulator != null) {
      // If modulator already exists, make sure it is running
      if (!brightnessModulator.isRunning()) {
        brightnessModulator.start();
      }
    } else {
      // Create new global modulator
      brightnessModulator = new ResolumeBrightnessModulator(this.lx);
      this.lx.engine.modulation.startModulator(brightnessModulator);
    }

    return (ResolumeBrightnessModulator) brightnessModulator;
  }

  public ResolumeTempoModulator addTempoModulator() {
    return addTempoModulator(this.lx.engine.modulation);
  }

  public ResolumeTempoModulator addTempoModulator(LXModulationEngine modulationEngine) {
    LXModulator tempoModulator = findModulator(ResolumeTempoModulator.class);
    if (tempoModulator != null) {
      if (!tempoModulator.isRunning()) {
        tempoModulator.start();
      }
    } else {
      tempoModulator = new ResolumeTempoModulator(this.lx);
      this.lx.engine.modulation.startModulator(tempoModulator);
    }
    return (ResolumeTempoModulator) tempoModulator;
  }

  private LXModulator findModulator(Class<? extends LXModulator> clazz) {
    for (LXModulator modulator : this.lx.engine.modulation.getModulators()) {
      if (clazz.isInstance(modulator)) {
        return modulator;
      }
    }
    return null;
  }

  /**
   * Loads 'resolume.properties', after maven resource filtering has been applied.
   */
  private String loadVersion() {
    String version = "1.0.0";
    Properties properties = new Properties();
    try (InputStream inputStream =
           getClass().getClassLoader().getResourceAsStream("resolume.properties")) {
      if (inputStream != null) {
        properties.load(inputStream);
        version = properties.getProperty("resolume.version", version);
      }
    } catch (IOException e) {
      LOG.error(e, "Failed to load version information");
    }
    return version;
  }
}