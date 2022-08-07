package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.util.TE;

import java.util.ArrayList;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64RingPattern extends TEMidiFighter64Subpattern {
  private static final double PERIOD_MSEC = 100.0;
  private static final int[] flashColors = {
          LXColor.rgb(255,0,0),
          LXColor.rgb(255,170,0),
          LXColor.rgb(255,255,0),
          LXColor.rgb(0,255,0),
          LXColor.rgb(0,170,170),
          LXColor.rgb(0,0,255),
          LXColor.rgb(255,0,255),
          LXColor.rgb(255,255,255),
  };
  private int flashColor = TRANSPARENT;
  boolean active = false;
  boolean stopRequest = false;
  double startTime;

  double time;
  float ringWidth = 0.2f;
  private TEWholeModel model;
  private LXPoint[] pointArray;

  public MF64RingPattern(TEMidiFighter64DriverPattern driver) {
    super(driver);
    this.model = this.driver.getModel();

    // get safe list of all pattern points.
    ArrayList<LXPoint> newPoints = new ArrayList<>(model.points.length);
    newPoints.addAll(model.edgePoints);
    newPoints.addAll(model.panelPoints);
    pointArray =  newPoints.toArray(new LXPoint[0]);

    }

  /**
   * Converts a value  between 0.0 and 1.0, representing a sawtooth
   * waveform, to a position on a square wave between 0.0 to 1.0, using the
   * specified duty cycle.
   * @param n  value between 0.0 and 1.0
   * @param dutyCycle - percentage of time the wave is "on", range 0.0 to 1.0
   * @return
   */
  public static float square(float n,float dutyCycle) {
    return (float) ((Math.abs((n % 1)) <= dutyCycle) ? 1.0 : 0.0);
  }

  @Override
  public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
    this.flashColor = flashColors[mapping.col];

    this.active = true;
    stopRequest = false;
    startTime = System.currentTimeMillis();
  }

  @Override
  public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
    this.stopRequest = true;
  }

  private void paintAll(int colors[], int color) {
    time = System.currentTimeMillis();

    // calculate milliseconds per beat at current bpm
    float interval = (float) (1000.0 / (driver.getTempo().bpm() / 60.0));
    float ringSawtooth = (float) (time - startTime) / interval;

    // if we've completed a cycle see if we reset or stop
    if (ringSawtooth >= 1f) {

       if (stopRequest == true) {
         this.active = false;
         this.stopRequest = false;
         color = TRANSPARENT;
       }
       startTime = time;
       ringSawtooth = 0;
    }

    // define a ring moving out from the model center at 1 cycle/beat
    for (LXPoint point : this.pointArray) {
      float k = (1.0f-ringSawtooth) + point.rcn;
      int on = (int) (k * square(k,ringWidth));
      colors[point.index] = color * on;
    }
  }

  @Override
  public void run(double deltaMsec, int colors[]) {
    if (this.active == true) {
      paintAll(colors, this.flashColor);
    }
  }
}
