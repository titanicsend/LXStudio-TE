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

        // Get all remapped addresses from global remappings
        List<String> remappedAddresses = getRemappedAddresses(originalAddress);

        if (remappedAddresses.isEmpty()) {
          return;
        }

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
    } catch (Exception e) {
      LOG.error(e, "Error processing transmitted OSC message");
    }
  }

  /** Get all remapped addresses for a given source address */
  private List<String> getRemappedAddresses(String oscAddress) {
    List<String> results = new ArrayList<>();
    Map<String, List<String>> globalRemappings = config.getRemappings();

    for (Map.Entry<String, List<String>> entry : globalRemappings.entrySet()) {
      String sourcePattern = entry.getKey();

      // If oscAddress exactly matches sourcePattern OR contains a prefix,
      // replace the prefix as a message to publish.
      //
      // NOTE from LX OscMessage.hasPrefix: if the prefix is "/lx/some" we would want it to match
      // for the address "/lx/some/thing" but NOT for "/lx/something"
      if (OscMessage.hasPrefix(oscAddress, sourcePattern)) {
        for (String targetPattern : entry.getValue()) {
          results.add(oscAddress.replace(sourcePattern, targetPattern));
        }
      }
    }

    return results;
  }

  /** Send a remapped OSC message through the LX engine (assuming all values are floats) */
  private void sendRemappedMessage(
      OscMessage originalMessage, String originalAddress, String remappedAddress) {
    try {
      // Send the remapped message - LX engine will route it to appropriate outputs based on
      // filters
      float value = (originalMessage.size() > 0) ? originalMessage.getFloat(0) : 0.0f;
      lx.engine.osc.sendMessage(remappedAddress, value);
      LOG.debug("%s → %s (%s)", originalAddress, remappedAddress, value);
    } catch (Exception e) {
      LOG.error(e, "Failed to send remapped message");
    }
  }
}
