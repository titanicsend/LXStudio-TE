package titanicsend.app.autopilot;

import heronarts.lx.LX;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.pattern.LXPattern;
import titanicsend.app.autopilot.utils.TEMixerUtils;
import titanicsend.util.TE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TEPatternLibrary {
    private LX lx;
    private ArrayList<TEPatternRecord> patternRecords;

    // record -> list of patterns that match (on a single channel)
    private HashMap<TEPatternRecord, ArrayList<LXPattern>> rec2patterns = null;

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
        public Class<? extends LXPattern> patternClass;
        public TEPatternCoverageType coverageType;
        public TEPatternColorCategoryType colorCategoryType;
        public TEPhrase phraseType;

        public TEPatternRecord(
                Class<? extends LXPattern> p,
                TEPatternCoverageType c,
                TEPatternColorCategoryType cc,
                TEPhrase ph) {
            patternClass = p;
            coverageType = c;
            colorCategoryType = cc;
            phraseType = ph;
        }

        public String toString() {
            String[] parts = patternClass.toString().split(".");
            String classNameCleaned = patternClass.toString();
            try {
                classNameCleaned = parts[parts.length - 1];
            } catch (ArrayIndexOutOfBoundsException e) {
                //TE.err("Could not write name for class = \"%s\"", classNameCleaned);
            }
            return String.format("<Record class=\"%s\", cov=%s, phrase=%s>",
                    classNameCleaned, coverageType, phraseType);
        }
    }

    public TEPatternLibrary(LX lx) {
        this.lx = lx;
        this.patternRecords = new ArrayList<TEPatternRecord>();
    }

    public void addPattern(
            Class<? extends LXPattern> p,
            TEPatternCoverageType c,
            TEPatternColorCategoryType cc,
            TEPhrase ph) {

        TEPatternRecord rec = new TEPatternRecord(p, c, cc, ph);
        this.patternRecords.add(rec);
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
     * @param ph if null, throw exception. we have to have a phrase type!
     * @return ArrayList<LXPattern>
     */
    public ArrayList<LXPattern> filterPatterns(
            TEPatternCoverageType c, TEPatternColorCategoryType cc, TEPhrase ph) throws Exception {
        // do some checks
        if (!this.isReady())
            throw new Exception("Cannot filter patterns, you need to call indexPatterns() first!");
        if (ph == null)
            throw new Exception("Must specify phrase type!");

        // filter records
        Stream<TEPatternRecord> s = patternRecords.stream().filter(r -> r.phraseType == ph);
        if (c != null)
            s = s.filter(r -> r.coverageType == c);
        if (cc != null)
            s = s.filter(r -> r.colorCategoryType == cc);

        ArrayList<TEPatternRecord> matchingRecords = s.collect(Collectors.toCollection(ArrayList::new));

        // now for each record, pull in the corresponding pattern(s) and add to a list
        ArrayList<LXPattern> matchingPatterns = new ArrayList<>();
        for (TEPatternRecord r : matchingRecords) {
            for (LXPattern p : this.rec2patterns.get(r)) {
                matchingPatterns.add(p);
            }
        }

        return matchingPatterns;
    }

    public LXPattern pickRandomPattern(TEPhrase phraseType) throws Exception {
        // filter patterns
        ArrayList<LXPattern> matchingPatterns = this.filterPatterns(null, null, phraseType);

        // randomly pick one
        Random rand = new Random();
        int randomIndex = rand.nextInt(matchingPatterns.size());

        return matchingPatterns.get(randomIndex);
    }

    /**
     * Is this pattern library ready to be used? (indexPatterns() has been called)
     *
     * @return boolean
     */
    public boolean isReady() {
        return rec2patterns != null;
    }

    /**
     * Gets TEPatternLibrary ready for actual use
     *
     * Library has a list of TEPatternRecord, each of which need to be
     * mapped to one (or more!) LXPattern objects on an LXChannel.
     *
     * Each TEPatternRecord should only map to LXPatterns on the same
     * LXChannel.
     */
    public void indexPatterns() {
        this.rec2patterns = new HashMap<>();

        // report back some statistics on patterns indexed or not found
        HashMap<TEPhrase, Integer> patternsPerPhrase = new HashMap<>();
        HashMap<TEPhrase, Integer> missingPatternsPerPhrase = new HashMap<>();
        int totalFound = 0;
        int totalNotFound = 0;

        for (TEPatternRecord r : this.patternRecords) {
            TEChannelName name = TEMixerUtils.getChannelNameFromPhraseType(r.phraseType);
            LXChannel ch = TEMixerUtils.getChannelByName(lx, name);
            if (ch == null)
                TE.err("[TEPatternLibrary] Could not load channel=%s, it is null", name);

            int found = 0;
            rec2patterns.put(r, new ArrayList<>());
            for (LXPattern p : ch.getPatterns()) {
                if (r.patternClass == p.getClass()) {
                    rec2patterns.get(r).add(p);

                    // count by phrase type
                    if (!patternsPerPhrase.containsKey(r.phraseType))
                        patternsPerPhrase.put(r.phraseType, 0);
                    patternsPerPhrase.put(r.phraseType, patternsPerPhrase.get(r.phraseType) + 1);

                    //TE.log("[TEPatternLibrary] Index pattern=%s to channel=%s, record=%s", p.getLabel(), name, r);
                    found++;
                }
            }

            if (found == 0) {
                TE.err("No LXPattern found for %s (channel=%s). Either add to AutoVJ.lxp, or remove from TEApp::initializePatternLibrary()", r, name);
                if (!missingPatternsPerPhrase.containsKey(r.phraseType))
                    missingPatternsPerPhrase.put(r.phraseType, 0);
                missingPatternsPerPhrase.put(r.phraseType, missingPatternsPerPhrase.get(r.phraseType) + 1);
                totalNotFound++;
            }

            totalFound += found;
        }

        // print a summary of indexed patterns
        if (totalFound > 0) {
            TE.log("[AutoVJ] Indexed %d total patterns!", totalFound);
            for (Map.Entry<TEPhrase, Integer> entry : patternsPerPhrase.entrySet()) {
                TEPhrase pt = entry.getKey();
                int count = entry.getValue();
                TE.log("\tChannel=%s has %d indexed patterns", TEMixerUtils.getChannelNameFromPhraseType(pt), count);
            }
        }

        if (totalNotFound > 0) {
            TE.log("[AutoVJ] %d total MISSING patterns", totalNotFound);
            for (Map.Entry<TEPhrase, Integer> entry : missingPatternsPerPhrase.entrySet()) {
                TEPhrase pt = entry.getKey();
                int count = entry.getValue();
                TE.log("\tChannel=%s has %d missing patterns", TEMixerUtils.getChannelNameFromPhraseType(pt), count);
            }
        }
    }
}
