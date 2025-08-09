package titanicsend.oscremapper;

import heronarts.lx.LX;

/** Logging utility for the OscRemapper plugin */
public class LOG {

  private static final String PREFIX = "[OscRemapper] ";
  private static boolean loggingEnabled = false;

  /** Set whether logging is enabled */
  public static void setEnabled(boolean enabled) {
    loggingEnabled = enabled;
    // Always log the state change
    LX.log(PREFIX + "Logging " + (enabled ? "ENABLED" : "DISABLED"));
  }

  /** Check if logging is currently enabled */
  public static boolean isEnabled() {
    return loggingEnabled;
  }

  public static void log(String message) {
    if (loggingEnabled) {
      LX.log(PREFIX + message);
    }
  }

  /** Log startup/important messages that should always be shown */
  public static void startup(String message) {
    LX.log(PREFIX + message);
  }

  public static void error(String message) {
    // Always show errors regardless of logging state
    LX.error(PREFIX + message);
  }

  public static void error(Exception e, String message) {
    // Always show errors regardless of logging state
    LX.error(e, PREFIX + message);
  }

  public static void warning(String message) {
    // Always show warnings regardless of logging state
    LX.warning(PREFIX + message);
  }
}
