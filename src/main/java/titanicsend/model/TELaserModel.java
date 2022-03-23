package titanicsend.model;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import titanicsend.lasercontrol.LaserControl;

import java.util.ArrayList;
import java.util.List;

public class TELaserModel extends TEModel {
  public LXVector origin;
  private LXVector direction;
  public int color;
  public String id;
  public LaserControl control;

  public TELaserModel(String id, double x, double y, double z) {
    super("Laser", makePoint(x, y, z));
    this.id = id;
    this.origin = new LXVector(this.points[0]);
    this.color = LXColor.rgb(255,0,0);
  }

  public String getId() {
    return this.id;
  }

  public LXVector getDirection() { return this.direction; }

  public void setDirection(LXVector direction) {
    if (this.id.startsWith("HP") && direction.y < 0) {
      LX.log("Refusing to aim " + this.id + " downward");
      direction.y = 0;
    }
    this.direction = direction;
  }

  private static List<LXPoint> makePoint(double x, double y, double z) {
    List<LXPoint> points = new ArrayList<>();
    points.add(new LXPoint(x, y, z));
    return points;
  }
}