package titanicsend.oscremapper.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration data structures for OSC Remapper plugin Parses remapper_config.yaml from te-app
 * resources
 *
 * <p>New structure supports separate destinations and remappings
 */
public class RemapperConfig {
  private List<Destination> destinations = new ArrayList<>();
  private Map<String, List<String>> remappings = new HashMap<>();

  public List<Destination> getDestinations() {
    return destinations;
  }

  public void setDestinations(List<Destination> destinations) {
    this.destinations = destinations;
  }

  public Map<String, List<String>> getRemappings() {
    return remappings;
  }

  public void setRemappings(Map<String, List<String>> remappings) {
    // Filter out loop-causing mappings
    this.remappings = filterLoopCausingMappings(remappings);
  }

  /**
   * Filter out mappings that would cause infinite loops and remove identity mappings (no-ops) Rule:
   * if dest.hasPrefix(src) then it's a loop causing mapping.
   */
  private Map<String, List<String>> filterLoopCausingMappings(
      Map<String, List<String>> originalRemappings) {
    Map<String, List<String>> filteredRemappings = new HashMap<>();

    for (Map.Entry<String, List<String>> entry : originalRemappings.entrySet()) {
      String sourceAddressPrefix = entry.getKey();
      filteredRemappings.put(
          sourceAddressPrefix,
          entry.getValue().stream()
              .map(destination -> ensureNonLoopingDestination(destination, sourceAddressPrefix))
              .collect(Collectors.toList()));
    }

    return filteredRemappings;
  }

  /**
   * Check if a mapping would cause a loop Rule. If dest.hasPrefix(src) then it's loop-causing. If a
   * mapping is invalid, throw an exception and crash the whole app, so operators know to fix it.
   */
  private String ensureNonLoopingDestination(
      String destinationAddressPrefix, String sourceAddressPrefix) {
    // Identity mappings are handled separately - not considered loops here
    if (sourceAddressPrefix.equals(destinationAddressPrefix)) {
      throw new RuntimeException(
          String.format(
              "Skipping identity mapping (no-op): %s → %s (original message already sent)",
              sourceAddressPrefix, destinationAddressPrefix));
    }

    if (destinationAddressPrefix.startsWith(sourceAddressPrefix)) {
      throw new RuntimeException(
          String.format(
              "loop-causing mapping: %s → %s", sourceAddressPrefix, destinationAddressPrefix));
    }

    return destinationAddressPrefix;
  }

  /** Destination configuration for OSC output endpoints */
  public static class Destination {
    private String name;
    private String ip;
    private int port;
    private String filter; // Optional explicit filter

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getIp() {
      return ip;
    }

    public void setIp(String ip) {
      this.ip = ip;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getFilter() {
      return filter;
    }

    public void setFilter(String filter) {
      this.filter = filter;
    }

    @Override
    public String toString() {
      return String.format("%s → %s:%d (filter: %s)", name, ip, port, filter);
    }
  }
}
