package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameter.Units;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * The SimplifyEffect forces output to be the same for all points within each view group, or within
 * subgroups as controlled by the depth parameter.
 *
 * @author Justin K. Belcher <justin@jkb.studio>
 */
@LXCategory("Model")
@LXComponentName("Simplify")
public class SimplifyEffect extends LXEffect
    implements heronarts.lx.LX.Listener, UIDeviceControls<SimplifyEffect> {

  public enum BlendMode {
    HSB("HSB") {
      private int num = 0;
      private float h, s, b;

      @Override
      void clear() {
        this.num = 0;
        this.h = 0f;
        this.s = 0f;
        this.b = 0f;
      }

      @Override
      void add(int color) {
        this.num++;
        this.h += LXColor.h(color);
        this.s += LXColor.s(color);
        this.b += LXColor.b(color);
      }

      @Override
      float brightness() {
        // This is crazy.  I think you want the luminosity divided by the max luminosity.
        // // Because full red hsb(0, 100, 100) should be 100% but hsb (0, 100, 100) should be 0.
        return this.b / this.num;
      }

      @Override
      int average() {
        return LXColor.hsb(this.h / this.num, this.s / this.num, this.b / this.num);
      }

      @Override
      int average(float brightness) {
        return LXColor.hsb((float) this.h / this.num, (float) this.s / this.num, brightness);
      }
    },
    RGB("RGB") {
      private int num = 0;
      private int r, g, b;

      @Override
      void clear() {
        this.r = this.g = this.b = 0;
        this.num = 0;
      }

      @Override
      void add(int color) {
        this.num++;
        this.r += (LXColor.red(color) & 0xFF);
        this.g += (LXColor.green(color) & 0xFF);
        this.b += (LXColor.blue(color) & 0xFF);
      }

      @Override
      float brightness() {
        return LXColor.luminosity(average());
      }

      @Override
      int average() {
        float r = (float) this.r / this.num;
        float g = (float) this.g / this.num;
        float b = (float) this.b / this.num;
        return LXColor.rgb((int) r, (int) g, (int) b);
      }

      @Override
      int average(float brightness) {
        int color = average();
        return LXColor.hsb(LXColor.h(color), LXColor.s(color), brightness);
      }
    };

    public final String label;

    private BlendMode(String label) {
      this.label = label;
    }

    abstract void clear();

    abstract void add(int color);

    /** Get brightness level of the average */
    abstract float brightness();

    abstract int average();

    /** Get the average color but reset the brightness to this new value */
    abstract int average(float brightness);
  }

  public final CompoundParameter amount =
      new CompoundParameter("Amount", 1)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Percentage of effect to apply");

  public final EnumParameter<BlendMode> blendMode =
      new EnumParameter<BlendMode>("Mode", BlendMode.HSB);

  public final DiscreteParameter depth =
      new DiscreteParameter("DepthMd", 1, 1, 4)
          .setDescription("Depth of [view] model hierarchy at which to extract models");

  public final CompoundParameter gate =
      new CompoundParameter("Gate", .1)
          .setUnits(Units.PERCENT_NORMALIZED)
          .setDescription("Percentages of points that need to be on to turn the model on");

  public final CompoundParameter gain =
      new CompoundParameter("Gain", 0)
          .setDescription("0 = variable output brightness.  1 = all or nothing brightness");

  final List<LXModel> models = new ArrayList<LXModel>();

  public SimplifyEffect(LX lx) {
    super(lx);

    addParameter("amount", this.amount);
    addParameter("depth", this.depth);
    addParameter("gate", this.gate);
    addParameter("gain", this.gain);

    this.lx.addListener(this);

    refreshModels();
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.view || p == this.depth) {
      refreshModels();
    }
  }

  @Override
  public void modelGenerationChanged(LX lx, LXModel model) {
    refreshModels();
  }

  protected void refreshModels() {
    // Candidate models are calculated from View groups + depth parameter
    final int depth = this.depth.getValuei();
    this.models.clear();
    extractModels(this.models, this.getModelView(), depth);
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    final double amount = this.amount.getValue();
    final BlendMode blendMode = this.blendMode.getEnum();
    final float gate = this.gate.getValuef();
    final float gain = this.gain.getValuef();

    for (LXModel m : this.models) {
      int numPoints = m.points.length;
      float lumens = 0;
      blendMode.clear();

      for (LXPoint p : m.points) {
        int c = this.colors[p.index];
        lumens += LXColor.luminosity(c) / 100;
        blendMode.add(c);
      }

      float level = lumens / numPoints;
      int color;
      if (level >= gate && level != 0f) {
        // Scale input brightness range (gate, 100) to output brightness range (0, 100)
        // IF avoids fail to 0 if both are 1.
        if (gate < 1f) {
          level = LXUtils.lerpf((level - gate) / (1f - gate), 1f, gain);
        }
        // color = blendMode.average(level);  // TODO: fix gain to match grayscale version
        color = blendMode.average();
      } else {
        color = LXColor.BLACK;
      }

      setModelColor(m, color, amount);
    }
  }

  /** Lerps every point in a model between its current color and the new color */
  private void setModelColor(LXModel m, int color, double lerp) {
    for (LXPoint p : m.points) {
      this.colors[p.index] = LXColor.lerp(this.colors[p.index], color, lerp);
    }
  }

  /** Recursive model extraction */
  protected void extractModels(List<LXModel> toList, LXModel fromModel, int depth) {
    if (depth > 0 && fromModel.children.length > 0) {
      for (LXModel child : fromModel.children) {
        extractModels(toList, child, depth - 1);
      }
      return;
    } else {
      toList.add(fromModel);
    }
  }

  @Override
  public void dispose() {
    this.lx.removeListener(this);
    this.models.clear();
    super.dispose();
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, SimplifyEffect device) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL);

    final int colWidth = 70;

    addColumn(
        uiDevice,
        sectionLabel("Amount"),
        newKnob(this.amount),
        newDropMenu(this.blendMode).setTopMargin(6));
    addColumn(
            uiDevice,
            sectionLabel("Source").setWidth(colWidth),
            newDropMenu(this.view).setWidth(colWidth).setTopMargin(6),
            controlLabel(ui, "View").setWidth(colWidth).setTopMargin(-3).setBottomMargin(7),
            newKnob(this.depth))
        .setChildSpacing(4)
        .setWidth(colWidth)
        .setLeftMargin(1);
    this.addVerticalBreak(ui, uiDevice).setLeftMargin(5);
    addColumn(uiDevice, "Output", newKnob(this.gate), newKnob(this.gain)).setChildSpacing(4);
  }
}
