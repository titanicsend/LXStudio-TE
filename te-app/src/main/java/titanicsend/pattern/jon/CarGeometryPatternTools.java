package titanicsend.pattern.jon;

import heronarts.lx.model.LXPoint;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.util.TE;

/** Tools for converting car model geometry for use in patterns, particularly in native shaders. */
public class CarGeometryPatternTools {

  // convert from normalized physical model coords
  // to normalized 2D GL surface coords
  protected static float modelToMapX(LXPoint pt) {
    return 2f * (-0.5f + pt.xn);
  }

  // convert from normalized physical model coords
  // to aspect corrected normalized 2D GL surface coords
  protected static float modelToMapY(LXPoint pt) {
    return 2f * (-0.5f + pt.yn);
  }

  // TODO - Static model variant.  Remove when we move to dynamic model
  // convert from normalized physical model coords
  // to normalized 2D GL surface coords
  protected static float modelToMapXStatic(LXPoint pt) {
    return 2f * (-0.5f + pt.zn);
  }

  /**
   * Get a specified number of model edges that are connected to at least one panel. The string
   * parameter regex is used to filter the edges by name.
   *
   * @param model the TE model object
   * @param regex a regular expression to be compared with edge names
   * @param lines an n x 4 array of line segments, in the form x1,y1,x2,y2
   * @param lineCount the maximum number of lines(edges) to retrieve.
   * @return the number of edges actually retrieved
   */
  public static int getPanelConnectedEdges(
      TEWholeModel model, String regex, float lines[][], int lineCount) {
    Pattern edgePattern = Pattern.compile(regex);

    List<TEEdgeModel> edges = model.getEdges();
    int edgeCount = 0;

    for (TEEdgeModel edge : edges) {
      if (edge.connectedPanels.size() >= 1) {
        for (TEPanelModel panel : edge.connectedPanels) {
          Matcher matcher = edgePattern.matcher(panel.getId());
          if (matcher.matches()) {
            // TE.log("Found edge w/panel(s): %s",edge.getId());
            getLineFromEdge(model, lines, edgeCount, edge.getId());
            edgeCount++;
            break;
          }
          if (edgeCount >= lineCount) break;
        }
      }
    }
    return edgeCount;
  }

  /**
   * Get all edges on a specified side of the car model. The signum parameter determines which side
   * to retrieve edges from: 1 for starboard (right) side, -1 for port (left) side.
   *
   * @param model the TE model object
   * @param signum 1 for starboard side, -1 for port side
   * @param lines an n x 4 array of line segments, in the form x1,y1,x2,y2
   * @param lineCount the maximum number of lines(edges) to retrieve.
   * @return the number of edges actually retrieved
   */
  public static int getAllEdgesOnSide(
      TEWholeModel model, float signum, float[][] lines, int lineCount) {
    // signum is 1 for starboard side, -1 for port side (I think!)

    int edgeCount = 0;
    List<TEEdgeModel> edges = model.getEdges();
    for (TEEdgeModel edge : edges) {
      if (edge != null) {
        // if it's on the side we want, or directly on the centerline,
        // add it to the list of lines.
        if (signum * edge.centroid.z >= 0.0) {
          getLineFromEdge(model, lines, edgeCount, edge.getId());
          edgeCount++;
          if (edgeCount >= lineCount) break;
        }
      }
    }
    return edgeCount;
  }

  // given an edge id, adds a model edge's vertices to our list of line segments
  protected static void getLineFromEdge(TEWholeModel model, float lines[][], int index, String id) {

    TEEdgeModel edge = model.getEdge(id);

    if (edge != null) {
      LXPoint v1 = edge.points[0];
      LXPoint v2 = edge.points[edge.points.length - 1];

      // set x1,y1,x2,y2 in line array
      lines[index][0] = modelToMapX(v1);
      lines[index][1] = modelToMapY(v1);
      lines[index][2] = modelToMapX(v2);
      lines[index][3] = modelToMapY(v2);
    } else {
      TE.log("Null edge %s", id);
    }
  }
}
