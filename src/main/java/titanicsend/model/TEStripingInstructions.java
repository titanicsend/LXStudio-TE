package titanicsend.model;

public class TEStripingInstructions {
  public int startingVertex;

  // One per universe
  public int[] universeLengths;

  // One per row
  public int[] rowLengths;
  public int[] leftNudges;
  public int[] rightNudges;
  public int[] gaps;

  public TEStripingInstructions(int startingVertex, int[] universeLengths,
                                int[] rowLengths, int[] leftNudges, int[] rightNudges,
                                int[] gaps) {
    this.startingVertex = startingVertex;
    this.universeLengths = universeLengths;
    this.rowLengths = rowLengths;
    this.leftNudges = leftNudges;
    this.rightNudges = rightNudges;
    this.gaps = gaps;
  }
}
