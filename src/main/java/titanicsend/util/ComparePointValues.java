package titanicsend.util;

import heronarts.lx.model.LXPoint;

import java.util.function.Function;
import java.util.Comparator;

// ComparePointValues is a comparator that can be used to find e.g. the min/max
// distance between any two x, y, or z coordinates between any two points. It
// returns two LXPoints withIt can be used like this:
//   ArrayList<LXPoint> pointsList = new ArrayList<>(Arrays.asList(this.points));
// 
//   LXPoint maxZValuePoint = pointsList.stream().max(Comparator.comparing(p -> p.z)).get();
//   LXPoint minZValuePoint = pointsList.stream().min(Comparator.comparing(p -> p.z)).get();
public class ComparePointValues implements Comparator<LXPoint> {
    private Function<LXPoint,Float> attributeGetter;

    public ComparePointValues(Function<LXPoint, Float> attributeGetter) {
        this.attributeGetter = attributeGetter;
    }

    public int compare(LXPoint a, LXPoint b) {
        if (this.attributeGetter.apply(a) > this.attributeGetter.apply(b))
            return -1; // highest value first
        if (this.attributeGetter.apply(a) == this.attributeGetter.apply(b))
            return 0;
        return 1;
    }
}