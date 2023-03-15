package titanicsend.model;

import java.util.*;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import titanicsend.util.PanelStriper;

public class TEPanelFactory {
  public static TEPanelModel build(
          String id, TEVertex v0, TEVertex v1, TEVertex v2, TEEdgeModel e0,
          TEEdgeModel e1, TEEdgeModel e2, String panelType,
          TEStripingInstructions stripingInstructions,
          LXPoint gapPoint, Properties views) {
    ArrayList<LXPoint> points = new ArrayList<LXPoint>();

    float centroidX = (v0.x + v1.x + v2.x) / 3.0F;
    float centroidY = (v0.y + v1.y + v2.y) / 3.0F;
    float centroidZ = (v0.z + v1.z + v2.z) / 3.0F;

    LXVector centroid = new LXVector(centroidX, centroidY, centroidZ);

    String flavor;

    if (panelType.equals(TEPanelModel.LIT)) {
      List<LXPoint> stripedPoints = new ArrayList<>();
      if (stripingInstructions == null) {
        LX.log("Panel " + id + " has no striping instructions; won't render.");
      }
      try {
        flavor = PanelStriper.stripe(id, v0, v1, v2, stripedPoints,
                                     stripingInstructions, gapPoint);
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

    int[] channelLengths;
    if (stripingInstructions == null) channelLengths = null;
    else channelLengths = stripingInstructions.channelLengths;

    // Adding tags based on views defined in resources/vehicle/views.properties
    String[] tags = new String[] { id };
    for (String view : views.stringPropertyNames()) {
      List<String> ids = Arrays.asList(views.getProperty(view).split(","));
      if (ids.contains(id)) {
        String[] newTags = new String[tags.length + 1]; // Resize the tags array to fit all IDs
        System.arraycopy(tags, 0, newTags, 0, tags.length); // Copy the old tags into the new array
        newTags[tags.length] = view;
        tags = newTags;
      }
    }
    
    return new TEPanelModel(id, points, v0, v1, v2, e0, e1, e2,
            panelType, flavor, centroid, channelLengths, tags);
  }
}