package titanicsend.lx;

public class LXUtilsWithRange {
  /**
   * Returns true if value is between [min, max] inclusive
   *
   * @param val Value
   * @param min Min value
   * @param max Max value
   * @return True if contained in range
   */
  public static boolean inRange(int val, int min, int max) {
    return (val >= min) && (val <= max);
  }

  /**
   * Returns true if value is between [min, max] inclusive
   *
   * @param val Value
   * @param min Min value
   * @param max Max value
   * @return True if contained in range
   */
  public static boolean inRange(float val, float min, float max) {
    return (val >= min) && (val <= max);
  }

  /**
   * Returns true if value is between [min, max] inclusive
   *
   * @param val Value
   * @param min Min value
   * @param max Max value
   * @return True if contained in range
   */
  public static boolean inRange(double val, double min, double max) {
    return (val >= min) && (val <= max);
  }
}
