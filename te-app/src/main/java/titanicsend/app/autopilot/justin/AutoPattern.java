/**
 * Copyright 2022- Justin Belcher, Mark C. Slee, Heron Arts LLC
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
 * @author Justin K. Belcher <justin@jkb.studio>
 */
package titanicsend.app.autopilot.justin;

import heronarts.lx.pattern.LXPattern;
import java.util.ArrayList;
import java.util.List;

/** Describes how to automate a pattern */
public class AutoPattern {

  public final Class<? extends LXPattern> pattern;
  public final List<AutoParameter> parameters = new ArrayList<AutoParameter>();

  public AutoPattern(Class<? extends LXPattern> pattern) {
    this.pattern = pattern;
  }

  public AutoPattern addParameter(AutoParameter param) {
    this.parameters.add(param);
    return this;
  }
}
