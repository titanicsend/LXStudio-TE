package titanicsend.app.autopilot;

import heronarts.lx.osc.OscMessage;

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

    public static final String PREFIX = "/lx";

    public static final String SLUG_TEMPO_CHANGE = "/tempo/setBPM";
    public static final String SLUG_DOWNBEAT = "/tempo/downbeat";
    public static final String SLUG_BEAT = "/tempo/beat";
    public static final String SLUG_PHRASE_CHANGE = "/phrase/";

    public static boolean isTempoChange(String oscAddress) {
        return oscAddress.equals(PREFIX + SLUG_TEMPO_CHANGE);
    }

    public static boolean isDownbeat(String oscAddress) {
        return oscAddress.equals(PREFIX + SLUG_DOWNBEAT);
    }

    public static boolean isBeat(String oscAddress) {
        return oscAddress.equals(PREFIX + SLUG_BEAT);
    }

    public static boolean isPhraseChange(String oscAddress) {
        return oscAddress.startsWith(PREFIX + SLUG_PHRASE_CHANGE);
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
