package titanicsend.util;

import heronarts.lx.LX;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/** For utilty funcs that we want really fast syntactic sugar access to! */
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

  public static String resourcedir;

  public static Scanner loadFile(String filename) {
    try {
      File f = new File(resourcedir + "/" + filename);
      return new Scanner(f);
    } catch (FileNotFoundException e) {
      throw new Error(filename + " not found below " + System.getProperty("user.dir"));
    }
  }
}
