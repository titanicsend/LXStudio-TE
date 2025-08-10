package titanicsend.oscremapper;

import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.osc.LXOscConnection;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.utils.LXUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import titanicsend.oscremapper.config.ConfigLoader;
import titanicsend.oscremapper.config.RemapperConfig;
import titanicsend.oscremapper.ui.UIOscRemapperPlugin;

/**
 * Plugin for Chromatik that provides OSC remapping and forwarding capabilities. Based on the Beyond
 * plugin architecture from: https://github.com/jkbelcher/Beyond
 */
@LXPlugin.Name("OscRemapper")
public class OscRemapperPlugin implements LXStudio.Plugin {

  public final TriggerParameter setUpOscOutputs =
      new TriggerParameter("Set Up OSC Outputs", this::runSetup)
          .setDescription("Add OSC Outputs from YAML Config");

  public final TriggerParameter reloadYamlConfig =
      new TriggerParameter("Reload YAML Config", this::reloadConfiguration)
          .setDescription("Reload YAML Config Outputs and Re-mappings, and Re-Setup the Outputs");

  public final BooleanParameter oscRemappingEnabled =
      new BooleanParameter("OSC Remapping", false)
          .setDescription("Enable remapping /lx OSC messages to /test");

  public final BooleanParameter remapperLoggingEnabled =
      new BooleanParameter("Enable Remapper Logs", false)
          .setDescription("Enable/disable plugin logging");

  private final LX lx;
  private final Path configPath;
  private final OscRemapperTransmissionListener transmissionListener;
  // Remote name to OSC Output connection
  private final Map<String, LXOscConnection.Output> remoteOutputs = new HashMap<>();
  // Config object (recreated upon reload)
  private RemapperConfig config;

  public OscRemapperPlugin(LX lx, Path configPath) {
    this.lx = lx;
    this.configPath = configPath;
    LOG.startup("OscRemapperPlugin(LX) constructor called - version: " + loadVersion());
    LOG.startup("Using config path: " + configPath);

    // Load configuration from YAML file
    this.config = ConfigLoader.loadConfig(configPath);
    LOG.startup(
        "Loaded configuration with "
            + this.config.getDestinations().size()
            + " destinations and "
            + this.config.getRemappings().size()
            + " remappings");

    // Set up transmission listener for OSC remapping
    this.transmissionListener = new OscRemapperTransmissionListener(this.lx, this.config);

    // Listen for parameter changes
    this.oscRemappingEnabled.addListener(
        p -> {
          if (this.oscRemappingEnabled.isOn()) {
            startOscCapture();
          } else {
            stopOscCapture();
          }
        });

    // Listen for logging parameter changes
    this.remapperLoggingEnabled.addListener(
        p -> {
          LOG.setEnabled(this.remapperLoggingEnabled.isOn());
        });
  }

  @Override
  public void initialize(LX lx) {}

  @Override
  public void initializeUI(LXStudio lx, LXStudio.UI ui) {}

  @Override
  public void onUIReady(LXStudio lx, LXStudio.UI ui) {
    // Add right below devSwitch/controlPanel
    new UIOscRemapperPlugin(ui, this, ui.leftPane.model.getContentWidth())
        .addToContainer(ui.leftPane.model, 1);
  }

  // ---------------------- OSC Output Management -----------------------------------------

  /** Set up OSC outputs for all configured remotes */
  private void runSetup() {
    // Clear existing outputs
    remoteOutputs.clear();

    // Create or find OSC output for each configured destination
    for (RemapperConfig.Destination destination : config.getDestinations()) {
      try {
        LXOscConnection.Output output =
            confirmOscOutput(
                this.lx,
                destination.getName(),
                destination.getIp(),
                destination.getPort(),
                destination.getFilter());

        if (output != null) {
          remoteOutputs.put(destination.getName(), output);
          LOG.debug("Added: %s", destination);
        } else {
          LOG.error("Failed to create OSC output for destination: %s", destination);
        }
      } catch (Exception e) {
        LOG.error(e, "Error setting up destination: %s", destination);
      }
    }

    LOG.debug("Setup complete - %d outputs active", remoteOutputs.size());
  }

  /** Create or find a dedicated OSC output for remapped messages */
  public static LXOscConnection.Output confirmOscOutput(
      LX lx, String destinationName, String host, int port, String filter) {
    // Check if we already have an output with this exact configuration
    for (LXOscConnection.Output output : lx.engine.osc.outputs) {
      if (output.hasFilter.isOn()
          && filter.equals(output.filter.getString())
          && host.equals(output.host.getString())
          && port == output.port.getValuei()) {
        LOG.debug("Found existing OSC output for %s: %s:%d", destinationName, host, port);
        return output;
      }
    }

    // Create new output
    LXOscConnection.Output oscOutput = lx.engine.osc.addOutput();
    if (!LXUtils.isEmpty(host)) {
      oscOutput.host.setValue(host);
    }
    oscOutput.port.setValue(port);
    oscOutput.filter.setValue(filter);
    oscOutput.hasFilter.setValue(true);

    try {
      oscOutput.active.setValue(true);
      LOG.debug(
          "Created new OSC output for %s: %s:%d (filter: %s)", destinationName, host, port, filter);
    } catch (Exception e) {
      LOG.error(e, "Failed to activate OSC output for %s. Check IP and port.", destinationName);
      return null;
    }

    return oscOutput;
  }

  // ---------------------- Config (Outputs + Remapping) ----------------------------------

  /** Reload configuration from YAML file and re-setup all outputs */
  private void reloadConfiguration() {
    LOG.debug("Reloading configuration...");

    try {
      // Stop current OSC capture if active
      if (oscRemappingEnabled.isOn()) {
        stopOscCapture();
      }

      // Clear existing outputs (ONLY those managed by this plugin).
      for (LXOscConnection.Output output : remoteOutputs.values()) {
        if (output != null) {
          try {
            output.active.setValue(false);
            lx.engine.osc.removeOutput(output);
            LOG.debug(
                "Removed existing output: %s:%d", output.host.getString(), output.port.getValuei());
          } catch (Exception e) {
            LOG.error(e, "Error removing existing output");
          }
        }
      }
      remoteOutputs.clear();

      // Reload configuration, and update the listener.
      this.updateRemapperConfig(ConfigLoader.loadConfig(configPath));

      // Re-setup outputs
      runSetup();

      // Restart OSC capture if it was enabled
      if (oscRemappingEnabled.isOn()) {
        startOscCapture();
      }

      LOG.debug("Configuration reload complete!");

    } catch (Exception e) {
      LOG.error(e, "Failed to reload configuration");
    }
  }

  private void updateRemapperConfig(RemapperConfig newConfig) {
    this.config = newConfig;
    this.transmissionListener.setConfig(newConfig);
    LOG.debug(
        "Reloaded configuration with %d destinations and %d remappings",
        this.config.getDestinations().size(), this.config.getRemappings().size());
  }

  // ---------------------- OSC Capture / Remapping ---------------------------------------

  /** Start OSC remapping by listening to transmission events */
  private void startOscCapture() {
    try {
      // Add transmission listener to capture ALL outgoing OSC messages
      lx.engine.osc.addMessageListener(this.transmissionListener);
      LOG.debug("OSC remapping started - listening to all transmitted OSC messages");
    } catch (Exception e) {
      LOG.error(e, "Failed to start OSC remapping");
    }
  }

  /** Stop OSC remapping */
  private void stopOscCapture() {
    try {
      // Remove transmission listener
      lx.engine.osc.removeMessageListener(this.transmissionListener);
      LOG.debug("OSC remapping stopped");
    } catch (Exception e) {
      LOG.error(e, "Failed to stop OSC remapping");
    }
  }

  // ---------------------- Boilerplate ---------------------------------------------------

  /** Loads 'oscremapper.properties', after maven resource filtering has been applied. */
  private String loadVersion() {
    String version = "1.0.0";
    Properties properties = new Properties();
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("oscremapper.properties")) {
      if (inputStream != null) {
        properties.load(inputStream);
        version = properties.getProperty("oscremapper.version", version);
      }
    } catch (IOException e) {
      LOG.error(e, "Failed to load version information");
    }
    return version;
  }

  @Override
  public void dispose() {
    stopOscCapture();
  }
}
