package titanicsend.pattern.justin;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.pattern.LXPattern;
import java.util.ArrayList;
import java.util.List;
import titanicsend.color.TEColorParameter;

/** A test pattern for lighting up specific Mothership windows and slices */
@LXCategory(LXCategory.TEST)
@LXComponentName("Test Mothership Window")
public class TestMothershipWindow extends LXPattern {

  public enum Side {
    BOTH("both", "b"),
    PORT("port", "p"),
    STARBOARD("starboard", "s");

    public final String label;
    public final String firstLetter;

    private Side(String label, String firstLetter) {
      this.label = label;
      this.firstLetter = firstLetter;
    }
  }

  public final EnumParameter<Side> side =
      new EnumParameter<>("Side", Side.BOTH)
          .setIncrementMode(DiscreteParameter.IncrementMode.RELATIVE)
          .setWrappable(false);

  public final DiscreteParameter slice =
      new DiscreteParameter("Slice", 0, 0, 24)
          .setOptions(
              new String[] {
                "All", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
                "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"
              })
          .setIncrementMode(DiscreteParameter.IncrementMode.RELATIVE)
          .setWrappable(false);

  public final DiscreteParameter window =
      new DiscreteParameter("Window", 0, 0, 9)
          .setOptions(new String[] {"All", "W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8", "W9"})
          .setIncrementMode(DiscreteParameter.IncrementMode.RELATIVE)
          .setWrappable(false);

  public final TEColorParameter color =
      (TEColorParameter)
          new TEColorParameter("Color")
              .setColorSource(TEColorParameter.ColorSource.STATIC)
              .setColor(LXColor.RED);

  private boolean needsRefresh = true;
  private final List<LXModel> models = new ArrayList<>();

  public TestMothershipWindow(LX lx) {
    super(lx);

    addParameter("side", this.side);
    addParameter("slice", this.slice);
    addParameter("window", this.window);
    addParameter("color", this.color);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.side || p == this.slice || p == this.window) {
      this.needsRefresh = true;
    }
  }

  @Override
  public void onModelChanged(LXModel model) {
    super.onModelChanged(model);
    this.needsRefresh = true;
  }

  private void refresh() {
    this.models.clear();

    final int slice = this.slice.getValuei();
    final Side side = this.side.getEnum();
    final int window = this.window.getValuei();

    // Slice
    String sliceString = "slice";
    if (slice != 0) {
      sliceString += String.format("%02d", slice);
    }

    // Optional port or starboard
    if (side != Side.BOTH) {
      if (slice == 0) {
        sliceString = side.label;
      } else {
        sliceString += side.firstLetter;
      }
    }
    final List<LXModel> slices = getModel().sub(sliceString);

    // Window
    if (window == 0) {
      this.models.addAll(slices);
    } else {
      final String windowString = "w" + window;
      for (LXModel s : slices) {
        this.models.addAll(s.sub(windowString));
      }
    }
  }

  @Override
  protected void run(double v) {
    if (this.needsRefresh) {
      this.needsRefresh = false;
      refresh();
    }

    final int color = this.color.calcColor();

    for (LXModel m : this.models) {
      setColor(m, color);
    }
  }
}
