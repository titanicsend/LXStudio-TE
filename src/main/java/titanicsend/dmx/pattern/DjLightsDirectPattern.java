package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.dmx.model.AdjStealthModel;
import titanicsend.dmx.model.DmxModel;
import titanicsend.ui.UIUtils;

/**
 * Simple DJ lights pattern allowing direct control of parameters.
 */
@LXCategory("Test")
public class DjLightsDirectPattern extends DjLightsPattern implements UIDeviceControls<DjLightsDirectPattern> {

  public DjLightsDirectPattern(LX lx) {
    super(lx);

    addParameter("pan", this.pan);
    addParameter("tilt", this.tilt);   
    addParameter("red", this.red);
    addParameter("green", this.green);
    addParameter("blue", this.blue);
    addParameter("white", this.white);
    addParameter("dimmer", this.dimmer);
    addParameter("shutter", this.shutter);
    addParameter("focus", this.focus);
    addParameter("colorTemp", this.colorTemp);
    addParameter("colorEffect", this.colorEffect);
    addParameter("colorFade", this.colorFade);
    addParameter("ptSpeed", this.ptSpeed);
    addParameter("programs", this.programs);
  }

  @Override
  protected void run(double deltaMs) {
    double pan = this.pan.getNormalized();
    double tilt = this.tilt.getNormalized();   
    double red = this.red.getNormalized();
    double green = this.green.getNormalized();
    double blue = this.blue.getNormalized();
    double white = this.white.getNormalized();
    double dimmer = this.dimmer.getNormalized();
    int shutter = this.shutter.getDmxValue();
    double focus = this.focus.getNormalized();
    double colorTemp = this.colorTemp.getNormalized();
    int colorEffect = this.colorEffect.getDmxValue();
    int colorFade = this.colorFade.getDmxValue();
    double ptSpeed = this.ptSpeed.getNormalized();
    int programs = this.programs.getDmxValue();

    for (DmxModel d : this.modelTE.djLights) {
      if (d instanceof AdjStealthModel) {
        setDmxNormalized(d, AdjStealthModel.INDEX_PAN, pan);
        setDmxNormalized(d, AdjStealthModel.INDEX_TILT, tilt);
        setDmxNormalized(d, AdjStealthModel.INDEX_RED, red);
        setDmxNormalized(d, AdjStealthModel.INDEX_GREEN, green);
        setDmxNormalized(d, AdjStealthModel.INDEX_BLUE, blue);
        setDmxNormalized(d, AdjStealthModel.INDEX_WHITE, white);
        setDmxNormalized(d, AdjStealthModel.INDEX_DIMMER, dimmer);
        setDmxValue(d, AdjStealthModel.INDEX_SHUTTER, shutter);
        setDmxNormalized(d, AdjStealthModel.INDEX_FOCUS, focus);
        setDmxNormalized(d, AdjStealthModel.INDEX_COLOR_TEMP, colorTemp);
        setDmxValue(d, AdjStealthModel.INDEX_COLOR_EFFECT, colorEffect);
        setDmxValue(d, AdjStealthModel.INDEX_COLOR_FADE, colorFade);
        setDmxNormalized(d, AdjStealthModel.INDEX_PT_SPEED, ptSpeed);
        setDmxValue(d, AdjStealthModel.INDEX_PROGRAMS, programs);
      }
    }
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, DjLightsDirectPattern device) {
    UIUtils.buildMftStyleDeviceControls(ui, uiDevice, device);
  }
}
