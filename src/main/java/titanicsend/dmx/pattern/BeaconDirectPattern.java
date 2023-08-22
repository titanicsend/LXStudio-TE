/**
 * Copyright 2023- Justin Belcher, Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.dmx.model.BeaconModel;
import titanicsend.dmx.model.DmxModel;
import titanicsend.ui.UIUtils;

/**
 * Simple beacon pattern allowing direct control of parameters.
 */
@LXCategory("Test")
public class BeaconDirectPattern extends BeaconPattern implements UIDeviceControls<BeaconDirectPattern> {

  public BeaconDirectPattern(LX lx) {
    super(lx);

    addParameter("Pan", this.pan);
    addParameter("Tilt", this.tilt);
    addParameter("Cyan", this.cyan);
    addParameter("Magenta", this.magenta);
    addParameter("Yellow", this.yellow);
    addParameter("ClrWheel", this.colorWheel);
    addParameter("Gobo1", this.gobo1);
    addParameter("Gobo1Rotate", this.gobo1rotation);
    addParameter("Gobo2", this.gobo2);
    addParameter("Prism1", this.prism1);
    addParameter("Prism1Rotate", this.prism1rotation);
    addParameter("Prism2Rotate", this.prism2rotation);
    addParameter("Focus", this.focus);
    addParameter("Shutter", this.shutter);
    addParameter("Dimmer", this.dimmer);
    addParameter("Frost1", this.frost1);
    addParameter("Frost2", this.frost2);
    addParameter("ptSpd", this.ptSpeed);
    addParameter("Control", this.control);  // Use caution!
  }

  @Override
  public void run(double deltaMs) {

    // Reminder: Don't use Normalized for DmxDiscreteParameters,
    // they likely do not scale linearly to 0-255.
    double pan = this.pan.getNormalized();
    double tilt = this.tilt.getNormalized();
    double cyan = this.cyan.getNormalized();
    double magenta = this.magenta.getNormalized();
    double yellow = this.yellow.getNormalized();
    int colorWheel = this.colorWheel.getDmxValue();
    int gobo1 = this.gobo1.getDmxValue();
    double gobo1rotate = this.gobo1rotation.getNormalized();
    int gobo2 = this.gobo2.getDmxValue();
    int prism1 = this.prism1.getDmxValue();
    double prism1rotate = this.prism1rotation.getNormalized();
    double prism2rotate = this.prism2rotation.getNormalized();
    double focus = this.focus.getNormalized();
    int shutter = this.shutter.getDmxValue();
    double dimmer = this.dimmer.getNormalized();
    double frost1 = this.frost1.getNormalized();
    double frost2 = this.frost2.getNormalized();
    int ptSpd = this.ptSpeed.getDmxValue();
    int control = this.control.getDmxValue();

    for (DmxModel d : this.modelTE.beacons) {      
      setDmxNormalized(d, BeaconModel.INDEX_PAN, pan);
      setDmxNormalized(d, BeaconModel.INDEX_TILT, tilt);
      setDmxNormalized(d, BeaconModel.INDEX_CYAN, cyan);
      setDmxNormalized(d, BeaconModel.INDEX_MAGENTA, magenta);
      setDmxNormalized(d, BeaconModel.INDEX_YELLOW, yellow);
      setDmxValue(d, BeaconModel.INDEX_COLOR_WHEEL, colorWheel);
      setDmxValue(d, BeaconModel.INDEX_GOBO1, gobo1);
      setDmxNormalized(d, BeaconModel.INDEX_GOBO1_ROTATION, gobo1rotate);
      setDmxValue(d, BeaconModel.INDEX_GOBO2, gobo2);
      setDmxValue(d, BeaconModel.INDEX_PRISM1, prism1);
      setDmxNormalized(d, BeaconModel.INDEX_PRISM1_ROTATION, prism1rotate);
      setDmxNormalized(d, BeaconModel.INDEX_PRISM2_ROTATION, prism2rotate);
      setDmxNormalized(d, BeaconModel.INDEX_FOCUS, focus);
      setDmxValue(d, BeaconModel.INDEX_SHUTTER, shutter);
      setDmxNormalized(d, BeaconModel.INDEX_DIMMER, dimmer);
      setDmxNormalized(d, BeaconModel.INDEX_FROST1, frost1);
      setDmxNormalized(d, BeaconModel.INDEX_FROST2, frost2);
      setDmxValue(d, BeaconModel.INDEX_PT_SPEED, ptSpd);
      // CONTROL_NORMAL is a default value so isn't required to be set by pattern
      setDmxValue(d, BeaconModel.INDEX_CONTROL, control);
    }
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, BeaconDirectPattern device) {
    UIUtils.buildMftStyleDeviceControls(ui, uiDevice, device);
  }
}
