package titanicsend.lasercontrol;

import heronarts.glx.GLX;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.osc.LXOscConnection;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.TriggerParameter;
import studio.jkb.beyond.BeyondPlugin;
import studio.jkb.beyond.BeyondVariable;
import studio.jkb.beyond.parameter.BeyondBpmSync;
import studio.jkb.beyond.parameter.BeyondCompoundParameter;
import titanicsend.color.TEColorParameter;
import titanicsend.color.TEGradientSource;

public class TELaserTask extends LXComponent {

  public static final boolean DEFAULT_ENABLE_IN_PRODUCTION = true;

  private static TELaserTask current;

  public static TELaserTask get() {
    return current;
  }

  public final BooleanParameter sendBrightness =
      new BooleanParameter("SendBrightness", DEFAULT_ENABLE_IN_PRODUCTION)
          .setDescription("Send the laser fader to Pangolin Beyond master brightness with OSC");

  public final BooleanParameter sendColor =
      new BooleanParameter("SendColor", DEFAULT_ENABLE_IN_PRODUCTION)
          .setDescription("Send the laser color to Pangolin Beyond with OSC");

  public final BooleanParameter sendTempo =
      new BooleanParameter("SendTempo", DEFAULT_ENABLE_IN_PRODUCTION)
          .setDescription("Send beats and BPM to Pangolin Beyond with OSC");

  public final BeyondCompoundParameter brightness;
  private final TEBeyondColorSync colorSync;

  // User-editable laser color and an internal relay helper
  public final TEColorParameter color;

  // Internal helper that is not a registered parameter
  private final BeyondBpmSync bpm;

  public final TriggerParameter setUpOsc =
      new TriggerParameter("Set Up Now", this::runSetup)
          .setDescription("Add an OSC output for Beyond with the appropriate filter");

  public TELaserTask(LX lx) {
    super(lx);
    current = this;

    addParameter("sendBrightness", this.sendBrightness);
    addParameter("sendColor", this.sendColor);
    addParameter("sendTempo", this.sendTempo);
    addParameter(
        "brightness",
        this.brightness = new BeyondCompoundParameter(lx, BeyondVariable.BRIGHTNESS, "Lasers"));
    addParameter("color", this.color = new TEColorParameter(TEGradientSource.get(), "Lasers"));

    // NOTE(look): merge conflict from 'justin/laserChan', I think this is OK to remove but want to verify later
    //this.brightness = new BeyondCompoundParameter(lx, BeyondVariable.BRIGHTNESS, "Lasers");
    //this.color = new TEColorParameter(TEGradientSource.get(), "Lasers");
    
    this.colorSync = new TEBeyondColorSync(lx, this.color);
    this.bpm = new BeyondBpmSync(lx);

    this.brightness.setOutputEnabled(this.sendBrightness.isOn());
    this.colorSync.setOutputEnabled(this.sendColor.isOn());
    this.bpm.setOutputEnabled(this.sendTempo.isOn());
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.sendBrightness) {
      this.brightness.setOutputEnabled(this.sendBrightness.isOn());
    } else if (p == this.sendColor) {
      this.colorSync.setOutputEnabled(this.sendColor.isOn());
    } else if (p == this.sendTempo) {
      this.bpm.setOutputEnabled(this.sendTempo.isOn());
    }
  }

  private void runSetup() {
    // Confirm the OSC output for lasers (with correct filter) exists, or create a new one if not
    LXOscConnection.Output output =
        BeyondPlugin.confirmOscOutput(this.lx, PangolinHost.HOSTNAME, PangolinHost.PORT);

    // If someone clicked the button, let's make sure the output is turned on
    output.active.setValue(true);

    ((GLX) this.lx).ui.showContextDialogMessage("OSC output for lasers is ready to use!");
  }

  @Override
  public void dispose() {
    this.colorSync.dispose();
    this.bpm.dispose();

    super.dispose();
  }
}
