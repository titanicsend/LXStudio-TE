/**
 * License notes: Expecting to contribute these DMX modulators back to LX upstream
 */

package titanicsend.modulator.dmx;

import heronarts.lx.LXCategory;
import heronarts.lx.dmx.DmxModulator;
import heronarts.lx.dmx.LXDmxEngine;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;

/**
 * Maps a sequential set of DMX channels onto a 2D grid of size [rows x columns]
 *
 * @author Justin K. Belcher
 */
@LXModulator.Global("DMX Grid")
@LXModulator.Device("DMX Grid")
@LXCategory("DMX")
public class DmxGridModulator extends DmxModulator {

  public final DiscreteParameter rows =
    new DiscreteParameter("Rows", 1, 100)
    .setDescription("Number of rows in the grid");

  public final DiscreteParameter columns =
    new DiscreteParameter("Columns", 1, 100)
    .setDescription("Number of columns in the grid");

  private int[][] values;

  public DmxGridModulator() {
    this("DMX Grid");
  }

  public DmxGridModulator(String label) {
    super(label);
    addParameter("rows", this.rows);
    addParameter("columns", this.columns);
    resize();
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.rows || p == this.columns) {
      resize();
      if (this.lx != null) {
        readDmx();
      }
    }
  }

  private void resize() {
    final int rows = this.rows.getValuei();
    final int columns = this.columns.getValuei();
    this.values = new int[rows][columns];
  }

  @Override
  protected double computeValue(double deltaMs) {
    return readDmx();
  }

  /**
   * Retrieve DMX input values and store in 2D array
   *
   * @return average normalized value
   */
  private double readDmx() {
    int universe = this.universe.getValuei();
    int channel = this.channel.getValuei();
    final int rows = this.rows.getValuei();
    final int columns = this.columns.getValuei();

    int r = 0, c = 0, sum = 0;
    final int resolution = rows * columns;    
    for (int i = 0; i < resolution; i++) {
      this.values[r][c] = dmxGetValuei(universe, channel);
      sum += this.values[r][c];

      // Left->Right, Top->Bottom, wrap
      if (++c >= columns) {
        c = 0;
        r++;
      }

      // DMX data is assumed to wrap sequentially onto following universes
      if (++channel >= LXDmxEngine.MAX_CHANNEL) {
        channel = 0;
        if (++universe >= LXDmxEngine.MAX_UNIVERSE) {
          // Grid did not fit within ArtNet data
          break;
        }
      }
    }

    return sum / (double)resolution / 255.;
  }

  private int dmxGetValuei(int universe, int channel) {
    return this.lx.engine.dmx.getByte(universe, channel) & 0xff;
  }

  public int getValue(int row, int column) {
    return this.values[row][column];
  }

}
