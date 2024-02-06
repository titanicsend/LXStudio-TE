package titanicsend.pattern.jon;

import heronarts.lx.model.LXPoint;
import java.util.ArrayList;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;

public class ModelBender {
  private final String[] endEdgeIds = {
    // aft
    "110-111",
    "109-110",
    "109-111",
    "27-109",
    "28-109",
    "109-112",
    "109-113",
    "112-113",
    "112-124",
    "113-124",
    "116-124",
    "117-124",

    // fore
    "82-92",
    "81-82",
    "81-92",
    "70-81",
    "73-81",
    "81-89",
    "81-91",
    "89-91",
    "89-126",
    "91-126",
    "125-126",
    "126-129"
  };

  protected ArrayList<Float> modelZ;
  protected float endXMax;

  /**
   * Get a list of points on the ends of the car
   *
   * @param model
   * @return list of points
   */
  protected ArrayList<LXPoint> getEndPoints(TEWholeModel model) {
    ArrayList<LXPoint> endPoints = new ArrayList<LXPoint>();

    // add end panel points
    for (TEPanelModel panel : model.getAllPanels()) {
      String id = panel.getId();
      if (id.startsWith("F") || id.startsWith("A")) {
        for (LXPoint point : panel.getPoints()) {
          endPoints.add(point);
        }
      }
    }

    // add end edge points
    for (String edgeId : endEdgeIds) {
      for (LXPoint point : model.edgesById.get(edgeId).getPoints()) {
        endPoints.add(point);
      }
    }

    return endPoints;
  }

  public void adjustEndGeometry(TEWholeModel model) {

    // save the model's original z coordinates so we can restore them
    // after all views are created.  endXMax is the largest x coordinate at
    // the boundary of side and end (more-or-less).  We use it as the
    // starting x for our taper.
    modelZ = new ArrayList<Float>();
    endXMax = Math.abs(model.vertexesById.get(116).x);
    for (LXPoint p : model.getPoints()) {
      modelZ.add(p.z);
    }

    // set new z bounds for our modified model
    model.zMax += endXMax;
    model.zMin -= endXMax;
    model.zRange = model.zMax - model.zMin;

    // adjust the z coordinates of the end points to taper them
    // with decreasing x. This gives the model a slightly pointy
    // nose and makes texture mapping easier and better looking.
    ArrayList<LXPoint> endPoints = getEndPoints(model);
    for (LXPoint p : endPoints) {
      // kick out gap points or they'll break normalization.
      if (model.isGapPoint(p)) continue;

      double zOffset = endXMax - Math.abs(p.x);
      p.z += (p.z >= 0) ? zOffset : -zOffset;
    }
    model.normalizePoints();
  }

  public void restoreModel(TEWholeModel model) {
    // restore the model's original z bounds
    model.zMax -= endXMax;
    model.zMin += endXMax;
    model.zRange = model.zMax - model.zMin;

    int i = 0;
    for (LXPoint p : model.getPoints()) {
      p.z = modelZ.get(i++);
    }
  }
}
