package titanicsend.resolume;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.osc.LXOscConnection;
import heronarts.lx.osc.LXOscEngine;
import heronarts.lx.osc.LXOscListener;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;

/**
 * ResolumeSyncTask - OSC Message Monitor and Future Resolume Integration
 * 
 * Currently monitors all incoming and outgoing OSC messages from the LX Engine.
 * Future functionality will include remapping and forwarding to Resolume.
 */
public class ResolumeSyncTask extends LXComponent implements LXOscListener, LXOscEngine.IOListener {

  private static ResolumeSyncTask current;

  public static ResolumeSyncTask get() {
    return current;
  }

  public final BooleanParameter enabled =
      new BooleanParameter("Enabled", true)
          .setDescription("Enable OSC message monitoring");

  // Commented out - Future Resolume functionality
  /*
  public final StringParameter resolumeIP =
      new StringParameter("IP", "127.0.0.1")
          .setDescription("IP address of Resolume machine");

  public final StringParameter resolumePort =
      new StringParameter("Port", "7000")
          .setDescription("OSC port for Resolume");

  public final TriggerParameter setUpOsc =
      new TriggerParameter("Set Up OSC", this::setupOscOutput)
          .setDescription("Configure OSC output for Resolume");

  public final TriggerParameter reloadConfig =
      new TriggerParameter("Reload Config", this::loadConfiguration)
          .setDescription("Reload configuration from YAML file");

  private LXOscConnection.Output oscOutput;
  private final Map<String, String> oscMappings = new HashMap<>();
  private static final String CONFIG_FILE_PATH = "resources/resolume-setup/sync-config.yaml";
  private String resolumeAddress = "";
  private int resolumePortNum = 7000;
  */

  public ResolumeSyncTask(LX lx) {
    super(lx);
    current = this;

    addParameter("enabled", this.enabled);
    
    // Register as both an OSC listener (for incoming messages) and IO listener (for monitoring outputs)
    this.lx.engine.osc.addListener(this);
    this.lx.engine.osc.addIOListener(this);
    
    // Create a monitoring OSC output to capture outgoing messages
    createMonitoringOutput();
    
    LX.log("Resolume Sync: OSC Message Monitor initialized with IO listener");
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    // Currently no parameter handling needed
  }

  @Override
  public void oscMessage(OscMessage message) {
    if (!this.enabled.isOn()) {
      return;
    }

    String incomingPath = message.getAddressPattern().toString();
    
    // Log all OSC messages we receive (incoming to LX)
    StringBuilder logMessage = new StringBuilder();
    logMessage.append("OSC Monitor - Incoming: ").append(incomingPath);
    
    if (message.size() > 0) {
      logMessage.append(" [");
      for (int i = 0; i < message.size(); i++) {
        if (i > 0) logMessage.append(", ");
        Object arg = message.get(i);
        if (arg instanceof Float) {
          logMessage.append("f:").append(arg);
        } else if (arg instanceof Integer) {
          logMessage.append("i:").append(arg);
        } else if (arg instanceof String) {
          logMessage.append("s:\"").append(arg).append("\"");
        } else {
          logMessage.append(arg.getClass().getSimpleName()).append(":").append(arg);
        }
      }
      logMessage.append("]");
    }
    
    LX.log(logMessage.toString());
  }

  // IOListener methods to monitor OSC outputs
  @Override
  public void inputAdded(LXOscEngine engine, LXOscConnection.Input input) {
    if (this.enabled.isOn()) {
      LX.log("OSC Monitor - Input Added: " + input.host.getString() + ":" + input.port.getValuei());
    }
  }

  @Override
  public void inputRemoved(LXOscEngine engine, LXOscConnection.Input input) {
    if (this.enabled.isOn()) {
      LX.log("OSC Monitor - Input Removed: " + input.host.getString() + ":" + input.port.getValuei());
    }
  }

  @Override
  public void outputAdded(LXOscEngine engine, LXOscConnection.Output output) {
    if (this.enabled.isOn()) {
      LX.log("OSC Monitor - Output Added: " + output.host.getString() + ":" + output.port.getValuei());
    }
  }

  @Override
  public void outputRemoved(LXOscEngine engine, LXOscConnection.Output output) {
    if (this.enabled.isOn()) {
      LX.log("OSC Monitor - Output Removed: " + output.host.getString() + ":" + output.port.getValuei());
    }
  }

  private void createMonitoringOutput() {
    // For now, we'll just log that we're setting up monitoring
    // The actual outgoing message monitoring will need to be implemented
    // by hooking into the LXOscEngine.sendMessage() calls differently
    LX.log("OSC Monitor: Setting up outgoing message monitoring...");
    
    // TODO: Implement actual outgoing message monitoring
    // This might require extending LXOscConnection.Output or using parameter listeners
  }

  @Override
  public void dispose() {
    if (this.lx != null && this.lx.engine != null && this.lx.engine.osc != null) {
      this.lx.engine.osc.removeListener(this);
      this.lx.engine.osc.removeIOListener(this);
    }
    super.dispose();
  }

  // Commented out - Future Resolume functionality
  /*
  private void forwardMessage(OscMessage originalMessage, String newPath) {
    if (this.oscOutput == null || !this.oscOutput.active.isOn()) {
      LX.log("Resolume Sync: OSC output not ready, message not sent.");
      return;
    }
    
    try {
      if (originalMessage.size() > 0) {
        Object firstArg = originalMessage.get(0);
        if (firstArg instanceof Float) {
          this.lx.engine.osc.sendMessage(newPath, (Float) firstArg);
        } else if (firstArg instanceof Integer) {
          this.lx.engine.osc.sendMessage(newPath, ((Integer) firstArg).floatValue());
        } else if (firstArg instanceof Double) {
          this.lx.engine.osc.sendMessage(newPath, ((Double) firstArg).floatValue());
        } else if (firstArg instanceof String) {
          this.lx.engine.osc.sendMessage(newPath, (String) firstArg);
        } else {
          this.lx.engine.osc.sendMessage(newPath, 1);
        }
      } else {
        this.lx.engine.osc.sendMessage(newPath, 1);
      }
    } catch (Exception e) {
      LX.error(e, "Resolume Sync: Failed to forward message: " + newPath);
    }
  }

  private void setupOscOutput() {
    updateResolumeAddress();
    
    try {
      // Look for an existing output to our destination
      for (LXOscConnection.Output output : this.lx.engine.osc.outputs) {
        if (output.host.getString().equals(this.resolumeAddress) && 
            output.port.getValuei() == this.resolumePortNum) {
          this.oscOutput = output;
          break;
        }
      }

      // If none found, create one
      if (this.oscOutput == null) {
        this.oscOutput = this.lx.engine.osc.addOutput();
        this.oscOutput.host.setValue(this.resolumeAddress);
        this.oscOutput.port.setValue(this.resolumePortNum);
      }
      
      this.oscOutput.active.setValue(true);

      ((GLX) this.lx).ui.showContextDialogMessage(
          "OSC output for Resolume is ready to use!\n" +
          "Target: " + this.resolumeAddress + ":" + this.resolumePortNum);

      LX.log("Resolume Sync: Dedicated OSC output configured for " + 
             this.resolumeAddress + ":" + this.resolumePortNum);
             
    } catch (Exception e) {
      LX.error("Resolume Sync: Failed to setup OSC output - " + e.getMessage());
      ((GLX) this.lx).ui.showContextDialogMessage(
          "Failed to setup Resolume OSC output:\n" + e.getMessage());
    }
  }

  private void loadConfiguration() {
    Path configFile = Path.of(CONFIG_FILE_PATH);
    if (!Files.exists(configFile)) {
      LX.log("Resolume Sync: Config file not found: " + configFile.toAbsolutePath());
      return;
    }

    try {
      String yamlContent = Files.readString(configFile);
      parseYamlConfig(yamlContent);
      LX.log("Resolume Sync: Successfully parsed " + this.oscMappings.size() + " mappings from config.");
    } catch (IOException e) {
      LX.error("Resolume Sync: Failed to load config file: " + e.getMessage());
    }
  }

  private void parseYamlConfig(String yamlContent) {
    this.oscMappings.clear();
    
    try {
      Pattern ipPattern = Pattern.compile("ip:\\s*\"([^\"]+)\"");
      Pattern portPattern = Pattern.compile("port:\\s*(\\d+)");
      
      Matcher ipMatcher = ipPattern.matcher(yamlContent);
      if (ipMatcher.find()) {
        this.resolumeIP.setValue(ipMatcher.group(1));
      }
      
      Matcher portMatcher = portPattern.matcher(yamlContent);
      if (portMatcher.find()) {
        this.resolumePort.setValue(portMatcher.group(1));
      }
      updateResolumeAddress();

      boolean inMappingsSection = false;
      String[] lines = yamlContent.split("\n");
      
      for (String line : lines) {
        line = line.trim();
        if (line.equals("mappings:")) {
          inMappingsSection = true;
          continue;
        }
        if (inMappingsSection && line.matches("^[a-zA-Z_]+:.*") && !line.startsWith("\"")) {
          inMappingsSection = false;
          continue;
        }
        if (inMappingsSection && line.matches("^\"[^\"]+\":\\s*\"[^\"]+\".*")) {
          Matcher lineMatcher = Pattern.compile("\"([^\"]+)\":\\s*\"([^\"]+)\"").matcher(line);
          if (lineMatcher.find()) {
            this.oscMappings.put(lineMatcher.group(1), lineMatcher.group(2));
          }
        }
      }
    } catch (Exception e) {
      LX.error("Resolume Sync: Error parsing YAML config - " + e.getMessage());
    }
  }

  private void updateResolumeAddress() {
    this.resolumeAddress = this.resolumeIP.getString();
    try {
      this.resolumePortNum = Integer.parseInt(this.resolumePort.getString());
    } catch (NumberFormatException e) {
      LX.error("Resolume Sync: Invalid port number: " + this.resolumePort.getString());
      this.resolumePortNum = 7000;
    }
  }
  */
}