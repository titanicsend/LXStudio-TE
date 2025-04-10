package titanicsend.app.autopilot.events;

import titanicsend.app.autopilot.TEPhrase;

/**
 * Struct for holding OSC phrase events, what kind they are, and when they occurred.
 *
 * <p>Useful for looking at past phrases in order to predict or plan for future ones.
 */
public class TEPhraseEvent {
  private final long startedAtMs;
  private final TEPhrase phraseType;
  private final double bpm;

  private int numBeatsLong = 0; // set this after completed!

  public TEPhraseEvent(TEPhrase phraseType, double bpm) {
    this.startedAtMs = System.currentTimeMillis();
    this.phraseType = phraseType;
    this.bpm = bpm;
  }

  public TEPhraseEvent(long startedAt, TEPhrase phraseType, double bpm) {
    this.startedAtMs = startedAt;
    this.phraseType = phraseType;
    this.bpm = bpm;
  }

  public void addBeat() {
    numBeatsLong += 1;
  }

  public void setNumBeatsLong(int n) {
    numBeatsLong = n;
  }

  public double getBpm() {
    return bpm;
  }

  public TEPhrase getPhraseType() {
    return phraseType;
  }

  public long getStartedAtMs() {
    return startedAtMs;
  }
}
