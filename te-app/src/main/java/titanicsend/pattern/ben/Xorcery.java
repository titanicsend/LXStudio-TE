package titanicsend.pattern.ben;

import static java.lang.Math.abs;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.pixelblaze.PixelblazePort;

@LXCategory("Combo FG")
public class Xorcery extends PixelblazePort {
  private double t1;
  private double t2;
  private double t3;
  private double t4;

  private double scale = 5;
  private double breath;
  private double modRange;
  private double measureWave;

  private float sinT, cosT;

  public Xorcery(LX lx) {
    super(lx);
  }

  @Override
  public void configureControls() {
    controls.setRange(TEControlTag.SIZE, 5, 1, 10);
  }

  @Override
  public void setup() {}

  @Override
  public void onPointsUpdated() {}

  @Override
  public void render3D(int index, float x, float y, float z) {
    double h, v;

    // translate so origin is at vehicle center and
    // rotate according to spin control setting
    //
    x -= 0.5f;
    y -= 0.5f;
    z -= 0.5f;
    float outX = (cosT * x) - (sinT * z);
    z = (sinT * x) + (cosT * z);
    x = outX;

    y += (float) (t1 + measureWave * .05); // add a slight bounce to movement
    x += (float) (breath * measureWave); // breathing effect
    h = t2 + wave(xorf(scale * x, xorf(scale * z, scale * y)) / 50 * (t3 * 10 + 4 * t4) % modRange);

    v = (abs(h) + modRange + t1) % 1;
    v = triangle(v);
    v = v * v;

    // original hsv calculates teal through purple colors (.45 - .85):
    // h = triangle(h) * .2 + triangle(x + y + z) * .2 + .45
    // hsv(h, 1, v)
    // for paint(), don't downscale the range
    h = triangle(h) + triangle(x + y + z);
    paint((float) h); // color gradient based on edge/panel
    setAlpha((float) v); // cut out areas that would otherwise be dark
  }

  @Override
  public void beforeRender(double deltaMs) {
    t1 = 1.0 - time(.1);
    t2 = Math.sin(t1 * PI2);
    t3 = triangle(time(.5));
    t4 = Math.sin(time(.34) * PI2);

    scale = getSize();
    breath = 0.3 * getWow1();
    modRange = 0.3 + (triangle(t1) * 0.2 * getWow2());
    measureWave = wave(measure());

    double theta = (float) getRotationAngleFromSpin();
    cosT = (float) Math.cos(theta);
    sinT = (float) Math.sin(theta);
  }

  double xorf(double v1, double v2) {
    v1 *= 65536;
    v2 *= 65536;
    return ((long) v1 ^ (long) v2) / 65536.0;
  }
}
