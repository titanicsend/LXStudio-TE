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
  Path all_points_path = Path.of("resources/model_csv/all_points.csv");
  Path laser_points_path = Path.of("resources/model_csv/laser_edges.csv");

  public ModelFileWriter(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
    addCommonControls();
  }

  public void writeLaserPoints(int numInterpolations) throws IOException {
    if (doneWriting) {
      return;
    }

    List<String> edge_progressions = getLaserEdgesProgression(); // Assuming this gets your edges
    LXPoint[] start_end_edge_points =
        interpolateVerticesToPoints(edge_progressions.get(0), numInterpolations);
    Files.writeString(laser_points_path, "tx,ty,tz\n", CREATE, APPEND);

    for (int j = 0; j < start_end_edge_points.length; j++) {
      LXPoint point1 = start_end_edge_points[j];

      // Original point
      Files.writeString(
          laser_points_path, point1.x + "," + point1.y + "," + point1.z + "\n", CREATE, APPEND);
    }

    TE.log("Successfully wrote the laser edges model to CSV.");
  }

  /**
   * This writes all the pixels of the car out to a CSV file.
   *
   * <p>This model can be used for different purposes. Some examples are: - mapping textures to TE
   * pixels from outside Chromatik - used as a 3D model for simulation or projection mapping from
   * outside Chromatik
   */
  private void writeAllPoints() throws IOException {
    if (doneWriting) {
      return;
    }

    // write CSV header
    Files.writeString(all_points_path, "tx,ty,tz\n", CREATE, APPEND);

    for (LXPoint point : this.model.points) {
      Files.writeString(
          all_points_path, point.x + "," + point.y + "," + point.z + "\n", CREATE, APPEND);
    }

    TE.log("Successfully wrote the entire model to CSV.");
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    try {
      writeAllPoints();
      writeLaserPoints(10);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      doneWriting = true;
    }
  }

  public static List<String> getLaserEdgesProgression() {
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

  /**
   * Given an edge progression like V1-V2-V3-V4-V5 it returns a set of LXPoints between each two
   * consecutive vertices like (V1-V2), (V2-V3), ...
   *
   * <p>This function accepts numInterpolations which defines how many points we should add in
   * between each two consecutive vertices. If numInterpolations = 2, for (V1, V2) we generate
   * points like: - V1_Point, Interpolated_Point_1, Interpolated_Point_2, V2_Point
   *
   * @param edge_progression String representing the edge progression like "V1-V2-V3-V4"
   * @param numInterpolations Number point added in between each two consecutive vertices
   * @return Array of LXPoints that will be in this interpolated model
   */
  public static LXPoint[] interpolateVerticesToPoints(
      String edge_progression, int numInterpolations) {
    // Split the input string from V1-V2-V3-V4 to ["V1", "V2", "V3", "V4"]
    List<String> edges = Arrays.asList(edge_progression.split("-"));

    // This will be used as the output list of LXPoints.
    List<LXPoint> output_points = new ArrayList<>();

    // Process pairs of edges in the progression, find the points in between them
    // and add the points to the output points in this loop.
    for (int i = 0; i < edges.size() - 1; i++) {
      String edge_name = edges.get(i) + "-" + edges.get(i + 1); // Construct standard edge names

      String sorted_edge_name = sortAndJoin(edge_name);
      TEEdgeModel edge_model = TEApp.wholeModel.edgesById.get(sorted_edge_name);
      List<LXPoint> edge_points = Arrays.asList(edge_model.points);
      int last_index = edge_points.size() - 1;

      // We assume the order of the input edge name was correct, if not,
      // we swap the start and end of the edge using these indexes, so we
      // can keep the ordering of the edges in the final output.
      int first_index = 0;
      int second_index = last_index;
      int interpolation_inc = last_index / (numInterpolations - 1);

      if (!sorted_edge_name.equals(edge_name)) {
        first_index = last_index;
        second_index = 0;

        // Add the lower point to the list
        output_points.add(edge_points.get(first_index));

        for (int k = first_index; k > second_index; k -= interpolation_inc) {
          output_points.add(edge_points.get(k));
        }

      } else {
        // Add the lower point to the list
        output_points.add(edge_points.get(first_index));

        for (int k = first_index; k < second_index; k += interpolation_inc) {
          output_points.add(edge_points.get(k));
        }
      }

      // Add the other point unconditionally for the last edge
      if (i == edges.size() - 2) {
        output_points.add(edge_points.get(second_index));
      }
    }

    LXPoint[] points = new LXPoint[output_points.size()];
    points = output_points.toArray(points);
    return points;
  }

  /**
   * Utility function that given an edge name like V1-V2 it splits the string to sort them in the
   * order of the edge values, and returns a string like "V2-V1" if V2 < V1.
   *
   * @param edge_name Edge name in a format like "V1-V2"
   * @return sorted edge name like "V2-V1" if V2 < V1.
   */
  private static String sortAndJoin(String edge_name) {
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
}
