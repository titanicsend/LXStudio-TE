package titanicsend.pattern.mike;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.pattern.LXPattern;
import titanicsend.pattern.TEPattern;

@LXCategory("Combo FG")
public class AlternatingPattern extends TEPattern {
  public final LinkedColorParameter oddColor =
          registerColor("Odd", "odd", TEPattern.ColorType.PANEL,
                  "Color of the odd pixels");

  public final LinkedColorParameter evenColor =
          registerColor("Even", "even", TEPattern.ColorType.SECONDARY,
                  "Color of the even pixels");

  public final DiscreteParameter stripeLength =
          new DiscreteParameter("Length", 1, 10)
                  .setDescription("Number of pixels to go before switching colors");

  public AlternatingPattern(LX lx) {
    super(lx);
    addParameter("stripeLength", this.stripeLength);
  }

  @Override
  public void run(double deltaMs) {
    int stripeLength = this.stripeLength.getValuei();
    int oddColor = this.oddColor.calcColor();
    int evenColor = this.evenColor.calcColor();
    for (int i = 0; i < colors.length; ++i) {
      int phase = (i / stripeLength) % 2;
      this.colors[i] = (phase == 0) ? oddColor : evenColor;
    }
  }
}
