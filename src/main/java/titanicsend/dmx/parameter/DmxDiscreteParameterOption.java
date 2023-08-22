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

/**
 * A value for a DMX parameter, can be a single value or a range
 */
public class DmxDiscreteParameterOption {
  public final String label;
  public final int min;
  public final int max;
  public final int range;

  public DmxDiscreteParameterOption(String label, int value) {
    this(label, value, value);
  }

  public DmxDiscreteParameterOption(String label, int min, int max) {
    this.label = label;
    if (min <= max) {
      this.min = min;
      this.max = max;
    } else {
      this.min = max;
      this.max = min;        
    }
    this.range = max - min;
  }

  @Override
  public String toString() {
    return label;    
  }

}
