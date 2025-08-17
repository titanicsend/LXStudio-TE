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
 *
 * <p>[SYSEX DOCS](https://gist.github.com/Janiczek/04a87c2534b9d1435a1d8159c742d260)
 */
@LXMidiSurface.Name("Arturia MiniLab3 Effects")
@LXMidiSurface.DeviceName("Minilab3 MIDI")
public class EffectsMiniLab3 extends LXMidiSurface
    implements LXMidiSurface.Bidirectional, GlobalEffectManager.Listener {

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

  private GlobalEffectManager effectManager;
  private ObservableList.Listener<GlobalEffect<? extends LXEffect>> effectListener;
  private boolean isRegistered = false;
  private boolean shiftOn = false;

  private boolean isBankA = false;
  private boolean isDAW = false;

  public EffectsMiniLab3(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
  }

  // ------------------------------------------------------------------------------------
  // Connection
  // ------------------------------------------------------------------------------------

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
    this.effectManager = GlobalEffectManager.get(this.lx);

    List<GlobalEffect<? extends LXEffect>> slots = effectManager.slots;

    // Subscribe to state updates on enabled param for registered effects.
    this.effectManager.addListener(this);
    // Subscribe to effects getting registered/unregistered.
    this.effectListener =
        // TODO(look): think through whether these listeners need to do anything beyond
        //             calling updatePadLEDs().
        new ObservableList.Listener<>() {
          @Override
          public void itemAdded(GlobalEffect<? extends LXEffect> item) {
            if (slots != null) {
              int slotIndex = slots.indexOf(item);
              TE.log("Effect Slot added [" + slotIndex + "]: " + item);
              updatePadLEDs();
            }
          }

          @Override
          public void itemRemoved(GlobalEffect<? extends LXEffect> item) {
            if (slots != null) {
              int slotIndex = slots.indexOf(item);
              TE.log("Effect Slot removed [" + slotIndex + "]: " + item);
              updatePadLEDs();
            }
          }
        };
    effectManager.slots.addListener(this.effectListener);
  }

  private void unregister() {
    this.isRegistered = false;
    effectManager.removeListener(this);
    effectManager.slots.removeListener(effectListener);
    clearPadLEDs();
  }

  /**
   * Update LED colors when any individual effect's ENABLED/DISABLED state changes.
   *
   * @param slotIndex
   */
  @Override
  public void globalEffectStateUpdated(int slotIndex) {
    verbose("Global Effect state updated [" + slotIndex + "]");
    updatePadLEDs();
  }

  // ------------------------------------------------------------------------------------
  // Receiving MIDI Messages
  // ------------------------------------------------------------------------------------

  // User switched to Arturia Mode: F0 00 20 6B 7F 42 02 00 40 62 01 F7
  // User switched to DAW Mode:     F0 00 20 6B 7F 42 02 00 40 62 02 F7
  // User switched to Bank A:       F0 00 20 6B 7F 42 02 00 40 63 00 F7
  // User switched to Bank B:       F0 00 20 6B 7F 42 02 00 40 63 01 F7

  private static final byte SYSEX_RECEIVED_TYPE_MODE = 0x62;
  private static final byte SYSEX_RECEIVED_TYPE_BANK = 0x63;
  private static final byte[] SYSEX_RECEIVED_TYPES =
      new byte[] {SYSEX_RECEIVED_TYPE_MODE, SYSEX_RECEIVED_TYPE_BANK};

  private static final byte SYSEX_RECEIVED_MODE_ARTURIA = 0x01;
  private static final byte SYSEX_RECEIVED_MODE_DAW = 0x02;
  private static final byte[] SYSEX_RECEIVED_MODES =
      new byte[] {SYSEX_RECEIVED_MODE_ARTURIA, SYSEX_RECEIVED_MODE_DAW};

  private static final byte SYSEX_RECEIVED_BANK_A = 0x00;
  private static final byte SYSEX_RECEIVED_BANK_B = 0x01;
  private static final byte[] SYSEX_RECEIVED_BANKS =
      new byte[] {SYSEX_RECEIVED_BANK_A, SYSEX_RECEIVED_BANK_B};

  @Override
  public void sysexReceived(LXSysexMessage sysex) {
    verbose("Minilab3 Sysex: " + sysex);

    byte[] msg = sysex.getMessage();
    verbose("msg length: " + msg.length);

    // -1: mode byte not seen
    // 0:  is's a Mode update
    // 1:  it's a Bank update
    int type = -1;

    // -1: not seen
    // 0:  Arturia (if isMode), Bank A (not isMode)
    // 1:  DAW (if isMode),     Bank B (not isMode)
    int option = -1;

    for (int i = 0; i < msg.length; i++) {
      byte b = msg[i];
      verbose(String.format("msg[%02d]: %02X", i, b));

      switch (i) {
        case 0:
          expectByte(i, (byte) 0xF0, b);
          continue;
        case 1:
          expectByte(i, (byte) 0x00, b);
          continue;
        case 2:
          expectByte(i, (byte) 0x20, b);
          continue;
        case 3:
          expectByte(i, (byte) 0x6B, b);
          continue;
        case 4:
          expectByte(i, (byte) 0x7F, b);
          continue;
        case 5:
          expectByte(i, (byte) 0x42, b);
          continue;
        case 6:
          expectByte(i, (byte) 0x02, b);
          continue;
        case 7:
          expectByte(i, (byte) 0x00, b);
          continue;
        case 8:
          expectByte(i, (byte) 0x40, b);
          continue;
        case 9:
          type = expectOneOf(i, SYSEX_RECEIVED_TYPES, b);
          continue;
        case 10:
          if (type == 0) {
            try {
              option = expectOneOf(i, SYSEX_RECEIVED_MODES, b);
            } catch (IllegalArgumentException e) {
              verbose(
                  "Additional mode (besides default Arturia and DAW) configured in Arturia MCC app: "
                      + b);
            }

          } else if (type == 1) {
            option = expectOneOf(i, SYSEX_RECEIVED_BANKS, b);
          } else {
            throw new IllegalArgumentException(String.format("Invalid SYSEX_RECEIVED_TYPE" + type));
          }
          continue;
        case 11:
          expectByte(i, (byte) 0xF7, b);
          continue;
        default:
          throw new IllegalArgumentException(String.format("Invalid SYSEX_RECEIVED_TYPE" + type));
      }
    }

    if (type == 0) {
      if (option > 0) {
        modeReceived(option == 0 ? true : false);
      } else {
        verbose("Non-standard mode received; ignoring");
      }
    } else if (type == 1) {
      bankReceived(option == 0 ? true : false);
    } else {
      throw new IllegalArgumentException(String.format("Invalid SYSEX_RECEIVED_TYPE" + type));
    }
  }

  private static void expectByte(int index, byte expected, byte actual) {
    if (actual != expected) {
      throw new AssertionError(
          String.format(
              "Sysex message index %02d expected [%02X] but found [%02X]",
              index, expected, actual));
    }
  }

  private static int expectOneOf(int index, byte[] options, byte actual) {
    for (int i = 0; i < options.length; i++) {
      if (options[i] == actual) {
        return i;
      }
    }
    throw new AssertionError(
        String.format("Sysex message index %02d invalid: found [%02X]", index, actual));
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

  // ------------------------------------------------------------------------------------
  // Receive Physical Inputs (allows remap to logical)
  // ------------------------------------------------------------------------------------

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
      toggleEffect(padIndex);
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
    verbose("Mode: DAW = " + isDAW);
    this.isDAW = isDAW;
  }

  private void bankReceived(boolean isBankA) {
    // To determine: This will be received if user changes it.  Does it also get received as an
    // echo if we set it with a sysex?
    verbose("Bank: " + (isBankA ? "A" : "B"));
    this.isBankA = isBankA;
    sendBank(this.isBankA);
  }

  // ------------------------------------------------------------------------------------
  // Receive Logical Inputs (mapped from physical)
  // ------------------------------------------------------------------------------------

  /** Launch, aka run, an effect or variation. */
  private void launch(int index) {
    verbose("Launch effect or variation index #: " + index);
  }

  /** Toggle whether an effect is being edited */
  private void toggleEffect(int index) {
    // Enable/Disable effect
    press(index);
    /*
    Alternate approach:

    // If we are not yet editing this pad, start editing it. (Stop editing other ones first.)
    // If we ARE editing this pad, stop editing it. (AKA toggle off)
    verbose("Toggle Edit Effect #: " + index);
    */
  }

  /** Set the value of an effect parameter at a given index */
  private void parameterSetValue(int parameterIndex, int value) {
    verbose("Effect Parameter " + parameterIndex + ": set to " + value);
  }

  /** Set the value of a global parameter */
  private void globalParameterSetValue(int globalParamIndex, int value) {
    verbose("Global Parameter " + globalParamIndex + ": set to " + value);
  }

  // ------------------------------------------------------------------------------------
  // Send Sysex
  // ------------------------------------------------------------------------------------

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

  private void sendBank(byte bank) {
    List<GlobalEffect<? extends LXEffect>> slots = effectManager.slots;
    if (slots == null || slots.isEmpty()) {
      padVerbose("Slots is empty, exit");
    }

    //    // TEMP: just to keep an eye on the effect states while developing
    //    this.effectManager.debugStates();

    // Clear first
    clearPadLEDs();

    if (bank == SYSEX_BANK_A) {
      verbose("Send Bank A");
      for (int slotIndex = 0; slotIndex < Math.min(slots.size(), 4); slotIndex++) {
        updatePadForSlot(slotIndex);
      }
    } else if (bank == SYSEX_BANK_B) {
      verbose("Send Bank B");
      for (int slotIndex = 4; slotIndex < Math.min(slots.size(), 8); slotIndex++) {
        updatePadForSlot(slotIndex);
      }
    } else {
      throw new IllegalArgumentException("Unknown bank: " + bank);
    }
  }

  // ------------------------------------------------------------------------------------
  // Send Pad Colors
  // ------------------------------------------------------------------------------------

  private void press(int padIndex) {
    int slotIndex = padToSlotIndex(padIndex);
    verbose("PRESS: " + padIndex + " (Slot: " + slotIndex + ")");
    if (slotIndex < 0) {
      verbose("\tPad doesn't map to slot");
    } else if (slotIndex >= effectManager.slots.size()) {
      verbose("Out of range: " + padIndex);
      return;
    }

    GlobalEffect<? extends LXEffect> globalEffect = effectManager.slots.get(slotIndex);
    GlobalEffect.State currState = globalEffect.getState();
    if (currState == null) {
      verbose("Current state is null: " + globalEffect.getName());
      return;
    } else if (currState == GlobalEffect.State.EMPTY) {
      verbose("Current state is empty: " + globalEffect.getName());
      return;
    } else if (globalEffect.effect == null) {
      throw new IllegalStateException("LXEffect is null, but state is neither EMPTY nor null");
    } else {
      verbose("Current state: " + currState + " for effect: " + globalEffect.effect.getLabel());
    }

    globalEffect.getEnabledParameter().toggle();
    TE.log(
        String.format(
            "PRESS: %d: %s -> %s", padIndex, currState.name(), globalEffect.getState().toString()));
  }

  private void updatePadLEDs() {
    padVerbose(String.format("<<< Updating Pad LEDs (BANK: %s) >>>", this.isBankA ? "A" : "B"));
    sendBank(this.isBankA);
  }

  private void updatePadForSlot(int slotIndex) {
    GlobalEffect<? extends LXEffect> globalEffect = getSlot(slotIndex);
    int padIndex = slotToPadIndex(slotIndex);

    verbose(
        String.format(
            "\tSlot %d :: Pad %d :: %s",
            slotIndex,
            padIndex,
            globalEffect == null
                ? "null"
                : String.format("%s (%s)", globalEffect.getName(), globalEffect.getState())));

    if (globalEffect != null) {
      padVerbose("\t\tSlot " + slotIndex + " is " + globalEffect.getName());
      GlobalEffect.State state = globalEffect.getState();
      if (state != null) {
        padVerbose("\t\t\t state is " + state);
        switch (state) {
          case EMPTY -> {
            setPadLEDColor(padIndex, 0x00, 0x00, 0xFF);
          }
          case DISABLED -> {
            setPadLEDColor(padIndex, 0x00, 0xFF, 0x00);
          }
          case ENABLED -> {
            setPadLEDColor(padIndex, 0xFF, 0x00, 0x00);
          }
        }
      } else {
        padVerbose("\t\t\t state is null");
        // State is null
        setPadLEDColor(padIndex, 0x00, 0x00, 0x00);
      }
    } else {
      padVerbose("\t\tSlot " + slotIndex + " is null");
      // GlobalEffect is null
      setPadLEDColor(padIndex, 0x10, 0x00, 0x00);
    }
  }

  // ------------------------------------------------------------------------------------
  // Virtual Slot <--> Pad Mapping (to use the 4 pads aligned with the rows of knobs)
  // ------------------------------------------------------------------------------------

  private int padToSlotIndex(int padIndex) {
    int slotIndex = -1;
    // Bank A, the 4 buttons aligned with knobs
    if (padIndex >= 1 && padIndex <= 4) {
      // Pad 1: Slot 0
      // Pad 4: Slot 3
      slotIndex = padIndex - 1;
    } else if (padIndex >= 9 && padIndex <= 12) {
      // Pad 9:  Slot 4
      // Pad 12: Slot 7
      slotIndex = padIndex - 5;
    }
    return slotIndex;
  }

  private int slotToPadIndex(int slotIndex) {
    if (slotIndex < 0) {
      throw new IllegalArgumentException("Slot index is negative: " + slotIndex);
    } else if (slotIndex <= 3) {
      // Bank A
      return slotIndex + 1;
    } else if (slotIndex <= 7) {
      // Bank B
      return slotIndex + 5;
    } else if (slotIndex < effectManager.slots.size()) {
      throw new IllegalArgumentException(
          "EffectManager has more than expected maximum of 8 slots: "
              + slotIndex
              + " < "
              + effectManager.slots.size());
    } else {
      throw new IllegalArgumentException(
          "Slot index is out of range: " + slotIndex + " >= " + effectManager.slots.size());
    }
  }

  private List<GlobalEffect<? extends LXEffect>> allSlots() {
    return effectManager.slots;
  }

  private GlobalEffect<? extends LXEffect> getSlot(int index) {
    if (allSlots() == null || index < 0 || index >= allSlots().size()) {
      return null;
    }
    return allSlots().get(index);
  }

  private void clearPadLEDs() {
    // Send individual SysEx message to turn off each pad
    for (int i = 0; i < NUM_PADS; i++) {
      setPadLEDColor(i, 0, 0, 0); // Turn off (RGB = 0,0,0)
    }
  }

  private void setPadLEDColor(int padIndex, int red, int green, int blue) {
    if (padIndex < 0 || padIndex >= NUM_PADS) {
      throw new IllegalStateException("Pad index must be 0-8");
    }

    // Can it be done with a simple NoteOn?  Or do we have to use sysex to get full RGB?
    // sendNoteOn(MIDI_CHANNEL_PADS, (byte) PAD_NOTES[padIndex], LXColor.rgb(red, green, blue));

    /*
    SysEx format:
      F0                     # sysex header
      00 20 6B 7F 42         # Arturia header
      02 02 16 ID RR GG BB   # set color of button ID to 0xRRGGBB
      F7                     # sysex footer
    */
    // ID for pads 1-8 in DAW mode: 0x04 to 0x0B
    byte[] sysex = new byte[14];
    sysex[0] = START_SYSEX; // SysEx start
    sysex[1] = MIDI_MFR_ID_0; // Arturia manufacturer ID
    sysex[2] = MIDI_MFR_ID_1;
    sysex[3] = MIDI_MFR_ID_2;
    sysex[4] = (byte) 0x7F;
    sysex[5] = (byte) 0x42;
    sysex[6] = (byte) 0x02; // Mode command: set button color
    sysex[7] = SYSEX_MODE_DAW;
    sysex[8] = SYSEX_COMMAND_SET_COLOR; // Set color command
    sysex[9] = (byte) (0x04 + padIndex); // Pad ID (0x04-0x0B for pads 1-8)
    sysex[10] = (byte) (red & 0x7F); // R
    sysex[11] = (byte) (green & 0x7F); // G
    sysex[12] = (byte) (blue & 0x7F); // B
    sysex[13] = END_SYSEX; // SysEx end

    sendSysex(sysex);
  }

  // ------------------------------------------------------------------------------------
  // Shutdown
  // ------------------------------------------------------------------------------------

  /** Temporary for dev */
  private void verbose(String message) {
    LXMidiEngine.error(message);
  }

  private void padVerbose(String message) {
    boolean debugPads = false;
    if (debugPads) {
      LXMidiEngine.error(message);
    }
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
