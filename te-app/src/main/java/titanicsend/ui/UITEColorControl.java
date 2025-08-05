/**
 * This file is mainly a copy of Heron Art's P4LX from:
 * https://github.com/heronarts/P4LX/blob/master/src/main/java/heronarts/p4lx/ui/component/UIColorControl.java
 * ...It was modified slightly for TE.
 *
 * <p>Copyright 2017- Mark C. Slee, Heron Arts LLC
 *
 * <p>This file is part of the LX Studio software library. By using LX, you agree to the terms of
 * the LX Studio Software License and Distribution Agreement, available at: http://lx.studio/license
 *
 * <p>Please note that the LX license is not open-source. The license allows for free,
 * non-commercial use.
 *
 * <p>HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE,
 * WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */
package titanicsend.ui;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UIFocus;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.color.TEColorParameter;

public class UITEColorControl extends UITEColorPicker
    implements UIDeviceControls.ParameterControl<TEColorParameter>, UIFocus {

  public UITEColorControl(UI ui, TEColorParameter color) {
    this(ui, color, color.offset);
  }

  public UITEColorControl(UI ui, TEColorParameter color, LXParameter parameter) {
    super(color);
    // TODO: create a normal UIKnob for subparameters other than Offset (or decline building it?)
    setDeviceMode(true);
  }

  public UITEColorControl(float x, float y, TEColorParameter color) {
    super(x, y, color);
    setDeviceMode(true);
    setCorner(Corner.TOP_RIGHT);
  }
}
