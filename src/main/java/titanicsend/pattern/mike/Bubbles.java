package titanicsend.pattern.mike;

import static java.util.Collections.shuffle;
import static titanicsend.util.TEColor.TRANSPARENT;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LinkedColorParameter;
import java.util.*;
import titanicsend.color.TEColorType;
import titanicsend.model.*;
import titanicsend.pattern.TEPattern;

@LXCategory("Panel FG")
public class Bubbles extends TEPattern {
  private static final double BUBBLE_LIFETIME_MSEC =
      300.0; // Grow bubble at a rate such that it lives this long
  private static final double BUBBLE_THICKNESS = 0.025; // Expressed as a fraction of the panel
  private static final int NUM_BUBBLES = 5;
  private final HashMap<TEPanelModel, Double> bubbleFraction; // -1 if no bubble; else, 0.0-1.1
  private final List<TEPanelModel> newBubbleQueue;

  public final LinkedColorParameter color =
      registerColor("Color", "color", TEColorType.PRIMARY, "Color of the bubbles");

  public Bubbles(LX lx) {
    super(lx);

    this.bubbleFraction = new HashMap<>();
    this.newBubbleQueue = new ArrayList<>();

    int bubblesLeftToMake = NUM_BUBBLES;
    // Initialize existing bubbles out-of-phase with each other by this fraction
    double bubbleOffset = 0.0;

    for (TEPanelModel panel : modelTE.panelsById.values()) {
      if (panel.panelType.equals(TEPanelModel.LIT)) {
        double fraction;
        if (bubblesLeftToMake > 0) {
          fraction = bubbleOffset;
          bubbleOffset += BUBBLE_LIFETIME_MSEC / NUM_BUBBLES;
          bubblesLeftToMake--;
        } else {
          fraction = -1.0;
        }
        this.bubbleFraction.put(panel, fraction);
        this.newBubbleQueue.add(panel);
      }

      clearPixels();
    }

    shuffle(this.newBubbleQueue);
  }

  private void newBubble() {
    TEPanelModel panel = this.newBubbleQueue.remove(0);
    this.bubbleFraction.put(panel, 0.0);
    this.newBubbleQueue.add(panel);
  }

  public void run(double deltaMs) {
    double growthFraction = deltaMs / BUBBLE_LIFETIME_MSEC;
    for (Map.Entry<TEPanelModel, Double> entry : this.bubbleFraction.entrySet()) {
      TEPanelModel panel = entry.getKey();
      double fraction = entry.getValue();
      if (fraction >= 0.0) {
        double newValue = fraction + growthFraction;
        if (newValue < 1.0) {
          this.bubbleFraction.put(panel, newValue);
        } else {
          this.bubbleFraction.put(panel, -1.0); // *pop!*
          newBubble();
        }
      }
    }

    int bubbleColor = this.color.calcColor();

    for (Map.Entry<TEPanelModel, Double> entry : this.bubbleFraction.entrySet()) {
      TEPanelModel panel = entry.getKey();
      double fraction = entry.getValue();
      for (TEPanelModel.LitPointData lpd : panel.litPointData) {
        int color;
        if (lpd.radiusFraction >= fraction && lpd.radiusFraction < fraction + BUBBLE_THICKNESS) {
          color = bubbleColor;
        } else {
          color = TRANSPARENT;
        }
        colors[lpd.point.index] = color;
      }
    }
  }
}
