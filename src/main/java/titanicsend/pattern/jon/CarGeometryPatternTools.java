package titanicsend.pattern.jon;

import heronarts.lx.model.LXPoint;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.util.TE;

/** Tools for converting car model geometry for use in patterns, particularly in native shaders. */
public class CarGeometryPatternTools {
  // convert from normalized physical model coords
  // to aspect corrected normalized 2D GL surface coords
  protected static float modelToMapX(LXPoint pt) {
    // correct for aspect ratio of render target
    return 1.33333f * ((-0.5f + pt.zn));
  }

  // convert from normalized physical model coords
  // to aspect corrected normalized 2D GL surface coords
  protected static float modelToMapY(LXPoint pt) {
    return -0.5f + pt.yn;
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

    Set<TEEdgeModel> edges = model.getAllEdges();
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

  // given an edge id, adds a model edge's vertices to our list of line segments
  protected static void getLineFromEdge(TEWholeModel model, float lines[][], int index, String id) {

    HashMap<String, TEEdgeModel> edges = model.edgesById;

    TEEdgeModel edge = edges.get(id);
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
