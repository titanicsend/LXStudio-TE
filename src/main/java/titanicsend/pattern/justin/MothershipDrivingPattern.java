package titanicsend.pattern.justin;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import titanicsend.util.TECategory;

/**
 * Driving lights and running lights for Mothership.
 * Tags are set in the fixture files.
 */
@LXCategory(TECategory.UTILITY)
@LXComponentName("Mothership Driving")
public class MothershipDrivingPattern extends TwoColorPattern {

  public MothershipDrivingPattern(LX lx) {
    super(lx);

    this.color1.setColor(LXColor.WHITE);
    this.color2.setColor(LXColor.RED);
    this.tag1.reset("drive");
    this.tag2.reset("run");
  }
}
