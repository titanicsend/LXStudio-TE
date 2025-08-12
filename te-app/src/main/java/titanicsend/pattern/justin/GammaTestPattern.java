package titanicsend.pattern.justin;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pattern with a selection of gradients for calibrating gamma. Recommended to use with a View that
 * displays each fixture in its own coordinate space.
 */
@LXCategory("Test")
@LXComponentName("Gamma Test")
public class GammaTestPattern extends LXPattern implements UIDeviceControls<GammaTestPattern> {

  public enum ColorMode {
    RED,
    GREEN,
    BLUE,
    WHITE,
    ALTERNATE
  }

  public final EnumParameter<ColorMode> colorMode =
      new EnumParameter<>("Color", ColorMode.RED)
          .setDescription("Color Mode")
          .setIncrementMode(EnumParameter.IncrementMode.RELATIVE)
          .setWrappable(false);

  public enum Layout {
    PIXEL_ORDER,
    X,
    Y,
    Z,
    RADIUS
  }

  public final EnumParameter<Layout> layout =
      new EnumParameter<>("Layout", Layout.PIXEL_ORDER)
          .setIncrementMode(DiscreteParameter.IncrementMode.RELATIVE)
          .setWrappable(false);

  public enum Direction {
    NORMAL,
    INVERT,
    ALTERNATE;
  }

  public final EnumParameter<Direction> direction =
      new EnumParameter<>("Direction", Direction.NORMAL)
          .setIncrementMode(DiscreteParameter.IncrementMode.RELATIVE)
          .setWrappable(false);

  boolean modelChanged = true;

  private final List<LXModel> mutableRenderModels = new ArrayList<>();
  public final List<LXModel> renderModels = Collections.unmodifiableList(this.mutableRenderModels);

  public GammaTestPattern(LX lx) {
    super(lx);

    addParameter("colorMode", this.colorMode);
    addParameter("layout", this.layout);
    addParameter("direction", this.direction);
  }

  /* Help geometry functions find the correct normalization bounds */

  @Override
  protected void onModelChanged(LXModel model) {
    this.modelChanged = true;
  }

  private void refreshRenderModels() {
    LXModel topModel = this.lx.getModel();
    LXModel mv = this.getModelView();

    this.mutableRenderModels.clear();

    if (topModel == mv) {
      this.mutableRenderModels.add(topModel);
    } else {
      this.mutableRenderModels.addAll(List.of(mv.children));
    }
  }

  @Override
  public boolean isHiddenControl(LXParameter parameter) {
    // Include the View parameter in the remote controls
    if (parameter == this.view) {
      return false;
    }
    return super.isHiddenControl(parameter);
  }

  @Override
  protected void run(double deltaMs) {
    if (this.modelChanged) {
      this.modelChanged = false;
      refreshRenderModels();
    }

    final Layout layout = this.layout.getEnum();
    final Direction direction = this.direction.getEnum();
    final boolean invert = direction.equals(Direction.INVERT);
    final boolean alternate = direction.equals(Direction.ALTERNATE);

    double hue = 0;
    double sat = 100;
    final ColorMode color = this.colorMode.getEnum();
    switch (color) {
      case RED -> {
        hue = 0;
      }
      case GREEN -> {
        hue = 120;
      }
      case BLUE -> {
        hue = 240;
      }
      case WHITE -> {
        hue = 0;
        sat = 0;
      }
    }

    // Not alternating colors
    int iModel = 0;
    for (LXModel m : this.renderModels) {
      int numPoints = m.points.length;
      if (numPoints == 1) {
        setColor(m.points[0], LXColor.hsb(hue, sat, 100));
      } else {
        for (int i = 0; i < numPoints; i++) {
          LXPoint p = m.points[i];
          double n = 0;
          switch (layout) {
            case PIXEL_ORDER -> {
              n = i / (numPoints - 1.);
            }
            case X -> {
              n = p.xn;
            }
            case Y -> {
              n = p.yn;
            }
            case Z -> {
              n = p.zn;
            }
            case RADIUS -> {
              n = p.rcn;
            }
          }
          if ((!alternate && invert) || (alternate && (iModel % 2 == (invert ? 1 : 0)))) {
            n = 1 - n;
          }
          setColor(p, LXColor.hsb(hue, sat, 100. * n));
        }
      }
      if (color == ColorMode.ALTERNATE) {
        hue += 120;
        hue %= 360;
      }
      ++iModel;
    }
  }

  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, GammaTestPattern device) {
    uiDevice.setLayout(UI2dContainer.Layout.VERTICAL, 4);

    uiDevice.addChildren(
        newRow(ui, this.view).setTopMargin(4),
        newRow(ui, this.colorMode),
        newRow(ui, this.layout),
        newRow(ui, this.direction));
    uiDevice.setContentWidth(154);
  }

  private UI2dComponent newRow(LXStudio.UI ui, DiscreteParameter parameter) {
    return UI2dContainer.newHorizontalContainer(
        16,
        2,
        controlLabel(ui, parameter.getLabel(), 50)
            .setHeight(16)
            .setTopMargin(0)
            .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE),
        newDropMenu(parameter, 100));
  }
}
