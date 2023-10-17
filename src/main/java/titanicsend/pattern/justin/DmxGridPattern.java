package titanicsend.pattern.justin;

import java.util.HashMap;
import java.util.Map;

import heronarts.lx.LX;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.studio.TEApp;
import heronarts.lx.transform.LXVector;
import titanicsend.modulator.dmx.DmxGridModulator;
import titanicsend.pattern.TEPattern;

/**
 * Simple DMX Grid Pattern.  Uses the first instance of a grid modulator, to be expanded later.
 *
 * @author Justin K. Belcher
 */
@LXComponentName("DMX Grid")
public class DmxGridPattern extends TEPattern {

  public final LinkedColorParameter color = new LinkedColorParameter("Color", LXColor.WHITE);

  private final Map<LXModel, LXVector> locations = new HashMap<LXModel, LXVector>();

  public DmxGridPattern(LX lx) {
    super(lx);
    addParameter("color", this.color);
  }

  @Override
  protected void run(double deltaMs) {
    clearPixels();

    DmxGridModulator mod = getModulator();
    if (mod == null) {
      return;
    }

    int numRows = mod.rows.getValuei();
    int numColumns = mod.columns.getValuei();

    for (LXModel model : TEApp.wholeModel.children) {
      LXVector vector = getLocation(model);
      int row = (int) ((numRows - 1) * (1 - vector.y));
      int column = (int) ((numColumns - 1) * (1 - vector.z));

      float brightness = mod.getValue(row, column) / 255f;
      int color = getGridColor(brightness);
      setFixtureColor(model, color);
    }
  }

  /**
   * Calculate the normalized average x,y,z for an LXModel
   */
  private LXVector getLocation(LXModel model) {
    LXVector vector = locations.get(model);
    if (vector == null) {
      vector = new LXVector(
          (model.average.x - TEApp.wholeModel.xMin) / TEApp.wholeModel.xRange,
          (model.average.y - TEApp.wholeModel.yMin) / TEApp.wholeModel.yRange,
          (model.average.z - TEApp.wholeModel.zMin) / TEApp.wholeModel.zRange
          );
      this.locations.put(model, vector);
    }
    return vector;
  }

  void setFixtureColor(LXModel m, int color) {
    for (LXPoint p : m.points) {
      this.colors[p.index] = color;
    }
  }

  private DmxGridModulator getModulator() {
    // Simple for now, return the first instance of the modulator.
    for (LXModulator m : this.lx.engine.modulation.modulators) {
      if (m instanceof DmxGridModulator) {
        return (DmxGridModulator)m;
      }
    }
    return null;
  }

  private int getGridColor(float brightness) {
    int color = this.color.calcColor();
    float r = (float) (0xff & LXColor.red(color)) * brightness;
    float g = (float) (0xff & LXColor.green(color)) * brightness;
    float b = (float) (0xff & LXColor.blue(color)) * brightness;
    return LXColor.rgb((int) r,(int) g,(int) b);
  }

  @Override
  public void dispose() {
    this.locations.clear();
    super.dispose();
  }
}
