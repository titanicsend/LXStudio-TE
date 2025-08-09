package titanicsend.oscremapper.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import titanicsend.oscremapper.LOG;

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
   * if dest.hasPrefix(src) then it's a loop causing mapping Note: Identity mappings (src == dest)
   * are filtered out as no-ops since original message is already sent
   */
  private Map<String, List<String>> filterLoopCausingMappings(
      Map<String, List<String>> originalRemappings) {
    Map<String, List<String>> filteredRemappings = new HashMap<>();

    for (Map.Entry<String, List<String>> entry : originalRemappings.entrySet()) {
      String sourcePattern = entry.getKey();
      List<String> originalDestinations = entry.getValue();
      List<String> filteredDestinations = new ArrayList<>();

      for (String destination : originalDestinations) {
        if (isLoopCausingMapping(sourcePattern, destination)) {
          LOG.log("🚫 Filtering out loop-causing mapping: " + sourcePattern + " → " + destination);
        } else if (sourcePattern.equals(destination)) {
          LOG.log(
              "🔄 Skipping identity mapping (no-op): "
                  + sourcePattern
                  + " → "
                  + destination
                  + " (original message already sent)");
        } else {
          filteredDestinations.add(destination);
        }
      }

      // Only add the mapping if there are valid destinations left
      if (!filteredDestinations.isEmpty()) {
        filteredRemappings.put(sourcePattern, filteredDestinations);
      } else {
        LOG.log(
            "🚫 Removing entire mapping "
                + sourcePattern
                + " - all destinations were loops or no-ops");
      }
    }

    LOG.log(
        "✅ Filtered mappings: "
            + originalRemappings.size()
            + " → "
            + filteredRemappings.size()
            + " source patterns");
    return filteredRemappings;
  }

  /**
   * Check if a mapping would cause a loop Rule: if dest.hasPrefix(src) then it's a loop causing
   * mapping Note: Identity mappings (src == dest) are handled separately as no-ops
   */
  private boolean isLoopCausingMapping(String sourcePattern, String destination) {
    // Identity mappings are handled separately - not considered loops here
    if (sourcePattern.equals(destination)) {
      return false;
    }

    // Handle wildcard patterns
    if (sourcePattern.endsWith("/*") && destination.endsWith("/*")) {
      String sourcePrefix = sourcePattern.substring(0, sourcePattern.length() - 2);
      String destPrefix = destination.substring(0, destination.length() - 2);

      // Check if dest has prefix of src: dest.hasPrefix(src)
      if (destPrefix.startsWith(sourcePrefix)) {
        return true;
      }
    }

    // Handle exact to wildcard
    if (!sourcePattern.endsWith("/*") && destination.endsWith("/*")) {
      String destPrefix = destination.substring(0, destination.length() - 2);
      if (sourcePattern.startsWith(destPrefix)) {
        return true;
      }
    }

    // Handle wildcard to exact
    if (sourcePattern.endsWith("/*") && !destination.endsWith("/*")) {
      String sourcePrefix = sourcePattern.substring(0, sourcePattern.length() - 2);
      if (destination.startsWith(sourcePrefix)) {
        return true;
      }
    }

    return false;
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
      return String.format(
          "Destination{name='%s', ip='%s', port=%d, filter='%s'}", name, ip, port, filter);
    }
  }
}
