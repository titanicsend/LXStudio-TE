package titanicsend.app.autopilot;

import heronarts.lx.LX;
import heronarts.lx.pattern.LXPattern;

import java.util.ArrayList;

public class TEPatternLibrary {
    private LX lx;
    private ArrayList<TEPatternRecord> patterns;

    /**
     * For patterns we catalog, how do they cover the cor?
     */
    public enum TEPatternCoverageType { PANELS, PANELS_PARTIAL, EDGES, BOTH; }

    /**
     * For patterns we catalog, can they conform to
     * the current palette settings? Or not?
     */
    public enum TEPatternColorCategoryType { PALETTE, WHITE, NONCONFORMING; }

    public class TEPatternRecord {
        public Class<? extends LXPattern> pattern;
        public TEPatternCoverageType coverageType;
        public TEPatternColorCategoryType colorCategoryType;
        public TEPhrase phraseType;

        public TEPatternRecord(
                Class<? extends LXPattern> p,
                TEPatternCoverageType c,
                TEPatternColorCategoryType cc,
                TEPhrase ph) {
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

    public void addPattern(
            Class<? extends LXPattern> p,
            TEPatternCoverageType c,
            TEPatternColorCategoryType cc,
            TEPhrase ph) {

        TEPatternRecord rec = new TEPatternRecord(p, c, cc, ph);
        this.patterns.add(rec);
        try {
            this.lx.registry.addPattern(p);
        } catch (IllegalStateException e) {
            // Pattern already added to LX registry! Ignore.
            // This allows us to add a different set of patterns
            // from what are registered for manual VJ TE operation.
        }
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
