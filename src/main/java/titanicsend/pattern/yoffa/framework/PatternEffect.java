package titanicsend.pattern.yoffa.framework;

import heronarts.lx.Tempo;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import java.util.*;
import titanicsend.pattern.TEPerformancePattern;

public abstract class PatternEffect {

    protected final TEPerformancePattern pattern;
    private boolean shouldBlend;

    public PatternEffect(PatternTarget target) {
        this.pattern = target.pattern;
    }

    public final void onActive() {
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

    // must be thread safe
    protected void setColor(LXPoint point, int color) {
        int[] colors = pattern.getColors();
        if (shouldBlend()) {
            colors[point.index] = LXColor.blend(colors[point.index], color, LXColor.Blend.ADD);
        } else {
            colors[point.index] = color;
        }
    }

    protected List<LXPoint> getPoints() {
        return this.pattern.getModel().getPoints();
    }

    protected Tempo getTempo() {
        return pattern.getTempo();
    }
}
