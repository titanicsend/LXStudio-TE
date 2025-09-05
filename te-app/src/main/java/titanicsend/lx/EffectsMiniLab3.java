package titanicsend.lx;

import static heronarts.lx.midi.LXSysexMessage.END_SYSEX;
import static heronarts.lx.midi.LXSysexMessage.START_SYSEX;

import heronarts.lx.LX;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.LXSysexMessage;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.MidiPitchBend;
import heronarts.lx.midi.surface.LXMidiParameterControl;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.utils.ObservableList;
import java.util.List;
import titanicsend.app.effectmgr.GlobalEffectManager;
import titanicsend.app.effectmgr.Slot;
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
 *
 * <p>[Another
 * guide](https://github.com/PrzemekBarski/arturia-keylab-essential-mk3-programming-guide)
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
  public static final int NUM_PADS_PHYSICAL = 8;

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

  private static final byte BYTE_UNKNOWN = Byte.MIN_VALUE;

  public static final byte MIDI_MFR_ID_0 = 0x00;
  public static final byte MIDI_MFR_ID_1 = 0x20;
  public static final byte MIDI_MFR_ID_2 = 0x6B;
  public static final byte MIDI_DEVICE_ID = 0x7F;
  public static final byte MIDI_PRODUCT_FAMILY = 0x42;

  private static final byte[] SYSEX_HEADER =
      new byte[] {
        START_SYSEX,
        MIDI_MFR_ID_0,
        MIDI_MFR_ID_1,
        MIDI_MFR_ID_2,
        MIDI_DEVICE_ID,
        MIDI_PRODUCT_FAMILY
      };
  private static final int SYSEX_HEADER_LENGTH = SYSEX_HEADER.length;

  public static final byte SYSEX_MODE = 0x62;
  public static final byte SYSEX_MODE_ARTURIA = 0x01;
  public static final byte SYSEX_MODE_DAW = 0x02;
  private static final byte[] SYSEX_MODES = new byte[] {SYSEX_MODE_ARTURIA, SYSEX_MODE_DAW};

  public static final byte SYSEX_BANK = 0x63;
  public static final byte SYSEX_BANK_A = 0x00;
  public static final byte SYSEX_BANK_B = 0x01;
  private static final byte[] SYSEX_BANKS = new byte[] {SYSEX_BANK_A, SYSEX_BANK_B};

  private static final byte[] SYSEX_TYPES = new byte[] {SYSEX_MODE, SYSEX_BANK};

  public static final byte SYSEX_COMMAND_SET_COLOR = 0x16;

  private static final int BANK_COLORS_LENGTH = 8 * 3;

  // Configurable Colors

  public enum PadColor {
    OFF(LXColor.BLACK),
    ENABLED(LXColor.RED),
    DISABLED(LXColor.rgb(100, 100, 0)),
    EMPTY(LXColor.grayn(.2f));
    public final int r, g, b;
    public final byte rByte, gByte, bByte;

    PadColor(int color) {
      // RGB values for pads range 0-127 (0x00 to 0x7F)
      r = (LXColor.red(color) & 0xFF) / 2;
      g = (LXColor.green(color) & 0xFF) / 2;
      b = (LXColor.blue(color) & 0xFF) / 2;

      rByte = getRbyte(color);
      gByte = getGbyte(color);
      bByte = getBbyte(color);
    }

    public static int getRint(int color) {
      return (LXColor.red(color) & 0xFF) / 2;
    }

    public static int getGint(int color) {
      return (LXColor.green(color) & 0xFF) / 2;
    }

    public static int getBint(int color) {
      return (LXColor.blue(color) & 0xFF) / 2;
    }

    public static byte getRbyte(int color) {
      return (byte) (getRint(color) & 0x7F);
    }

    public static byte getGbyte(int color) {
      return (byte) (getGint(color) & 0x7F);
    }

    public static byte getBbyte(int color) {
      return (byte) (getBint(color) & 0x7F);
    }
  }

  private final GlobalEffectManager effectManager;
  private boolean isRegistered = false;
  private boolean shiftOn = false;

  private boolean isBankA = false;
  private boolean isDAW = false;

  public final EnumParameter<LXMidiParameterControl.Mode> faderMode =
      new EnumParameter<>("Fader Mode", LXMidiParameterControl.Mode.SCALE)
          .setDescription("Fader behavior when controlling parameters");

  private final LXMidiParameterControl[] faders = new LXMidiParameterControl[NUM_FADERS];

  // Constructor

  public EffectsMiniLab3(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
    this.effectManager = GlobalEffectManager.get();

    for (int i = 0; i < NUM_FADERS; i++) {
      this.faders[i] = new LXMidiParameterControl();
    }
    updateFaderMode();

    addSetting("faderMode", this.faderMode);
  }

  // Connection

  @Override
  protected void onEnable(boolean on) {
    if (on) {
      register();
      try {
        initialize();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } else {
      if (this.isRegistered) {
        unregister();
      }
    }
  }

  @Override
  protected void onReconnect() {
    if (this.enabled.isOn()) {
      try {
        initialize();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void initialize() throws InterruptedException {
    sendModeDAW();
    Thread.sleep(3);
    sendBank(this.isBankA);
    Thread.sleep(3);
    sendPadColors();
  }

  private void register() {
    this.isRegistered = true;

    registerFaders();

    int slotIndex = 0;
    for (Slot<? extends LXDeviceComponent> slot : this.effectManager.slots) {
      setSlot(slotIndex++, slot);
    }

    int triggerSlotIndex = 0;
    for (Slot<? extends LXDeviceComponent> slot : this.effectManager.triggerSlots) {
      setTriggerSlot(triggerSlotIndex++, slot);
    }

    // Subscribe to GlobalEffect slots added/removed
    this.effectManager.slots.addListener(this.slotsListener);
    this.effectManager.triggerSlots.addListener(this.triggerSlotsListener);
  }

  private void unregister() {
    this.isRegistered = false;

    unregisterFaders();

    this.effectManager.slots.removeListener(this.slotsListener);
    this.effectManager.triggerSlots.removeListener(this.triggerSlotsListener);

    for (int i = 0; i < MAX_SLOTS; i++) {
      setSlot(i, null);
    }
    for (int i = 0; i < MAX_TRIGGER_SLOTS; i++) {
      setTriggerSlot(i, null);
    }
    clearPadColors();
  }

  private final ObservableList.Listener<Slot<? extends LXDeviceComponent>> slotsListener =
      new ObservableList.Listener<>() {
        @Override
        public void itemAdded(Slot<? extends LXDeviceComponent> item) {
          int slotIndex = effectManager.slots.indexOf(item);
          TE.log("Slot added [" + slotIndex + "]: " + item);
          // Update registeredSlot for the new item and all slots after
          for (int i = slotIndex; i < effectManager.slots.size(); i++) {
            Slot<? extends LXDeviceComponent> slot = effectManager.slots.get(i);
            setSlot(i, slot);
          }
        }

        @Override
        public void itemRemoved(Slot<? extends LXDeviceComponent> item) {
          // Refresh all slot registrations. If they didn't change it will fast-out.
          int slotIndex = 0;
          for (Slot<? extends LXDeviceComponent> slot : effectManager.slots) {
            setSlot(slotIndex, slot);
            slotIndex++;
          }
          // One was removed, so set the next location to null
          setSlot(slotIndex, null);
          TE.log("Slot removed: " + item);
        }
      };
  private final ObservableList.Listener<Slot<? extends LXDeviceComponent>> triggerSlotsListener =
      new ObservableList.Listener<>() {
        @Override
        public void itemAdded(Slot<? extends LXDeviceComponent> item) {
          int slotIndex = effectManager.triggerSlots.indexOf(item);
          TE.log("TriggerSlot added [" + slotIndex + "]: " + item);
          // Update registeredSlot for the new item and all slots after
          for (int i = slotIndex; i < effectManager.triggerSlots.size(); i++) {
            Slot<? extends LXDeviceComponent> slot = effectManager.triggerSlots.get(i);
            setTriggerSlot(i, slot);
          }
        }

        @Override
        public void itemRemoved(Slot<? extends LXDeviceComponent> item) {
          // Refresh all triggerslot registrations. If they didn't change it will fast-out.
          int slotIndex = 0;
          for (Slot<? extends LXDeviceComponent> slot : effectManager.triggerSlots) {
            setTriggerSlot(slotIndex, slot);
            slotIndex++;
          }
          // One was removed, so set the next location to null
          setTriggerSlot(slotIndex, null);
          TE.log("Slot removed: " + item);
        }
      };

  // Slots

  private static final int MAX_SLOTS = 8;
  private static final int MAX_TRIGGER_SLOTS = NUM_KEYS;

  private final Slot<? extends LXDeviceComponent>[] registeredSlots = new Slot<?>[MAX_SLOTS];
  private final Slot<? extends LXDeviceComponent>[] registeredTriggerSlots =
      new Slot<?>[MAX_TRIGGER_SLOTS];

  private void setSlot(int slotIndex, Slot<? extends LXDeviceComponent> slot) {
    if (slotIndex < 0) {
      throw new IllegalArgumentException("Invalid slotIndex: " + slotIndex);
    }
    if (slotIndex >= MAX_SLOTS) {
      // Ignore slots beyond our capability
      return;
    }

    final Slot<? extends LXDeviceComponent> oldSlot = this.registeredSlots[slotIndex];
    if (oldSlot != slot) {
      if (oldSlot != null) {
        unregisterSlot(slotIndex, oldSlot);
      }
      this.registeredSlots[slotIndex] = slot;
      if (slot != null) {
        registerSlot(slotIndex, slot);
      }
      slotStateChanged(slotIndex);
      String slotName = slot != null ? slot.getName() : "<< EMPTY >>";
      verbose("Slot [" + slotIndex + "] set to: " + slotName);
    }
  }

  private void setTriggerSlot(int triggerSlotIndex, Slot<? extends LXDeviceComponent> slot) {
    if (triggerSlotIndex < 0) {
      throw new IllegalArgumentException("Invalid triggerSlotIndex: " + triggerSlotIndex);
    }
    if (triggerSlotIndex >= MAX_TRIGGER_SLOTS) {
      // Ignore slots beyond our capability
      return;
    }

    final Slot<? extends LXDeviceComponent> oldSlot = this.registeredTriggerSlots[triggerSlotIndex];
    if (oldSlot != slot) {
      if (oldSlot != null) {
        unregisterTriggerSlot(triggerSlotIndex, oldSlot);
      }
      this.registeredTriggerSlots[triggerSlotIndex] = slot;
      if (slot != null) {
        registerTriggerSlot(triggerSlotIndex, slot);
      }
      String slotName = slot != null ? slot.getName() : "<< EMPTY >>";
      verbose("TriggerSlot [" + triggerSlotIndex + "] set to: " + slotName);
    }
  }

  private void registerSlot(int slotIndex, Slot<? extends LXDeviceComponent> slot) {
    slot.addListener(this.slotStateListener);
  }

  private void unregisterSlot(int slotIndex, Slot<? extends LXDeviceComponent> slot) {
    slot.removeListener(this.slotStateListener);
  }

  private void registerTriggerSlot(int slotIndex, Slot<? extends LXDeviceComponent> slot) {
    // Placeholder
  }

  private void unregisterTriggerSlot(int slotIndex, Slot<? extends LXDeviceComponent> slot) {
    // Placeholder
  }

  private final Slot.Listener slotStateListener =
      (slot, state) -> {
        // Update LED colors when the state of a slot changes
        for (int i = 0; i < MAX_SLOTS; i++) {
          if (registeredSlots[i] == slot) {
            slotStateChanged(i);
            break;
          }
        }
      };

  private void slotStateChanged(int slotIndex) {
    int padIndex = slotToPadIndex(slotIndex);
    if (padIndex >= 0 && padIndex < NUM_PADS) {
      sendPadColor(padIndex);
    }
  }

  // Faders

  @Override
  public void onParameterChanged(LXParameter p) {
    super.onParameterChanged(p);
    if (p == this.faderMode) {
      updateFaderMode();
    }
  }

  private void updateFaderMode() {
    final LXMidiParameterControl.Mode mode = this.faderMode.getEnum();
    for (LXMidiParameterControl fader : this.faders) {
      fader.setMode(mode);
    }
  }

  private void registerFaders() {
    // Global Audio gain
    this.faders[0].setTarget(this.lx.engine.audio.meter.gain);

    // Example: Global Audio Stems gain
    // this.faders[0].setTarget(AudioStems.get().gain);
  }

  private void unregisterFaders() {
    for (LXMidiParameterControl fader : this.faders) {
      fader.setTarget(null);
    }
  }

  // Receiving MIDI Messages

  @Override
  public void sysexReceived(LXSysexMessage sysex) {
    // verbose("Minilab3 Sysex: " + sysex);

    // User switched to Arturia Mode: F0 00 20 6B 7F 42 02 00 40 62 01 F7
    // User switched to DAW Mode:     F0 00 20 6B 7F 42 02 00 40 62 02 F7
    // User switched to Bank A:       F0 00 20 6B 7F 42 02 00 40 63 00 F7
    // User switched to Bank B:       F0 00 20 6B 7F 42 02 00 40 63 01 F7

    byte[] msg = sysex.getMessage();
    //    verbose("msg length: " + msg.length);

    byte type = BYTE_UNKNOWN;
    byte option = BYTE_UNKNOWN;

    for (int i = 0; i < msg.length; i++) {
      byte b = msg[i];
      //      verbose(String.format("msg[%02d]: %02X", i, b));

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
          expectOneOf(i, SYSEX_TYPES, b);
          type = b;
          continue;
        case 10:
          switch (type) {
            case SYSEX_MODE:
              try {
                expectOneOf(i, SYSEX_MODES, b);
              } catch (IllegalArgumentException e) {
                LXMidiEngine.error(
                    "Additional mode (besides default Arturia and DAW) configured in Arturia MCC app: "
                        + b);
              }
              break;
            case SYSEX_BANK:
              expectOneOf(i, SYSEX_BANKS, b);
              break;
            default:
              // throw new IllegalArgumentException(
              LXMidiEngine.error("Invalid SYSEX_RECEIVED_TYPE " + type);
          }
          option = b;
          continue;
        case 11:
          expectByte(i, (byte) 0xF7, b);
          continue;
        default:
          // throw new IllegalArgumentException(
          LXMidiEngine.error("Invalid SYSEX_RECEIVED_TYPE " + type);
      }
    }

    // Check for invalid bytes
    if (type == BYTE_UNKNOWN) {
      verbose("Non-standard sysex mode received; ignoring");
      return;
    }
    if (option == BYTE_UNKNOWN) {
      verbose("Non-standard sysex option received; ignoring");
      return;
    }

    // Valid sysex received!
    switch (type) {
      case SYSEX_MODE -> {
        modeReceived(option == SYSEX_MODE_DAW);
      }
      case SYSEX_BANK -> {
        bankReceived(option == SYSEX_BANK_A);
      }
    }
  }

  private static void expectByte(int index, byte expected, byte actual) {
    if (actual != expected) {
      LXMidiEngine.error( // throw new AssertionError(
          String.format(
              "Sysex message index %02d expected [%02X] but found [%02X]",
              index, expected, actual));
    }
  }

  private static void expectOneOf(int index, byte[] options, byte actual) {
    for (int i = 0; i < options.length; i++) {
      if (options[i] == actual) {
        return;
      }
    }
    LXMidiEngine.error( // throw new AssertionError(
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
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    noteReceived(note, false);
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
        faderReceived(cc, i, value);
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
  }

  private void dawKnobReceived(int value) {
    // 61 = down fast, 62 = down, 65 = up, 66 = up fast
  }

  private void knobReceived(int knob, int value) {
    parameterSetValue(knob, value);
  }

  private void faderReceived(MidiControlChange cc, int fader, int value) {
    this.faders[fader].setValue(cc);

    // Broadcast for FYI but not really needed
    globalParameterSetValue(fader, value);
  }

  private void padReceived(int padIndex, boolean on, int velocity) {
    // Here we could alter behavior for different modes we are trialing

    if (on) {
      toggleEffect(padIndex);
    }
  }

  private void padLoopReceived() {}

  private void padStopReceived() {}

  private void padPlayReceived() {}

  private void padRecordReceived() {}

  private void padTapReceived() {}

  private void keyReceived(int note, int keyIndex, boolean on) {
    // Here we could alter behavior for different modes we are trialing.

    // To use all keys:
    launch(keyIndex, on);

    // Shift could be used in conjunction w/ keys, ex:
    // if (shiftOn) { // Do it different }

    // To use only the white keys:
    /* for (int i = 0; i < KEYS_WHITE.length; i++) {
      if (note == KEYS_WHITE[i]) {
        launch(i);
        return;
      }
    } */
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
    verbose("Received Mode: " + (isDAW ? "DAW" : "Arturia"));
    this.isDAW = isDAW;
    sendModeDAW();
  }

  private void bankReceived(boolean isBankA) {
    // To determine: This will be received if user changes it.  Does it also get received as an
    // echo if we set it with a sysex?
    verbose("Received Bank: " + (isBankA ? "A" : "B"));
    this.isBankA = isBankA;
    // sometimes upon reconnect, buttons go all white but we do get a sysex for bank received -
    // use this to send pad colors to stay consistent
    sendPadColors();
  }

  // Receive Logical Inputs (mapped from physical)

  /** Launch, aka run, an effect or variation. */
  private void launch(int index, boolean on) {
    if (index < 0) {
      throw new IllegalArgumentException("Invalid trigger index: " + index);
    }
    if (index >= MAX_TRIGGER_SLOTS) {
      TE.error("Trigger index exceeds number of trigger slots on MiniLab3: " + index);
      return;
    }

    Slot<? extends LXDeviceComponent> slot = this.registeredTriggerSlots[index];
    if (slot != null) {
      if (slot.trigger(on)) {
        // Log only the key press, not key release
        if (on) {
          // This will be fun to grep after a show:
          TE.log("MiniLab3: Launched Trigger " + index + ": " + slot.getName());
        }
      } else {
        verbose("Missing preset or triggerParameter for trigger slot: " + index);
      }
    } else {
      verbose("Unoccupied tigger slot: " + index);
    }
  }

  /** Toggle whether an effect is being edited */
  private void toggleEffect(int padIndex) {
    // Enable/Disable effect
    //    press(index);
    /*
    Alternate approach:

    // If we are not yet editing this pad, start editing it. (Stop editing other ones first.)
    // If we ARE editing this pad, stop editing it. (AKA toggle off)
    verbose("Toggle Edit Effect #: " + index);
    */

    int slotIndex = padToSlotIndex(padIndex);
    verbose("PRESS Pad: " + padIndex + " (Slot: " + slotIndex + ")");
    if (slotIndex < 0) {
      verbose("\tPad doesn't map to slot");
      return;
    } else if (slotIndex >= effectManager.slots.size()) {
      verbose("Out of range: " + padIndex);
      return;
    }

    Slot<? extends LXDeviceComponent> slot = effectManager.slots.get(slotIndex);
    Slot.State currState = slot.getState();
    if (currState == null) {
      verbose("Current state is null: " + slot.getName());
      return;
    } else if (currState == Slot.State.EMPTY) {
      verbose("Current state is empty: " + slot.getName());
      return;
    } else if (slot.device == null) {
      throw new IllegalStateException(
          "LXDeviceComponent is null, but state is neither EMPTY nor null");
    } else {
      verbose("Current state: " + currState + " for effect: " + slot.device.getLabel());
    }

    slot.getEnabledParameter().toggle();
    TE.log(
        String.format(
            "PRESS: %d: %s -> %s", padIndex, currState.name(), slot.getState().toString()));
  }

  /** Set the value of an effect parameter at a given index */
  private void parameterSetValue(int parameterIndex, int value) {
    verbose("Effect Parameter " + parameterIndex + ": set to " + value);

    int bankRelativeSlotIndex = parameterIndex % 4;
    int slotIndex = isBankA ? bankRelativeSlotIndex : 4 + bankRelativeSlotIndex;
    if (slotIndex >= effectManager.slots.size()) {
      return;
    }
    Slot<? extends LXDeviceComponent> slot = effectManager.slots.get(slotIndex);
    if (slot == null) {
      return;
    }

    // Effect Slot 0:
    // - primary param; knob 0 (labeled "1" on the device)
    // - secondary param; knob 4 (labeled "5" on the device)
    boolean isPrimaryParam = (parameterIndex / 4) == 0;

    LXListenableNormalizedParameter effectParam =
        isPrimaryParam ? slot.getLevelParameter() : slot.getSecondaryParameter();
    if (effectParam == null) {
      return;
    }
    effectParam.setNormalized(value / 127f);
  }

  /** Set the value of a global parameter */
  private void globalParameterSetValue(int globalParamIndex, int value) {
    verbose("Global Parameter " + globalParamIndex + ": set to " + value);
    // Nothing to do here, we already set the value through the LXMidiParameterControl fader
  }

  // Virtual Slot <--> Pad Mapping (to use the 4 pads aligned with the rows of knobs)

  private int padToSlotIndex(int padIndex) {
    // Optional: This map could vary by mode

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
    List<Slot<? extends LXDeviceComponent>> slots = this.effectManager.slots;
    if (slotIndex < 0) {
      throw new IllegalArgumentException("Slot index is negative: " + slotIndex);
    } else if (slotIndex <= Math.min(3, slots.size())) {
      // Bank A
      return slotIndex + 1;
    } else if (slotIndex <= Math.min(7, slots.size())) {
      // Bank B
      return slotIndex + 5;
    } else if (slotIndex < slots.size()) {
      throw new IllegalArgumentException(
          "EffectManager has more than expected maximum of 8 slots: "
              + slotIndex
              + " < "
              + slots.size());
    } else {
      throw new IllegalArgumentException(
          "Slot index is out of range: " + slotIndex + " >= " + slots.size());
    }
  }

  private List<Slot<? extends LXDeviceComponent>> allSlots() {
    return effectManager.slots;
  }

  private Slot<? extends LXDeviceComponent> getSlot(int index) {
    if (allSlots() == null || index < 0 || index >= allSlots().size()) {
      return null;
    }
    return allSlots().get(index);
  }

  // Send Sysex

  private static void applySysexHeader(byte[] sysex) {
    System.arraycopy(SYSEX_HEADER, 0, sysex, 0, SYSEX_HEADER.length);
  }

  private void sendModeDAW() {
    sendMode(SYSEX_MODE_DAW);
  }

  private void sendMode(byte mode) {
    // NOT WORKING:

    if (mode == SYSEX_MODE_DAW) {
      // Initialize DAW connection
      byte[] sysex = new byte[12];
      applySysexHeader(sysex);
      sysex[7] = 0x02; // or 00?
      sysex[8] = 0x40;
      sysex[9] = 0x6A;
      sysex[10] = 0x21;
      sysex[11] = END_SYSEX;
      sendSysex(sysex);
    }
  }

  private void sendBank(boolean bankA) {
    sendBank(bankA ? SYSEX_BANK_A : SYSEX_BANK_B);
  }

  private void sendBank(byte bank) {
    // TODO: set the bank with sysex
    // NOTE(look): not sure if working - i used the protocols from bank received here...
    byte[] sysex = new byte[13];
    applySysexHeader(sysex);
    sysex[7] = 0x02;
    sysex[8] = 0x00;
    sysex[9] = 0x40;
    sysex[10] = 0x03;
    sysex[11] = bank;
    //    sysex[10] = 0x63;
    //    sysex[11] = bank ? 0x01 : 0x02;
    sysex[12] = END_SYSEX;
    sendSysex(sysex);

    /*
      private void handleSysExData(String sysEx) {
    switch (sysEx) {
      case "f000206b7f420200406301f7":
      case "f000206b7f420200400300f7":
        this.toBankMode(PadBank.BANK_A);
        break;
      case "f000206b7f420200406302f7":
      case "f000206b7f420200400301f7":
        this.toBankMode(PadBank.BANK_B);
        break;
     */
  }

  // Send Pad Colors

  private static int getSysexPadId(int padIndex, boolean isPersistent) {
    // 04..0B => Temporary color for pads 1..8 (bank A)
    // 14..1B => Temporary color for pads 9..16 (bank B)
    // 34..3B => Persistent color for pads 1..8 (bank A)
    // 44..4B => Persistent color for pads 9..16 (bank B)

    int sysexPadId = 0x04 + (padIndex % NUM_PADS_PHYSICAL);
    if (padIndex >= NUM_PADS_PHYSICAL) {
      // Add 16 for Bank B
      sysexPadId += 16;
    }
    if (isPersistent) {
      // Add 48 for persistent mode
      sysexPadId += 48;
    }
    return sysexPadId;
  }

  private void sendPadColors() {
    // padVerbose("<<< Sending Pad LEDs >>>");
    for (int i = 0; i < NUM_PADS; i++) {
      sendPadColor(i);
    }
  }

  private void clearPadColors() {
    // Send individual SysEx message to turn off each pad
    for (int i = 0; i < NUM_PADS; i++) {
      sendPadColor(i, PadColor.OFF);
    }
  }

  private void sendPadColor(int padIndex) {
    sendPadColor(padIndex, getPadColor(padIndex));
  }

  private PadColor getPadColor(int padIndex) {
    int slotIndex = padToSlotIndex(padIndex);

    if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
      // Pad index does not map to a slot
      return PadColor.OFF;
    } else {
      Slot<? extends LXDeviceComponent> slot = this.registeredSlots[slotIndex];

      // Unoccupied
      if (slot == null) {
        return PadColor.EMPTY;
      }

      // Occupied slot
      Slot.State state = slot.getState();
      //      padVerbose("\t\t\t state is " + state);
      switch (state) {
        case EMPTY -> {
          return PadColor.EMPTY;
        }
        case DISABLED -> {
          return PadColor.DISABLED;
        }
        case ENABLED -> {
          return PadColor.ENABLED;
        }
      }
    }
    return PadColor.OFF;
  }

  /** Send the LED color for pad index 0-15 */
  private void sendPadColor(int padIndex, PadColor padColor) {
    int sysexPadId = getSysexPadId(padIndex, true);

    byte[] sysex = new byte[14];
    applySysexHeader(sysex);
    sysex[6] = 0x02; // Mode command: set button color
    sysex[7] = 0x02; // dawMode ? SYSEX_MODE_DAW : SYSEX_MODE_ARTURIA;
    sysex[8] = SYSEX_COMMAND_SET_COLOR;
    sysex[9] = (byte) sysexPadId;
    sysex[10] = padColor.rByte; // 7-bit RGB brightness (0x00 to 0x7F)
    sysex[11] = padColor.gByte;
    sysex[12] = padColor.bByte;
    sysex[13] = END_SYSEX;

    /* padVerbose(
    String.format(
        "\t\t\t\tSET %02X: (%d,%d,%d)", sysexPadId, padColor.r, padColor.g, padColor.b)); */

    sendSysex(sysex);
  }

  /**
   * Untested, but in theory this should work to send a full bank of colors in one sysex. Unknown if
   * this is compatible with persistent mode.
   */
  private void sendBankColors(int bankIndex, byte[] colors) {
    if (colors == null || colors.length != BANK_COLORS_LENGTH) {
      throw new IllegalArgumentException("Bank colors must be array of length 24");
    }

    byte[] sysex = new byte[35];
    applySysexHeader(sysex);
    sysex[6] = 0x04;
    sysex[7] = 0x02; // dawMode ? SYSEX_MODE_DAW : SYSEX_MODE_ARTURIA;
    sysex[8] = SYSEX_COMMAND_SET_COLOR;
    sysex[9] = (byte) bankIndex;
    System.arraycopy(colors, 0, sysex, 10, BANK_COLORS_LENGTH);
    sysex[34] = END_SYSEX;
    sendSysex(sysex);
  }

  // Shutdown

  /** Temporary for dev */
  private void verbose(String message) {
    LX.error("[MiniLab3] " + message);
  }

  private void padVerbose(String message) {
    boolean debugPads = true;
    if (debugPads) {
      verbose(message);
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
