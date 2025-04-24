package titanicsend.util;

import heronarts.lx.LX;

public class TE {
  public static void log(String format, Object... arguments) {
    LX.log(String.format(format, arguments));
  }

  public static void warning(String format, Object... arguments) {
    LX.warning(String.format(format, arguments));
  }

  public static void error(String format, Object... arguments) {
    LX.error(String.format(format, arguments));
  }

  public static void error(Throwable x, String format, Object... arguments) {
    LX.error(x, String.format(format, arguments));
  }

  public static void error(Throwable x, String message) {
    LX.error(x, message);
  }

  public static boolean isEmpty(String s) {
    return s == null || s.trim().isEmpty();
  }
}
