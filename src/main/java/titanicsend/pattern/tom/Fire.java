package titanicsend.pattern.tom;

import static processing.core.PApplet.lerpColor;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.Click;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.color.TEColorType;
import titanicsend.pattern.TEPattern;

public class Fire extends TEPattern {
  private float ROW_HEIGHT = 50000;
  private float COLUMN_WIDTH = 50000;
  private int NUM_COLUMNS;
  private int NUM_ROWS;
  private int[][] buffer;
  private int[] gradient;

  protected final CompoundParameter fuel = new CompoundParameter("Fuel", 1);

  protected final CompoundParameter colorPosition = new CompoundParameter("Color Position", 0.5);

  public final LinkedColorParameter fireColor =
      registerColor("Color", "fireColor", TEColorType.PRIMARY, "Color of the fire");
  protected final Click rate = new Click(100);

  public Fire(LX lx) {
    super(lx);
    NUM_ROWS = calculateRows();
    NUM_COLUMNS = calculateColumns();
    // gradient = calculateGradient(this.fireColor.calcColor(), 36);
    buffer = new int[NUM_ROWS][NUM_COLUMNS];
    startModulator(this.rate);
    addParameter("fuel", fuel);
    addParameter("colorPosition", colorPosition);
  }

  public void spreadFire() {
    checkFuel();

    int[][] newBuffer = buffer.clone();
    for (int row = 1; row < NUM_ROWS; row++) {
      for (int column = 0; column < NUM_COLUMNS; column++) {
        int rand = (int) Math.round(Math.random() * 3.0) & 3;
        newBuffer[row][Math.min(NUM_COLUMNS - 1, Math.max(0, column - rand + 2))] =
            Math.max(0, buffer[row - 1][column] - (1 & rand));
      }
    }

    for (LXPoint point : model.points) {
      if (!this.modelTE.isGapPoint(point)) colors[point.index] = color(point);
    }
  }

  @Override
  public void run(double deltaMsec) {
    if (this.rate.click()) {
      spreadFire();
    }
  }

  private void checkFuel() {
    int value;
    if (fuel.getValue() > 0.5) {
      value = gradient.length - 1;
    } else {
      value = 0;
    }

    for (int i = 0; i < NUM_COLUMNS; i++) {
      buffer[0][i] = value;
    }
  }

  private int color(LXPoint point) {
    int row = (int) ((point.y - modelTE.boundaryPoints.minYBoundaryPoint.y) / ROW_HEIGHT);
    int column = (int) ((point.z - modelTE.boundaryPoints.minZBoundaryPoint.z) / COLUMN_WIDTH);
    return gradient[buffer[row][column]];
  }

  private int calculateRows() {
    return (int)
            ((modelTE.boundaryPoints.maxYBoundaryPoint.y
                    - modelTE.boundaryPoints.minYBoundaryPoint.y)
                / ROW_HEIGHT)
        + 1;
  }

  private int calculateColumns() {
    return (int)
            ((modelTE.boundaryPoints.maxZBoundaryPoint.z
                    - modelTE.boundaryPoints.minZBoundaryPoint.z)
                / COLUMN_WIDTH)
        + 1;
  }

  private int[] calculateGradient(int middle, int steps) {
    int[] gradient = new int[steps];
    double pos = this.colorPosition.getValue();
    for (int i = 0; i < steps * pos; i++) {
      gradient[i] = lerpColor(LXColor.BLACK, middle, (float) (i / (steps * pos)), 3);
    }

    for (int i = (int) (steps * pos); i < steps; i++) {
      gradient[i] = lerpColor(middle, LXColor.WHITE, (float) ((i - steps * pos) / steps), 3);
    }

    return gradient;
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    super.onParameterChanged(parameter);
    if (parameter.getPath().equals("fireColor")) {
      this.gradient = calculateGradient(((LinkedColorParameter) parameter).calcColor(), 36);
    } else if (parameter.getPath().equals("colorPosition")) {
      this.gradient = calculateGradient(fireColor.calcColor(), 36);
    }
  }
}
