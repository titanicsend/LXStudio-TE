package titanicsend.pattern.jon;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;


import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import heronarts.lx.studio.TEApp;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;

import javax.imageio.ImageIO;

/** Write model points, and other data to one or more CSV files. */
@LXCategory("Utility")
public class ModelFileWriter extends TEPerformancePattern {
  boolean doneWriting = false;
  public static final int IMAGE_WIDTH = 640;
  public static final int IMAGE_HEIGHT = 480;
  public static final double EPSILON = 1e-6;
  private static final int MARGIN = 0;


  public ModelFileWriter(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
    addCommonControls();
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    Path all_points = Path.of("resources/model/csv/TEPoints_All.csv");
    Path all_map = Path.of("resources/model/map/TEPoints_All_map.png");
    Path all_map_uv = Path.of("resources/model/uv/TEPoints_All_map_uv.csv");

    Path panel_points = Path.of("resources/model/csv/TEPoints_Panels.csv");
    Path panel_map = Path.of("resources/model/map/TEPoints_Panels_map.png");
    Path panel_map_uv = Path.of("resources/model/uv/TEPoints_Panels_map_uv.csv");

    Path edge_points = Path.of("resources/model/csv/TEPoints_Edges.csv");
    Path edge_map = Path.of("resources/model/map/TEPoints_Edges_map.png");
    Path edge_map_uv = Path.of("resources/model/uv/TEPoints_Edges_map_uv.csv");

    if (doneWriting) {
      return;
    }

    try {
      // Clear the existing data
      Files.deleteIfExists(all_points);
      Files.deleteIfExists(panel_points);
      Files.deleteIfExists(edge_points);

      // write CSV header
      Files.writeString(all_points, "tx,ty,tz\n", CREATE, APPEND);
      Files.writeString(panel_points, "tx,ty,tz\n", CREATE, APPEND);
      Files.writeString(edge_points, "tx,ty,tz\n", CREATE, APPEND);

      // Write all points
      for (LXPoint point : getModel().getPoints()) {
        Files.writeString(
                all_points, point.x + "," + point.y + "," + point.z + "\n", CREATE, APPEND);
      }
      generateMapAndUv(all_points, all_map, all_map_uv, new int[]{2, 1, 0} /*z, y, x*/);

      // Write panel points only
      for (TEPanelModel panel : TEApp.wholeModel.panelsById.values()) {
        for (LXPoint point : panel.points) {
          Files.writeString(
                  panel_points, point.x + "," + point.y + "," + point.z + "\n", CREATE, APPEND);
        }
      }
      generateMapAndUv(panel_points, panel_map, panel_map_uv, new int[]{2, 1, 0} /*z, y, x*/);

      // Write edge points only
      for (TEEdgeModel edge : TEApp.wholeModel.edgesById.values()) {
        for (LXPoint point : edge.points) {
          Files.writeString(
                  edge_points, point.x + "," + point.y + "," + point.z + "\n", CREATE, APPEND);
        }
      }
      generateMapAndUv(edge_points, edge_map, edge_map_uv, new int[]{2, 1, 0} /*z, y, x*/);

      doneWriting = true;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public void generateMapAndUv(Path csvPath, Path outputFilePath, Path uvOutputFilePath, int[] columnOrder) throws IOException {
    if (columnOrder.length != 3) {
      throw new IllegalArgumentException("Column order must contain exactly 3 elements");
    }

    List<String> lines = Files.readAllLines(csvPath);
    double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
    double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};

    // Find min and max values for normalization. Always use the whole model to have the same scale for all png outputs
    for (LXPoint point : getModel().getPoints()) {
      double[] values = {point.x, point.y, point.z};
      for (int i = 0; i < 3; i++) {
        min[i] = Math.min(min[i], values[columnOrder[i]]);
        max[i] = Math.max(max[i], values[columnOrder[i]]);
      }
    }

    // Create image
    BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

    // Set background color (black in this case)
    Graphics2D g2d = image.createGraphics();
    g2d.setColor(Color.BLACK);
    g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
    g2d.dispose();

    List<String> uv_points = new ArrayList<>();

    // Iterate through points and draw
    for (int i = 1; i < lines.size(); i++) { // Skip header line
      String[] parts = lines.get(i).split(",");
      double x = Double.parseDouble(parts[columnOrder[0]]);
      double y = Double.parseDouble(parts[columnOrder[1]]);
      double z = Double.parseDouble(parts[columnOrder[2]]);

      // Normalize and map to pixel coordinates
      int px = (int) ((x - min[0]) / Math.max(max[0] - min[0], EPSILON) * (IMAGE_WIDTH - 2 * Math.max(MARGIN, EPSILON)) + Math.max(MARGIN, EPSILON));
      int py = (int) ((y - min[1]) / Math.max(max[1] - min[1], EPSILON) * (IMAGE_HEIGHT - 2 * Math.max(MARGIN, EPSILON)) + Math.max(MARGIN, EPSILON));

      if (px >= 0 && px < IMAGE_WIDTH && py >= 0 && py < IMAGE_HEIGHT) {
        // Calculate color based on z value (using a simple gradient from blue to red)
        Color color = Color.white;
        image.setRGB(px, py, color.getRGB());

        // Add UV coordinates to the list
        uv_points.add(px + "," + py);
      }
    }

    // Save the image
    ImageIO.write(image, "png", outputFilePath.toFile());

    // Save UV map as CSV
    Files.write(uvOutputFilePath, uv_points, CREATE, TRUNCATE_EXISTING);
  }
}
