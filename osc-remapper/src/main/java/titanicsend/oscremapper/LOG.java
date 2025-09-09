package titanicsend.oscremapper;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import heronarts.lx.LX;

/** Logging utility for the OscRemapper plugin */
public class LOG {

  private static final String PREFIX = "[OscRemapper] ";
  private static boolean verbose = false;

  public static void setVerbose(boolean enabled) {
    verbose = enabled;
    // Always log the state change
    LX.log(PREFIX + "Logging " + (enabled ? "ENABLED" : "DISABLED"));
  }

  public static boolean isVerbose() {
    return verbose;
  }

  @FormatMethod
  public static void debug(@FormatString String message, Object... arguments) {
    if (verbose) {
      LX.log(PREFIX + String.format(message, arguments));
    }
  }

  @FormatMethod
  public static void startup(@FormatString String message, Object... arguments) {
    LX.log(PREFIX + String.format(message, arguments));
  }

  @FormatMethod
  public static void error(@FormatString String message, Object... arguments) {
    LX.error(PREFIX + String.format(message, arguments));
  }

  @FormatMethod
  public static void error(Exception e, @FormatString String message, Object... arguments) {
    LX.error(e, PREFIX + String.format(message, arguments));
  }

  @FormatMethod
  public static void warning(@FormatString String message, Object... arguments) {
    LX.warning(PREFIX + String.format(message, arguments));
  }
}
