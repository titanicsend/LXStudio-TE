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

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import titanicsend.dmx.model.DmxWholeModel;

/**
 * The DMX equivalent to ModelBuffer uses two classes, DmxBuffer and DmxModelBuffer,
 * due to effectively being a 2D array.
 * 
 * DmxBuffer is one fixture, DmxModelBuffer is all the fixtures
 */
public class DmxModelBuffer extends DmxFullBuffer {

  private final LX lx;
  private DmxBuffer[] array;

  public boolean modified = false;

  private final LX.Listener modelListener = new LX.Listener() {
    @Override
    public void modelChanged(LX lx, LXModel model) {
      // TE is a static model so this isn't necessary
      // if (array.length != model.size) {
      //   initArray(getWholeModel(model));
      // }
    }
  };

  static public DmxWholeModel getWholeModel(LXModel model) {
    if (model instanceof DmxWholeModel) {
      return (DmxWholeModel)model;
    } else {
      return null;
    }
  }

  public DmxModelBuffer(LX lx) {
    this(lx, getWholeModel(lx.getModel()));
  }

  public DmxModelBuffer(LX lx, DmxWholeModel model) {
    this.lx = lx;
    initArray(model);
    lx.addListener(this.modelListener);
  }

  private void initArray(DmxWholeModel dmxWholeModel) {
    if (dmxWholeModel != null) {      
      this.array = createFullBuffer(dmxWholeModel);
    } else {
      this.array = new DmxBuffer[0];
      LX.error("Unable to create DMX buffer, DmxWholeModel not found");
    }
  }

  public DmxBuffer get(int index) {
    return this.array[index];
  }

  public void resetModified() {
    this.modified = false;
  }

  @Override
  public DmxBuffer[] getArray() {
    return this.array;
  }

  public void dispose() {
    this.lx.removeListener(this.modelListener);
    this.array = null;
  }

}
