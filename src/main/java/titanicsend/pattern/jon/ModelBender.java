package titanicsend.pattern.jon;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.util.ArrayList;

import heronarts.lx.studio.TEApp;
import titanicsend.model.*;

public class ModelBender {
  private static final String[] endEdgeIds = {
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

  // Class used to save the original width coordinate of the points
  // we're going to change. We keep a reference to the original point
  // object so that if someone reorders the points in the model, we can
  // still restore it correctly.
  protected class SavedPoint {
    private final LXPoint p;
    private final float v;

    public SavedPoint(LXPoint p, float v) {
      this.p = p;
      this.v = v;
    }

    public void restore() {
      if (TEApp.wholeModel.isStatic()) {
        p.z = v;
      } else {
        p.x = v;
      }
    }
  }

  protected final ArrayList<SavedPoint> savedModelCoord = new ArrayList<SavedPoint>();

  // endDepthMax is the largest depth (x or z) coordinate at the boundary of
  // side and end.  We use it as the starting depth for our taper.
  protected float endDepthMax;

  // carLengthMin and carLengthMax are the min and max lengths coordinates
  // of the main car (as opposed to the whole model, which may contain
  // other vehicles or objects).
  //
  // NOTE: For this purpose, we are assuming that only the Titanic's End main
  // car model has metadata identifying the end panels and edges, so we can
  // use it to find the model extents for just the car, even in a larger model.
  // If we add similar metadata to the Mothership, we'll need to revisit this.
  protected float carLengthMin;
  protected float carLengthMax;

  protected float oldLengthMin;
  protected float oldLengthMax;

  /**
   * Get a list of points on the ends of the car,
   * and find the min and max length (z or x) axis values for the
   * car (as opposed to the whole model).
   *
   * @param model
   * @return list of points
   */
  protected ArrayList<LXPoint> getEndPoints(TEWholeModel model) {
    ArrayList<LXPoint> endPoints = new ArrayList<LXPoint>();

    // reset car length min and max
    carLengthMin = Float.MAX_VALUE;
    carLengthMax = Float.MIN_VALUE;

    boolean isStatic = model.isStatic();

    // add end panel points
    for (TEPanelModel panel : model.getPanels()) {
      String id = panel.getId();
      if (id.startsWith("F") || id.startsWith("A")) {
        for (LXPoint point : panel.model.getPoints()) {
          if (isStatic) {
            carLengthMin = Math.min(carLengthMin, point.z);
            carLengthMax = Math.max(carLengthMax, point.z);
            endDepthMax =  Math.max(endDepthMax, Math.abs(point.x));
          } else {
            carLengthMin = Math.min(carLengthMin, point.x);
            carLengthMax = Math.max(carLengthMax, point.x);
            endDepthMax =  Math.max(endDepthMax, Math.abs(point.z));
          }
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
          if (isStatic) {
            carLengthMin = Math.min(carLengthMin, point.z);
            carLengthMax = Math.max(carLengthMax, point.z);
            endDepthMax =  Math.max(endDepthMax, Math.abs(point.x));
          } else {
            carLengthMin = Math.min(carLengthMin, point.x);
            carLengthMax = Math.max(carLengthMax, point.x);
            endDepthMax =  Math.max(endDepthMax, Math.abs(point.z));
          }
          endPoints.add(point);
        }
      }
    }

    return endPoints;
  }

  /**
   * Adjust the geometry of the end of the car to taper it.
   * STATIC MODEL VERSION
   * @param model pointer to the (static) model
   * @return true if the model was adjusted and requires view
   *        renormalization, false otherwise.
   */
  public boolean adjustEndGeometry(TEWholeModelStatic model) {

    // get the list of points we need to adjust and find the
    // car dimensions needed to build the taper.
    ArrayList<LXPoint> endPoints = getEndPoints(model);

    // set new z bounds for our modified model
    oldLengthMin = model.zMin;
    oldLengthMax = model.zMax;

    model.zMax = carLengthMax + endDepthMax;
    model.zMin -= carLengthMin - endDepthMax;
    model.zRange = model.zMax - model.zMin;

    // adjust the z coordinates of the end points to taper them
    // with decreasing x. This gives the model a slightly pointy
    // nose and makes texture mapping easier and better looking.
    this.savedModelCoord.clear();

    for (LXPoint p : endPoints) {
      // save the original coordinate for each point we're modifying
      // so we can restore it later
      this.savedModelCoord.add(new SavedPoint(p, p.z));

      // kick out gap points or they'll break normalization.
      if (model.isGapPoint(p)) continue;

      float zOffset = endDepthMax - Math.abs(p.x);
      p.z += (p.z >= 0) ? zOffset : -zOffset;
    }
    model.normalizePoints();

    return true;
  }

  public void restoreModel(TEWholeModelStatic model) {
    // restore the model's original z bounds
    model.zMax = oldLengthMax;
    model.zMin = oldLengthMin;
    model.zRange = model.zMax - model.zMin;

    // restore the original z coordinates
    for (SavedPoint sp : this.savedModelCoord) {
      sp.restore();
    }
  }

  /**
   * Adjust the geometry of the end of the car to taper it to improve
   * texture mapping.
   * DYNAMIC MODEL VERSION
   * @param model pointer to the (dynamic) model
   * @return true if the model was adjusted and requires view
   *        renormalization, false otherwise.
   */
  public boolean adjustEndGeometry(TEWholeModelDynamic model, LXModel baseModel) {

    // get the list of points we need to adjust and find the
    // car dimensions needed to build the taper.
    ArrayList<LXPoint> endPoints = getEndPoints(model);

    this.savedModelCoord.clear();

    // if the model doesn't include the main car, nothing more to do.
    if (endPoints.size() == 0) {
      return false;
    }

    // set new x bounds for our modified model
    oldLengthMin = baseModel.xMin;
    oldLengthMax = baseModel.xMax;

    // adjust the model's x bounds to make room for the taper
    // if the car is part of a larger model space, check to see if there
    // is already enough room for the taper without adjusting the bounds.
    baseModel.xMax = Math.max(baseModel.xMax, carLengthMax + endDepthMax);
    baseModel.xMin = Math.min(baseModel.xMin, carLengthMin - endDepthMax);
    baseModel.xRange = baseModel.xMax - baseModel.xMin;

    // adjust the x coordinates of the end points to taper them
    // with decreasing z. This gives the model a slightly pointy
    // nose and makes texture mapping easier and better looking.
    for (LXPoint p : endPoints) {
      // save the original coordinate for each point we're modifying
      // so we can restore it later
      this.savedModelCoord.add(new SavedPoint(p, p.x));

      // kick out gap points or they'll break normalization.
      if (model.isGapPoint(p)) continue;

      float xOffset = endDepthMax - Math.abs(p.z);
      p.x += (p.x >= 0) ? xOffset : -xOffset;
    }
    baseModel.normalizePoints();

    return true;
  }

  // Dynamic model version
  public void restoreModel(TEWholeModelDynamic model, LXModel baseModel) {
    // if nothing is saved, we don't need to restore it.
    if (this.savedModelCoord.size() == 0) {
      return;
    }

    // restore the model's original boundaries and range
    baseModel.xMax = oldLengthMax;
    baseModel.xMin = oldLengthMin;
    baseModel.xRange = baseModel.xMax - baseModel.xMin;

    // restore the original x coordinates for the end points
    for (SavedPoint sp : this.savedModelCoord) {
      sp.restore();
    }
  }
}
