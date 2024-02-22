package titanicsend.pattern.sina;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.studio.TEApp;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.glengine.GLEngine;

@LXCategory("AAA")
public class TETextureWriter extends TEPerformancePattern {

  int x_max_ = GLEngine.getWidth() - 1;
  int y_max_ = GLEngine.getHeight() - 1;

  private final int width_;
  private final int height_;

  Map<String, Boolean> wrote_image_ = new HashMap<>();

  public TETextureWriter(LX lx) {
    super(lx);

    addCommonControls();

    width_ = 680; // GLEngine.getWidth();
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

  private void process_points(
      LXPoint[] points, int color, BufferedImage buffer, String file_name, String type) {
    if (type.equals("3d")) {
      for (LXPoint point : points) {
        float zn = 0.5f * ((point.x >= 0) ? 1f + point.zn : 1f - point.zn);
        float yn = point.yn;

        // use normalized point coordinates to calculate x/y coordinates and then the
        // proper index in the image buffer.  the 'z' dimension of TE corresponds
        // with 'x' dimension of the image based on the side that we're painting.
        int xi = (int) (0.5f + zn * x_max_);
        int yi = (int) (0.5f + yn * y_max_);

        //        int index = 4 * ((yi * GLEngine.getWidth()) + xi);
        try {
          // Check if the current color at xi, yi is black (assuming ARGB format where high byte is
          // alpha and black is 0x00000000)
          int currentColor = buffer.getRGB(xi, yi);
          if ((currentColor & 0x00FFFFFF) == 0) { // Masking to ignore the alpha channel if present
            buffer.setRGB(xi, yi, color);
          }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
      }

    } else if (type.equals("2d")) {
      for (LXPoint point : points) {
        if (this.modelTE.isGapPoint(point)) {
          continue;
        }

        double xn = point.xn;
        double zn = (1f - point.zn);
        double yn = point.yn;

        // use normalized point coordinates to calculate x/y coordinates and then the
        // proper index in the image buffer.  the 'z' dimension of TE corresponds
        // with 'x' dimension of the image based on the side that we're painting.
        int xi = (int) Math.round(zn * x_max_);
        int yi = (int) Math.round(yn * y_max_);

        try {
          // Check if the current color at xi, yi is black (assuming ARGB format where high byte is
          // alpha and black is 0x00000000)
          int currentColor = buffer.getRGB(xi, yi);
          if ((currentColor & 0x00FFFFFF) == 0) { // Masking to ignore the alpha channel if present
            buffer.setRGB(xi, yi, color);
          }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
      }
    }

    if (!wrote_image_.containsKey(file_name)) {
      // Write the image to file
      File outputFile = new File("./resources/texture_maps/" + type + "/" + file_name + ".png");

      try {
        ImageIO.write(buffer, "png", outputFile);
      } catch (IOException ignored) {
      }
      wrote_image_.put(file_name, true);
    }
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
    int color_edges = LXColor.rgb(0, 255, 255);
    final String type = "3d"; // "3d" or "2d"

    // Initialize the image buffer with the specified size

    List<String> front_panels =
        Arrays.asList(
            "SAA", "SAB", "SAD", "SAC", "SBC", "SBB", "SBA", "SBE", "SBD", "SCC", "SCB", "SCA");

    List<String> left_panels = Arrays.asList("ASA", "ASB");

    List<String> left_side_panels = Arrays.asList("FB", "FA");

    BufferedImage front_panel_buffer = initialize_image();
    LXPoint[] front_panel_points = get_points_for_panels(front_panels);
    process_points(front_panel_points, color, front_panel_buffer, "front_panels", type);

    BufferedImage left_side_panel_buffer = initialize_image();
    LXPoint[] left_side_panel_points = get_points_for_panels(left_side_panels);
    process_points(left_side_panel_points, color, left_side_panel_buffer, "left_side_panels", type);

    BufferedImage left_panel_buffer = initialize_image();
    LXPoint[] points = get_points_for_panels(left_panels);
    process_points(points, color, left_panel_buffer, "left_panels", type);

    BufferedImage all_panel_buffer = initialize_image();
    points = new LXPoint[TEApp.wholeModel.panelPoints.size()];
    points = TEApp.wholeModel.panelPoints.toArray(points);
    process_points(points, color, all_panel_buffer, "all_panels", type);

    // Overlay edges on the panels
    points = new LXPoint[TEApp.wholeModel.edgePoints.size()];
    points = TEApp.wholeModel.edgePoints.toArray(points);
    process_points(points, color_edges, all_panel_buffer, "overlayed_edges", type);

    BufferedImage all_edge_buffer = initialize_image();
    points = new LXPoint[TEApp.wholeModel.edgePoints.size()];
    points = TEApp.wholeModel.edgePoints.toArray(points);
    process_points(points, color, all_edge_buffer, "all_edges", type);

    BufferedImage end_point_buffer = initialize_image();
    points = TEApp.wholeModel.points;
    process_points(points, color, end_point_buffer, "all_points", type);
  }
}
