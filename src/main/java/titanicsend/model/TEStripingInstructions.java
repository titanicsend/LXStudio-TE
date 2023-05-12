package titanicsend.model;

public class TEStripingInstructions {
  public static final int DEFAULT_CHANNEL_LENGTH = 250;

  // One per universe; can be null which means they're all DEFAULT_CHANNEL_LENGTH
  public int[] channelLengths;

  // One per row
  public int[] rowLengths;
  public int[] beforeNudges; // No need for afterNudges; they were factored into rowLengths
  public int[] gaps;

  public TEStripingInstructions(int[] channelLengths,
                                int[] rowLengths, int[] beforeNudges,
                                int[] gaps) {
    assert rowLengths != null;
    assert beforeNudges != null;
    assert gaps != null;

    this.channelLengths = channelLengths;
    this.rowLengths = rowLengths;
    this.beforeNudges = beforeNudges;
    this.gaps = gaps;
  }
}
