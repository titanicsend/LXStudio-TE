package titanicsend.oscremapper;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
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

  @FormatMethod
  public static void debug(@FormatString String message, Object... arguments) {
    if (loggingEnabled) {
      LX.log(PREFIX + String.format(message, arguments));
    }
  }

  /** Log startup/important messages that should always be shown */
  @FormatMethod
  public static void startup(@FormatString String message, Object... arguments) {
    LX.log(PREFIX + String.format(message, arguments));
  }

  @FormatMethod
  public static void error(@FormatString String message, Object... arguments) {
    // Always show errors regardless of logging state
    LX.error(PREFIX + String.format(message, arguments));
  }

  @FormatMethod
  public static void error(Exception e, @FormatString String message, Object... arguments) {
    // Always show errors regardless of logging state
    LX.error(e, PREFIX + String.format(message, arguments));
  }

  @FormatMethod
  public static void warning(@FormatString String message, Object... arguments) {
    // Always show warnings regardless of logging state
    LX.warning(PREFIX + String.format(message, arguments));
  }
}
