package titanicsend.ndi;

import heronarts.glx.ui.UI2dContainer;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.structure.LXFixture;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIControls;
import heronarts.lx.studio.ui.fixture.UIFixture;
import heronarts.lx.studio.ui.fixture.UIFixtureControls;
import heronarts.lx.transform.LXMatrix;
import heronarts.lx.transform.LXTransform;
import java.util.List;

@LXCategory("NDI")
@LXComponentName("NDI Out")
public class NDIOutFixture extends LXFixture
    implements UIFixtureControls<NDIOutFixture>, UIControls {

  public final DiscreteParameter heightPixels =
      new DiscreteParameter("Height Pixels", 480, 10, 4096)
          .setUnits(LXParameter.Units.INTEGER)
          .setDescription("Height of the fixture in pixels");

  public final DiscreteParameter widthPixels =
      new DiscreteParameter("Width Pixels", 480, 10, 4096)
          .setUnits(LXParameter.Units.INTEGER)
          .setDescription("Width of the fixture in pixels");

  public final CompoundParameter widthFt =
      new CompoundParameter("Width Feet", 50, 1, 150)
          .setDescription("Width of the fixture in feet");

  public final CompoundParameter heightFt =
      new CompoundParameter("Height Feet", 50, 1, 150)
          .setDescription("Height of the fixture in feet");

  public final StringParameter stream =
      new StringParameter("Stream", "TitanicsEnd").setDescription("Name of the NDI output stream");

  public NDIOutFixture(LX lx) {
    super(lx, "NDI Out");
    addMetricsParameter("heightPixels", this.heightPixels);
    addMetricsParameter("widthPixels", this.widthPixels);
    addGeometryParameter("widthFt", this.widthFt);
    addGeometryParameter("heightFt", this.heightFt);
    addParameter("stream", this.stream);

    // Default position to avoid overlap with TE main car
    this.x.setValue(500);
    this.hasCustomPointSize.setValue(true);
    this.pointSize.setValue(0.1);
  }

  @Override
  protected int size() {
    return this.heightPixels.getValuei() * this.widthPixels.getValuei();
  }

  @Override
  protected void computePointGeometry(LXMatrix matrix, List<LXPoint> points) {
    // Copied from LX's GridFixture:

    // Create points in the same sequence they will be sampled for NDI:
    // Left-to-right (increasing X), bottom-to-top (increasing Y)
    LXTransform transform = new LXTransform(matrix);
    int numRows = this.heightPixels.getValuei();
    int numColumns = this.widthPixels.getValuei();
    float rowSpacing = this.heightFt.getValuef() / (numRows - 1) * 12;
    float columnSpacing = this.widthFt.getValuef() / (numColumns - 1) * 12;

    int pi = 0;
    for (int r = 0; r < numRows; ++r) {
      transform.push();
      for (int c = 0; c < numColumns; ++c) {
        final LXPoint p = points.get(pi++);
        p.set(transform);
        p.setNormal(-matrix.m13, -matrix.m23, -matrix.m33);
        transform.translateX(columnSpacing);
      }
      transform.pop();
      transform.translateY(rowSpacing);
    }
  }

  @Override
  protected String[] getDefaultTags() {
    return new String[] {"ndi"};
  }

  @Override
  protected void buildOutputs() {
    // NDI does not use normal outputs
  }

  // UIFixtureControls

  final float labelWidth = UIFixture.PARAMETER_LABEL_WIDTH;
  float controlWidth;

  @Override
  public void buildFixtureControls(LXStudio.UI ui, UIFixture uiFixture, NDIOutFixture fixture) {
    uiFixture.setLayout(UI2dContainer.Layout.VERTICAL, 2);
    this.controlWidth = uiFixture.getContentWidth() - labelWidth - 2;

    uiFixture.addTagSection();
    uiFixture.addGeometrySection();

    UIFixture.Section section = uiFixture.addSection("Parameters");

    addRow(uiFixture, section, this.widthPixels);
    addRow(uiFixture, section, this.heightPixels);
    addRow(uiFixture, section, this.widthFt);
    addRow(uiFixture, section, this.heightFt);
    addRow(uiFixture, section, this.stream);

    uiFixture.addRenderingSection();
  }

  private void addRow(UIFixture uiFixture, UIFixture.Section section, DiscreteParameter p) {
    section.addControlRow(
        uiFixture
            .newParameterLabel(p.getLabel(), this.labelWidth)
            .setDescription(p.getDescription()),
        uiFixture.newControlIntBox(p, this.controlWidth));
  }

  private void addRow(UIFixture uiFixture, UIFixture.Section section, CompoundParameter p) {
    section.addControlRow(
        uiFixture
            .newParameterLabel(p.getLabel(), this.labelWidth)
            .setDescription(p.getDescription()),
        uiFixture.newControlBox(p, this.controlWidth));
  }

  private void addRow(UIFixture uiFixture, UIFixture.Section section, StringParameter p) {
    section.addControlRow(
        uiFixture
            .newParameterLabel(p.getLabel(), this.labelWidth)
            .setDescription(p.getDescription()),
        uiFixture.newControlTextBox(p, this.controlWidth));
  }
}
