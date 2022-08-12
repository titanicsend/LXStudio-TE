package titanicsend.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.midi.*;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.p4lx.ui.UI2dContainer;
import titanicsend.pattern.mf64.*;

@LXCategory("Combo FG")
public class TEMidiFighter64DriverPattern extends TEPattern implements LXMidiListener {
  public static final String MIDI_NAME = "Midi Fighter 64";

  /* Use the Midi Fighter Utility from DJ Tech Tools to apply
   * these recommended settings to your midi controller:
   *   MIDI Channel = 3
   *   MIDI Type = Notes
   *   Corner Button Bank Change: Hold or Disabled
   *
   * Midi Fighter manual (official URL):
   * https://drive.google.com/file/d/0B-QvIds_FsH3WDBNWXUxWTlGVlU/view?resourcekey=0-eGf57BdEMP8GB2TaanYccg
   *
   * When sending notes, these are the pitches that correspond to its button grid:
   *   64 65 66 67  96 97 98 99
   *   60 61 62 63  92 93 94 95
   *   56 57 58 59  88 89 90 91
   *   52 53 54 55  84 85 86 87
   *   48 49 50 51  80 81 82 83
   *   44 45 46 47  76 77 78 79
   *   40 41 42 43  72 73 74 75
   *   36 37 38 39  68 69 70 71
   */

  // We're calling the bottom row 0, so this is the reverse of the diagram above.
  public static final int[] pitchFromXY = {
          36, 37, 38, 39,  68, 69, 70, 71,
          40, 41, 42, 43,  72, 73, 74, 75,
          44, 45, 46, 47,  76, 77, 78, 79,
          48, 49, 50, 51,  80, 81, 82, 83,
          52, 53, 54, 55,  84, 85, 86, 87,
          56, 57, 58, 59,  88, 89, 90, 91,
          60, 61, 62, 63,  92, 93, 94, 95,
          64, 65, 66, 67,  96, 97, 98, 99
  };

  public static final String DEVICE_NAME = "Midi Fighter 64";

  public static final int LED_OFF = 0;
  public static final int LED_GRAY_DIM = 1;
  public static final int LED_GRAY = 2;
  public static final int LED_WHITE = 3;
  public static final int LED_PINK = 4;
  public static final int LED_RED = 5; // And 60
  public static final int LED_RED_HALF = 6;
  public static final int LED_RED_DIM = 7;
  public static final int LED_WARM_WHITE = 8;
  public static final int LED_ORANGE = 9; // And 61
  public static final int LED_ORANGE_HALF = 10;
  public static final int LED_ORANGE_DIM = 11;
  public static final int LED_PALE_YELLOW = 12;
  public static final int LED_YELLOW = 13;
  public static final int LED_YELLOW_HALF = 14;
  public static final int LED_YELLOW_DIM = 15;
  public static final int LED_PALE_GREEN_BLUE = 16;
  public static final int LED_GREEN_HALF = 17; // And 18, 22, 25, 26
  public static final int LED_GREEN = 19; /// And 23, 27
  public static final int LED_PALE_BLUE_GREEN = 20; // And 24
  public static final int LED_GREEN_DIM = 21;
  public static final int LED_AQUA = 28;
  public static final int LED_AQUA_HALF = 29; // And 30
  public static final int LED_AQUA_DIM = 31;
  public static final int LED_BLUE_AQUA = 32;
  public static final int LED_BLUE_AQUA2 = 33; // If you have a better name, send a PR
  public static final int LED_BLUE_AQUA_HALF = 34;
  public static final int LED_BLUE_AQUA_DIM = 35;
  public static final int LED_SKY = 36;
  public static final int LED_AZURE = 37;
  public static final int LED_AZURE_HALF = 38;
  public static final int LED_AZURE_DIM = 39;
  public static final int LED_PERIWINKLE = 40;
  // 41-47 are more shades of blue than I can name
  public static final int LED_LAVENDER = 48;
  public static final int LED_PURPLE = 49;
  public static final int LED_PURPLE_HALF = 50;
  public static final int LED_PURPLE_DIM = 51;
  public static final int LED_PALE_MAGENTA = 52;
  public static final int LED_MAGENTA = 53;
  public static final int LED_MAGENTA_HALF = 54;
  public static final int LED_MAGENTA_DIM = 55;
  public static final int LED_MAGENTA_PINK = 56;
  public static final int LED_HOT_PINK = 57;
  public static final int LED_HOT_PINK_HALF = 58;
  public static final int LED_HOT_PINK_DIM = 59;
  public static final int LED_GOLDENROD = 62;
  public static final int LED_LAWN_GREEN = 63;
  // There are 64 more colors, but they seem like dupes.
  // There might be a small number of slightly unique ones in that range.

  // Holds the colors that will be sent out to the device. This array
  // starts with (7,0), (7,1), ... (7,7), (6,0) ... (0, 7) of the LEFT page,
  // and then the same ordering for the RIGHT page. In other words, if you're
  // looking at the device, the buttons correspond to what you see here onscreen.
  private static final int[] buttonColors = {
    LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA,
    LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA,
    LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA, LED_AQUA,
    LED_RED, LED_ORANGE, LED_YELLOW, LED_GREEN, LED_BLUE_AQUA, LED_AZURE, LED_MAGENTA, LED_WHITE,
    LED_RED_DIM, LED_ORANGE_DIM, LED_YELLOW_DIM, LED_GREEN_DIM, LED_BLUE_AQUA_DIM, LED_AZURE_DIM, LED_MAGENTA_DIM, LED_GRAY_DIM,
    LED_RED, LED_ORANGE, LED_YELLOW, LED_GREEN, LED_BLUE_AQUA, LED_AZURE, LED_MAGENTA, LED_WHITE,
    LED_RED_DIM, LED_ORANGE_DIM, LED_YELLOW_DIM, LED_GREEN_DIM, LED_BLUE_AQUA_DIM, LED_AZURE_DIM, LED_MAGENTA_DIM, LED_GRAY_DIM,
    LED_RED, LED_ORANGE, LED_YELLOW, LED_GREEN, LED_BLUE_AQUA, LED_AZURE, LED_MAGENTA, LED_WHITE,

    LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY,
    LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY,
    LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY,
    LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY,
    LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY,
    LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY,
    LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY,
    LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY, LED_GRAY,
  };

  private final MF64LoggerPattern logger = new MF64LoggerPattern(this);
  private final MF64FlashPattern flash = new MF64FlashPattern(this);
  private final MF64RingPattern ring = new MF64RingPattern(this);
  private final MF64SpiralSquares ssquare = new MF64SpiralSquares(this);
  private final MF64RandomPanel randomPanel = new MF64RandomPanel(this);
  private final MF64EdgeSparks eSparks = new MF64EdgeSparks(this);

  private final TEMidiFighter64Subpattern[] patterns = {
    logger, logger, logger, logger, logger, logger, logger, logger,
    logger, logger, logger, logger, logger, logger, logger, logger,
    logger, logger, logger, logger, logger, logger, logger, logger,
    eSparks, eSparks, eSparks, eSparks, eSparks, eSparks, eSparks, eSparks,
    randomPanel, randomPanel, randomPanel, randomPanel, randomPanel, randomPanel, randomPanel, randomPanel,
    ssquare, ssquare, ssquare, ssquare, ssquare, ssquare, ssquare, ssquare,
    ring,   ring, ring, ring, ring, ring, ring, ring,
    flash,  flash,  flash,  flash,  flash,  flash,  flash,  flash,

    logger, logger, logger, logger, logger, logger, logger, logger,
    logger, logger, logger, logger, logger, logger, logger, logger,
    logger, logger, logger, logger, logger, logger, logger, logger,
    logger, logger, logger, logger, logger, logger, logger, logger,
    logger, logger, logger, logger, logger, logger, logger, logger,
    logger, logger, logger, logger, logger, logger, logger, logger,
    logger, logger, logger, logger, logger, logger, logger, logger,
    logger, logger, logger, logger, logger, logger, logger, logger,
  };

  private LXMidiInput midiIn = null;
  private LXMidiOutput midiOut = null;

  public final BooleanParameter fakePush =
          new BooleanParameter("Push", false)
                  .setMode(BooleanParameter.Mode.MOMENTARY)
                  .setDescription("Simulates pushing the top-left button");

  public final DiscreteParameter pokeChannel =
          new DiscreteParameter("Channel", 0, 99)
                  .setDescription("Channel number");

  public final DiscreteParameter pokeVelocity =
          new DiscreteParameter("Vel", 0, 256)
                  .setDescription("Velocity");

  public final DiscreteParameter pokePitch =
          new DiscreteParameter("Pitch", 0, 256)
                  .setDescription("Pitch");

  public final BooleanParameter pokeButton =
          new BooleanParameter("Poke", false)
                  .setMode(BooleanParameter.Mode.MOMENTARY)
                  .setDescription("Sends a MIDI note to the device");

  // Converts a MIDI note from the MF64 into information about which
  // button was pressed
  public static class Mapping {
    // If an unexpected note is received, this is set to false
    // and all other attributes should be ignored.
    public boolean valid;
    // The MF64 has left and right virtual button pages.
    public enum Page {
      LEFT, RIGHT;
    }
    public Page page;
    // 0 is the bottom row, 7 the top.
    public int row;
    // 0 is the left column, 7 the right
    public int col;

    public Mapping() {}

    public void map(MidiNote note) {
      int pitch = note.getPitch();
      int channel = note.getChannel();
      LX.log("Channel=" + channel + " pitch=" + pitch + " vel=" + note.getVelocity());
      this.valid = true;
      if (channel == 2) {
        this.page = Page.LEFT;
      } else if (channel == 1) {
        this.page = Page.RIGHT;
      } else {
        LX.warning("Got wild-channel MIDI note " + note);
        this.valid = false;
        return;
      }

      if (pitch >= 36 && pitch <= 67) {
        this.row = (pitch / 4) - 9;
        this.col = pitch % 4;
      } else if (pitch >= 68 && pitch <= 99) {
        this.row = (pitch / 4) - 17;
        this.col = pitch % 4 + 4;
      } else {
        LX.warning("Got wild-pitch MIDI note " + note);
        this.valid = false;
      }
    }
  }

  private final Mapping mapping = new Mapping();

  public TEMidiFighter64DriverPattern(LX lx) {
    super(lx);

    addParameter("fakePush", this.fakePush);

    addParameter("pokeChannel", this.pokeChannel);
    addParameter("pokeVelocity", this.pokeVelocity);
    addParameter("pokePitch", this.pokePitch);
    addParameter("poke", this.pokeButton);

    this.fakePush.addListener((p) -> {
      this.mapping.page = Mapping.Page.LEFT;
      this.mapping.row = 7;
      this.mapping.col = 0;

      if (p.getValuef() != 0f) {
        this.patterns[0].buttonDown(this.mapping);
      }
      else {
        this.patterns[0].buttonUp(this.mapping);
      }
    });

    this.pokeButton.addListener((p) -> {
      this.midiOut.sendNoteOn(this.pokeChannel.getValuei(), this.pokePitch.getValuei(),
              this.pokeVelocity.getValuei());
    });

  }

  private void sendAllOff() {
    for (int channel = 2; channel >= 1; channel--) {
      for (int i = 0; i < 64; i++) {
        this.midiOut.sendNoteOn(channel, pitchFromXY[i], LED_OFF);
      }
    }
  }

  private void sendColors() {
    // Channel 2 is the left page
    for (int channel = 2; channel >= 1; channel--) {
      for (int i = 0; i < 64; i++) {
        int row = 7 - (i / 8);
        int col = i % 8;
        int ci = row * 8 + col + (channel == 2 ? 0 : 64);
        this.midiOut.sendNoteOn(channel, pitchFromXY[i], buttonColors[ci]);
      }
    }
  }

  @Override
  public void onActive() {
    for (LXMidiInput lmi : lx.engine.midi.inputs) {
      if (lmi.getName().equals(MIDI_NAME)) {
        if (this.midiIn != null) {
          LX.log("Multiple " + MIDI_NAME + " inputs found; will use the first.");
        } else {
          this.midiIn = lmi;
          lmi.open();
          lmi.addListener(this);
        }
      }
    }

    for (LXMidiOutput lmo : lx.engine.midi.outputs) {
      if (lmo.getName().equals(MIDI_NAME)) {
        if (this.midiOut != null) {
          LX.log("Multiple " + MIDI_NAME + " outputs found; will use the first.");
        } else {
          this.midiOut = lmo;
          lmo.open();
          sendColors();
        }
      }
    }

    if (this.midiIn == null && this.midiOut == null) {
      LX.log("Couldn't find any " + MIDI_NAME + " MIDI device");
    } else if (this.midiIn == null) {
      LX.log("Couldn't find any " + MIDI_NAME + " MIDI input");
    } else if (this.midiOut == null) {
      LX.log("Couldn't find any " + MIDI_NAME + " MIDI output");
    }
  }

  @Override
  public void onInactive() {
    if (this.midiIn != null) {
      this.midiIn.removeListener(this);
    }
    if (this.midiOut != null) {
      sendAllOff();
    }
    this.midiIn = null;
    this.midiOut = null;
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    this.mapping.map(note);
    int patternIndex = mapping.page == Mapping.Page.LEFT ? 0 : 64;
    patternIndex += (7 - mapping.row) * 8;
    patternIndex += mapping.col;
    this.patterns[patternIndex].buttonDown(this.mapping);
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    this.mapping.map(note);
    int patternIndex = mapping.page == Mapping.Page.LEFT ? 0 : 64;
    patternIndex += (7 - mapping.row) * 8;
    patternIndex += mapping.col;
    this.patterns[patternIndex].buttonUp(this.mapping);
  }

  @Override
  public void run(double deltaMs) {
    this.logger.run(deltaMs, colors);
    this.flash.run(deltaMs, colors);
    this.ring.run(deltaMs,colors);
    this.ssquare.run(deltaMs,colors);
    this.randomPanel.run(deltaMs,colors);
    this.eSparks.run(deltaMs,colors);
  }
}
