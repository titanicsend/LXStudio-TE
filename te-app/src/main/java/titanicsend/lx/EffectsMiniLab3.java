package titanicsend.lx;

import static heronarts.lx.midi.LXSysexMessage.END_SYSEX;
import static heronarts.lx.midi.LXSysexMessage.START_SYSEX;

import heronarts.lx.LX;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.LXSysexMessage;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.MidiPitchBend;
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
 * <p>To switch between "Bank A" and "Bank B", hold SHIFT and tap pad 2 ("Pad") to switch the pad
 * bank.
 *
 * <p>Octave shifts are invisible to the DAW (unless there's a sysex we could grab). Be careful to
 * keep the octaves centered.
 */
@LXMidiSurface.Name("Arturia MiniLab3 Effects")
@LXMidiSurface.DeviceName("Minilab3 MIDI")
public class EffectsMiniLab3 extends LXMidiSurface implements LXMidiSurface.Bidirectional {

  // MIDI Channels

  public static final int MIDI_CHANNEL_COMMON = 0;
  public static final int MIDI_CHANNEL_PITCH_WHEEL = 0;
  public static final int MIDI_CHANNEL_PADS = 9;

  // CCs in DAW Mode

  public static final int SHIFT = 27;

  public static final int FADER_1 = 14;
  public static final int FADER_2 = 15;
  public static final int FADER_3 = 30;
  public static final int FADER_4 = 31;

  public static final int KNOB_1 = 86;
  public static final int KNOB_2 = 87;
  public static final int KNOB_3 = 89;
  public static final int KNOB_4 = 90;
  public static final int KNOB_5 = 110;
  public static final int KNOB_6 = 111;
  public static final int KNOB_7 = 116;
  public static final int KNOB_8 = 117;

  // CCs that are the same in both DAW & Arturia modes

  public static final int KNOB_DAW = 28;
  public static final int MOD_WHEEL = 1;

  // CCs in Arturia Mode

  public static final int ARTURIA_SHIFT = 9;

  public static final int ARTURIA_FADER_1 = 82;
  public static final int ARTURIA_FADER_2 = 83;
  public static final int ARTURIA_FADER_3 = 85;
  public static final int ARTURIA_FADER_4 = 17;

  public static final int ARTURIA_KNOB_1 = 74;
  public static final int ARTURIA_KNOB_2 = 71;
  public static final int ARTURIA_KNOB_3 = 76;
  public static final int ARTURIA_KNOB_4 = 77;
  public static final int ARTURIA_KNOB_5 = 93;
  public static final int ARTURIA_KNOB_6 = 18;
  public static final int ARTURIA_KNOB_7 = 19;
  public static final int ARTURIA_KNOB_8 = 16;

  public static final int NUM_FADERS = 4;
  public static final int NUM_KNOBS = 8;

  public static final int[] FADER_CCs = new int[] {FADER_1, FADER_2, FADER_3, FADER_4};
  public static final int[] ARTURIA_FADER_CCs =
      new int[] {ARTURIA_FADER_1, ARTURIA_FADER_2, ARTURIA_FADER_3, ARTURIA_FADER_4};

  public static final int[] KNOB_CCs =
      new int[] {KNOB_1, KNOB_2, KNOB_3, KNOB_4, KNOB_5, KNOB_6, KNOB_7, KNOB_8};
  public static final int[] ARTURIA_KNOB_CCs =
      new int[] {
        ARTURIA_KNOB_1,
        ARTURIA_KNOB_2,
        ARTURIA_KNOB_3,
        ARTURIA_KNOB_4,
        ARTURIA_KNOB_5,
        ARTURIA_KNOB_6,
        ARTURIA_KNOB_7,
        ARTURIA_KNOB_8
      };

  // Notes

  // Pads
  public static final int PAD_1_A = 36;
  public static final int PAD_2_A = 37;
  public static final int PAD_3_A = 38;
  public static final int PAD_4_A = 39;
  public static final int PAD_5_A = 40;
  public static final int PAD_6_A = 41;
  public static final int PAD_7_A = 42;
  public static final int PAD_8_A = 43;
  public static final int PAD_1_B = 44;
  public static final int PAD_2_B = 45;
  public static final int PAD_3_B = 46;
  public static final int PAD_4_B = 47;
  public static final int PAD_5_B = 48;
  public static final int PAD_6_B = 49;
  public static final int PAD_7_B = 50;
  public static final int PAD_8_B = 51;

  public static final int PAD_START = PAD_1_A;
  public static final int PAD_END = PAD_8_B;
  public static final int NUM_PADS = 16;
  public static final int NUM_PADS_PHYSICAL = 8; // Needed?

  private static final int[] PAD_NOTES =
      new int[] {
        PAD_1_A, PAD_2_A, PAD_3_A, PAD_4_A, PAD_5_A, PAD_6_A, PAD_7_A, PAD_8_A,
        PAD_1_B, PAD_2_B, PAD_3_B, PAD_4_B, PAD_5_B, PAD_6_B, PAD_7_B, PAD_8_B
      };

  // Pads + Shift
  public static final int PAD_LOOP = 105;
  public static final int PAD_STOP = 106;
  public static final int PAD_PLAY = 107;
  public static final int PAD_REC = 108;
  public static final int PAD_TAP_TEMPO = 109; // Only in DAW mode

  // Keys (with octaves centered)
  public static final int KEY_START = 48;
  public static final int KEY_END = 72;
  public static final int NUM_KEYS = KEY_END - KEY_START + 1;

  public static final int[] KEYS_WHITE =
      new int[] {48, 50, 52, 53, 55, 57, 59, 60, 62, 64, 65, 67, 69, 71, 72};
  public static final int[] KEYS_BLACK = new int[] {49, 51, 54, 56, 58, 61, 63, 66, 68, 70};

  // Sysex

  public static final byte MIDI_MFR_ID_0 = 0x00;
  public static final byte MIDI_MFR_ID_1 = 0x20;
  public static final byte MIDI_MFR_ID_2 = 0x6B;

  public static final byte SYSEX_MODE = 0x62;
  public static final byte SYSEX_MODE_ARTURIA = 0x01;
  public static final byte SYSEX_MODE_DAW = 0x02;

  public static final byte SYSEX_BANK = 0x63;
  public static final byte SYSEX_BANK_A = 0x00;
  public static final byte SYSEX_BANK_B = 0x01;

  public static final byte SYSEX_COMMAND_SET_COLOR = 0x16;

  private enum EffectState {
    DISABLED,
    ENABLED,
    EMPTY,
    ;
  }

  EffectState[] states;

  private GlobalEffectManager effectManager;
  private ObservableList.Listener<GlobalEffect<? extends LXEffect>> effectListener;
  private boolean isRegistered = false;
  private boolean shiftOn = false;

  public EffectsMiniLab3(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
  }

  // Connection

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

  @Override
  protected void onReconnect() {
    if (this.enabled.isOn()) {
      initialize();
    }
  }

  private void initialize() {
    sendModeDAW();
    updatePadLEDs();
  }

  private void register() {
    this.isRegistered = true;
    this.effectManager = null;
    this.effectListener = null;
    this.effectManager = GlobalEffectManager.get();

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

  // Receiving MIDI Messages

  @Override
  public void sysexReceived(LXSysexMessage sysex) {
    verbose("Minilab3 Sysex: " + sysex);

    // User switched to Arturia Mode: F0 00 20 6B 7F 42 02 00 40 62 01 F7
    // User switched to DAW Mode:     F0 00 20 6B 7F 42 02 00 40 62 02 F7
    // User switched to Bank A:       F0 00 20 6B 7F 42 02 00 40 63 00 F7
    // User switched to Bank B:       F0 00 20 6B 7F 42 02 00 40 63 01 F7

    // TODO: send to modeReceived(bool) or bankReceived(bool)
  }

  @Override
  public void pitchBendReceived(MidiPitchBend pitchBend) {
    // verbose("Minilab3 Pitch Bend: " + pitchBend);
    if (pitchBend.getChannel() == MIDI_CHANNEL_PITCH_WHEEL) {
      touchStrip1Received(pitchBend.getNormalized());
    }
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    noteReceived(note, true);
    /* verbose(
    "Minilab3 Note ON  CH: "
        + note.getChannel()
        + "  Pitch:"
        + note.getPitch()
        + "  Velocity:"
        + note.getVelocity()); */
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    noteReceived(note, false);
    /* verbose(
    "Minilab3 Note OFF CH: "
        + note.getChannel()
        + "  Pitch:"
        + note.getPitch()
        + "  Velocity:"
        + note.getVelocity()); */
  }

  private void noteReceived(MidiNote note, boolean on) {
    final int channel = note.getChannel();
    final int pitch = note.getPitch();

    if (channel == MIDI_CHANNEL_COMMON) {
      // Special pads that required shift, not bank-dependent
      if (on) {
        switch (pitch) {
          case PAD_LOOP:
            padLoopReceived();
            return;
          case PAD_STOP:
            padStopReceived();
            return;
          case PAD_PLAY:
            padPlayReceived();
            return;
          case PAD_REC:
            padRecordReceived();
            return;
          case PAD_TAP_TEMPO:
            padTapReceived();
            return;
        }
      }

      // Keys
      if (inRange(pitch, KEY_START, KEY_END)) {
        int keyIndex = pitch - KEY_START;
        keyReceived(pitch, keyIndex, on);
        return;
      }

    } else if (channel == MIDI_CHANNEL_PADS) {
      // Pads
      if (inRange(pitch, PAD_START, PAD_END)) {
        int padIndex = pitch - PAD_START;
        padReceived(padIndex, on, note.getVelocity());
        return;
      }

    } else {
      LXMidiEngine.error("Minilab3 note received on unknown channel: " + note);
      return;
    }

    LXMidiEngine.error("Minilab3 received unmapped note: " + note);
  }

  @Override
  public void controlChangeReceived(MidiControlChange cc) {
    int number = cc.getCC();
    int value = cc.getValue();

    // Shift
    if (number == SHIFT || number == ARTURIA_SHIFT) {
      shiftReceived(value != 0);
      return;
    }

    // DAW Knob
    if (number == KNOB_DAW) {
      dawKnobReceived(value);
      return;
    }

    // Knobs
    for (int i = 0; i < NUM_KNOBS; i++) {
      if (number == KNOB_CCs[i] || number == ARTURIA_KNOB_CCs[i]) {
        knobReceived(i, value);
        return;
      }
    }

    // Faders
    for (int i = 0; i < NUM_FADERS; i++) {
      if (number == FADER_CCs[i] || number == ARTURIA_FADER_CCs[i]) {
        faderReceived(i, value);
        return;
      }
    }

    // Touch Strip 2
    if (number == MOD_WHEEL) {
      touchStrip2Received(value);
      return;
    }

    LXMidiEngine.error("Minilab3 unmapped control change: " + cc);
  }

  // Receive Physical Inputs (allows remap to logical)

  private void shiftReceived(boolean on) {
    this.shiftOn = on;
    if (!on) {
      updatePadLEDs();
    }
  }

  private void dawKnobReceived(int value) {
    // 61 = down fast, 62 = down, 65 = up, 66 = up fast
  }

  private void knobReceived(int knob, int value) {
    parameterSetValue(knob, value);
  }

  private void faderReceived(int fader, int value) {
    globalParameterSetValue(fader, value);
  }

  private void padReceived(int padIndex, boolean on, int velocity) {
    // Here we could alter behavior for different modes we are trialing

    if (on) {
      toggleEdit(padIndex);
      // press(padIndex);
    }
  }

  private void padLoopReceived() {}

  private void padStopReceived() {}

  private void padPlayReceived() {}

  private void padRecordReceived() {}

  private void padTapReceived() {}

  private void keyReceived(int note, int keyIndex, boolean on) {
    // Here we could alter behavior for different modes we are trialing.

    // Ignore key release
    if (!on) {
      return;
    }

    // To index using all keys:
    // launch(keyIndex);

    // To index only the white keys:
    for (int i = 0; i < KEYS_WHITE.length; i++) {
      if (note == KEYS_WHITE[i]) {
        launch(i);

        // Shift could be used in conjunction w/ keys, ex:
        // if (shiftOn) { // Do it different }
        return;
      }
    }
  }

  private void touchStrip1Received(double normalized) {
    verbose("Touch strip 1: " + normalized);
  }

  private void touchStrip2Received(int value) {
    verbose("Touch strip 2: " + value);
  }

  private void modeReceived(boolean isDAW) {
    // To determine: This will be received if user changes it.  Does it also get received as an
    // echo if we set it with a sysex?
  }

  private void bankReceived(boolean isBankA) {
    // To determine: This will be received if user changes it.  Does it also get received as an
    // echo if we set it with a sysex?
  }

  // Receive Logical Inputs (mapped from physical)

  /** Launch, aka run, an effect or variation. */
  private void launch(int index) {
    verbose("Launch effect or variation index #: " + index);
  }

  /** Toggle whether an effect is being edited */
  private void toggleEdit(int index) {
    // If we are not yet editing this pad, start editing it. (Stop editing other ones first.)
    // If we ARE editing this pad, stop editing it. (AKA toggle off)

    verbose("Toggle Edit Effect #: " + index);
  }

  /** Set the value of an effect parameter at a given index */
  private void parameterSetValue(int parameterIndex, int value) {
    verbose("Effect Parameter " + parameterIndex + ": set to " + value);
  }

  /** Set the value of a global parameter */
  private void globalParameterSetValue(int globalParamIndex, int value) {
    verbose("Global Parameter " + globalParamIndex + ": set to " + value);
  }

  // Send Sysex

  private void sendModeDAW() {
    sendMode(SYSEX_MODE_DAW);
  }

  private void sendMode(byte mode) {
    byte[] sysex = new byte[14];

    // TODO: flush out the details of this sysex
    /*
    sysex[0] = START_SYSEX; // SysEx start
    sysex[1] = MIDI_MFR_ID_0; // Arturia manufacturer ID
    sysex[2] = MIDI_MFR_ID_1;
    sysex[3] = MIDI_MFR_ID_2;
    sysex[4] = (byte) 0x7F;
    sysex[5] = (byte) 0x42;
    sysex[6] = (byte) 0x02; // Mode command
    sysex[7] = mode; // DAW mode
    sysex[13] = END_SYSEX;

    sendSysex(sysex);
    */
  }

  private void sendBank(boolean bankA) {
    sendBank(bankA ? SYSEX_BANK_A : SYSEX_BANK_B);
  }

  private void sendBank(byte bank) {}

  // Send Pad Colors

  private void press(int padIndex) {
    TE.log("PRESS: " + padIndex);
    //    setPadLEDColor(padIndex, 127, 127, 127);
    if (padIndex >= effectManager.slots.size()) {
      return;
    }
    EffectState currState = this.states[padIndex];
    GlobalEffect<? extends LXEffect> globalEffect = effectManager.slots.get(padIndex);
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

    // Can it be done with a simple NoteOn?  Or do we have to use sysex to get full RGB?
    // sendNoteOn(MIDI_CHANNEL_PADS, (byte) PAD_NOTES[padIndex], LXColor.rgb(red, green, blue));

    // SysEx format: F0 00 20 6B 7F 42 02 02 16 ID RR GG BB F7
    // ID for pads 1-8 in DAW mode: 0x04 to 0x0B
    byte[] sysex = new byte[14];

    // TODO: JKB to Look: I might have broken this command when bringing in the constants
    // Check for issues in int->byte conversions

    sysex[0] = START_SYSEX; // SysEx start
    sysex[1] = MIDI_MFR_ID_0; // Arturia manufacturer ID
    sysex[2] = MIDI_MFR_ID_1;
    sysex[3] = MIDI_MFR_ID_2;
    sysex[4] = (byte) 0x7F;
    sysex[5] = (byte) 0x42;
    sysex[6] = (byte) 0x02; // Mode command
    // sysex[7] = (byte) 0x02; // DAW mode
    sysex[7] = SYSEX_MODE_DAW;
    sysex[8] = SYSEX_COMMAND_SET_COLOR; // Set color command
    sysex[9] = (byte) (0x04 + (bankIndex * 0x08) + padIndex); // Pad ID (0x04-0x0B for pads 1-8)
    sysex[10] = (byte) (red & 0x7F); // R
    sysex[11] = (byte) (green & 0x7F); // G
    sysex[12] = (byte) (blue & 0x7F); // B
    sysex[13] = END_SYSEX; // SysEx end

    sendSysex(sysex);
  }

  // Shutdown

  /** Temporary for dev */
  private void verbose(String message) {
    LXMidiEngine.error(message);
  }

  @Override
  public void dispose() {
    if (this.isRegistered) {
      unregister();
    }
    super.dispose();
  }

  /**
   * Returns true if value is between [min, max] inclusive
   *
   * @param val Value
   * @param min Min value
   * @param max Max value
   * @return True if contained in range
   */
  public static boolean inRange(int val, int min, int max) {
    return (val >= min) && (val <= max);
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
