package titanicsend.pattern.jon;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.util.ArrayList;
import titanicsend.model.*;

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

  protected ArrayList<Float> savedModelCoord;
  protected float endDepthMax;

  /**
   * Get a list of points on the ends of the car
   *
   * @param model
   * @return list of points
   */
  protected ArrayList<LXPoint> getEndPoints(TEWholeModel model) {
    ArrayList<LXPoint> endPoints = new ArrayList<LXPoint>();

    // add end panel points
    for (TEPanelModel panel : model.getPanels()) {
      String id = panel.getId();
      if (id.startsWith("F") || id.startsWith("A")) {
        for (LXPoint point : panel.model.getPoints()) {
          endPoints.add(point);
        }
      }
    }

    // add end edge points
    TEEdgeModel endEdge;
    for (String edgeId : endEdgeIds) {
      endEdge = model.getEdge(edgeId);
      if (endEdge != null) {
        for (LXPoint point : endEdge.model.getPoints()) {
          endPoints.add(point);
        }
      }
    }

    return endPoints;
  }

  public void adjustEndGeometry(TEWholeModelStatic model) {

    // save the model's original z coordinates so we can restore them
    // after all views are created.  endXMax is the largest x coordinate at
    // the boundary of side and end (more-or-less).  We use it as the
    // starting x for our taper.
    savedModelCoord = new ArrayList<Float>();
    endDepthMax = Math.abs(model.vertexesById.get(116).x);
    for (LXPoint p : model.getPoints()) {
      savedModelCoord.add(p.z);
    }

    // set new z bounds for our modified model
    model.zMax += endDepthMax;
    model.zMin -= endDepthMax;
    model.zRange = model.zMax - model.zMin;

    // adjust the z coordinates of the end points to taper them
    // with decreasing x. This gives the model a slightly pointy
    // nose and makes texture mapping easier and better looking.
    ArrayList<LXPoint> endPoints = getEndPoints(model);
    for (LXPoint p : endPoints) {
      // kick out gap points or they'll break normalization.
      if (model.isGapPoint(p)) continue;

      double zOffset = endDepthMax - Math.abs(p.x);
      p.z += (p.z >= 0) ? zOffset : -zOffset;
    }
    model.normalizePoints();
  }

  public void restoreModel(TEWholeModelStatic model) {
    // restore the model's original z bounds
    model.zMax -= endDepthMax;
    model.zMin += endDepthMax;
    model.zRange = model.zMax - model.zMin;

    int i = 0;
    for (LXPoint p : model.getPoints()) {
      p.z = savedModelCoord.get(i++);
    }
  }

  // Dynamic model version
  public void adjustEndGeometry(TEWholeModelDynamic model, LXModel baseModel) {

    // save the model's original x (width) coordinates so we can restore them
    // after all views are created.  endDepthMax is the largest z coordinate at
    // the boundary of side and end (more-or-less).  We use it as the
    // starting z for our taper.
    // TODO - replace estimate w/appropriate real value when model.getVertex() is working.
    endDepthMax = 0.85f * baseModel.zMax; //Math.abs(model.getVertex(116).z);
    System.out.println("endDepthMax: " + endDepthMax);

    savedModelCoord = new ArrayList<Float>();
    for (LXPoint p : baseModel.getPoints()) {
      savedModelCoord.add(p.x);
    }

    // set new x bounds for our modified model
    baseModel.xMax += endDepthMax;
    baseModel.xMin -= endDepthMax;
    baseModel.xRange = baseModel.xMax - baseModel.xMin;

    // adjust the x coordinates of the end points to taper them
    // with decreasing z. This gives the model a slightly pointy
    // nose and makes texture mapping easier and better looking.
    ArrayList<LXPoint> endPoints = getEndPoints(model);
    for (LXPoint p : endPoints) {
      // kick out gap points or they'll break normalization.
      if (model.isGapPoint(p)) continue;

      float xOffset = endDepthMax - Math.abs(p.z);
      p.x += (p.x >= 0) ? xOffset : -xOffset;
    }

    baseModel.normalizePoints();
  }

  // Dynamic model version
  public void restoreModel(TEWholeModelDynamic model, LXModel baseModel) {
    // restore the model's original width (x) bounds
    baseModel.xMax -= endDepthMax;
    baseModel.xMin += endDepthMax;
    baseModel.xRange = baseModel.xMax - baseModel.xMin;

    int i = 0;
    for (LXPoint p : baseModel.getPoints()) {
      p.x = savedModelCoord.get(i++);
    }
  }
}
