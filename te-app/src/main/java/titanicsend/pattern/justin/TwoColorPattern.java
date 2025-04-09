package titanicsend.pattern.justin;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UITextBox;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;
import java.util.ArrayList;
import java.util.List;
import titanicsend.util.TECategory;

@LXCategory(TECategory.UTILITY)
@LXComponentName("Two Color")
public class TwoColorPattern extends LXPattern implements UIDeviceControls<TwoColorPattern> {

  public LinkedColorParameter color1 = new LinkedColorParameter("Color1", LXColor.WHITE);
  public LinkedColorParameter color2 = new LinkedColorParameter("Color2", LXColor.RED);

  public StringParameter tag1 = new StringParameter("Tag1", "tag1");
  public StringParameter tag2 = new StringParameter("Tag2", "tag2");

  private final List<LXPoint> points1 = new ArrayList<LXPoint>();
  private final List<LXPoint> points2 = new ArrayList<LXPoint>();

  public TwoColorPattern(LX lx) {
    super(lx);

    addParameter("color1", this.color1);
    addParameter("color2", this.color2);
    addParameter("tag1", this.tag1);
    addParameter("tag2", this.tag2);

    refresh1();
    refresh2();
  }

  @Override
  protected void onModelChanged(LXModel model) {
    refresh1();
    refresh2();
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.tag1) {
      refresh1();
    } else if (p == this.tag2) {
      refresh2();
    }
  }

  protected void refresh1() {
    refresh(this.points1, this.tag1.getString());
  }

  protected void refresh2() {
    refresh(this.points2, this.tag2.getString());
  }

  private void refresh(List<LXPoint> points, String tag) {
    points.clear();
    if (LXUtils.isEmpty(tag)) {
      return;
    }

    for (LXModel tagModel : this.model.sub(tag)) {
      points.addAll(tagModel.getPoints());
    }
  }

  @Override
  protected void run(double deltaMs) {
    int color1 = this.color1.getColor();
    int color2 = this.color2.getColor();

    for (LXPoint point : this.points1) {
      this.colors[point.index] = color1;
    }

    for (LXPoint point : this.points2) {
      this.colors[point.index] = color2;
    }
  }

  protected UITextBox textTag1;
  protected UITextBox textTag2;

  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, TwoColorPattern device) {
    uiDevice.setLayout(UI2dContainer.Layout.VERTICAL, 8);
    uiDevice.setContentWidth(155);

    uiDevice.addChildren(
        UIDevice.newHorizontalContainer(UIKnob.HEIGHT, 4)
            .addChildren(newColorControl(this.color1), newTextBox(this.tag1, 100).setY(10)),
        UIDevice.newHorizontalContainer(UIKnob.HEIGHT, 4)
            .addChildren(newColorControl(this.color2), newTextBox(this.tag2, 100).setY(10)));
  }
}
