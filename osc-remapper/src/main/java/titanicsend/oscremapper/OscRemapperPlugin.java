package titanicsend.oscremapper;

import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.osc.LXOscConnection;
import heronarts.lx.osc.LXOscEngine;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.osc.OscPacket;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.utils.LXUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import titanicsend.oscremapper.config.ConfigLoader;
import titanicsend.oscremapper.config.RemapperConfig;
import titanicsend.oscremapper.ui.UIOscRemapperPlugin;

/**
 * Plugin for Chromatik that provides OSC remapping and forwarding capabilities Based on the Beyond
 * plugin architecture
 */
@LXPlugin.Name("OscRemapper")
public class OscRemapperPlugin implements LXStudio.Plugin {

  private static final String DEFAULT_OSC_HOST = "127.0.0.1";
  private static final int DEFAULT_OSC_PORT = 7000;
  private static final String DEFAULT_OSC_FILTER = "/test";

  public final TriggerParameter setUpNow =
      new TriggerParameter("Set Up Now", this::runSetup)
          .setDescription(
              "Add an OSC output and add global modulators for brightness and tempo sync");

  public final TriggerParameter refreshConfig =
      new TriggerParameter("Refresh Config", this::refreshConfiguration)
          .setDescription("Reload configuration from YAML file and re-setup outputs");

  public final BooleanParameter loggingEnabled =
      new BooleanParameter("Enable Logs", false).setDescription("Enable/disable plugin logging");

  private final LX lx;
  private final Path configPath;
  private OscRemapperTransmissionListener transmissionListener;
  private RemapperConfig config;

  // Track OSC outputs by remote name
  private final Map<String, LXOscConnection.Output> remoteOutputs = new HashMap<>();

  // OSC Capture parameters
  public final BooleanParameter oscCaptureEnabled =
      new BooleanParameter("OSC Remap", false)
          .setDescription("Enable remapping /lx OSC messages to /test");

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
    this.transmissionListener = new OscRemapperTransmissionListener();

    // Listen for parameter changes
    this.oscCaptureEnabled.addListener(
        p -> {
          if (this.oscCaptureEnabled.isOn()) {
            startOscCapture();
          } else {
            stopOscCapture();
          }
        });

    // Listen for logging parameter changes
    this.loggingEnabled.addListener(
        p -> {
          LOG.setEnabled(this.loggingEnabled.isOn());
        });
  }

  @Override
  public void initialize(LX lx) {}

  @Override
  public void initializeUI(LXStudio lx, LXStudio.UI ui) {}

  @Override
  public void onUIReady(LXStudio lx, LXStudio.UI ui) {
    new UIOscRemapperPlugin(ui, this, ui.leftPane.content.getContentWidth())
        .addToContainer(ui.leftPane.content, 2);
  }

  /** Start OSC remapping by listening to transmission events */
  private void startOscCapture() {
    try {
      // Add transmission listener to capture ALL outgoing OSC messages
      lx.engine.osc.addTransmissionListener(this.transmissionListener);
      LOG.log("OSC remapping started - listening to all transmitted OSC messages");
    } catch (Exception e) {
      LOG.error(e, "Failed to start OSC remapping");
    }
  }

  /** Stop OSC remapping */
  private void stopOscCapture() {
    try {
      // Remove transmission listener
      lx.engine.osc.removeTransmissionListener(this.transmissionListener);
      LOG.log("OSC remapping stopped");
    } catch (Exception e) {
      LOG.error(e, "Failed to stop OSC remapping");
    }
  }

  /** TransmissionListener for capturing and remapping outgoing OSC messages */
  private class OscRemapperTransmissionListener implements LXOscEngine.TransmissionListener {
    @Override
    public void oscMessageTransmitted(OscPacket packet) {
      try {
        // Check if this is an OscMessage that we should remap
        if (packet instanceof OscMessage) {
          OscMessage message = (OscMessage) packet;
          String originalAddress = message.getAddressPattern().getValue();

          // Check if this address matches any global remapping
          if (shouldRemapAddress(originalAddress)) {
            LOG.log("🔍 Processing OSC message: " + originalAddress);
            // Get all remapped addresses from global remappings
            List<String> remappedAddresses = getRemappedAddresses(originalAddress);
            LOG.log(
                "📍 Found "
                    + remappedAddresses.size()
                    + " remapped addresses: "
                    + remappedAddresses);

            // Send each remapped message (LX OSC outputs will route based on filters)
            for (String remappedAddress : remappedAddresses) {
              try {
                sendRemappedMessage(message, originalAddress, remappedAddress);
              } catch (Exception e) {
                LOG.error(
                    e,
                    "Failed to send remapped message: "
                        + originalAddress
                        + " → "
                        + remappedAddress);
              }
            }
          }
        }
      } catch (Exception e) {
        LOG.error(e, "Error processing transmitted OSC message");
      }
    }

    /** Check if an address should be remapped based on global remapping rules */
    private boolean shouldRemapAddress(String oscAddress) {
      Map<String, List<String>> globalRemappings = config.getRemappings();
      for (String sourcePattern : globalRemappings.keySet()) {
        if (matchesPattern(oscAddress, sourcePattern)) {
          return true;
        }
      }
      return false;
    }

    /** Get all remapped addresses for a given source address */
    private List<String> getRemappedAddresses(String oscAddress) {
      List<String> results = new ArrayList<>();
      Map<String, List<String>> globalRemappings = config.getRemappings();

      // Try exact match first
      List<String> exactMatches = globalRemappings.get(oscAddress);
      if (exactMatches != null) {
        results.addAll(exactMatches);
      }

      // Also try prefix matching (don't return early from exact match)
      for (Map.Entry<String, List<String>> entry : globalRemappings.entrySet()) {
        String sourcePattern = entry.getKey();
        List<String> targetPatterns = entry.getValue();

        if (sourcePattern.endsWith("/*")) {
          String sourcePrefix = sourcePattern.substring(0, sourcePattern.length() - 2);

          if (oscAddress.startsWith(sourcePrefix + "/")) {
            for (String targetPattern : targetPatterns) {
              if (targetPattern.endsWith("/*")) {
                String targetPrefix = targetPattern.substring(0, targetPattern.length() - 2);
                results.add(targetPrefix + oscAddress.substring(sourcePrefix.length()));
              } else {
                results.add(targetPattern);
              }
            }
          }
        }
      }

      return results;
    }

    /** Check if an OSC address matches a pattern (supporting /* wildcards) */
    private boolean matchesPattern(String address, String pattern) {
      if (pattern.equals(address)) {
        return true; // Exact match
      }

      if (pattern.endsWith("/*")) {
        String prefix = pattern.substring(0, pattern.length() - 2);
        return address.startsWith(prefix + "/");
      }

      return false;
    }

    /** Send a remapped OSC message through the LX engine (assuming all values are floats) */
    private void sendRemappedMessage(
        OscMessage originalMessage, String originalAddress, String remappedAddress) {
      try {
        // Send the remapped message - LX engine will route it to appropriate outputs based on
        // filters
        float value = (originalMessage.size() > 0) ? originalMessage.getFloat(0) : 0.0f;
        lx.engine.osc.sendMessage(remappedAddress, value);
        LOG.log(originalAddress + " → " + remappedAddress + " (" + value + ")");
      } catch (Exception e) {
        LOG.error(e, "Failed to send remapped message");
      }
    }
  }

  /** Cleanup resources when the plugin is disposed */
  public void dispose() {
    stopOscCapture();
  }

  /** Set up OSC outputs for all configured remotes */
  private void runSetup() {
    LOG.log(
        "[OscRemapper] Setting up OSC outputs for "
            + config.getDestinations().size()
            + " destinations");

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
          LOG.log(
              "[OscRemapper] ✅ "
                  + destination.getName()
                  + " → "
                  + destination.getIp()
                  + ":"
                  + destination.getPort()
                  + " (filter: "
                  + destination.getFilter()
                  + ")");
        } else {
          LOG.error("Failed to create OSC output for destination: " + destination.getName());
        }
      } catch (Exception e) {
        LOG.error(e, "Error setting up destination: " + destination.getName());
      }
    }

    LOG.log("[OscRemapper] Setup complete - " + remoteOutputs.size() + " outputs active");
  }

  /** Refresh configuration from YAML file and re-setup all outputs */
  private void refreshConfiguration() {
    LOG.log("🔄 Refreshing configuration...");

    try {
      // Stop current OSC capture if active
      if (oscCaptureEnabled.isOn()) {
        stopOscCapture();
      }

      // Clear existing outputs
      for (LXOscConnection.Output output : remoteOutputs.values()) {
        if (output != null) {
          try {
            output.active.setValue(false);
            lx.engine.osc.removeOutput(output);
            LOG.log(
                "Removed existing output: "
                    + output.host.getString()
                    + ":"
                    + output.port.getValuei());
          } catch (Exception e) {
            LOG.error(e, "Error removing existing output");
          }
        }
      }
      remoteOutputs.clear();

      // Reload configuration
      this.config = ConfigLoader.loadConfig(configPath);
      LOG.log(
          "✅ Reloaded configuration with "
              + this.config.getDestinations().size()
              + " destinations and "
              + this.config.getRemappings().size()
              + " remappings");

      // Re-setup outputs
      runSetup();

      // Restart OSC capture if it was enabled
      if (oscCaptureEnabled.isOn()) {
        startOscCapture();
      }

      LOG.log("🎯 Configuration refresh complete!");

    } catch (Exception e) {
      LOG.error(e, "Failed to refresh configuration");
    }
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
        LOG.log("Found existing OSC output for " + destinationName + ": " + host + ":" + port);
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
      LOG.log(
          "Created new OSC output for "
              + destinationName
              + ": "
              + host
              + ":"
              + port
              + " (filter: "
              + filter
              + ")");
    } catch (Exception e) {
      LOG.error(e, "Failed to activate OSC output for " + destinationName + ". Check IP and port.");
      return null;
    }

    return oscOutput;
  }

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
}
