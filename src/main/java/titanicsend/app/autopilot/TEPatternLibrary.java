package titanicsend.app.autopilot;

import heronarts.lx.LX;
import heronarts.lx.pattern.LXPattern;
import titanicsend.pattern.jeff.EdgeProgressions;

import java.util.ArrayList;

public class TEPatternLibrary {
    private LX lx;
    private ArrayList<TEPatternRecord> patterns;

    /**
     * For patterns we catalog, do they span panels,
     * edges, or both?
     */
    public enum TEPatternCoverageType { PANELS, EDGES, BOTH; }

    /**
     * For patterns we catalog, can they conform to
     * the current palette settings? Or not?
     */
    public enum TEPatternColorCategoryType { PALETTE, NONCONFORMING; }

    public class TEPatternRecord {
        public Class<? extends LXPattern> pattern;
        public TEPatternCoverageType coverageType;
        public TEPatternColorCategoryType colorCategoryType;
        public TEPhrase phraseType;

        public TEPatternRecord(Class<? extends LXPattern> p, TEPatternCoverageType c, TEPatternColorCategoryType cc, TEPhrase ph) {
            pattern = p;
            coverageType = c;
            colorCategoryType = cc;
            phraseType = ph;
        }
    }

    public TEPatternLibrary(LX lx) {
        this.lx = lx;
        this.patterns = new ArrayList<TEPatternRecord>();
    }

    public void addPattern(Class<? extends LXPattern> p, TEPatternCoverageType c, TEPatternColorCategoryType cc, TEPhrase ph) {
        TEPatternRecord rec = new TEPatternRecord(p, c, cc, ph);
        this.patterns.add(rec);
        this.lx.registry.addPattern(p);
    }

    /**
     * Get patterns in our TE library that match ONLY a certain set of criteria.
     *
     * @param c if non-null, return only patterns matching this coverage type
     * @param cc if non-null, return only patterns matching this color category type
     * @param ph if non-null, return only patterns matching this phrase type
     * @return an LXPattern class that can be instantiated and loaded onto an LXChannel
     */
    public Class<? extends LXPattern> filterPatterns(
            TEPatternCoverageType c, TEPatternColorCategoryType cc, TEPhrase ph) {
        return null; //TODO
    }
}
