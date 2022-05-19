package titanicsend.model;

import java.util.*;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import titanicsend.util.Dimensions;

public abstract class TEModel extends LXModel {
  private final String teModelType;
  private final Dimensions dimensions;

  public TEModel(String teModelType, List<LXPoint> points) {
    super(points, teModelType);
    this.teModelType = teModelType;
    this.dimensions = Dimensions.fromPoints(points);
  }

  public abstract String getId();

  public String repr() {
    return teModelType + this.getId();
  }

  public Dimensions getDimensions() {
    return dimensions;
  }
}
