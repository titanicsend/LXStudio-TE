package titanicsend.app.autopilot;

import java.util.HashMap;
import java.util.Map;
import titanicsend.util.TE;

public class TEDeckGroup {
    public static int TE_DEFAULT_NUM_DECKS = 4;

    // maps a 1-INDEXED set of decks to a current fader value
    private HashMap<Integer, Integer> deck2faderValue;
    private int numDecks; // actual number of decks

    private int masterDeck = 1;

    public TEDeckGroup(int numDecks) {
        this.numDecks = numDecks;
        this.deck2faderValue = new HashMap<>();

        // initialize deck values to 0
        for (int i = 1; i < this.numDecks + 1; i++) {
            this.deck2faderValue.put(i, 0);
        }
    }

    /**
     * Update the fader value for the deck number given.
     *
     * Returns the deck with the highest fader value after this operation.
     *
     * @param deckNum
     * @param newFaderValue
     * @return int num of deck with higest fader value
     */
    public int updateFaderValue(int deckNum, int newFaderValue) {
        // is this a valid deck?
        if (!deck2faderValue.containsKey(deckNum)) {
            TE.err("ERROR! Deck=%d does not exist!", deckNum);
            return -1;
        }

        // if so, store new fader value
        deck2faderValue.put(deckNum, newFaderValue);

        // now find the largest deck number
        int deckNumWithHighestFaderValue = -1;
        int highestFaderVal = -1;
        for (Map.Entry<Integer, Integer> entry : this.deck2faderValue.entrySet()) {
            int deck = entry.getKey();
            int val = entry.getValue();
            if (val > highestFaderVal) {
                highestFaderVal = val;
                deckNumWithHighestFaderValue = deck;
            }
        }

        if (highestFaderVal == 0) {
            // if highest value is 0, just return the
            // previous master deck number
            return masterDeck;
        } else {
            masterDeck = deckNumWithHighestFaderValue;
            return deckNumWithHighestFaderValue;
        }
    }

    public int getMasterDeck() {
        return masterDeck;
    }
}
