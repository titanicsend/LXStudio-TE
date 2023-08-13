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

package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXBuffer;
import heronarts.lx.LXLayeredComponent;
import heronarts.lx.pattern.LXPattern;
import titanicsend.dmx.DmxEngine;
import titanicsend.dmx.DmxModelBuffer;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.parameter.DmxDiscreteParameter;
import titanicsend.dmx.parameter.DmxParameter;

public abstract class DmxPattern extends LXPattern {

  // Comparable to int[] colors array.
  // TODO: shield this from rogue subclass reassignment
  // TODO: consider switching to 2D array instead
  protected DmxModelBuffer dmx = null;  

  public DmxPattern(LX lx) {
    super(lx);
  }

  /**
   * Intercept the buffer assignment, called by channel.
   * Store the matching DmxModelBuffer after
   * obtaining it from the singleton DmxEngine which is tracking
   * 1:1 instances of LXBuffer and DmxBuffer[] aka DmxModelBuffer
   */
  @Override
  public LXLayeredComponent setBuffer(LXBuffer buffer) {
    this.dmx = DmxEngine.get().getDmxModelBuffer(buffer, this.getChannel());
    return super.setBuffer(buffer);
  }

  /**
   * Convenience method for patterns to set a value for a DMX fixture into the current buffer.
   */
  public double setDmxValue(DmxModel dmxModel, String field, double value) {
    int fieldIndex = dmxModel.getFieldIndex(field);
    if (fieldIndex == DmxModel.FIELD_NOT_FOUND) {
      LX.error("DMX field not found: " + field);
      return 0;
    }

    return setDmxValue(dmxModel, fieldIndex, value);
  }

  /**
   * Convenience method for patterns to set a value for a DMX fixture into the current buffer.
   * Similar to: colors[point.index] = value
   */
  public double setDmxValue(DmxModel dmxModel, int fieldIndex, int value) {
    DmxParameter parameter = this.dmx.get(dmxModel.index).get(fieldIndex);
    parameter.setDmxValue(value);
    /*if (parameter instanceof DmxDiscreteParameter) {
      ((DmxDiscreteParameter)parameter).setDmxValue(value);
    } else {
      parameter.setValue(value);
    }*/
    return parameter.getValue();
  }

  /**
   * Convenience method for patterns to set a value for a DMX fixture into the current buffer.
   * Similar to: colors[point.index] = value
   */
  public double setDmxValue(DmxModel dmxModel, int fieldIndex, double value) {
    // TODO: add friendly safety checking
    DmxParameter parameter = this.dmx.get(dmxModel.index).get(fieldIndex);
    parameter.setDmxValue(value);
    return parameter.getValue();
  }

  /**
   * Convenience method for patterns to set a normalized value for a DMX fixture into the current buffer.
   */
  public double setDmxNormalized(DmxModel dmxModel, String field, double value) {
    int fieldIndex = dmxModel.getFieldIndex(field);
    if (fieldIndex == DmxModel.FIELD_NOT_FOUND) {
      LX.error("DMX field not found: " + field);
      return 0;
    }

    return setDmxNormalized(dmxModel, fieldIndex, value);
  }

  /**
   * Convenience method for patterns to set a normalized value for a DMX fixture into the current buffer.
   * Similar to: colors[point.index] = value
   */
  public double setDmxNormalized(DmxModel dmxModel, int fieldIndex, double value) {
    // TODO: add friendly safety checking
    DmxParameter parameter = this.dmx.get(dmxModel.index).get(fieldIndex);    
    parameter.setNormalized(value);
    return parameter.getNormalized();
  }
}
