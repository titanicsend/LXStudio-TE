package titanicsend.app.autopilot.events;

/**
 * Tracks when we effectively change the master deck based on fader value position on the mixer.
 *
 * <p>This is important because ShowKontrol is really bad about sending phrase messages when master
 * deck changes -- we get a large build up of phrase messages that causes issues with AutoVJ
 * otherwise!
 */
public class TEMasterChangeEvent {
  private long timestamp; // when this change happened
  private int deckNum; // deck num with this new fader val
  private int faderVal; // faderVal changed to

  public TEMasterChangeEvent(long timestamp, int deckNum, int faderVal) {
    this.timestamp = timestamp;
    this.deckNum = deckNum;
    this.faderVal = faderVal;
  }

  public int getDeckNum() {
    return deckNum;
  }

  public int getFaderVal() {
    return faderVal;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
