package titanicsend.app.autopilot;

import java.util.concurrent.ConcurrentHashMap;

public enum TEPhrase {
    TRO("tro"),
    UP("up"),
    DOWN("down"),
    CHORUS("chorus"),
    UNKNOWN("unknown");

    private final String name;

    private static final ConcurrentHashMap<String, TEPhrase> stringToPhrase = initializeMap();

    private TEPhrase(String name) {
        this.name = name;
    }

    private static ConcurrentHashMap<String, TEPhrase> initializeMap() {
        /*
            Static map initialization:
            https://stackoverflow.com/a/509016
         */
        ConcurrentHashMap<String, TEPhrase> m = new ConcurrentHashMap<String, TEPhrase>();

        // ADD ALL PHRASES HERE
        m.put("tro", TRO);
        m.put("up", UP);
        m.put("down", DOWN);
        m.put("chorus", CHORUS);
        return m;
    }

    public static TEPhrase resolvePhrase(String showKontrolPhraseString) {
        String[] parts = showKontrolPhraseString.split("/");
        String phrase = parts[3];
        if (stringToPhrase.containsKey(phrase)) {
            return stringToPhrase.get(phrase);
        }
        return UNKNOWN;
    }
}
