/**
 * Copyright 2022- Justin Belcher, Mark C. Slee, Heron Arts LLC
 *
 * <p>This file is part of the LX Studio software library. By using LX, you agree to the terms of
 * the LX Studio Software License and Distribution Agreement, available at: http://lx.studio/license
 *
 * <p>Please note that the LX license is not open-source. The license allows for free,
 * non-commercial use.
 *
 * <p>HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE,
 * WITH RESPECT TO THE SOFTWARE.
 *
 * @author Justin K. Belcher <justin@jkb.studio>
 */
package titanicsend.app.autopilot.justin;

import heronarts.lx.utils.LXUtils;

/** Describes how to automate a parameter */
public class AutoParameter {

  public static enum Scale {
    ABSOLUTE,
    NORMALIZED
  }

  public static final double DEFAULT_MIN_PERIOD = 15;
  public static final double DEFAULT_MAX_PERIOD = 45;
  public static final double DEFAULT_RANGE = 1;

  /** Specifies min/max values are absolute or normalized */
  public final Scale scale;

  /** Path to the parameter. Must match the value passed to addParameter(path, ...) */
  public final String path;

  /** Minimum parameter value while modulated */
  public final double min;

  /** Maximum parameter value while modulated */
  public final double max;

  /**
   * Range which is active between min & max. By default the entire range will be active. If active
   * range is less than total, random placement will be applied.
   */
  public final double range;

  /** Minimum modulation period in seconds */
  public final double minPeriodSec;

  /** Maximum modulation period in seconds */
  public final double maxPeriodSec;

  public AutoParameter(String path, Scale scale, double min, double max) {
    this(path, scale, min, max, DEFAULT_MIN_PERIOD, DEFAULT_MAX_PERIOD, max - min);
  }

  public AutoParameter(String path, Scale scale, double min, double max, double range) {
    this(path, scale, min, max, DEFAULT_MIN_PERIOD, DEFAULT_MAX_PERIOD, range);
  }

  public AutoParameter(
      String path, Scale scale, double min, double max, double minPeriod, double maxPeriod) {
    this(path, scale, min, max, minPeriod, maxPeriod, max - min);
  }

  public AutoParameter(
      String path,
      Scale scale,
      double min,
      double max,
      double minPeriod,
      double maxPeriod,
      double range) {
    this.path = path;
    this.scale = scale;
    this.min = LXUtils.min(min, max);
    this.max = LXUtils.max(min, max);
    this.range = LXUtils.min(Math.abs(range), this.max - this.min);
    this.minPeriodSec = LXUtils.min(minPeriod, maxPeriod);
    this.maxPeriodSec = LXUtils.max(minPeriod, maxPeriod);
  }
}
