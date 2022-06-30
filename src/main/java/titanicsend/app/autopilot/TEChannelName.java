package titanicsend.app.autopilot;

import java.util.List;

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
