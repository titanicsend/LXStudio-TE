package titanicsend.model;

public class TEStripingInstructions {
  public String startingEdgeId;

  // One per universe; can be null which means they're all 500
  public int[] universeLengths;

  // One per row
  public int[] rowLengths;
  public int[] beforeNudges; // No need for afterNudges; they were factored into rowLengths
  public int[] gaps;

  public TEStripingInstructions(String startingEdgeId, int[] universeLengths,
                                int[] rowLengths, int[] beforeNudges,
                                int[] gaps) {
    assert startingEdgeId != null;
    assert rowLengths != null;
    assert beforeNudges != null;
    assert gaps != null;

    this.startingEdgeId = startingEdgeId;
    this.universeLengths = universeLengths;
    this.rowLengths = rowLengths;
    this.beforeNudges = beforeNudges;
    this.gaps = gaps;
  }
}
