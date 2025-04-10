package titanicsend.pattern.cesar;

import static titanicsend.util.TEColor.TRANSPARENT;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import titanicsend.color.TEColorType;
import titanicsend.pattern.TEPattern;
import titanicsend.util.TECategory;

@LXCategory(TECategory.UTILITY)
public class HandTracker extends TEPattern {
  public final BooleanParameter circle =
      new BooleanParameter("Circle", false).setDescription("Square or circle");
  public final CompoundParameter junk1 =
      new CompoundParameter("Junk1", 0, 0, 100).setDescription("Placeholder 1");
  public final CompoundParameter junk2 =
      new CompoundParameter("Junk2", 0, 0, 100).setDescription("Placeholder 2");
  public final CompoundParameter targetY =
      new CompoundParameter("Altitude", 25.5, 0, 100).setDescription("Target height from ground");
  public final CompoundParameter targetX =
      new CompoundParameter("Azimuth", 61, -100, 100)
          .setDescription("Target position left and right");
  public final CompoundParameter targetH =
      new CompoundParameter("Height", 4.2, 0.25, 100)
          .setDescription("Target height")
          .setExponent(2);

  public final CompoundParameter targetW =
      new CompoundParameter("Width", 4, 0.25, 100).setDescription("Target width").setExponent(2);

  public final LinkedColorParameter color =
      registerColor("Color", "color", TEColorType.PRIMARY, "Color of the pattern");

  public final StringParameter indexTip =
      new StringParameter("Index Tip", "10,50")
          .setDescription("Following a finger tip (X, Z) 0-100 like '(10,50)'");

  // Utility methods for use during the static-to-dynamic model transition.
  // These can be safely removed when we are fully committed to the
  // dynamic model

  public float getModelX(LXPoint p) {
    return (modelTE.isStatic()) ? p.z : p.x;
  }

  public float getModelXMax() {
    return (modelTE.isStatic()) ? modelTE.maxZ() : modelTE.maxX();
  }

  public HandTracker(LX lx) {
    super(lx);
    addParameter("circle", this.circle);

    // Three placeholders just to put all four real controls
    // on the same MIDI knob row
    addParameter("junk1", this.junk1);
    addParameter("junk2", this.junk2);

    addParameter("Altitude", this.targetY);
    addParameter("Azimuth", this.targetX);
    addParameter("Height", this.targetH);
    addParameter("Width", this.targetW);
    addParameter("indexTip", this.indexTip);
  }

  @Override
  public void run(double deltaMs) {
    float y = this.targetY.getNormalizedf();
    float x = this.targetX.getNormalizedf();
    float w = this.targetW.getNormalizedf();
    float h = this.targetH.getNormalizedf();

    int color = this.color.calcColor();

    boolean doCircle = this.circle.isOn();

    for (LXPoint point : this.lx.getModel().points) {
      // if (this.modelTE.isGapPoint(point)) continue;
      boolean lit =
          (point.xn > x - w) && (point.xn < x + w) && (point.yn > y - h) && (point.yn < y + h);

      colors[point.index] = lit ? color : TRANSPARENT;

      /*
      float xPercent = 100.0f * getModelX(point) / xMax;
      float yPercent = 100.0f * point.y / yMax;
      float dy = yPercent - y;
      float dx = xPercent - x;
      boolean opaque;
      if (doCircle) {
        dx *= 0.85; // Make ellipse into circle
        double distance = Math.sqrt(dy * dy + dx * dx);
        opaque = distance < targetH.getValue();
      } else {
        boolean isCloseToZ = Math.abs(dx) < targetW.getValue();
        boolean isCloseToY = Math.abs(dy) < targetH.getValue();
        opaque = isCloseToZ && isCloseToY;
      }
      if (opaque) {
        colors[point.index] = color;
      } else {
        colors[point.index] = TRANSPARENT;
      }*/
    }
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    super.onParameterChanged(p);
    if (p == this.indexTip) {
      String[] points = ((StringParameter) p).getString().split(",");
      float yFromPoint = Float.parseFloat(points[1]);
      float zFromPoint = Float.parseFloat(points[0]);

      this.targetY.setValue(yFromPoint);
      this.targetX.setValue(zFromPoint);
    }
  }
}
