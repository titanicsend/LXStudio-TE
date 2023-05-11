package titanicsend.pattern.yoffa.framework;

import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.TEPattern;

public class PatternTarget {

    TEPerformancePattern pattern;
    public final TEShaderView defaultView;
    public TEPattern.ColorType colorType = TEPattern.ColorType.PRIMARY;

    public PatternTarget(TEPerformancePattern pattern, TEShaderView defaultView) {
        this.pattern = pattern;
        this.defaultView = defaultView;
    }

    public PatternTarget(TEPerformancePattern pattern, TEShaderView defaultView, TEPattern.ColorType ct) {
        this(pattern, defaultView);
        this.colorType = ct;
    }

}
