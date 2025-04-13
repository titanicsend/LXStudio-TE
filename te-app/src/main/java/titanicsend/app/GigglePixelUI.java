package titanicsend.app;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UITextBox;
import heronarts.lx.parameter.*;
import heronarts.lx.studio.LXStudio;

@Deprecated
public class GigglePixelUI extends UICollapsibleSection {
  public enum GPMode {
    OFF("Off"),
    PEEK("Peek"),
    SUBSCRIBE("Subscribe"),
    BROADCAST("Broadcast");
    public final String label;

    GPMode(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public final EnumParameter<GPMode> gpMode =
      new EnumParameter<>("GP Mode", GPMode.OFF)
          .setDescription("Should GigglePixel listen, broadcast, or neither?");

  public GigglePixelUI(
      final LXStudio.UI ui,
      float width,
      GigglePixelListener listener,
      GigglePixelBroadcaster broadcaster) {
    super(ui, 0, 0, width, 16);

    width = getContentWidth();
    setTitle("GigglePixel");
    setLayout(UI2dContainer.Layout.VERTICAL);

    UIDropMenu dropmenu = new UIDropMenu(0, 0, width, 16, gpMode);
    dropmenu.addToContainer(this);
    UILabel label = new UILabel(0, 0, width, 12, "Peers:");
    label.setBottomMargin(4);
    label.addToContainer(this);

    listener.peersTextbox = new UITextBox(0, 0, width, 16);
    listener.peersTextbox.addToContainer(this);

    final LXParameterListener update =
        (p) -> {
          switch (gpMode.getEnum()) {
            case OFF:
              listener.peeking = false;
              listener.subscribing = false;
              broadcaster.enabled = false;
              break;
            case PEEK:
              listener.peeking = true;
              listener.subscribing = false;
              broadcaster.enabled = false;
              break;
            case SUBSCRIBE:
              listener.peeking = true;
              listener.subscribing = true;
              broadcaster.enabled = false;
              break;
            case BROADCAST:
              listener.peeking = false;
              listener.subscribing = false;
              broadcaster.enabled = true;
              break;
            default:
              throw new Error("Internal Error");
          }
        };
    gpMode.addListener(update);
    update.onParameterChanged(null);
  }
}
