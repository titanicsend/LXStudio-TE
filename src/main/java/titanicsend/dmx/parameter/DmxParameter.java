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

import heronarts.lx.parameter.LXListenableParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameterListener;

public interface DmxParameter extends LXNormalizedParameter {

  public enum DmxBlendMode {
    LERP,
    JUMP_START,
    JUMP_END    
  }
  public DmxBlendMode getBlendMode();

  public double getMin();
  public double getMax();
  public double getRangeD();

  /**
   * Get value at a given alpha level.  Only some DMX parameters (such as dimmer) will scale for alpha.
   */
  public double getDmxValue(double alpha);
  public DmxParameter setDmxValue(double value);
  public DmxParameterLimiter getLimiter();
  /**
   * Get DMX value after limiter has been applied
   */
  public double getDmxValueLimited();  
  /**
   * Get DMX value after limiter has been applied, for given alpha level.
   */
  public double getValueLimited(double alpha);

  public DmxParameter copy();

  /**
   * Get number of output bytes for this parameter.  Most DMX parameters are 1 (8 bit) or 2 bytes (16 bit)
   */
  public int getNumBytes();
  /**
   * For now, have each parameter write its (limited) output value to the output array.
   * Later will remove this power from parameters.
   */
  public void writeBytes(byte[] output, int offset);

  public LXListenableParameter addListener(LXParameterListener listener);
  public LXListenableParameter removeListener(LXParameterListener listener);

}
