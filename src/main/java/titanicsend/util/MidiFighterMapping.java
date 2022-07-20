package titanicsend.util;

// Midi Fighter manual:
// https://drive.google.com/file/d/0B-QvIds_FsH3WDBNWXUxWTlGVlU/view?resourcekey=0-eGf57BdEMP8GB2TaanYccg

/*
  MidiFighter in "Notes" mode sends these pitches:

  64 65 66 67  96 97 98 99
  60 61 62 63  92 93 94 95
  56 57 58 59  88 89 90 91
  52 53 54 55  84 85 86 87
  48 49 50 51  80 81 82 83
  44 45 46 47  76 77 78 79
  40 41 42 43  72 73 74 75
  36 37 38 39  68 69 70 71
 */

import heronarts.lx.LX;
import heronarts.lx.midi.MidiNoteOn;

public class MidiFighterMapping {
  public enum Page {
    LEFT, RIGHT;
  }
  public boolean valid;
  public Page page;
  public int row;  // 0=bottom, 7=top
  public int col;  // 0=left, 7=right

  public MidiFighterMapping(MidiNoteOn note) {
    int pitch = note.getPitch();
    int channel = note.getChannel();
    this.valid = true;
    if (channel == 2) {
      this.page = Page.LEFT;
    } else if (channel == 1) {
      this.page = Page.RIGHT;
    } else {
      LX.log("Got wild-channel MIDI note " + note.toString());
      this.valid = false;
    }

    if (this.valid) {
      if (pitch >= 36 && pitch <= 67) {
        this.row = (pitch / 4) - 9;
        this.col = pitch % 4;
      } else if (pitch >= 68 && pitch <= 99) {
        this.row = (pitch / 4) - 17;
        this.col = pitch % 4 + 4;
      } else {
        LX.log("Got wild-pitch MIDI note " + note.toString());
        this.valid = false;
      }
    }
  }
}
