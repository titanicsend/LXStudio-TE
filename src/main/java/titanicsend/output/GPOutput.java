package titanicsend.output;

import heronarts.lx.LX;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.output.LXOutput;
import titanicsend.app.GigglePixelBroadcaster;
import titanicsend.app.TEApp;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import java.util.ArrayList;
import java.util.List;

public class GPOutput extends LXOutput {
  private GigglePixelBroadcaster broadcaster;

  public GPOutput(LX lx, GigglePixelBroadcaster broadcaster) {
    super(lx);
    this.broadcaster = broadcaster;
  }
  
  private boolean initialized = false;
  private boolean alwaysUsePalette = false;
  private ArrayList<LXPoint> gpPoints = new ArrayList<LXPoint>();
  
  private void findGigglePixelPoints() {
    /*
      GigglePixel color sync protocol methods
     */

    // Finds a set of points that GP should use to make its palette broadcasts.
    // By default, it will pick a point in the middle of SUA and SDC panels and
    // a point in the middle of one of each of their edges. 

    TEWholeModel model = TEApp.wholeModel;
    
    TEPanelModel sua = model.panelsById.get("SUA");
    TEPanelModel sdc = model.panelsById.get("SDC");

    if (sua != null) {
      int halfway = sua.points.length / 2;
      if (halfway < sua.points.length) gpPoints.add(sua.points[halfway]);

      halfway = sua.e0.points.length / 2;
      if (halfway < sua.e0.points.length) gpPoints.add(sua.e0.points[halfway]);
    }

    if (sdc != null) {
      int halfway = sdc.points.length / 2;
      if (halfway < sdc.points.length) gpPoints.add(sdc.points[halfway]);

      halfway = sdc.e0.points.length / 2;
      if (halfway < sdc.e0.points.length) gpPoints.add(sdc.e0.points[halfway]);
    }
    
    if (gpPoints.size() > 0) {
      this.initialized = true;
    }
  }
  
  // Handy public method for changing the GP points, if you like!
  public void setGigglePixelPoints(ArrayList<LXPoint> points) {
	this.gpPoints = new ArrayList<LXPoint>(points);
	this.initialized = true;
  }
  
  // Handy public method for toggling GP pixel source
  public void usePalette(boolean alwaysUsePalette) {
	this.alwaysUsePalette = alwaysUsePalette;
  }

  @Override
  protected void onSend(int[] colors, byte[][] glut, double brightness) {
    // Look for standard GP points on the model.
    // Take out the surrounding IF if you want to refresh from the model every frame
    if (!this.initialized) {
       findGigglePixelPoints();
    }

    // If you want to come back later and allow patterns to choose their GP points,
    // loop over every channel here.  Then combine those distinct pixels with
    // the colors array to get your GP output colors

    List<Integer> gpColors = new ArrayList<>();
    // Start with the gpPoints, unless that's disabled
    if (!this.alwaysUsePalette) {
      for (LXPoint point : this.gpPoints) {
        gpColors.add(colors[point.index]);
      }
    }
    // And then append the active swatch
    for (LXDynamicColor dc : lx.engine.palette.swatch.colors) {
      gpColors.add(dc.getColor());
    }
    this.broadcaster.setColors(gpColors);
  }
}
