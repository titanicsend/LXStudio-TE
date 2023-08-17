package titanicsend.app.autopilot;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.osc.OscMessage;
import titanicsend.lasercontrol.PangolinHost;
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
    public static final String SLUG_PALETTE_HUE = "/color/hue";

    // beat-related OSC addresses
    public static final String SLUG_BEAT = "/tempo/beat";

    // phrase-related OSC addresses
    public static final String SLUG_PHRASE_CHANGE = "/phrase/";

    // fader related OSC addresses
    //    ie:  /te/fader deck=4:value=103
    public static final String SLUG_DECK_FADER_CHANGE = "/mixer/fader";

    public static boolean isFaderChange(String oscAddress) {
        return oscAddress.startsWith(PREFIX_TE + SLUG_DECK_FADER_CHANGE);
    }

    public static boolean isTempoChange(String oscAddress) {
        return oscAddress.startsWith(PREFIX_LX + SLUG_TEMPO_CHANGE);
    }

    public static String makePaletteHueAddress() {
        return PREFIX_LX + SLUG_PALETTE_HUE;
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

    /**
     * Extract the deck number:
     *       /te/mixer/fader/4 31
     *
     * Would extract -> 4
     * @return int deck number
     */
    public int extractDeck() {
        String[] initialParts = message.toString().split(" ");
        String[] addrParts = initialParts[0].toString().split("/");
        return Integer.parseInt(addrParts[4]);
    }

    /**
     * Extract the fader number:
     *       /te/mixer/fader/4 31
     *
     * Would extract -> 31
     * @return int fader value
     */
    public int extractFaderValue() {
        String[] parts = message.toString().split(" ");
        return Integer.parseInt(parts[1]);
    }

    /**
     * Beat count OSC messages are in the format of:
     *
     *      /lx/tempo/beat I %BEATMARKER%
     *
     * @return int beat num, 0-indexed
     */
    public int extractBeatCount() {
        if (!TEOscMessage.isBeat(message.getAddressPattern().toString()))
            // this isn't a beat, exit
            return -1;

        // parse just using the string representation for now
        String[] parts = message.toString().split(" ");
        int extractedBeatNum = Integer.parseInt(parts[1]);
        return extractedBeatNum - 1;
    }

    public static boolean isPhraseChange(String oscAddress) {
        return oscAddress.startsWith(PREFIX_TE + SLUG_PHRASE_CHANGE);
    }

    public static String makeOscPhraseChangeAddress(TEPhrase p) {
        return PREFIX_TE + SLUG_PHRASE_CHANGE + p.toString().toLowerCase();
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

    public static void applyTEOscOutputSettings(LX lx) {
      lx.engine.osc.transmitActive.setValue(true);
      lx.engine.osc.transmitHost.setValue(PangolinHost.HOSTNAME);
      lx.engine.osc.transmitPort.setValue(PangolinHost.PORT);
    }

    /**
     * Sends an integer value to Pangolin.
     *
     * @param address String
     * @param value int
     */
    public static void sendOscToPangolin(LX lx, String address, int value, boolean verbose) {
        if (verbose)
            TE.log("Sending OSC to %s:%d = OSC OUT: %s %d;", PangolinHost.HOSTNAME, PangolinHost.PORT, address, value);
        lx.engine.osc.sendMessage(address, value);
    }
}
