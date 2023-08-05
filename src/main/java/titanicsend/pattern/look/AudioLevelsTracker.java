package titanicsend.pattern.look;

import heronarts.lx.audio.GraphicMeter;
import org.opencv.core.Mat;
import titanicsend.util.TEMath;

import java.util.ArrayList;
import java.util.List;

public class AudioLevelsTracker {
    private final GraphicMeter eq;

    private final List<Mat.Tuple2<Integer>> bandRanges = new ArrayList<>();
    private final List<Float> minimums = new ArrayList<>();
    private final List<Float> maximums = new ArrayList<>();
    private final List<TEMath.EMA> movingAvgs = new ArrayList<>();

    public AudioLevelsTracker(GraphicMeter eq) {
        this.eq = eq;
    }

    public void addBandRange(int start, int end) {
        bandRanges.add(new Mat.Tuple2<>(start, end));
        System.out.printf("[AudioLevelsTracker.addBandRange] idx=%s, start=%s, end=%s\n", bandRanges.size()-1, start, end);
        minimums.add(Float.POSITIVE_INFINITY);
        maximums.add(Float.NEGATIVE_INFINITY);
        movingAvgs.add(new TEMath.EMA(0.0, 0.01));
    }

    public float sample(int idx, double deltaMs) {
        Mat.Tuple2<Integer> loBand = bandRanges.get(idx);
        float curSample = eq.getAveragef(loBand.get_0(), loBand.get_1());
//        float curSample = eq.getAveragef(0, halfNBands);
        movingAvgs.get(idx).update(curSample, deltaMs);
        if (curSample < minimums.get(idx)) {
            minimums.set(idx, curSample);
        }
        if (curSample > maximums.get(idx)) {
            maximums.set(idx, curSample);
        }
        float runningAvgSample = movingAvgs.get(idx).getValuef();
        float curDifferenceFromAvg = runningAvgSample - curSample;
        float normSample = curSample / (maximums.get(idx) - minimums.get(idx));
        float scaledSample = normSample * 2 - 1;
        float iScaledSample = Math.abs(scaledSample)+0.05f;
//            System.out.printf("curSample = %s, runningAvgSample = %s, curDifferenceFromAvg = %s, normSample = %s, scaledSample = %s, iScaledSample = %s\n", curSample, runningAvgSample, curDifferenceFromAvg, normSample, scaledSample, iScaledSample);
        // TODO: return a struct with differently scaled vals? or allow client to do the scaling itself?
        return iScaledSample;
    }
}