package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPattern;
import titanicsend.util.TEMath;

/*
 *  TempoReactive shows how to use what LX knows about the current
 *  tempo, and the timing of beats.
 *
 *  BassReactive responds to the realtime analyzed audio stream, but we will
 *  have a more reliable source of information about the current song's
 *  tempo from the DJ's decks. They perform more advanced tempo detection
 *  and allow a DJ to set a specific defined tempo. That metronome frequency
 *  is expressed as BPM (beats per minute). Its phase in time is set with a
 *  "tap" or `trigger()`. In addition, a downbeat is the first beat in a
 *  measure. LX can sync its internal tempo metronome to an external source
 *  and consume a signal about which beat is the downbeat (the start of a measure).
 *
 *  Our art direction requires that patterns are responsive to both tempo AND
 *  sound data. This pattern only demonstrates tempo responsiveness.
 *
 *  A pulse will travel across each edge once per 4-beat measure. Turning up
 *  the energy parameter will make the pulse longer, and begin to pulse the
 *  travelling motion itself to the individual beats, not just the measure.
 */

@LXCategory("TE Examples")
public class TempoReactiveEdge extends TEPattern {
  // Titanic's End wants all patterns to implement a parameter called "Energy",
  // which controls the amount of motion, light, movement, action, particles, etc.
  public final CompoundParameter energy =
      new CompoundParameter("Energy", .1, 0, 1)
          .setDescription("Pulse width and beat tween movement suddenness");

  public TempoReactiveEdge(LX lx) {
    super(lx);
    addParameter("energy", energy);
  }

  public void run(double deltaMs) {
    clearPixels(); // Sets all pixels to transparent for starters

    // 0..1 ramp (sawtooth) of current position within a 4-beat measure.
    double measure = wholeNote();

    // 0, 1, 2, 3, 0, 1, 2, 3 etc. via floored int casting (truncates decimal portion)
    int beatInMeasure = (int) (measure * 4);
    /* Ccould also do lx.engine.tempo.beatCount() % 4)
     * And instead of 4, we could use lx.engine.tempo.beatsPerMeasure.getValue()
     * However, name the last 3:4 or 6:8 song with a beat you heard on playa.
     * Some house feels 2:4; I'm not sure if ShowKontrol will give us beats per measure
     */

    // 0..1 ramp (sawtooth) of current position in a quarter note beat.
    double beat = lx.engine.tempo.basis();

    // Max width of the traveling pulse, expressed as a fractional percentage of this edge, up to
    // 37.5%
    double pulseWidthFrac = energy.getNormalized() * 3 / 8;

    /* Given a `beat` from 0..1, calculate the position (also 0..1) within
     *  this beat returning a curve with an "ease-out" motion; this is swing.
     *  The shape of the curve is variable. The end effect is that the
     *  motion of the travelling pulse starts to pulse on-beat as we
     *  turn up the energy.
     *  See https://www.desmos.com/calculator/uasacds4ra
     */

    double swingBeat = TEMath.easeOutPow(beat, energy.getNormalized());

    /* Pause to convince yourself this would be the pulse head's position
     * before any swing (movement pulsing with each beat). This is the head
     * position from 0 to (100% + pulseWidthFrac).
     * This line is instructional, not used. We'll modify this in a second.
     */
    // double pulseHeadFrac = measure * (1 + pulseWidthFrac);

    // Now, pulse's head position WITH quarter note swing, still
    // from 0 to (100% + pulseWidthFrac).
    // Compare to pulseHeadFrac above.
    double pulseHeadFrac = (beatInMeasure + swingBeat) / 4 * (1 + pulseWidthFrac);

    for (TEEdgeModel edge : modelTE.getEdges()) {
      double pulseTailFrac = pulseHeadFrac - pulseWidthFrac;

      for (TEEdgeModel.Point point : edge.edgePoints) {
        // point.frac is the 0..1 fractional percentage this point is into its edge
        if (point.frac >= pulseTailFrac && point.frac < pulseHeadFrac)
          colors[point.point.index] = LXColor.WHITE;
      }
    }
  }
}
