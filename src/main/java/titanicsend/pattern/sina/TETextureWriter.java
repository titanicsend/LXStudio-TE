package titanicsend.pattern.sina;

import static org.lwjgl.system.linux.X11.False;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.studio.TEApp;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import processing.core.PVector;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.glengine.GLEngine;
import java.awt.Graphics;
import titanicsend.pattern.jon.ModelBender;

@LXCategory("AAA")
public class TETextureWriter extends TEPerformancePattern {

  private final int x_max_;
  private final int y_max_;

  private final int width_;
  private final int height_;

  Map<String, Boolean> wrote_image_ = new HashMap<>();

  public TETextureWriter(LX lx) {
    super(lx);

    addCommonControls();

    width_ = GLEngine.getWidth();
    height_ = GLEngine.getHeight();
    x_max_ = width_ - 1;
    y_max_ = height_ - 1;
  }

  private BufferedImage initialize_image() {
    // Initialize the image buffer with the specified size
    BufferedImage image = new BufferedImage(width_, height_, BufferedImage.TYPE_INT_ARGB);
    // Fill the entire image with black
    Graphics graphics = image.createGraphics();
    graphics.setColor(Color.BLACK);
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.dispose();

    return image;
  }

  private void process_points(LXPoint[] points, int color, BufferedImage buffer, String file_name) {
//    ModelBender mb = new ModelBender();
//    mb.adjustEndGeometry(TEApp.wholeModel);
    TEApp.wholeModel.normalizePoints();

    for (LXPoint point : points) {
      if (this.modelTE.isGapPoint(point)) {
        continue;
      }

      double zn = (1f - point.zn);
      double yn = point.yn;

      // use normalized point coordinates to calculate x/y coordinates and then the
      // proper index in the image buffer.  the 'z' dimension of TE corresponds
      // with 'x' dimension of the image based on the side that we're painting.
      int xi = (int) Math.round(zn * x_max_);
      int yi = (int) Math.round(yn * y_max_);

//      colors[point.index] = color;
      buffer.setRGB(xi, yi, color);
    }

    if (!wrote_image_.containsKey(file_name)) {
      // Write the image to file
      File outputFile = new File("./resources/texture_maps/" + file_name + ".png");
      try {
        ImageIO.write(buffer, "png", outputFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
      wrote_image_.put(file_name, true);
    }

//    mb.restoreModel(TEApp.wholeModel);
//    TEApp.wholeModel.normalizePoints();
  }

  LXPoint[] get_points_for_panels(List<String> panel_names) {
    List<LXPoint> points_list = new ArrayList<>();
    for (String panel_name : panel_names) {
      List<LXPoint> panel_points =
          Arrays.asList(TEApp.wholeModel.panelsById.get(panel_name).points);
      points_list.addAll(panel_points);
    }

    LXPoint[] points = new LXPoint[points_list.size()];
    points = points_list.toArray(points);

    return points;
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {
    int color = calcColor();

    // Initialize the image buffer with the specified size


    List<String> front_panels =
        Arrays.asList(
            "SAA", "SAB", "SAD", "SAC", "SBC", "SBB", "SBA", "SBE", "SBD", "SCC", "SCB", "SCA");

    List<String> left_panels = Arrays.asList("ASA", "ASB");

    List<String> left_side_panels = Arrays.asList("FB", "FA");

    BufferedImage front_panel_buffer = initialize_image();
    LXPoint[] front_panel_points = get_points_for_panels(front_panels);
    process_points(front_panel_points, color, front_panel_buffer, "front_panels");

    BufferedImage left_side_panel_buffer = initialize_image();
    LXPoint[] left_side_panel_points = get_points_for_panels(left_side_panels);
    process_points(left_side_panel_points, color, left_side_panel_buffer, "left_side_panels");

    BufferedImage left_panel_buffer = initialize_image();
    LXPoint[] points = get_points_for_panels(left_panels);
    process_points(points, color, left_panel_buffer, "left_panels");

    BufferedImage all_panel_buffer = initialize_image();
    points = new LXPoint[TEApp.wholeModel.panelPoints.size()];
    points = TEApp.wholeModel.panelPoints.toArray(points);
    process_points(points, color, all_panel_buffer, "all_panels");

    BufferedImage all_edge_buffer = initialize_image();
    points = new LXPoint[TEApp.wholeModel.edgePoints.size()];
    points = TEApp.wholeModel.edgePoints.toArray(points);
    process_points(points, color, all_edge_buffer, "all_edges");

    ModelBender mb = new ModelBender();
    ArrayList<LXPoint> end_points = mb.getEndPoints(TEApp.wholeModel);
    BufferedImage end_point_buffer = initialize_image();
    points = new LXPoint[end_points.size()];
    points = end_points.toArray(points);
    process_points(points, color, end_point_buffer, "end_edges");
  }
}
