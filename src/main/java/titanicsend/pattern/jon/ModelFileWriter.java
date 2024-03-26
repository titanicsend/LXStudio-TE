package titanicsend.pattern.jon;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/** Write model points, and other data to one or more CSV files. */
@LXCategory("Utility")
public class ModelFileWriter extends TEPerformancePattern {
  boolean doneWriting = false;
  Path path = Path.of("resources/TEPoints.txt");

  public ModelFileWriter(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
    addCommonControls();

    // write CSV header
    try {
      Files.writeString(path, "x,y,z\n", CREATE, APPEND);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    for (LXPoint point : getModel().getPoints()) {
      if (!doneWriting) {
        try {
          Files.writeString(path, point.x + "," + point.y + "," + point.z + "\n", CREATE, APPEND);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    doneWriting = true;
  }
}
