package titanicsend.app.autopilot;

import heronarts.lx.osc.OscMessage;
import titanicsend.util.TE;

/**
 * Struct wrapper around LX's OscMessage class.
 *
 * This is used to tag the message with the inbound time to
 * allow for better estimation of BPM and other time-oriented
 * calculations.
 */
public class TEOscMessage {
    public final OscMessage message;
    public final long timestamp;

    public TEOscMessage(OscMessage message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // delineate between OSC that directly affects LX vs special
    // ones meant to be processed internally by TE
    public static final String PREFIX_LX = "/lx";
    public static final String PREFIX_TE = "/te";

    // tempo-related OSC addresses
    // see method description for `extractBpm()` for why SLUG_STRING_TEMPO_CHANGE exists.
    public static final String SLUG_TEMPO_CHANGE = "/tempo/setBPM";
    public static final String SLUG_STRING_TEMPO_CHANGE = "/tempo/string/setBPM";

    // beat-related OSC addresses
    public static final String SLUG_BEAT = "/tempo/beat";

    // phrase-related OSC addresses
    public static final String SLUG_PHRASE_CHANGE = "/phrase/";

    public static boolean isTempoChange(String oscAddress) {
        return oscAddress.startsWith(PREFIX_LX + SLUG_TEMPO_CHANGE);
    }

    /**
     * See method description for `extractBpm()` for why this exists.
     * @param oscAddress
     * @return
     */
    public static boolean isStringTempoChange(String oscAddress) {
        return oscAddress.startsWith(PREFIX_LX + SLUG_STRING_TEMPO_CHANGE);
    }

    public static boolean isBeat(String oscAddress) {
        return oscAddress.startsWith(PREFIX_LX + SLUG_BEAT);
    }

    public static boolean isPhraseChange(String oscAddress) {
        return oscAddress.startsWith(PREFIX_TE + SLUG_PHRASE_CHANGE);
    }

    /**
     * There is some weirdness with the way LX parses OSC messages
     * that are supposed to be floats. It doesn't work, and this was easier
     * than figuring out what the issue was and patching it.
     *
     * For whatever reason, BPM-related template values in the "EXECUTOR | BEAT CHANGE"
     * field just don't behave like other fields! You have to format as a string. So
     * here we are...
     *
     * @param OscMessage msg, string should be formatted like "/some/addr/here 123:62.4"
     * @return double which would be 123.624 in this case
     */
    public static double extractBpm(OscMessage otherMsg) {
        double newTempo = 0.0;
        try {
            String[] parts = otherMsg.toString().split(" ")[1].split(":");
            double newBpmWhole = Double.parseDouble(parts[0]);
            double newBpmDecimal = Double.parseDouble(parts[1]) / 100.0;
            newTempo = newBpmWhole + newBpmDecimal;
        } catch (Exception e) {
            TE.err("Could not parse OscMessage='%s', error: %s", otherMsg.toString(), e.toString());
        }
        return newTempo;
    }

    public static TEPhrase extractCanonicalPhraseType(String oscAddress) {
        // should look like: /lx/phrase/PHRASE_TYPE
        if (!isPhraseChange(oscAddress)) return null;

        // extract the relevant part
        String[] parts = oscAddress.split("/");
        String phraseTypeString = parts[3];

        // lookup the type of phrase
        return TEPhrase.resolvePhrase(phraseTypeString);
    }
}
