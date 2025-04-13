package titanicsend.pattern.jon;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.studio.TEApp;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;
import titanicsend.util.TECategory;

/** Write model points, and other data to one or more CSV files. */
@LXCategory(TECategory.UTILITY)
public class ModelFileWriter extends TEPerformancePattern {
  boolean doneWriting = false;
  public static final int IMAGE_WIDTH = 640;
  public static final int IMAGE_HEIGHT = 480;
  public static final double EPSILON = 1e-6;
  private static final int MARGIN = 0;
  private LXPoint[] allPoints;

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
      writeModel(all_points_array, "all", all_points_array);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
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
    writeModel(panel_points_list, "panels", true);

    // Write "edge" points only
    List<LXPoint> edge_points_list = new ArrayList<>();
    for (TEEdgeModel edge : TEApp.wholeModel.getEdges()) {
      edge_points_list.addAll(Arrays.asList(edge.points));
    }
    writeModel(edge_points_list, "edges", true);

    // Write each panel separately.
    for (TEPanelModel panel : TEApp.wholeModel.getPanels()) {
      String model_name = panel.getId();
      List<LXPoint> panel_points = panel.model.getPoints();
      writeModel(panel_points, model_name, true, "separate_panels");
    }

    // Write all points except for side panels AA, AB, FA, FB.
    // These panels are mapped with very few points from the shader textures, and
    // they don't look normal/good for some patterns used in the boot up sequence.
    List<String> ignoredPanels =
        Arrays.asList(
            "AA", "AB", "FA", "FB", "FSB", "FPB", "FSA", "FPA", "FSC", "FPC", "APA", "ASA", "APB",
            "APC", "ASC", "ASB");
    List<String> ignoredEdges =
        Arrays.asList(
            "113-124", "109-113", "109-112", "112-124", "81-89", "81-91", "91-126", "89-126",
            "89-91", "70-81", "73-81", "81-82", "81-92", "27-109", "28-109", "109-110", "109-111",
            "28-113", "28-111", "27-112", "27-110", "112-116", "113-117", "116-124", "117-124",
            "89-125", "70-89", "70-82", "73-92", "73-91", "91-129", "125-126", "126-129");

    List<LXPoint> all_no_side_panels_points = new ArrayList<>();

    for (TEPanelModel panel : TEApp.wholeModel.getPanels()) {
      if (ignoredPanels.contains(panel.getId())) {
        continue;
      }
      all_no_side_panels_points.addAll(Arrays.asList(panel.points));
    }

    for (TEEdgeModel edge : TEApp.wholeModel.getEdges()) {
      TE.log(edge.getId());
      if (ignoredEdges.contains(edge.getId())) {
        continue;
      }
      all_no_side_panels_points.addAll(Arrays.asList(edge.points));
    }
    writeModel(all_no_side_panels_points, "no_sides_all", true);
  }

  /**
   * Writes the 3D model data (coordinates), PNG mask, and UV coordinates to files.
   *
   * <p>This method handles the core logic of generating the model representation for use with TE
   * (and potentially other tools). It produces three output files:
   *
   * <ol>
   *   <li>**{modelName}_3d.csv:** A CSV file containing the 3D coordinates (x, y, z) of each point
   *       in the provided `points` array.
   *   <li>**{modelName}_mask.png:** A PNG image representing a mask. White pixels in the mask
   *       correspond to the locations of the 3D points projected onto a 2D plane.
   *   <li>**{modelName}_uv.csv:** A CSV file containing UV coordinates. Each line in this file maps
   *       a 3D point from `points` to its corresponding pixel location in the generated mask image.
   * </ol>
   *
   * <p>The UV coordinates enable a direct, lossless mapping between the 3D model and a 2D texture,
   * which can be useful for applying textures or effects outside of the LX environment.
   *
   * @param points The array of {@link LXPoint} objects representing the 3D model.
   * @param modelName The base name for the output files.
   * @param useAllPoints If true, the PNG mask will be generated using the bounding box of the
   *     `allPoints` array (set using `writeModel` with the `allPoints` argument). If false, the
   *     mask will be generated using the bounding box of the `points` array provided to this
   *     method.
   * @param dirName Optional directory name to write the files into. If null, the files are written
   *     to the default "resources/model/" directory.
   * @throws IOException If an error occurs during file writing.
   */
  private void writeModel(LXPoint[] points, String modelName, boolean useAllPoints, String dirName)
      throws IOException {
    // Construct the file paths, incorporating the optional directory
    Path basePath = Path.of("resources/model/");
    if (dirName != null) {
      basePath = basePath.resolve(dirName); // Append dirName to the base path
      Files.createDirectories(basePath); // Create the directory if it doesn't exist
    }

    Path path3dModel = basePath.resolve(String.format("%s_3d.csv", modelName));
    Path pathMask = basePath.resolve(String.format("%s_mask.png", modelName));
    Path pathUv = basePath.resolve(String.format("%s_uv.csv", modelName));

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

    // Calculate min and max using either provided points or allPoints
    for (LXPoint point : (useAllPoints && allPoints != null) ? allPoints : points) {
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
      int px = (int) ((x - min[0]) / Math.max(max[0] - min[0], EPSILON) * (IMAGE_WIDTH - 1));
      int py = (int) ((y - min[1]) / Math.max(max[1] - min[1], EPSILON) * (IMAGE_HEIGHT - 1));

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

  // Overloads for array version
  private void writeModel(LXPoint[] points, String modelName, boolean useAllPoints)
      throws IOException {
    writeModel(points, modelName, useAllPoints, null);
  }

  public void writeModel(LXPoint[] points, String modelName, LXPoint[] allPoints, String dirName)
      throws IOException {
    this.allPoints = allPoints;
    writeModel(points, modelName, true, dirName);
  }

  public void writeModel(LXPoint[] points, String modelName, LXPoint[] allPoints)
      throws IOException {
    writeModel(points, modelName, allPoints, null);
  }

  // Overloads for List version
  public void writeModel(
      List<LXPoint> points, String modelName, boolean useAllPoints, String dirName)
      throws IOException {
    LXPoint[] pointsArray = points.toArray(new LXPoint[0]);
    writeModel(pointsArray, modelName, useAllPoints, dirName);
  }

  public void writeModel(List<LXPoint> points, String modelName, boolean useAllPoints)
      throws IOException {
    writeModel(points, modelName, useAllPoints, null);
  }

  public void writeModel(List<LXPoint> points, String modelName) throws IOException {
    writeModel(points, modelName, false, null);
  }
}
