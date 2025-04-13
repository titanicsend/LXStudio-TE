package titanicsend.pattern.tom;

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
  private final float ROW_HEIGHT = 50000;
  private final float COLUMN_WIDTH = 50000;
  private final int NUM_COLUMNS;
  private final int NUM_ROWS;
  private final int[][] buffer;
  private int[] gradient;

  protected final CompoundParameter fuel = new CompoundParameter("Fuel", 1);

  protected final CompoundParameter colorPosition = new CompoundParameter("Color Position", 0.5);

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
    int row = (int) ((point.y - modelTE.minY()) / ROW_HEIGHT);
    int column = (int) ((point.z - modelTE.minZ()) / COLUMN_WIDTH);
    return gradient[buffer[row][column]];
  }

  private int calculateRows() {
    return (int) ((modelTE.maxY() - modelTE.minY()) / ROW_HEIGHT) + 1;
  }

  private int calculateColumns() {
    return (int) ((modelTE.maxZ() - modelTE.minZ()) / COLUMN_WIDTH) + 1;
  }

  private int[] calculateGradient(int middle) {
    int[] gradient = new int[36];
    double pos = this.colorPosition.getValue();
    for (int i = 0; i < 36 * pos; i++) {
      // gradient[i] = lerpColor(LXColor.BLACK, middle, (float) (i / (steps * pos)), 3);
      gradient[i] = LXColor.lerp(LXColor.BLACK, middle, (float) (i / (36 * pos)));
    }

    for (int i = (int) (36 * pos); i < 36; i++) {
      // gradient[i] = lerpColor(middle, LXColor.WHITE, (float) ((i - steps * pos) / steps), 3);
      gradient[i] = LXColor.lerp(middle, LXColor.WHITE, (float) ((i - 36 * pos) / 36));
    }

    return gradient;
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    super.onParameterChanged(parameter);
    if (parameter.getPath().equals("fireColor")) {
      this.gradient = calculateGradient(((LinkedColorParameter) parameter).calcColor());
    } else if (parameter.getPath().equals("colorPosition")) {
      this.gradient = calculateGradient(getSwatchColor(TEColorType.PRIMARY));
    }
  }
}
