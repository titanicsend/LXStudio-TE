package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BooleanParameter.Mode;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import titanicsend.dmx.model.AdjStealthModel;
import titanicsend.dmx.model.DmxModel;
import titanicsend.ui.UIUtils;

/**
 * Any easy pattern to use for controlling DJ lights.
 * This might be sufficient for show time if their position is mainly static.
 */
@LXCategory("DMX")
public class DjLightsEasyPattern extends DjLightsPattern implements UIDeviceControls<DjLightsEasyPattern>{

  CompoundParameter focus = new CompoundParameter("Focus");

  public final BooleanParameter mirror = (BooleanParameter)
      new BooleanParameter("MIRROR", false)
      .setDescription("Alternates beam angle between fixtures")
      .setMode(Mode.MOMENTARY);

  public final BooleanParameter strobe = (BooleanParameter)
      new BooleanParameter("Strobe!", false)
      .setDescription("Press for strobe action!")
      .setMode(Mode.MOMENTARY);

  DiscreteParameter strobeSpeed = new DiscreteParameter("StrbSpd", 245, 10, 245)
      .setWrappable(false)
      .setDescription("Strobe Speed");

  public final ColorParameter linkedColor = 
      new ColorParameter("Color");

  public DjLightsEasyPattern(LX lx) {
    super(lx);

    addParameter("pan", this.pan);
    addParameter("tilt", this.tilt);
    addParameter("strobeSpeed", this.strobeSpeed);
    addParameter("strobe", this.strobe);
    addParameter("color", this.linkedColor);
    addParameter("dimmer", this.dimmer);
    addParameter("focus", this.focus);
    addParameter("mirror", this.mirror);

    //linkedColor.mode.setValue(LinkedColorParameter.Mode.PALETTE);
    
    this.dimmer.setNormalized(.5);
    
    this.setCustomRemoteControls(new LXListenableNormalizedParameter[] {
        this.pan,
        this.tilt,
        this.strobeSpeed,
        this.strobe,
        this.linkedColor.hue,
        this.linkedColor.saturation,
        this.linkedColor.brightness,
        this.dimmer,
        this.focus,
        this.mirror
    });
  }

  @Override
  protected void run(double deltaMs) {
    double pan = this.pan.getNormalized();
    double tilt = this.tilt.getNormalized();   
    boolean strobe = this.strobe.getValueb();
    int strobeSpeed = this.strobeSpeed.getValuei();
    boolean mirror = this.mirror.getValueb();
    double dimmer = this.dimmer.getNormalized();
    double focus = this.focus.getNormalized();

    int color = this.linkedColor.calcColor();
    int r = ((color >> 16) & 0xff);
    int g = ((color >> 8) & 0xff);
    int b = (color & 0xff);
    int w = (r < g) ? ((r < b) ? r : b) : ((g < b) ? g : b);
    r -= w;
    g -= w;
    b -= w;

    int i=0;
    for (DmxModel d : this.modelTE.djLights) {
      if (d instanceof AdjStealthModel) {
        if (mirror && i++ % 2 == 1) {
          setDmxNormalized(d, AdjStealthModel.INDEX_PAN, 1-pan);          
        } else {
          setDmxNormalized(d, AdjStealthModel.INDEX_PAN, pan);
        }
        setDmxNormalized(d, AdjStealthModel.INDEX_TILT, tilt);
        setDmxValue(d, AdjStealthModel.INDEX_RED, r);
        setDmxValue(d, AdjStealthModel.INDEX_GREEN, g);
        setDmxValue(d, AdjStealthModel.INDEX_BLUE, b);
        setDmxValue(d, AdjStealthModel.INDEX_WHITE, w);
        setDmxNormalized(d, AdjStealthModel.INDEX_DIMMER, dimmer);

        if (strobe) {
          setDmxValue(d, AdjStealthModel.INDEX_SHUTTER, strobeSpeed);
        } else {
          setDmxValue(d, AdjStealthModel.INDEX_SHUTTER, AdjStealthModel.SHUTTER_OPEN);
        }
        setDmxNormalized(d, AdjStealthModel.INDEX_FOCUS, focus);
      }
    }
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, DjLightsEasyPattern device) {
    UIUtils.buildMftStyleDeviceControls(ui, uiDevice, device);
  }
}
