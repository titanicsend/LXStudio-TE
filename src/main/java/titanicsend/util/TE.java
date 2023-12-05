package titanicsend.util;

import heronarts.lx.LX;

/** For utilty funcs that we want really fast syntatic sugar access to! */
public class TE {
  public static void log(String format, Object... arguments) {
    LX.log(String.format(format, arguments));
  }

  public static void err(String format, Object... arguments) {
    LX.error(String.format(format, arguments));
  }

  public static void err(Throwable x, String message) {
    LX.error(x, message);
  }

  public static boolean isEmpty(String s) {
    return s == null || s.trim().isEmpty();
  }
}
