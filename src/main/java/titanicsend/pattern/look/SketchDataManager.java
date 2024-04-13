package titanicsend.pattern.look;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import titanicsend.util.TE;

public class SketchDataManager {

  public static final class SketchData {
    public final int orig_idx;
    public final int num_points;
    public final float total_dist;
    public final float[][] points;

    // Gson will override fields using reflection, if defined in json
    public SketchData() {
      orig_idx = -1;
      num_points = -1;
      total_dist = -1;
      points = new float[][]{};
    }
  }

  public final List<SketchData> sketches = new ArrayList<>();

  private static SketchDataManager instance;

  public static synchronized SketchDataManager get() {
    if (instance == null) {
      instance = new SketchDataManager();
    }
    return instance;
  }

  private SketchDataManager() {
    try {
      Gson gson = new Gson();
      JsonReader reader = new JsonReader(loadFile("resources/shaders/data/te_lines_200.json"));
      SketchData[] sketchData = gson.fromJson(reader, SketchData[].class);

      for (SketchData d : sketchData) {
        sketches.add(d);
      }

    } catch (Exception e) {
      TE.err(e, "Error reading sketch data");
    }
  }

  protected static BufferedReader loadFile(String filename) {
    try {
      File f = new File(filename);
      return new BufferedReader(new FileReader(f));
    } catch (FileNotFoundException e) {
      throw new Error(filename + " not found below " + System.getProperty("user.dir"));
    }
  }
}
