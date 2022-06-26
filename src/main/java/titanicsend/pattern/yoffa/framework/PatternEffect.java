package titanicsend.pattern.yoffa.framework;

import heronarts.lx.Tempo;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPattern;
import titanicsend.util.Dimensions;

import java.util.*;

public abstract class PatternEffect {

    protected final TEAudioPattern pattern;
    protected final Map<LXPoint, Dimensions> pointsToCanvas;
    private boolean shouldBlend;
    private long startTime = System.currentTimeMillis();


    public PatternEffect(PatternTarget target) {
        this.pattern = target.pattern;
        this.pointsToCanvas = target.pointsToCanvas;
    }

    public final void onActive() {
        startTime = System.currentTimeMillis();
        onPatternActive();
    }

    protected void onPatternActive() {}

    protected void onParameterChanged(LXParameter parameter) {}

    protected void onPatternInactive() {}

    public abstract void run(double deltaMs);

    public abstract Collection<? extends LXParameter> getParameters();

    public boolean hasParameters() {
        return getParameters() != null && getParameters().size() > 0;
    }

    public boolean shouldBlend() {
        return shouldBlend;
    }

    public PatternEffect setShouldBlend(boolean shouldBlend) {
        this.shouldBlend = shouldBlend;
        return this;
    }

    //must be thread safe
    protected void setColor(LXPoint point, int color) {
        int[] colors = pattern.getColors();
        if (shouldBlend()) {
            colors[point.index] = LXColor.blend(colors[point.index], color, LXColor.Blend.ADD);
        } else {
            colors[point.index] = color;
        }
    }

    protected double getDurationSec() {
        return (System.currentTimeMillis() - startTime) / 1000.;
    }

    protected Set<LXPoint> getAllPoints() {
        return pointsToCanvas.keySet();
    }

    protected Tempo getTempo() {
        return pattern.getTempo();
    }

}
