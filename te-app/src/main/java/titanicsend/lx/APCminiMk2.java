/**
 * Copyright 2024- Justin Belcher, Mark C. Slee, Heron Arts LLC
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
 * @author Justin K. Belcher <justin@jkb.studio>
 */
package titanicsend.lx;

import heronarts.lx.LX;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.surface.LXMidiSurface;
import titanicsend.midi.MidiNames;
import titanicsend.util.TE;

/** APC mini Mk2 MIDI control surface extension */
@LXMidiSurface.Name("Akai APC mini Mk2 Pacman")
@LXMidiSurface.DeviceName(MidiNames.APCMiniMk2)
public class APCminiMk2 extends heronarts.lx.midi.surface.APCminiMk2 {

  public APCminiMk2(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
    TE.log("APC mini Mk2 surface initialized");
  }

  @Override
  protected void onEnable(boolean on) {
    super.onEnable(on);
    if (on) {
      TE.log("APC mini Mk2 surface enabled");
    } else {
      TE.log("APC mini Mk2 surface disabled");
    }
  }

  @Override
  protected void onReconnect() {
    super.onReconnect();
    TE.log("APC mini Mk2 surface reconnected");
  }
}
