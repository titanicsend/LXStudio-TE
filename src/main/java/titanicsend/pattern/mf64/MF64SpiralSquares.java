package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TE;
import titanicsend.util.TEMath;

import java.util.ArrayList;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64SpiralSquares extends TEMidiFighter64Subpattern {
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

  VariableSpeedTimer time;
  float sinT,cosT;
  private TEWholeModel model;
  private LXPoint[] pointArray;

  public MF64SpiralSquares(TEMidiFighter64DriverPattern driver) {
    super(driver);
    this.model = this.driver.getModel();

    // get safe list of all pattern points.
    ArrayList<LXPoint> newPoints = new ArrayList<>(model.points.length);
    newPoints.addAll(model.edgePoints);
    newPoints.addAll(model.panelPoints);
    pointArray =  newPoints.toArray(new LXPoint[0]);

    time = new VariableSpeedTimer();
    this.active = false;
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
  }

  @Override
  public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
    this.active = false;
  }

  private void paintAll(int colors[], int color) {

    // calculate time scale at current bpm
    time.setScale(4f * (float) (driver.getTempo().bpm() / 60.0));

    // rotation rate is one per second. We sneakily control speed
    // by controlling the speed of time, so we can avoid trig operations
    // at pixel time.
    cosT = (float) Math.cos(TEMath.TAU / 1000);
    sinT = (float) Math.sin(TEMath.TAU / 1000);
    float t1 = time.getTime();

    // a squared spiral from Pixelblaze pattern "Tunnel of Squares" at
    // https://github.com/zranger1/PixelblazePatterns/tree/master/2D_and_3D
    for (LXPoint point : this.pointArray) {
      float x = point.zn - 0.5f;  float y = point.yn  - 0.5f;

      float x1 = Math.signum(x);
      float y1 = Math.signum(y);

      // set up our square spiral
      float sx = x1 * cosT + y1 * sinT;
      float sy = y1 * cosT - x1 * sinT;

//      float dx = (float) Math.abs(Math.sin(4.0*Math.log(x * sx + y * sy) + Math.atan2(y,x) - t1));
      float dx = (float) Math.abs(Math.sin(4.0*Math.log(x * sx + y * sy) + point.azimuth - t1));
      int on = ((dx * dx * dx) < 0.2) ? 1 : 0;

      colors[point.index] = color * on;
    }
  }

  @Override
  public void run(double deltaMsec, int colors[]) {
    time.tick();
    if (this.active == true) {
      paintAll(colors, this.flashColor);
    }
  }
}
