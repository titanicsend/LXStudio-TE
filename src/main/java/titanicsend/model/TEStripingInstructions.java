package titanicsend.model;

public class TEStripingInstructions {
  public String startingEdgeId;

  public static final int DEFAULT_CHANNEL_LENGTH = 500;

  // One per universe; can be null which means they're all DEFAULT_CHANNEL_LENGTH
  public int[] channelLengths;

  // One per row
  public int[] rowLengths;
  public int[] beforeNudges; // No need for afterNudges; they were factored into rowLengths
  public int[] gaps;

  public TEStripingInstructions(String startingEdgeId, int[] channelLengths,
                                int[] rowLengths, int[] beforeNudges,
                                int[] gaps) {
    assert startingEdgeId != null;
    assert rowLengths != null;
    assert beforeNudges != null;
    assert gaps != null;

    if (startingEdgeId == "") {
      throw new IllegalArgumentException("Refusing to allow striping instructions with empty starting edge ID");
    }

    this.startingEdgeId = startingEdgeId;
    this.channelLengths = channelLengths;
    this.rowLengths = rowLengths;
    this.beforeNudges = beforeNudges;
    this.gaps = gaps;
  }
}
