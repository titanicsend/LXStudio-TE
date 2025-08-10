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

  public static void debug(String message, Object... arguments) {
    if (loggingEnabled) {
      LX.log(PREFIX + String.format(message, arguments));
    }
  }

  /** Log startup/important messages that should always be shown */
  public static void startup(String message, Object... arguments) {
    LX.log(PREFIX + String.format(message, arguments));
  }

  public static void error(String message, Object... arguments) {
    // Always show errors regardless of logging state
    LX.error(PREFIX + String.format(message, arguments));
  }

  public static void error(Exception e, String message, Object... arguments) {
    // Always show errors regardless of logging state
    LX.error(e, PREFIX + String.format(message, arguments));
  }

  public static void warning(String message, Object... arguments) {
    // Always show warnings regardless of logging state
    LX.warning(PREFIX + String.format(message, arguments));
  }
}
