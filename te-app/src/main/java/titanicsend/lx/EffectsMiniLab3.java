package titanicsend.lx;

import static titanicsend.lx.DirectorAPCminiMk2.inRange;

import heronarts.lx.LX;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.utils.ObservableList;
import java.util.List;
import titanicsend.app.effectmgr.GlobalEffect;
import titanicsend.app.effectmgr.GlobalEffectManager;
import titanicsend.util.TE;

/**
 * Important: before using, press SHIFT and then tap pad 3 ("Prog") to switch "programs", until you
 * see "DAW" mode show up on the LCD display.
 *
 * <p>By default, the device is in "Arturia" mode, which won't modify button colors based on
 * external SysEx messages.
 *
 * <p>Also, to switch between "Bank A" and "Bank B", hold SHIFT and tap pad 2 ("Pad") to switch the
 * pad bank.
 */
@LXMidiSurface.Name("Minilab3 Effects Manager")
@LXMidiSurface.DeviceName("Minilab3")
public class EffectsMiniLab3 extends LXMidiSurface implements LXMidiSurface.Bidirectional {

  public static final int FADER_1 = 82;
  public static final int FADER_2 = 83;
  public static final int FADER_3 = 85;
  public static final int FADER_4 = 17;

  public static final int KNOB_1 = 74;
  public static final int KNOB_2 = 71;
  public static final int KNOB_3 = 76;
  public static final int KNOB_4 = 77;
  public static final int KNOB_5 = 93;
  public static final int KNOB_6 = 18;
  public static final int KNOB_7 = 19;
  public static final int KNOB_8 = 16;

  public static final int SHIFT = 27;

  public static final int NUM_PADS = 8;

  // Pad Bank A
  public static final int PAD_1_A = 36;
  public static final int PAD_2_A = 37;
  public static final int PAD_3_A = 38;
  public static final int PAD_4_A = 39;
  public static final int PAD_5_A = 40;
  public static final int PAD_6_A = 41;
  public static final int PAD_7_A = 42;
  public static final int PAD_8_A = 43;

  // Pad Bank B
  public static final int PAD_1_B = 44;
  public static final int PAD_2_B = 45;
  public static final int PAD_3_B = 46;
  public static final int PAD_4_B = 47;
  public static final int PAD_5_B = 48;
  public static final int PAD_6_B = 49;
  public static final int PAD_7_B = 50;
  public static final int PAD_8_B = 51;

  private GlobalEffectManager effectManager;
  private ObservableList.Listener<GlobalEffect<? extends LXEffect>> effectListener;

  public enum EffectState {
    EMPTY,
    DISABLED,
    ENABLED
  }

  private EffectState[] states;
  private boolean isRegistered = false;
  private boolean shiftOn = false;

  public EffectsMiniLab3(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
  }

  private void press(int padIndex) {
    TE.log("PRESS: " + padIndex);
    //    setPadLEDColor(padIndex, 127, 127, 127);
    if (padIndex >= this.states.length) {
      return;
    }
    EffectState currState = this.states[padIndex];
    GlobalEffect<? extends LXEffect> globalEffect = this.effectManager.slots.get(padIndex);
    if (currState != null && globalEffect.effect != null) {
      globalEffect.getEnabledParameter().toggle();
      switch (currState) {
        case DISABLED -> this.states[padIndex] = EffectState.ENABLED;
        case ENABLED -> this.states[padIndex] = EffectState.DISABLED;
        default -> TE.warning("unexpected pad press for empty slot " + padIndex);
      }
      TE.log("PRESS: " + padIndex + " " + currState.name() + " -> " + this.states[padIndex].name());
      updatePadLEDs();
    }
  }

  private void tap(int padIndex) {}

  private void noteReceived(MidiNote note, boolean on) {
    final int pitch = note.getPitch();

    // Global momentary
    if (pitch == SHIFT) {
      this.shiftOn = on;
      if (!on) {
        updatePadLEDs();
      }
      return;
    }

    if (inRange(pitch, PAD_1_B, PAD_8_B)) {
      int padIndex = pitch - PAD_1_B;
      if (on) {
        press(padIndex);
      }
      return;
    }

    if (inRange(pitch, PAD_1_A, PAD_8_A)) {
      int padIndex = pitch - PAD_1_A;
      if (on) {
        press(padIndex);
      }
      return;
    }
    //
    //    // Clip grid buttons
    //    if (inRange(pitch, CLIP_LAUNCH, CLIP_LAUNCH_MAX)) {
    //      if (pitch < 0 || pitch >= COLOR_NUM) {
    //        LXMidiEngine.error("Grid button not assigned to color: " + note);
    //        return;
    //      }
    //      if (isWhiteButton(pitch)) {
    //        // Ignore white in Chromatik, this is only for lasers.
    //        return;
    //      }
    //
    //      int color = noteToColor[pitch];
    //      float h = LXColor.h(color);
    //      float s = LXColor.s(color);
    //      float b = LXColor.b(color);
    //      if (this.paletteManager != null) {
    //        // Pass color to palette manager, which will push it immediately
    //        this.paletteManager.setColor(color);
    //      }
    //      return;
    //    } else if (inRange(pitch, SCENE_LAUNCH, SCENE_LAUNCH_MAX)) {
    //      // placeholder for using scene launch buttons
    //      return;
    //    } else if (inRange(pitch, CHANNEL_BUTTON, CHANNEL_BUTTON_MAX)) {
    //      // placeholder for using channel buttons
    //      return;
    //    }

    LXMidiEngine.error("APCminiMk2 received unmapped note: " + note);
  }

  @Override
  public void controlChangeReceived(MidiControlChange cc) {
    int number = cc.getCC();
    //
    //    if (number == MASTER_FADER) {
    //      this.masterFader.setValue(cc);
    //      return;
    //    }
    //
    //    if (number >= CHANNEL_FADER && number <= CHANNEL_FADER_MAX) {
    //      int channel = number - CHANNEL_FADER;
    //      this.channelFaders[channel].setValue(cc);
    //      return;
    //    }

    LXMidiEngine.error("APCmini unmapped control change: " + cc);
  }

  @Override
  protected void onEnable(boolean on) {
    if (on) {
      register();
      initialize();
      clearPadLEDs();
    } else {
      if (this.isRegistered) {
        unregister();
      }
    }
  }

  private void initialize() {
    updatePadLEDs();
  }

  private void register() {
    this.isRegistered = true;
    this.effectManager = null;
    this.effectListener = null;
    this.states = new EffectState[0];
    this.effectManager = GlobalEffectManager.get();
    // this.effectManager = (GlobalEffectManager) this.lx.engine.getChild("effectManager");

    List<GlobalEffect<? extends LXEffect>> slots = this.effectManager.slots;
    GlobalEffect<? extends LXEffect> curr;
    states = new EffectState[this.effectManager.slots.size()];
    for (int i = 0; i < states.length; i++) {
      curr = slots.get(i);
      if (curr == null || curr.effect == null) {
        states[i] = EffectState.DISABLED;
      } else {
        states[i] = curr.getEnabledParameter().isOn() ? EffectState.ENABLED : EffectState.DISABLED;
      }
    }

    this.effectListener =
        new ObservableList.Listener<>() {
          @Override
          public void itemAdded(GlobalEffect<? extends LXEffect> item) {
            int slotIndex = effectManager.slots.indexOf(item);
            states[slotIndex] = EffectState.DISABLED;
            TE.log("Effect Slot added [" + slotIndex + "]: " + item);
            updatePadLEDs();
          }

          @Override
          public void itemRemoved(GlobalEffect<? extends LXEffect> item) {
            int slotIndex = effectManager.slots.indexOf(item);
            states[slotIndex] = EffectState.EMPTY;
            TE.log("Effect Slot removed [" + slotIndex + "]: " + item);
            updatePadLEDs();
          }
        };
    this.effectManager.slots.addListener(this.effectListener);
  }

  private void unregister() {
    this.isRegistered = false;
    effectManager.slots.removeListener(effectListener);
    clearPadLEDs();
  }

  @Override
  protected void onReconnect() {
    if (this.enabled.isOn()) {
      initialize();
    }
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    noteReceived(note, true);
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    noteReceived(note, false);
  }

  private void updatePadLEDs() {
    // Send individual SysEx message for each pad
    for (int i = 0; i < NUM_PADS; i++) {
      if (this.states != null && i < this.states.length) {
        switch (this.states[i]) {
          case EMPTY -> {
            setPadA(i, 0x19, 0x19, 0x19);
            setPadB(i, 0x19, 0x19, 0x19);
          }
          case DISABLED -> {
            setPadA(i, 0x19, 0x19, 0xFF);
            setPadB(i, 0x19, 0x19, 0xFF);
          }
          case ENABLED -> {
            setPadA(i, 0xFF, 0x19, 0x19);
            setPadB(i, 0xFF, 0x19, 0x19);
          }
        }
      } else {
        setPadA(i, 0x10, 0x10, 0x10);
        setPadB(i, 0x10, 0x10, 0x10);
      }
    }
  }

  private void clearPadLEDs() {
    // Send individual SysEx message to turn off each pad
    for (int i = 0; i < NUM_PADS; i++) {
      setPadA(i, 0, 0, 0); // Turn off (RGB = 0,0,0)
      setPadB(i, 0, 0, 0); // Turn off (RGB = 0,0,0)
    }
  }

  private void setPadA(int padIndex, int red, int green, int blue) {
    setPadLEDColor(0, padIndex, red, green, blue);
  }

  private void setPadB(int padIndex, int red, int green, int blue) {
    setPadLEDColor(1, padIndex, red, green, blue);
  }

  private void setPadLEDColor(int bankIndex, int padIndex, int red, int green, int blue) {
    if (bankIndex < 0 || bankIndex > 1) {
      throw new IllegalStateException("Bank index must be 0 (A) or 1 (B)");
    }
    if (padIndex < 0 || padIndex >= NUM_PADS) {
      throw new IllegalStateException("Pad index must be 0-8");
    }

    // SysEx format: F0 00 20 6B 7F 42 02 02 16 ID RR GG BB F7
    // ID for pads 1-8 in DAW mode: 0x04 to 0x0B
    byte[] sysex = new byte[14];

    sysex[0] = (byte) 0xF0; // SysEx start
    sysex[1] = (byte) 0x00; // Arturia manufacturer ID
    sysex[2] = (byte) 0x20;
    sysex[3] = (byte) 0x6B;
    sysex[4] = (byte) 0x7F;
    sysex[5] = (byte) 0x42;
    sysex[6] = (byte) 0x02; // Mode command
    sysex[7] = (byte) 0x02; // DAW mode
    sysex[8] = (byte) 0x16; // Set color command
    sysex[9] = (byte) (0x04 + (bankIndex * 0x08) + padIndex); // Pad ID (0x04-0x0B for pads 1-8)
    sysex[10] = (byte) (red & 0x7F); // R
    sysex[11] = (byte) (green & 0x7F); // G
    sysex[12] = (byte) (blue & 0x7F); // B
    sysex[13] = (byte) 0xF7; // SysEx end

    sendSysex(sysex);
  }

  @Override
  public void dispose() {
    if (this.isRegistered) {
      unregister();
    }
    super.dispose();
  }
}
/*
UPDATE: BETTER SYSEX DOCS: https://gist.github.com/Janiczek/04a87c2534b9d1435a1d8159c742d260


F0                     # sysex header
00 20 6B 7F 42         # Arturia header
02 02 16 ID RR GG BB   # set color of button ID to 0xRRGGBB
F7                     # sysex footer


 */

/*

Notes on SysEx protocol: https://forum.arturia.com/t/sysex-protocol-documentation/5746

General SYSEX: [0xF0, 0x00, 0x20, 0x6B, 0x7F, 0x42, ...<data>, 0xF7]
<data> for:
Memory Request: [0x01, 0x00, 0x40, 0x01] // returns <data>:

Arturia Test(?): [0x04, 0x01, 0x60, 0x01, 0x31, 0x32, 0x33, 0x00]
Arturia Connect: [0x04, 0x01, 0x60, 0x01, 0x00, 0x02, 0x00]
Arturia Disconnect: [0x04, 0x01, 0x60, 0x0A, 0x0A, 0x5F, 0x51, 0x00] followed by [0x02, 0x02, 0x40, 0x6A, 0x10]

DAW Connect: [0x02, 0x02, 0x40, 0x6A, 0x21]
DAW Disconnect: [0x02, 0x02, 0x40, 0x6A, 0x20]

// r g b = 0-127
Set Shift-LED: [0x02, 0x02, 0x16, <id>, <r>, <g>, <b>] // persistent: 0x57 Loop, 0x58 Stop, 0x59 Play, 0x5A Record, 0x5B Tap
Set PAD LEDs: [0x04, 0x02, 0x16, 0x00] followed by 8x [<r>, <g>, <b>] for each pad // impermanent, mode or bank switch resets to white

Set Display+Text: [0x04, 0x02, 0x60, ...<mode>, ...<line1>, ...<line2>] where
<mode>:
  default: [],
  two lines: [0x1F, 0x02, 0x01, 0x00] // seems identical to default?
  encoder: [0x1F, 0x03, 0x01, <value>, 0x00, 0x00] (value 0-127)
  fader: [0x1F, 0x04, 0x01, <value>, 0x00, 0x00] (value 0-127)
  pressure: [0x1F, 0x05, 0x01, <value>, 0x00, 0x00] (value 0-127)
  leftright: [0x1F, 0x06, 0x01, <???>, <option>, 0x00] // Shows line 2, but not Line1. option 0x00 bottom bar, 0x01 no bar
  icons: [0x1F, 0x07, 0x01, <top_icon>, <bottom_icon>, 0x01, 0x00] // icons are 0: Empty, 1 Heart, 2 Play, 3 Record, 4 Note, 5 Checkmark
<line1>: [0x01, ...<up to 30 chars as bytes>, 0x00] // Zero terminated string? Display seems to show about 19 characters
<line2>: [0x02, ...<up to 30 chars as bytes>, 0x00] // Zero terminated string? Larger font so only 15 characters displayed

// note this is missing the pitchbend/modwheel display modes. DAW mode also has these hardwired to be active on inputs.

enum DAW_CC : byte // all on channel 0x00 (aka 1) besides the modwheel
{
    MODWHEEL = 1, // on keyboard channel
    SHIFT = 27,
    ENC_TURN = 28, ENC_SHIFT_TURN = 29, // always relative around 64+-3
    ENC_CLICK = 118, ENC_SHIFT_CLICK = 119,
    FADER1 = 0x0E, FADER2 = 0x0F, FADER3 = 0x1E, FADER4 = 0x1F,
    ENC1 = 86, ENC2 = 87, ENC3 = 89, ENC4 = 90, // forced absolute mode with device accel
    ENC5 = 110, ENC6 = 111, ENC7 = 116, ENC8 = 117, // forced absolute mode with device accel
    PAD_SHIFTLOOP = 105, PAD_SHIFTSTOP = 106, PAD_SHIFTPLAY = 107, PAD_SHIFTREC = 108, PAD_SHIFTTAP = 109,
}
enum DAW_NOTE : byte // Always on Channel 0x09 (aka 10 percussion)
{
    PADA1 = 36, PADA2 = 37, PADA3 = 38, PADA4 = 39, PADA5 = 40, PADA6 = 41, PADA7 = 42, PADA8 = 43,
    PADB1 = 44, PADB2 = 45, PADB3 = 46, PADB4 = 47, PADB5 = 48, PADB6 = 49, PADB7 = 50, PADB8 = 51,
}

*/
