package titanicsend.app.autopilot;

import java.util.List;

/**
 * Enum for names of channels. Each enum is a string
 * mapping to an index. This index MUST MATCH the index in
 * your .lxp file! It's the index into the mixer array to
 * select channels. So if autopilot switches channel names or
 * order, please change it here!
 *
 * ...and don't forget to add it to listChannels() !
 */
public enum TEChannelName {
    UP(0),
    DOWN(1),
    CHORUS(2),
    STROBES(3),
    TRIGGERS(4),
    TRO(5); // in-tro and out-tro's;

    private final int index;

    private TEChannelName(int idx) {
        index = idx;
    }

    public int getIndex() {
        return index;
    }

    public static List<TEChannelName> listChannels() {
        return List.of(TRO, UP, DOWN, CHORUS, STROBES, TRIGGERS);
    }
}
