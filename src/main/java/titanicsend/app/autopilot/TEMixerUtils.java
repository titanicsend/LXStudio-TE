package titanicsend.app.autopilot;

import heronarts.lx.LX;
import heronarts.lx.clip.LXClip;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.pattern.LXPattern;

import java.util.List;
import java.util.Random;

/**
 * Contains useful methods for manipulating and controlling
 * the LX mixer that are specific to TE.
 */
public class TEMixerUtils {
    public static void setChannelExclusivelyVisible(LX lx, TEChannelName channel) {
        List<TEChannelName> channels = TEChannelName.listChannels();
        for (TEChannelName c : channels) {
            double faderLevel = 0.0;
            if (c == channel) {
                faderLevel = 1.0;
            }

            lx.engine.mixer.channels.get(c.getIndex()).fader.setValue(faderLevel);
        }
    }

    public static TEChannelName getChannelNameFromPhraseType(TEPhrase phraseType) {
        if (phraseType == TEPhrase.CHORUS)
            return TEChannelName.CHORUS;
        else if (phraseType == TEPhrase.UP)
            return TEChannelName.UP;
        else if (phraseType == TEPhrase.DOWN)
            return TEChannelName.DOWN;
        else if (phraseType == TEPhrase.TRO)
            return TEChannelName.TRO;
        return null;
    }

    public static LXChannel getChannelByName(LX lx, TEChannelName name) {
        if (name == null)
            return null;
        return (LXChannel) lx.engine.mixer.channels.get(name.getIndex());
    }

    public static LXClip pickRandomClipFromChannel(LX lx, TEChannelName channelName) {
        LXChannel triggersChannel = (LXChannel) lx.engine.mixer.channels.get(channelName.getIndex());
        Random random = new Random();
        int numClips = triggersChannel.clips.size();
        int randIdx = random.nextInt(0, numClips);
        LXClip clip = triggersChannel.clips.get(randIdx);
//        System.out.printf("From channel=%d, picked pattern %d / %d to get pattern=%d\n", channelName.getIndex(), randIdx, numClips, clip.getIndex());
        return clip;
    }

    public static int pickRandomPatternFromChannel(LXChannel channel) {
        List<LXPattern> patterns = channel.getPatterns();
        Random random = new Random();
        int randIdx = random.nextInt(0, patterns.size());
        return randIdx;
    }
}
