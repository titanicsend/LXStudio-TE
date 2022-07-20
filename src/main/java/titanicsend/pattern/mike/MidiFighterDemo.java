package titanicsend.pattern.mike;

import heronarts.lx.LX;
import heronarts.lx.midi.LXMidiListener;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNoteOn;
import titanicsend.pattern.TEPattern;
import titanicsend.util.MidiFighterMapping;

public class MidiFighterDemo extends TEPattern implements LXMidiListener {

  public MidiFighterDemo(LX lx) {
    super(lx);
  }

  public void noteOnReceived(MidiNoteOn note) {
    MidiFighterMapping mapping = new MidiFighterMapping(note);
    if (mapping.valid) {
      String pageStr = mapping.page == MidiFighterMapping.Page.LEFT ? "left" : "right";
      LX.log("MIDI Fighter page=" + pageStr + " row=" + mapping.row + " col=" + mapping.col);
    }
  }

  public void controlChangeReceived(MidiControlChange cc) {
    LX.log("Got cc " + cc.toString());
  }

  public void run(double deltaMs) {

  }
}
