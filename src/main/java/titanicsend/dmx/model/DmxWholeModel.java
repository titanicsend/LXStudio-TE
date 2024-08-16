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
package titanicsend.dmx.model;

import java.util.List;

/** Top level interface for a model containing DMX fixtures. */
public interface DmxWholeModel {

  /** Total number of DMX fixtures in the model */
  public int sizeDmx();

  public List<DmxModel> getDmxModels();

  default void clearBeacons() { }

  default void addBeacon(DmxModel dmxModel) { }

  public interface DmxWholeModelListener {
    public void dmxModelsChanged(List<DmxModel> dmxModels);
  }

  default public void addDmxListener(DmxWholeModelListener listener) { }

  default public void removeDmxListener(DmxWholeModelListener listener) { }

  /**
   * Sequence of events:
   * LX.modelChanged
   * TEWholeModel.modelTEChanged
   * DmxWholeModel.dmxModelsChanged
   *   --model buffers monitor this one and update
   */
  default void notifyDmxWholeModelListeners() { }
}
