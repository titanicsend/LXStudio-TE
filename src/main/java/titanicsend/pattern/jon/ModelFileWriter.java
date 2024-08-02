package titanicsend.pattern.jon;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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

    if (doneWriting) {
      return;
    }

    try {
      ensureModelDirectoryExists();

      // Write all points
      // This is the main model and is required to be written out.
      LXPoint[] all_points_array = lx.getModel().points;
      writeModel(all_points_array, "all");

      writeOptionalModels();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Regardless of the success or failure of the model writing, consider this pattern's work done.
    doneWriting = true;
  }

  private void ensureModelDirectoryExists() throws IOException {
    Path modelDirectory = Path.of("resources/model"); // Construct the path

    // Check if the directory exists, and create it if not
    if (!Files.exists(modelDirectory)) {
      Files.createDirectories(modelDirectory);
      TE.log("Model directory created: " + modelDirectory);
    } else {
      TE.log("Model directory already exists: " + modelDirectory);
    }
  }

  private void writeOptionalModels() throws IOException {
    // Write "panel" points only
    List<LXPoint> panel_points_list = new ArrayList<>();
    for (TEPanelModel panel : TEApp.wholeModel.getPanels()) {
      panel_points_list.addAll(Arrays.asList(panel.points));
    }
    writeModel(panel_points_list, "panels");

    // Write "edge" points only
    List<LXPoint> edge_points_list = new ArrayList<>();
    for (TEEdgeModel edge : TEApp.wholeModel.getEdges()) {
      edge_points_list.addAll(Arrays.asList(edge.points));
    }
    writeModel(edge_points_list, "edges");
  }

  /**
   * Writes 3D model data, PNG mask, and UV coordinates from a list of LXPoint.
   *
   * @param points The List of LXPoint to export.
   * @param modelName The base name for the output files.
   * @throws IOException If an error occurs during file writing.
   */
  public void writeModel(List<LXPoint> points, String modelName) throws IOException {
    // Convert List<LXPoint> to LXPoint[]
    LXPoint[] pointsArray = points.toArray(new LXPoint[0]);

    // Call the original writeModel method
    writeModel(pointsArray, modelName);
  }

  /**
   * This function will write 3 main model files.
   *
   * <p>1. 3d_model: this is the coordinates of all 3d points for the points provide to the function
   * 2. PNG mask: this is a mask that can be used to mask a texture to only points that will be used
   * by TE 3. UV Coordinates: correlating the 3d_model points to the pixel coordinates in the PNG.
   * This can be used to perform lossless mapping from outside of LX.
   *
   * @param points LXPoint[] for the model that needs to be exported.
   * @param modelName name used to save the model. the name will be suffixed by "_3d.csv",
   *     "_mask.png", and "_uv.csv" for each output file.
   */
  public void writeModel(LXPoint[] points, String modelName) throws IOException {
    Path path3dModel = Path.of(String.format("resources/model/%s_3d.csv", modelName));
    Path pathMask = Path.of(String.format("resources/model/%s_mask.png", modelName));
    Path pathUv = Path.of(String.format("resources/model/%s_uv.csv", modelName));

    Files.deleteIfExists(path3dModel);
    Files.deleteIfExists(pathMask);
    Files.deleteIfExists(pathUv);

    // Write 3D model data as CSV
    try (BufferedWriter writer = Files.newBufferedWriter(path3dModel, CREATE, APPEND)) {
      writer.write("tx,ty,tz\n"); // Header row
      for (LXPoint point : points) {
        writer.write(String.format("%f,%f,%f\n", point.x, point.y, point.z));
      }
    }

    double[] min = {Double.MAX_VALUE, Double.MAX_VALUE}; // Min x, y
    double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE}; // Max x, y

    // Find min and max values for x and y
    for (LXPoint point : points) {
      min[0] = Math.min(min[0], point.x);
      max[0] = Math.max(max[0], point.x);
      min[1] = Math.min(min[1], point.y);
      max[1] = Math.max(max[1], point.y);
    }

    BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();
    g2d.setColor(Color.BLACK);
    g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
    g2d.dispose();

    List<String> uvPoints = new ArrayList<>();
    TE.log("Received " + points.length + " points.");

    // Generate mask and UV coordinates
    for (LXPoint point : points) {
      double x = point.x;
      double y = point.y;

      // Restored original pixel calculations
      int px = (int) ((x - min[0]) / Math.max(max[0] - min[0], 1e-6) * (IMAGE_WIDTH - 1));
      int py = (int) ((y - min[1]) / Math.max(max[1] - min[1], 1e-6) * (IMAGE_HEIGHT - 1));

      if (px >= 0 && px < IMAGE_WIDTH && py >= 0 && py < IMAGE_HEIGHT) {
        image.setRGB(px, py, Color.WHITE.getRGB()); // White for mask
        uvPoints.add(px + "," + py);
      } else {
        TE.err("Point outside bounds: (" + px + ", " + py + ")");
      }
    }

    ImageIO.write(image, "png", pathMask.toFile());
    Files.write(pathUv, uvPoints, CREATE, TRUNCATE_EXISTING);
  }
}
