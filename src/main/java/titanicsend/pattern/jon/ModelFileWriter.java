package titanicsend.pattern.jon;

import static java.nio.file.StandardOpenOption.*;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.studio.TEApp;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;

/** Write model points, and other data to one or more CSV files. */
@LXCategory("Utility")
public class ModelFileWriter extends TEPerformancePattern {
  boolean doneWriting = false;
  Path path = Path.of("resources/TEPoints.csv");

  public ModelFileWriter(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
    addCommonControls();

    // write CSV header
    try {
      Files.writeString(path, "tx,ty,tz\n", CREATE, APPEND);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> splitEdgeString(String longEdgeString) {
    return Arrays.asList(longEdgeString.split("-"));
  }

  public static String sortAndJoin(String edge_name) {
    // Check for valid input
    if (edge_name == null || !edge_name.contains("-")) {
      throw new IllegalArgumentException("Input string must contain exactly one '-'.");
    }

    // Split the string into two numbers
    String[] parts = edge_name.split("-");

    // Convert the parts to integers
    int num1 = Integer.parseInt(parts[0]);
    int num2 = Integer.parseInt(parts[1]);

    // Sort the numbers in ascending order
    int[] sortedNums = {num1, num2};
    Arrays.sort(sortedNums);

    // Join the sorted numbers with a "-"
    return sortedNums[0] + "-" + sortedNums[1];
  }

  LXPoint[] get_start_end_points_for_edges(String longEdgeString) {
    List<String> edges = splitEdgeString(longEdgeString); // Split the input

    List<LXPoint> points_list = new ArrayList<>();
    for (int i = 0; i < edges.size() - 1; i++) { // Process pairs of edges
      String edge_name = edges.get(i) + "-" + edges.get(i + 1); // Construct standard edge names

      TE.log(edge_name);

      String sorted_edge_name = sortAndJoin(edge_name);
      TEEdgeModel edge_model = TEApp.wholeModel.edgesById.get(sorted_edge_name);
      List<LXPoint> edge_points = Arrays.asList(edge_model.points);
      int last_index = edge_points.size() - 1;

      // We assume the order of the input edge name was correct, if not,
      // we swap the start and end of the edge using these indexes, so we
      // can keep the ordering of the edges in the final output.
      int first_index = 0;
      int second_index = last_index;
      if (!sorted_edge_name.equals(edge_name)) {
        first_index = last_index;
        second_index = 0;
      }

      // Add the lower point to the list
      points_list.add(edge_points.get(first_index));

      // Add the other point unconditionally for the last edge
      if (i == edges.size() - 2) {
        points_list.add(edge_points.get(second_index));
      }
    }

    LXPoint[] points = new LXPoint[points_list.size()];
    points = points_list.toArray(points);
    return points;
  }

  private List<String> getEdgeProgressions() {
    List<String> progressions = new ArrayList<>();

    progressions.add("124-113-109-111-119-118-30");
    progressions.add("117-113-28-111-102-119-31-118-30");
    progressions.add("124-117-115-113-28-26-111-102-119-121-31-30");
    progressions.add("117-10-115-28-26-102-121-39-31-33-30");
    progressions.add("10-100-115-26-99-102-101-121-39-33-30");
    progressions.add("10-11-100-26-99-101-39-33-30");
    progressions.add("11-98-100-99-101-39-33-30");
    progressions.add("11-98-99-101-39-33-30");
    progressions.add("101-44-39-37-33-30");
    progressions.add("44-37-123");
    progressions.add("44-50-37-123");
    progressions.add("44-47-50-123");
    progressions.add("47-45-50-123");
    progressions.add("47-45-123");
    progressions.add("47-45-122");
    progressions.add("47-122");
    progressions.add("47-54-122");
    progressions.add("47-51-54-82-122");
    progressions.add("51-82");
    progressions.add("65-67-90-47");
    progressions.add("65-93-67-90-51");
    progressions.add("65-60-93-90-69-51");
    progressions.add("60-127-93-69-82");
    progressions.add("60-125-127-69-70-82");
    progressions.add("125-89-127-70-82");
    progressions.add("125-126-89-70-82");

    return progressions;
  }

  private List<String> getFullGraphEdges() {
    List<String> progressions = new ArrayList<>();
    progressions.add(
        "124-113-109-28-113-124-117-113-115-117-10-115-28-111-109-28-26-115-"
            + "100-10-11-100-26-99-100-98-11-98-99-102-26-111-102-101-99-101-121-102-119-"
            + "111-119-121-39-101-44-39-31-121-119-31-118-119-118-30-31-30-33-31-33-39-37-33-"
            + "37-44-50-37-123-50-47-44-50-123-45-50-45-47-51-82-122-54-47-122-45-47-54-82-"
            + "51-54-51-47-90-51-69-82-70-69-90-67-65-60-125-126-89-125-127-60-93-65-67-93-"
            + "127-89-70-127-69-93-90");
    return progressions;
  }

  public void writeEdgePoints(int numInterpolations) throws IOException {
    if (doneWriting) {
      return;
    }

    List<String> list_of_list_of_edges = getFullGraphEdges(); // Assuming this gets your edges
    String file_name_prefix = "full_graph_";

    for (int i = 0; i < list_of_list_of_edges.size(); i++) {
      LXPoint[] start_end_edge_points =
          get_start_end_points_for_edges(list_of_list_of_edges.get(i));

      Path path = Path.of("resources/edge_points/" + file_name_prefix + i + ".csv");
      Files.writeString(path, "tx,ty,tz\n", CREATE, APPEND);

      // Iterate through consecutive pairs of points
      for (int j = 0; j < start_end_edge_points.length - 1; j++) {
        LXPoint point1 = start_end_edge_points[j];
        LXPoint point2 = start_end_edge_points[j + 1];

        // Original point
        Files.writeString(path, point1.x + "," + point1.y + "," + point1.z + "\n", CREATE, APPEND);

        // Interpolate between points
        for (int k = 1; k <= numInterpolations; k++) {
          double t = k / (double) (numInterpolations + 1); // Interpolation fraction

          double interpolatedX = point1.x + (point2.x - point1.x) * t;
          double interpolatedY = point1.y + (point2.y - point1.y) * t;
          double interpolatedZ = point1.z + (point2.z - point1.z) * t;

          Files.writeString(
              path,
              interpolatedX + "," + interpolatedY + "," + interpolatedZ + "\n",
              CREATE,
              APPEND);
        }
      }
    }
  }

  private void writeAllPoints() throws IOException {
    if (doneWriting) {
      return;
    }

    for (LXPoint point : this.model.points) {
      Files.writeString(path, point.x + "," + point.y + "," + point.z + "\n", CREATE, APPEND);
    }
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    try {
      writeEdgePoints(10);
      //    writeAllPoints();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      doneWriting = true;
    }
  }
}
