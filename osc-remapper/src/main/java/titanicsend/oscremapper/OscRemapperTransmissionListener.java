package titanicsend.oscremapper;

import heronarts.lx.LX;
import heronarts.lx.osc.LXOscEngine;
import heronarts.lx.osc.OscMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import titanicsend.oscremapper.config.RemapperConfig;

/** TransmissionListener for capturing and remapping outgoing OSC messages */
public class OscRemapperTransmissionListener implements LXOscEngine.MessageListener {
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
  public void willSend(String address, int value) {
    // Get all remapped addresses from global remappings
    List<String> remappedAddresses = getRemappedAddresses(address);

    // Send each remapped message (LX OSC outputs will route based on filters)
    for (String remappedAddress : remappedAddresses) {
      debug(address, remappedAddress, value);
      this.lx.engine.osc.sendMessageVariant(remappedAddress, value);
    }
  }

  @Override
  public void willSend(String address, float value) {
    List<String> remappedAddresses = getRemappedAddresses(address);

    for (String remappedAddress : remappedAddresses) {
      debug(address, remappedAddress, value);
      this.lx.engine.osc.sendMessageVariant(remappedAddress, value);
    }
  }

  @Override
  public void willSend(String address, String value) {
    List<String> remappedAddresses = getRemappedAddresses(address);

    for (String remappedAddress : remappedAddresses) {
      debug(address, remappedAddress, value);
      this.lx.engine.osc.sendMessageVariant(remappedAddress, value);
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

  private void debug(String originalAddress, String remappedAddress, int value) {
    LOG.debug("%s → %s (%s)", originalAddress, remappedAddress, value);
  }

  private void debug(String originalAddress, String remappedAddress, float value) {
    LOG.debug("%s → %s (%s)", originalAddress, remappedAddress, value);
  }

  private void debug(String originalAddress, String remappedAddress, String value) {
    LOG.debug("%s → %s (%s)", originalAddress, remappedAddress, value);
  }
}
