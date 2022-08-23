package titanicsend.app;

import heronarts.lx.parameter.*;
import heronarts.lx.studio.LXStudio;
import heronarts.p4lx.ui.UI2dContainer;
import heronarts.p4lx.ui.component.UICollapsibleSection;
import heronarts.p4lx.ui.component.UIDropMenu;
import heronarts.p4lx.ui.component.UILabel;
import heronarts.p4lx.ui.component.UITextBox;

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

  public enum GPPalMode {
    SEND_SAMPLES("Samples"),
    SEND_PALETTE("Palette"),
    SEND_SAMPLES_THEN_PALETTE("Samples + palette"),
    SEND_PALETTE_THEN_SAMPLES("Palette + samples");
    public final String label;

    GPPalMode(String label) {
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

  public final EnumParameter<GPPalMode> gpPalMode =
          new EnumParameter<>("GP Palette Mode", GPPalMode.SEND_PALETTE)
                  .setDescription("Which colors should GP send?");

  public GigglePixelUI(final LXStudio.UI ui, float width,
                       GigglePixelListener listener,
                       GigglePixelBroadcaster broadcaster) {
    super(ui, 0, 0, width, 48);
    setTitle("GigglePixel");
    setLayout(UI2dContainer.Layout.VERTICAL);
    UIDropMenu dropmenu = new UIDropMenu(0, 0, width, 16, gpMode);
    dropmenu.addToContainer(this);
    dropmenu = new UIDropMenu(0, 16, width, 16, gpPalMode);
    dropmenu.addToContainer(this);
    UILabel label = new UILabel(0, 32, width, 16, "Peers:");
    label.addToContainer(this);
    listener.peersTextbox = new UITextBox(0, 0, width, 16);
    listener.peersTextbox.addToContainer(this);
    final LXParameterListener updatePal = (p) -> {
      broadcaster.palMode = gpPalMode.getEnum();
    };
    gpPalMode.addListener(updatePal);
    updatePal.onParameterChanged(null);
    final LXParameterListener updateMode = (p) -> {
      switch(gpMode.getEnum()) {
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
    gpMode.addListener(updateMode);
    updateMode.onParameterChanged(null);
  }
}
