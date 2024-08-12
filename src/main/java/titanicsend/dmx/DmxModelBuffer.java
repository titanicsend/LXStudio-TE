/**
 * Copyright 2023- Justin Belcher, Mark C. Slee, Heron Arts LLC
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
 * @author Mark C. Slee <mark@heronarts.com>
 */
package titanicsend.dmx;

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.model.DmxWholeModel;

import java.util.List;

/**
 * The DMX equivalent to ModelBuffer uses two classes, DmxBuffer and DmxModelBuffer, due to
 * effectively being a 2D array.
 *
 * DmxBuffer is one fixture, DmxModelBuffer is all the fixtures
 */
public class DmxModelBuffer extends DmxFullBuffer implements DmxWholeModel.DmxWholeModelListener {

  private final LX lx;
  private final DmxWholeModel dmxWholeModel;
  private DmxBuffer[] array;

  public boolean modified = false;

  public static DmxWholeModel getWholeModel(LXModel model) {
    if (model instanceof DmxWholeModel) {
      return (DmxWholeModel) model;
    } else {
      return null;
    }
  }

  public DmxModelBuffer(LX lx) {
    this(lx, getWholeModel(lx.getModel()));
  }

  public DmxModelBuffer(LX lx, DmxWholeModel model) {
    this.lx = lx;
    this.dmxWholeModel = model;
    initArray(model);
    this.dmxWholeModel.addDmxListener(this);
  }
  @Override
  public void dmxModelsChanged(List<DmxModel> dmxModels) {
    initArray(this.dmxWholeModel);
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
    this.dmxWholeModel.removeDmxListener(this);
    this.array = null;
  }

}
