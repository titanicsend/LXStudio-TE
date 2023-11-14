/**
 * This file is mainly a copy of Heron Art's P4LX from:
 * https://github.com/heronarts/P4LX/blob/master/src/main/java/heronarts/p4lx/ui/component/UIColorControl.java
 * ...It was modified slightly for TE.
 * 
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
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

package titanicsend.ui;

import heronarts.glx.ui.UIFocus;
import heronarts.glx.ui.component.UIKnob;
import titanicsend.pattern.TEColorParameter;

public class UITEColorControl extends UITEColorPicker implements UIFocus {

  public UITEColorControl(float x, float y, TEColorParameter color) {
    super(x, y, UIKnob.WIDTH, UIKnob.HEIGHT, color);
    setDeviceMode(true);
    setCorner(Corner.TOP_RIGHT);
  }
}
