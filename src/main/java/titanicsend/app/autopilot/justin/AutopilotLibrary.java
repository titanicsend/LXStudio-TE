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

import java.util.HashMap;
import java.util.Map;

import heronarts.lx.pattern.LXPattern;

public class AutopilotLibrary {

  private final Map<Class<? extends LXPattern>, AutoPattern> patternLookup = new HashMap<Class<? extends LXPattern>, AutoPattern>();

  public AutopilotLibrary() {

  }

  public AutoPattern addPattern(Class<? extends LXPattern> pattern) {
    if (patternLookup.containsKey(pattern)) {
      throw new IllegalStateException("Cannot add same pattern class to the Autopilot Library twice: " + pattern);
    }
    AutoPattern entry = new AutoPattern(pattern);
    patternLookup.put(pattern, entry);
    return entry;
  }

  public AutoPattern getPattern(LXPattern pattern) {
    return patternLookup.get(pattern.getClass());
  }
}
