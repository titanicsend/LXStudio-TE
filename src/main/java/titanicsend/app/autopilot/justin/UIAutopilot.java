/**
 * Copyright 2022- Justin Belcher, Mark C. Slee, Heron Arts LLC
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
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package titanicsend.app.autopilot.justin;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UISwitch;
import heronarts.lx.LX;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedFunctionalParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;

public class UIAutopilot extends UICollapsibleSection {

  int y = 3;

  public UIAutopilot(UI ui, Autopilot autopilot, float w) {
    super(ui, 0, 0, w, 0);
    this.setTitle("JKB AUTOPILOT");

    UI2dContainer row = createRow();

    int col = 0;
    for (LXNormalizedParameter param : autopilot.visibleParameters) {
      if (col++ == 4) {
        col = 0;
        y += row.getHeight();
        row = createRow();
      }

      if (param instanceof BoundedParameter || param instanceof DiscreteParameter || param instanceof BoundedFunctionalParameter) {
        new UIKnob(0, 0)
          .setParameter(param)
          .addToContainer(row);
      } else if (param instanceof BooleanParameter) {
        new UISwitch(0, 0)
          .setParameter(param)
          .addToContainer(row);
      }
    }

    LX.error("Initializing JKB autopilot " + autopilot.visibleParameters.size());

    this.setContentHeight(y + row.getHeight());
  }

  UI2dContainer createRow() {
    UI2dContainer row = (UI2dContainer)
      new UI2dContainer(0, y, getContentWidth(), UIKnob.HEIGHT)
        .setLayout(UI2dContainer.Layout.HORIZONTAL)
        .setChildSpacing(3)
        .addToContainer(this);

    return row;
  }

}
