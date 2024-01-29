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
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.glengine.GLEngine;
import java.awt.Graphics;

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
    for (LXPoint point : points) {
      if (this.modelTE.isGapPoint(point)) {
        continue;
      }

      float zn = (1f - point.zn);
      float yn = point.yn;

      // use normalized point coordinates to calculate x/y coordinates and then the
      // proper index in the image buffer.  the 'z' dimension of TE corresponds
      // with 'x' dimension of the image based on the side that we're painting.
      int xi = Math.round(zn * x_max_);
      int yi = Math.round(yn * y_max_);

      colors[point.index] = color;
      buffer.setRGB(xi, yi, color);
    }

    if (!wrote_image_.containsKey(file_name)) {
      // Write the image to file
      File outputFile =
          new File(
              "./resources/texture_maps/" + file_name + ".png");
      try {
        ImageIO.write(buffer, "png", outputFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
      wrote_image_.put(file_name, true);
    }
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {
    int color = calcColor();

    // Initialize the image buffer with the specified size
    BufferedImage buffer = initialize_image();

    List<String> front_panels =
        Arrays.asList(
            "SAA", "SAB", "SAD", "SAC", "SBC", "SBB", "SBA", "SBE", "SBD", "SCC", "SCB", "SCA");

    List<LXPoint> front_panel_points_list = new ArrayList<>();
    for (String panel_name : front_panels) {
      List<LXPoint> panel_points =
          Arrays.asList(TEApp.wholeModel.panelsById.get(panel_name).points);
      front_panel_points_list.addAll(panel_points);
    }

    LXPoint[] front_panel_points = new LXPoint[front_panel_points_list.size()];
    front_panel_points = front_panel_points_list.toArray(front_panel_points);
    process_points(front_panel_points, color, buffer, "front_panels");

    LXPoint[] all_panel_points = new LXPoint[TEApp.wholeModel.panelPoints.size()];
    all_panel_points = TEApp.wholeModel.panelPoints.toArray(all_panel_points);
    process_points(all_panel_points, color, buffer, "all_panels");
  }
}
