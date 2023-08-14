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
import titanicsend.dmx.model.ChauvetSpot160Model;
import titanicsend.dmx.model.DmxModel;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;

/**
 * Example TEPerformancePattern that uses DMX lights.
 */
@LXCategory("Test")
public class ExampleDmxTEPerformancePattern extends TEPerformancePattern {

  public ExampleDmxTEPerformancePattern(LX lx) {
    super(lx);

    this.controls.setRange(TEControlTag.SIZE, 0, 0, 1);
    this.controls.setRange(TEControlTag.XPOS, 0, 0, 360);
    this.controls.getControl(TEControlTag.XPOS).control.setWrappable(true);
    this.controls.setRange(TEControlTag.YPOS, 0, -130, 114);

    addCommonControls();
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {
    double pan = getNormalized(TEControlTag.XPOS);
    double tilt = getNormalized(TEControlTag.YPOS);
    double ptSpeed = getNormalized(TEControlTag.SPEED);
    double colorWheel = this.controls.color.getOffset();
    double gobo = getNormalized(TEControlTag.WOW1);
    double dimmer = getNormalized(TEControlTag.SIZE);
    int shutter = ChauvetSpot160Model.SHUTTER_OPEN;

    for (DmxModel d : this.modelTE.djLights) {
      if (d instanceof ChauvetSpot160Model) {
        setDmxNormalized(d, ChauvetSpot160Model.INDEX_PAN, pan);
        setDmxNormalized(d, ChauvetSpot160Model.INDEX_TILT, tilt);
        setDmxNormalized(d, ChauvetSpot160Model.INDEX_PT_SPEED, ptSpeed);
        setDmxNormalized(d, ChauvetSpot160Model.INDEX_COLOR_WHEEL, colorWheel);
        setDmxNormalized(d, ChauvetSpot160Model.INDEX_GOBO, gobo);
        setDmxNormalized(d, ChauvetSpot160Model.INDEX_DIMMER, dimmer); 
        setDmxValue(d, ChauvetSpot160Model.INDEX_SHUTTER, shutter);
        setDmxValue(d, ChauvetSpot160Model.INDEX_CONTROL, 0);
        setDmxValue(d, ChauvetSpot160Model.INDEX_MOVEMENT_MACROS, 0);
      }
    }
  }

  protected double getNormalized(TEControlTag tag) {
    return this.controls.getControl(tag).control.getNormalized();
  }

}
