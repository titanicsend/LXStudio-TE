package titanicsend.app.autopilot;

/**
 * Enum for names of channels. Each enum is a string mapping to an index. This index MUST MATCH the
 * index in your .lxp file! It's the index into the mixer array to select channels. So if autopilot
 * switches channel names or order, please change it here!
 */
public enum TEChannelName {
  UP(0),
  DOWN(1),
  CHORUS(2),
  STROBES(3),
  TRIGGERS(4);

  private final int index;

  private TEChannelName(int idx) {
    index = idx;
  }

  public int getIndex() {
    return index;
  }

  public static TEChannelName getChannelNameFromPhraseType(TEPhrase phraseType) {
    if (phraseType == TEPhrase.CHORUS) return TEChannelName.CHORUS;
    else if (phraseType == TEPhrase.UP) return TEChannelName.UP;
    else if (phraseType == TEPhrase.DOWN) return TEChannelName.DOWN;
    else if (phraseType == TEPhrase.TRO)
      // for now, maybe we'll have a special channel in future
      return TEChannelName.DOWN;
    return null;
  }
}
