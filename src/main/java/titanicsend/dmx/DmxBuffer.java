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

package titanicsend.dmx;

import heronarts.lx.parameter.LXParameterListener;
import titanicsend.dmx.parameter.DmxParameter;

/**
 * Similar to ModelBuffer.
 * Eventually might be merged with LXBuffer/ModelBuffer.
 * 
 * DmxBuffer is one fixture, DmxModelBuffer is all the fixtures
 */
public class DmxBuffer {

  // We're going to make things easy by using an object (DmxParameter)
  // instead of a primitive (byte/int) in the buffer.
  // There are two buffers per channel and ~5 DMX fixtures so far,
  // so I think the performance impact is minor and worth the tradeoff.
  // This decision could be re-evaluated with more programming time later.
  public final DmxParameter[] array;

  public boolean isActive = false;
  public boolean isModified = false;

  private LXParameterListener parameterListener = (p) -> {
    isActive = true;
    isModified = true;
  };

  public DmxBuffer(DmxParameter[] parameters) {
    this.array = parameters;
    listenParameters();
  }

  private void listenParameters() {
    for (int i = 0; i < this.array.length; i++) {
      this.array[i].addListener(this.parameterListener);
    }
  }

  public DmxParameter get(int index) {
    return this.array[index];
  }

  public void dispose() {
    for (int i = 0; i < this.array.length; i++) {
      this.array[i].removeListener(this.parameterListener);
      this.array[i] = null;
    }
  }
}
