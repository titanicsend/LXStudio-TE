package titanicsend.model;

public class TEStripingInstructions {
  public int startingVertex;

  // One per universe
  public int[] universeLengths;

  // One per row
  public int[] rowLengths;
  public int[] beforeNudges; // No need for afterNudges; they were factored into rowLengths
  public int[] gaps;

  public TEStripingInstructions(int startingVertex, int[] universeLengths,
                                int[] rowLengths, int[] beforeNudges,
                                int[] gaps) {
    this.startingVertex = startingVertex;
    this.universeLengths = universeLengths;
    this.rowLengths = rowLengths;
    this.beforeNudges = beforeNudges;
    this.gaps = gaps;
  }
}
