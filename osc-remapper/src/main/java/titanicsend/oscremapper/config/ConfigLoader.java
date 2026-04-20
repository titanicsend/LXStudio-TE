package titanicsend.oscremapper.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import titanicsend.oscremapper.LOG;

/** Loads and parses the OSC Remapper configuration from YAML file */
public class ConfigLoader {

  private static final String CONFIG_FILE_PATH = "resources/osc_remapper/remapper_config.yaml";

  /** Load configuration from the standard location */
  public static RemapperConfig loadConfig() {
    return loadConfig(Path.of(CONFIG_FILE_PATH));
  }

  /** Load configuration from specified path */
  public static RemapperConfig loadConfig(Path configPath) {
    try {
      LOG.startup("Loading OSC Remapper config from: %s", configPath.toAbsolutePath());

      if (!Files.exists(configPath)) {
        LOG.error("Config file not found: %s", configPath.toAbsolutePath());
        return createDefaultConfig();
      }

      try (InputStream inputStream = new FileInputStream(configPath.toFile())) {
        RemapperConfig config = parseYamlConfig(inputStream);
        LOG.startup(
            "Successfully parsed YAML config with %d destinations and %d remappings",
            config.getDestinations().size(), config.getRemappings().size());
        return config;
      }

    } catch (Exception e) {
      LOG.error(e, "Failed to load OSC Remapper config: %s", e.getMessage());
      return createDefaultConfig();
    }
  }

  /** Parse YAML configuration from input stream */
  private static RemapperConfig parseYamlConfig(InputStream inputStream) throws IOException {
    LOG.debug("Starting YAML parsing...");
    Yaml yaml = new Yaml();

    // Parse the YAML as a generic object structure
    Object data = yaml.load(inputStream);
    LOG.debug("YAML data type: %s", (data != null ? data.getClass().getSimpleName() : "null"));

    if (data == null) {
      LOG.error("YAML data is null - file might be empty or invalid");
      return createDefaultConfig();
    }

    RemapperConfig config = new RemapperConfig();
    List<RemapperConfig.Destination> destinations = new ArrayList<>();
    Map<String, List<String>> remappings = new HashMap<>();

    if (data instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> rootMap = (Map<String, Object>) data;
      LOG.debug("YAML root keys: %s", rootMap.keySet());

      // Handle destinations array format
      Object destinationsData = rootMap.get("destinations");
      LOG.debug(
          "Destinations data type: %s",
          (destinationsData != null ? destinationsData.getClass().getSimpleName() : "null"));

      if (destinationsData instanceof List) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinationsList = (List<Map<String, Object>>) destinationsData;
        LOG.debug("Destinations list size: %d", destinationsList.size());

        for (int i = 0; i < destinationsList.size(); i++) {
          Map<String, Object> destinationMap = destinationsList.get(i);
          LOG.debug("Processing destination %d: %s", i, destinationMap.keySet());
          RemapperConfig.Destination destination = parseDestination(destinationMap);
          if (destination != null) {
            destinations.add(destination);
            LOG.debug("Successfully parsed destination: %s", destination.getName());
          } else {
            LOG.error("Failed to parse destination %d", i);
          }
        }
      }

      // Handle remappings section
      Object remappingsData = rootMap.get("remappings");
      LOG.debug(
          "Remappings data type: %s",
          (remappingsData != null ? remappingsData.getClass().getSimpleName() : "null"));

      if (remappingsData instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> remappingsMap = (Map<String, Object>) remappingsData;
        LOG.debug("Found %d remapping entries", remappingsMap.size());

        for (Map.Entry<String, Object> entry : remappingsMap.entrySet()) {
          String source = entry.getKey();
          Object targetData = entry.getValue();

          // All remappings must be arrays (even for single destinations)
          if (!(targetData instanceof List)) {
            LOG.error(
                "Remapping for '%s' must be an array. Found: %s",
                source, targetData.getClass().getSimpleName());
            LOG.error(
                "Use format: '%s: [\"destination\"]' instead of '%s: \"destination\"'",
                source, source);
            continue; // Skip this invalid mapping
          }

          @SuppressWarnings("unchecked")
          List<Object> targetList = (List<Object>) targetData;
          List<String> destinationStrings = new ArrayList<>();

          LOG.debug("Source %s has %d destinations", source, targetList.size());

          for (Object target : targetList) {
            String targetStr = target.toString();
            destinationStrings.add(targetStr);
            LOG.debug("Remapping: %s -> %s", source, targetStr);
          }

          // Validate wildcard mappings
          if (!validateWildcardMapping(source, destinationStrings)) {
            LOG.error("Invalid wildcard mapping for '%s' - skipping", source);
            continue; // Skip this invalid mapping
          }

          remappings.put(source, destinationStrings);
        }
      }

    } else {
      LOG.error("YAML root is not a Map. Found: %s", data.getClass().getSimpleName());
    }

    config.setDestinations(destinations);
    config.setRemappings(remappings);
    LOG.debug("Loaded %d destinations and %d remappings", destinations.size(), remappings.size());

    // Log destinations
    for (RemapperConfig.Destination destination : destinations) {
      LOG.debug(
          "destination: %s -> %s:%d",
          destination.getName(), destination.getIp(), destination.getPort());
    }

    // Log final remappings (after loop filtering)
    for (Map.Entry<String, List<String>> mapping : config.getRemappings().entrySet()) {
      String source = mapping.getKey();
      List<String> destinationPaths = mapping.getValue();
      if (destinationPaths.size() == 1) {
        LOG.debug("remapping: %s -> %s", source, destinationPaths.get(0));
      } else {
        LOG.debug("remapping: %s -> %s", source, destinationPaths);
      }
    }

    return config;
  }

  /** Parse a single destination configuration from YAML map */
  private static RemapperConfig.Destination parseDestination(Map<String, Object> destinationMap) {
    try {
      LOG.debug("Parsing destination: %s", destinationMap.keySet());
      RemapperConfig.Destination destination = new RemapperConfig.Destination();

      // Set basic properties
      String name = getString(destinationMap, "name", "Unknown");
      String ip = getString(destinationMap, "ip", "127.0.0.1");
      int port = getInt(destinationMap, "port", 7000);
      String filter = getString(destinationMap, "filter", null);

      // Validate that filter is mandatory
      if (filter == null || filter.trim().isEmpty()) {
        LOG.error("Filter is mandatory but missing for destination: %s", name);
        LOG.error("Each destination must have a 'filter' field specifying the OSC prefix");
        return null;
      }

      destination.setName(name);
      destination.setIp(ip);
      destination.setPort(port);
      destination.setFilter(filter);

      LOG.debug(
          "Destination properties: name=%s, ip=%s, port=%d, filter=%s", name, ip, port, filter);

      return destination;

    } catch (Exception e) {
      LOG.error(e, "Failed to parse destination configuration: %s", e.getMessage());
      return null;
    }
  }

  /** Get string value from map with default */
  private static String getString(Map<String, Object> map, String key, String defaultValue) {
    Object value = map.get(key);
    return value != null ? value.toString() : defaultValue;
  }

  /** Get integer value from map with default */
  private static int getInt(Map<String, Object> map, String key, int defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        LOG.error("Invalid integer value for %s: %s", key, value);
      }
    }
    return defaultValue;
  }

  /** Create a default configuration when file loading fails */
  private static RemapperConfig createDefaultConfig() {
    LOG.debug("Creating default OSC Remapper configuration");

    RemapperConfig config = new RemapperConfig();

    // Create default destination with mandatory filter
    List<RemapperConfig.Destination> destinations = new ArrayList<>();
    RemapperConfig.Destination testDestination = new RemapperConfig.Destination();
    testDestination.setName("Failed to load config");
    testDestination.setIp("127.0.0.1");
    testDestination.setPort(7000);
    testDestination.setFilter("/failed"); // Mandatory filter
    destinations.add(testDestination);

    // Create default remapping
    Map<String, List<String>> remappings = new HashMap<>();
    remappings.put("/lx/failed", List.of("/failed/to/load/config"));

    config.setDestinations(destinations);
    config.setRemappings(remappings);

    return config;
  }

  /**
   * Validate wildcard mappings according to plugin rules
   *
   * @param source Source OSC pattern
   * @param destinations List of destination patterns
   * @return true if valid, false if invalid
   */
  private static boolean validateWildcardMapping(String source, List<String> destinations) {
    // Check if source is a wildcard pattern
    if (source.endsWith("/*")) {
      // Rule: Wildcard sources don't support multiple destinations
      if (destinations.size() > 1) {
        LOG.error(
            "Wildcard source '%s' cannot have multiple destinations (%d found)",
            source, destinations.size());
        LOG.error("Wildcard mappings must be 1:1. Found destinations: %s", destinations);
        return false;
      }

      // Rule: If source is wildcard, destination must also be wildcard
      String destination = destinations.get(0);
      if (!destination.endsWith("/*")) {
        LOG.error(
            "Wildcard source '%s' must map to wildcard destination, found: '%s'",
            source, destination);
        LOG.error("Use format: '/lx/tempo/*' -> ['/remote/tempo/*']");
        return false;
      }
    }

    return true; // Valid mapping
  }
}
