package titanicsend.model;

import java.util.*;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import titanicsend.util.PanelStriper;

public class TEPanelFactory {
  public static TEPanelModel build(String id, TEVertex v0, TEVertex v1, TEVertex v2,
                            TEEdgeModel e0, TEEdgeModel e1, TEEdgeModel e2,
                            String panelType) {
    ArrayList<LXPoint> points = new ArrayList<LXPoint>();

    float centroidX = (v0.x + v1.x + v2.x) / 3.0F;
    float centroidY = (v0.y + v1.y + v2.y) / 3.0F;
    float centroidZ = (v0.z + v1.z + v2.z) / 3.0F;

    LXVector centroid = new LXVector(centroidX, centroidY, centroidZ);

    String flavor;

    if (panelType.equals(TEPanelModel.LIT)) {
      List<LXPoint> stripedPoints = new ArrayList<>();
      try {
        flavor = PanelStriper.stripe(v0, v1, v2, stripedPoints);
      } catch (Throwable t) {
        LX.log("Problem striping Panel " + id);
        throw t;
      }
      points.addAll(stripedPoints);
    } else if (panelType.equals(TEPanelModel.SOLID)) {
      flavor = "solid";
      points.add(new LXPoint(centroid));
    } else {
      flavor = "unknown";
    }

    return new TEPanelModel(id, points, v0, v1, v2, e0, e1, e2, panelType, flavor, centroid);
  }
}