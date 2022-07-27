package titanicsend.app.autopilot;

import heronarts.lx.LX;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.pattern.LXPattern;
import titanicsend.app.autopilot.utils.TEMixerUtils;
import titanicsend.util.TE;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TEPatternLibrary {
    private LX lx;

    // list of all registered patterns with this pattern library
    private ArrayList<TEPatternRecord> patternRecords;

    // map pattern record -> list of patterns
    private HashMap<TEPatternRecord, ArrayList<LXPattern>> rec2patterns = null;

    // map a (phrase, pattern) -> pattern record
    private HashMap<PhrasePatternCompositeKey, TEPatternRecord> phrasePattern2rec = null;

    // record the number of bars particular patterns have played to
    // try to even out how many times we're picking certain patterns
    private HashMap<LXPattern, Double> patternHistoryCounter;
    private double numBarsTotal = 0.0;

    private float PROB_PICK_RANDOM_NEXT_PATTERN = 0.2f;

    public class PhrasePatternCompositeKey {
        public Class<? extends LXPattern> pattern;
        public TEPhrase phrase;
        public PhrasePatternCompositeKey(Class<? extends LXPattern> p, TEPhrase ph) {
            pattern = p;
            phrase = ph;
        }

        public String toString() {
            return String.format("<PhrasePattern: phrase=%s, pattern=%s>", phrase, pattern);
        }

        @Override
        public int hashCode() {
            int patternCode = pattern.hashCode();
            int phraseCode = phrase.name().hashCode();
            return 1000000 * patternCode + phraseCode;
        }

        @Override
        public boolean equals(Object obj) {
            PhrasePatternCompositeKey that = (PhrasePatternCompositeKey) obj;
            return this.pattern == that.pattern && this.phrase == that.phrase;
        }
    }

    /**
     * For patterns we catalog, how do they cover the cor?
     */
    public enum TEPatternCoverageType {
        PANELS, PANELS_PARTIAL, EDGES, BOTH;

        /**
         * Adjusts which types of patterns (with respect to car coverage)
         * can come after others.
         *
         * @param coverage
         * @return List<TEPatternCoverageType>
         */
        public static HashSet<TEPatternCoverageType> getCompatible(TEPatternCoverageType coverage) {
            if (coverage == PANELS) {
                return new HashSet<TEPatternCoverageType>(List.of(EDGES));
            } else if (coverage == EDGES) {
                return new HashSet<TEPatternCoverageType>(List.of(PANELS, PANELS_PARTIAL, BOTH));
            } else if (coverage == BOTH) {
                return new HashSet<TEPatternCoverageType>(List.of(EDGES));
            } else if (coverage == PANELS_PARTIAL) {
                return new HashSet<TEPatternCoverageType>(List.of(EDGES, PANELS_PARTIAL));
            }
            // default to allowing everything
            return new HashSet<TEPatternCoverageType>(List.of(PANELS, PANELS_PARTIAL, EDGES, BOTH));
        }
    }

    /**
     * For patterns we catalog, can they conform to
     * the current palette settings? Or not?
     */
    public enum TEPatternColorCategoryType {
        PALETTE, WHITE, NONCONFORMING;

        /**
         * Adjusts which types of patterns (with respect to color type)
         * can come after others.
         *
         * @param colorCat
         * @return List<TEPatternColorCategoryType>
         */
        public static HashSet<TEPatternColorCategoryType> getCompatible(TEPatternColorCategoryType colorCat) {
            if (colorCat == PALETTE) {
                return new HashSet(List.of(PALETTE, WHITE, NONCONFORMING));
            } else if (colorCat == WHITE) {
                return  new HashSet(List.of(PALETTE, WHITE, NONCONFORMING));
            } else if (colorCat == NONCONFORMING) {
                return  new HashSet(List.of(PALETTE, WHITE));
            }

            // default to allowing everything
            return  new HashSet(List.of(PALETTE, WHITE, NONCONFORMING));
        }
    }

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
        this.phrasePattern2rec = new HashMap<>();
        this.patternHistoryCounter = new HashMap<>();
    }

    public void addPattern(
            Class<? extends LXPattern> p,
            TEPatternCoverageType c,
            TEPatternColorCategoryType cc,
            TEPhrase ph) {

        //TE.log("Adding pattern: %s (phrase=%s)", p, ph);
        // add to mapping of rec -> patterns
        TEPatternRecord rec = new TEPatternRecord(p, c, cc, ph);
        this.patternRecords.add(rec);

        // add to reverse mapping: pattern -> rec
        PhrasePatternCompositeKey key = new PhrasePatternCompositeKey(p, ph);
        this.phrasePattern2rec.put(key, rec);

        // finally add to the LX registry, if not already added
        try {
            this.lx.registry.addPattern(p);
        } catch (IllegalStateException e) {
            // Pattern already added to LX registry! Ignore.
            // This allows us to add a different set of patterns
            // from what are registered for manual VJ TE operation.
        }
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
     * Get patterns in our TE library that match ONLY a certain set of criteria.
     *
     * @param c if non-null, return only patterns matching this coverage type
     * @param cc if non-null, return only patterns matching this color category type
     * @param ph if non-null, return only patterns matching this phrase type
     * @return an LXPattern class that can be instantiated and loaded onto an LXChannel
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

    public TEPatternRecord getRecFromPattern(LXPattern pat, TEPhrase phrase) {
        PhrasePatternCompositeKey key = new PhrasePatternCompositeKey(pat.getClass(), phrase);
        TEPatternRecord rec = phrasePattern2rec.get(key);
        //TE.log("Looking up record from pattern=%s, phrase=%s ... found=%s", pat, phrase, rec);
        return rec;
    }

    /**
     * From a string like (from `pattern.toString()`)
     *       PBXorcery[#31241][/lx/mixer/channel/1/pattern/16]
     *
     * Extract:
     *      "PBXorcery"
     *
     * @param patternToString
     * @return
     */
    private String pattern2Id(String patternToString) {
        String[] parts = patternToString.split("\\[");
        if (parts.length == 3)
            return parts[0];
        return "";
    }

    /**
     * From a string like (from `patternClass.toString()`)
     *
     *    "class titanicsend.pattern.pixelblaze.PBXorcery"
     * or
     *    "class titanicsend.pattern.yoffa.config.ShaderPanelsPatternConfig$Marbling"
     *
     *  Extract
     *       "PBXorcery" or "Marbling"
     *
     * @param classToString
     * @return
     */
    private String class2Id(String classToString) {
        //TE.log("Finding ID inside: '%s'", classToString);
        String[] parts = classToString.split("\\.");
        String classname = parts[parts.length - 1];
        if (classname.contains("$")) {
            String[] classnameParts = classname.split("\\$");
            return classnameParts[1];
        } else {
            return classname;
        }
    }

    public ArrayList<LXPattern> getCompatibleNextPatterns(TEPhrase oldPhrase, LXPattern curPattern, TEPhrase newPhrase) throws Exception {
        if (!this.isReady())
            throw new Exception("Cannot filter patterns, you need to call indexPatterns() first!");

        // get information about what's currently playing
        TEPatternRecord rec = getRecFromPattern(curPattern, oldPhrase);
        String curPatternId = pattern2Id(curPattern.toString());

        // see what's compatible
        HashSet<TEPatternCoverageType> compatibleCoverage = TEPatternCoverageType.getCompatible(rec.coverageType);
        HashSet<TEPatternColorCategoryType> compatibleColor = TEPatternColorCategoryType.getCompatible(rec.colorCategoryType);

        // now filter based on this
        Stream<TEPatternRecord> s = patternRecords.stream()
                                        .filter(r -> r.phraseType == newPhrase)
                                        .filter(r -> compatibleCoverage.contains(r.coverageType))
                                        .filter(r -> compatibleColor.contains(r.colorCategoryType))
                                        .filter(r -> !class2Id(r.patternClass.toString()).equals(curPatternId));
        ArrayList<TEPatternRecord> matchingRecords = s.collect(Collectors.toCollection(ArrayList::new));

        // ensure we actually have choices, fallback to just phrase compatibility
        // if we ran out by including all the filters
        if (matchingRecords.size() == 0) {
            Stream<TEPatternRecord> s2 = patternRecords.stream().filter(r -> r.phraseType == newPhrase);
            matchingRecords = s2.collect(Collectors.toCollection(ArrayList::new));
            TE.log("Did not find enough compatible patterns, filtering only by phrase now: %d found", matchingRecords.size());
        }

        // now for each record, pull in the corresponding pattern(s) and add to a list
        TE.log("Found %d matching patterns with: coverage=%s, color=%s, phrase=%s",
                matchingRecords.size(), rec.coverageType, rec.colorCategoryType, newPhrase);
        ArrayList<LXPattern> matchingPatterns = new ArrayList<>();
        for (TEPatternRecord r : matchingRecords) {
            for (LXPattern p : this.rec2patterns.get(r)) {
                matchingPatterns.add(p);
            }
        }

        // and shuffle to prevent ordering from having an effect
        Collections.shuffle(matchingPatterns);
        return matchingPatterns;
    }

    public LXPattern pickRandomPattern(TEPhrase phraseType) throws Exception {
        // filter patterns
        ArrayList<LXPattern> matchingPatterns = this.filterPatterns(null, null, phraseType);

        // randomly pick one
        Random rand = new Random();
        int randomIndex = rand.nextInt(matchingPatterns.size());
        TE.log("Picked randomly idx=%d from size=%d", randomIndex, matchingPatterns.size());

        return matchingPatterns.get(randomIndex);
    }

    /**
     * Given the currently playing pattern (on a channel determined by the current phrase
     * type) as well as the next phrase, pick a compatible next pattern to start fading into
     * on the new phrase's channel.
     *
     * Considerations include phrase, coloring, coverage, and play history. Of these, phrase
     * is the most important, as not every pattern is on every channel -- we must filter there.
     *
     * @param curPattern
     * @param curPhrase
     * @param nextPhrase
     * @return
     * @throws Exception
     */
    public LXPattern pickRandomCompatibleNextPattern(LXPattern curPattern, TEPhrase curPhrase, TEPhrase nextPhrase) throws Exception {
        // get coverage type and color from current pattern
        TE.log("... looking up pattern record for: pattern=%s, phrase=%s", curPattern, curPhrase);
        PhrasePatternCompositeKey key = new PhrasePatternCompositeKey(curPattern.getClass(), curPhrase);
        TEPatternRecord curPatternRecord = this.phrasePattern2rec.get(key);
        if (curPatternRecord == null) {
            for (Map.Entry<PhrasePatternCompositeKey, TEPatternRecord> entry : phrasePattern2rec.entrySet()) {
                PhrasePatternCompositeKey k = entry.getKey();
                TEPatternRecord r = entry.getValue();
                if (r.phraseType == curPhrase) {
                    TE.log("-> phrase=%s, found %s => %s", curPhrase, k, r);
                }
            }
            throw new Exception(
                    String.format("Could not find TEPatternRecord for pattern=%s, curPhrase=%s, nextPhrase=%s"
                            , curPattern, curPhrase, nextPhrase));
        }

        // filter patterns
        ArrayList<LXPattern> matchingPatterns = getCompatibleNextPatterns(curPhrase, curPattern, nextPhrase);
        if (matchingPatterns.size() == 0) {
            // this should not happen unless we don't have a pattern on a channel...
            throw new Exception(
                    String.format("No compatible patterns for: pattern=%s, curPhrase=%s, nextPhrase=%s"
                            , curPattern, curPhrase, nextPhrase));
        }

        // should we pick randomly, or select by least plays?
        Random rand = new Random();
        int patternIndex = 0;

        if (rand.nextFloat() <= PROB_PICK_RANDOM_NEXT_PATTERN) {
            // pick random one
            patternIndex = rand.nextInt(matchingPatterns.size());

        } else {
            // pick least played pattern
            Collections.sort(matchingPatterns, new Comparator<LXPattern>() {
                @Override
                public int compare(LXPattern a, LXPattern b) {
                    double barsA = patternHistoryCounter.get(a) == null ? 0 : patternHistoryCounter.get(a);
                    double barsB = patternHistoryCounter.get(b) == null ? 0 : patternHistoryCounter.get(b);
                    return Double.compare(barsA, barsB); // ascending
                }
            });
        }

        LXPattern selectedPattern = matchingPatterns.get(patternIndex);
        TE.log("Picked next pattern: %s", selectedPattern);
        return selectedPattern;
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
    public void indexPatterns() throws InterruptedException {
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

        // TODO(will) iterate through patterns and ensure each one has a record
    }

    /**
     * Keep track of how long each pattern has run historically.
     *
     * @param LXPattern cur
     * @param LXPattern curNext
     * @param double barCount that both these patterns ran
     */
    public void logPhrase(LXPattern cur, LXPattern curNext, double barCount) {
        numBarsTotal += barCount;

        // pattern from curChannel
        if (cur != null) {
            if (patternHistoryCounter.containsKey(cur))
                patternHistoryCounter.put(cur, patternHistoryCounter.get(cur) + barCount);
            else
                patternHistoryCounter.put(cur, barCount);
        }

        // pattern we were fading in, from the nextChannel
        if (curNext != null) {
            if (patternHistoryCounter.containsKey(curNext))
                patternHistoryCounter.put(curNext, patternHistoryCounter.get(curNext) + barCount);
            else
                patternHistoryCounter.put(curNext, barCount);
        }

//        for (Map.Entry<LXPattern, Double> entry : patternHistoryCounter.entrySet()) {
//            TE.log("-> counter: %s has %f bars played", entry.getKey(), entry.getValue());
//        }
    }
}
