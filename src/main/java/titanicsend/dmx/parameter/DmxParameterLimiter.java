/**
 * Copyright 2023- Justin Belcher, Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package titanicsend.dmx.parameter;

import heronarts.lx.utils.LXUtils;

/**
 * User limits on DMX parameter values, more restrictive than the possible fixture values.
 * Can be used as a safety limit on tilt range, for example.
 */
public class DmxParameterLimiter {

  static public final double NO_LIMIT = -99999;

  static public enum LimitType {
    CLIP("Clip"),
    ZOOM("Zoom");

    public final String label;

    private LimitType(String label) {
      this.label = label;
    }
  }

  private LimitType limitType;

  DmxParameter parameter;

  private double min = NO_LIMIT;
  private double max = NO_LIMIT;
  private double range = 1;
  private boolean hasLimit = false;

  public DmxParameterLimiter(DmxParameter parameter) {
    this(parameter, NO_LIMIT, NO_LIMIT);
  }

  public DmxParameterLimiter(DmxParameter parameter, double min, double max) {
    this(parameter, min, max, LimitType.CLIP);
  }

  public DmxParameterLimiter(DmxParameter parameter, double min, double max, LimitType limitType) {
    this.parameter = parameter;
    this.limitType = limitType;
    setLimits(min, max);
  }

  public DmxParameterLimiter setLimits(double min, double max) {
    this.min = validateLimit(min);
    this.max = validateLimit(max);
    if (this.min != NO_LIMIT) {
      this.hasLimit = true;
      if (this.max != NO_LIMIT) {
        if (this.min > this.max) {
          double tmp = this.min;
          this.min = this.max;
          this.max = tmp;
        }
        this.range = this.max - this.min;
      } else {
        this.range = this.parameter.getMax() - this.min;
      }
    } else {
      if (this.max != NO_LIMIT) {
        this.hasLimit = true;
        this.range = this.max - this.parameter.getMin();
      } else {
        this.hasLimit = false;
      }
    }
    return this;
  }

  public double getMin() {
    return this.min;
  }

  public double getMax() {
    return this.max;
  }

  protected double validateLimit(double limit) {
    if (limit == NO_LIMIT) {
      return limit;
    }
    return LXUtils.constrain(limit, this.parameter.getMin(), this.parameter.getMax());
  }

  public DmxParameterLimiter setLimitType(LimitType limitType) {
    this.limitType = limitType;
    return this;
  }

  public LimitType getLimitType() {
    return this.limitType;
  }

  public void clearLimit() {
    this.min = NO_LIMIT;
    this.max = NO_LIMIT;
    this.hasLimit = false;
  }

  public double limit(double value) {
    if (this.hasLimit) {
      switch (this.limitType) {
      case ZOOM:
        value = ((value - this.parameter.getMin()) / this.parameter.getRangeD() * range) + (this.min != NO_LIMIT ? this.min : this.parameter.getMin());
        break;
      case CLIP:
        if (this.min != NO_LIMIT) {
          value = LXUtils.max(value, min);      
        }
        if (this.max != NO_LIMIT) {
          value = LXUtils.min(value, max);
        }
        break;
      }
    }
    return value;
  }

}
