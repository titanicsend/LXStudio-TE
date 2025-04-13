/**
 * Chromatik Plugin Log
 *
 * @author Justin Belcher <justin@jkb.studio>
 */
package titanicsend.audio;

import heronarts.lx.LX;

/**
 * Provides static logging methods within the context of a plugin that prepend a plugin-specific
 * string before passing to LX logging methods.
 */
public class LOG {

  // Adjust per-plugin
  public static final String PREFIX = "AudioStems";

  private static final String LOG_PREFIX = "[" + PREFIX + "] ";

  private static String prefix(String message) {
    return LOG_PREFIX + (message != null ? message : "");
  }

  public static void log(String message) {
    LX.log(prefix(message));
  }

  public static void warning(String message) {
    LX.warning(prefix(message));
  }

  public static void error(String message) {
    LX.error(prefix(message));
  }

  public static void error(Throwable x) {
    LX.error(x, prefix(x.getClass().getName() + ":" + x.getLocalizedMessage()));
  }

  public static void error(Throwable x, String message) {
    LX.error(x, prefix(message));
  }
}
