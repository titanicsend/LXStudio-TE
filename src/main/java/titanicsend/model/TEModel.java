package titanicsend.model;

import java.util.*;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;

public abstract class TEModel extends LXModel {
  private final String teModelType;

  public TEModel(String teModelType, List<LXPoint> points) {
    super(points);
    this.teModelType = teModelType;
  }

  public abstract String getId();

  public String repr() {
    return teModelType + this.getId();
  }
}
