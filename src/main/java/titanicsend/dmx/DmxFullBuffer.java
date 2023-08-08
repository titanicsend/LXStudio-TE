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

import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.model.DmxWholeModel;

/**
 * DMX version of LXBuffer
 */
abstract public class DmxFullBuffer {
  abstract public DmxBuffer[] getArray();

  static public DmxBuffer[] createFullBuffer(DmxWholeModel dmxWholeModel) {
    DmxBuffer[] b = new DmxBuffer[dmxWholeModel.sizeDmx()];

    int i = 0;
    for (DmxModel dmxModel : dmxWholeModel.getDmxModels()) {
      b[i++] = dmxModel.createBuffer();
    }

    return b;
  }

  static public void copyFullBuffer(DmxBuffer[] src, DmxBuffer[] dst) {
    for (int i = 0; i < src.length; i++) {
      DmxBuffer s = src[i];
      DmxBuffer d = dst[i];
      for (int j = 0; j < s.array.length; j++) {
        d.array[j].setValue(s.array[j].getValue());
      }
      d.isActive = s.isActive;
      d.isModified = s.isModified;
    }
  }
}
