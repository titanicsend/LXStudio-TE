package titanicsend.oscremapper;

import heronarts.lx.LX;
import heronarts.lx.osc.LXOscEngine;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.osc.OscPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import titanicsend.oscremapper.config.RemapperConfig;

/** TransmissionListener for capturing and remapping outgoing OSC messages */
public class OscRemapperTransmissionListener implements LXOscEngine.TransmissionListener {
  private final LX lx;
  private RemapperConfig config;

  public OscRemapperTransmissionListener(LX lx, RemapperConfig config) {
    this.lx = lx;
    this.config = config;
  }

  public void setConfig(RemapperConfig config) {
    this.config = config;
  }

  @Override
  public void oscMessageTransmitted(OscPacket packet) {
    try {
      // Check if this is an OscMessage that we should remap
      if (packet instanceof OscMessage message) {
        String originalAddress = message.getAddressPattern().getValue();

        // Check if this address matches any global remapping
        if (shouldRemapAddress(originalAddress)) {
          LOG.log("Processing OSC message: %s", originalAddress);
          // Get all remapped addresses from global remappings
          List<String> remappedAddresses = getRemappedAddresses(originalAddress);
          LOG.log("Found %d remapped addresses: %s", remappedAddresses.size(), remappedAddresses);

          // Send each remapped message (LX OSC outputs will route based on filters)
          for (String remappedAddress : remappedAddresses) {
            try {
              sendRemappedMessage(message, originalAddress, remappedAddress);
            } catch (Exception e) {
              LOG.error(
                  e, "Failed to send remapped message: %s → %s", originalAddress, remappedAddress);
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
      LOG.log("%s → %s (%s)", originalAddress, remappedAddress, value);
    } catch (Exception e) {
      LOG.error(e, "Failed to send remapped message");
    }
  }
}
