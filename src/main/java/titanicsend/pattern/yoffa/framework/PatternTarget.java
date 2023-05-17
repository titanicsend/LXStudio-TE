package titanicsend.pattern.yoffa.framework;

import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.TEPattern;

public class PatternTarget {

    TEPerformancePattern pattern;
    public TEPattern.ColorType colorType = TEPattern.ColorType.PRIMARY;

    public PatternTarget(TEPerformancePattern pattern) {
        this.pattern = pattern;
    }

    public PatternTarget(TEPerformancePattern pattern, TEPattern.ColorType ct) {
        this(pattern);
        this.colorType = ct;
    }

}
