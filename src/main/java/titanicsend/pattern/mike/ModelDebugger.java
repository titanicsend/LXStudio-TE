package titanicsend.pattern.mike;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UITextBox;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.*;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import java.util.*;
import titanicsend.app.TEVirtualColor;
import titanicsend.model.TEVertex;
import titanicsend.pattern.TEPattern;

@LXCategory("Test")
public class ModelDebugger extends TEPattern implements UIDeviceControls<ModelDebugger> {
  public enum ObjectType {
    VERTEX("Vertex"),
    EDGE("Edge"),
    PANEL("Panel"),
    LASER("Laser");

    public final String label;

    ObjectType(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public final EnumParameter<ObjectType> objectType =
      new EnumParameter<ObjectType>("Object Type", ObjectType.PANEL)
          .setDescription("Which type of object to light up");

  public final DiscreteParameter pointIndex =
      new DiscreteParameter("Index", -1, 2000)
          .setDescription("Pixel within the selected object to light up (-1 for all)");

  public final StringParameter objectId =
      new StringParameter("ID").setDescription("ID of the object to light up (blank for all)");

  private UI2dComponent idErrLabel;
  private UI2dComponent pointErrLabel;

  public ModelDebugger(LX lx) {
    super(lx);

    addParameter("objectType", this.objectType);
    addParameter("pointIndex", this.pointIndex);
    addParameter("objectId", this.objectId);
  }

  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, ModelDebugger pattern) {
    uiDevice.setLayout(UI2dContainer.Layout.VERTICAL);
    uiDevice.setChildSpacing(6);
    uiDevice.setContentWidth(COL_WIDTH);

    UITextBox tb;

    uiDevice.addChildren(
        newDropMenu(objectType),
        controlLabel(ui, "ID"),
        tb = new UITextBox(0, 0, COL_WIDTH, 16).setParameter(objectId),
        controlLabel(ui, "Point"),
        newIntegerBox(pointIndex),
        this.idErrLabel = controlLabel(ui, "Bad ID"),
        this.pointErrLabel = controlLabel(ui, "Bad point"));

    tb.setEmptyValueAllowed(true);

    this.objectType.addListener(this::repaint);
    this.objectId.addListener(this::repaint);
    this.pointIndex.addListener(this::repaint);
  }

  public void repaint(LXParameter unused) {
    this.clearPixels();
    this.clearVertexes();
    List<LXModel> subModels = new ArrayList<>();
    List<TEVertex> vertexes = new ArrayList<>();
    String idStr = this.objectId.getString().trim().toUpperCase();
    boolean getAll = idStr.equals("");

    this.idErrLabel.setVisible(false);

    switch (this.objectType.getEnum()) {
      case VERTEX:
        try {
          vertexes.add(this.modelTE.getVertex(Integer.parseInt(idStr)));
        } catch (NumberFormatException ignored) {
        }
        break;
      case EDGE:
        if (getAll) subModels.addAll(this.modelTE.getEdges());
        else if (this.modelTE.hasEdge(idStr))
          subModels.add(this.modelTE.getEdge(idStr));
        else this.idErrLabel.setVisible(true);
        break;
      case PANEL:
        if (getAll) subModels.addAll(this.modelTE.getPanels());
        else if (this.modelTE.hasPanel(idStr))
          subModels.add(this.modelTE.getPanel(idStr));
        else this.idErrLabel.setVisible(true);
        break;
      case LASER:
        // TODO: Implement
        break;
      default:
        throw new Error("huh?");
    }

    // If no submodels, don't print error about invalid points.
    // If there are submodels, turn on the error for now and see if it gets turned off.
    boolean pointErr = !(subModels.isEmpty() && vertexes.isEmpty());

    int pi = this.pointIndex.getValuei();
    for (LXModel subModel : subModels) {
      for (int i = 0; i < subModel.points.length; i++) {
        if (pi < 0 || pi == i) {
          pointErr = false;
          LXPoint point = subModel.points[i];
          colors[point.index] = LXColor.WHITE;
        }
      }
    }

    for (TEVertex vertex : vertexes) {
      vertex.virtualColor = new TEVirtualColor(255, 0, 0, 255);
    }
    this.pointErrLabel.setVisible(pointErr);
  }

  public void clearVertexes() {
    for (TEVertex vertex : this.modelTE.getVertexes()) {
      vertex.virtualColor = new TEVirtualColor(255, 255, 255, 255);
    }
  }

  public void run(double deltaMs) {}
}
