package titanicsend.app.autopilot;

import heronarts.lx.LX;
import heronarts.lx.clip.LXClip;
import heronarts.lx.mixer.LXChannel;

import java.util.List;
import java.util.Random;

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

    public static LXClip pickRandomClipFromChannel(LX lx, TEChannelName channelName) {
        LXChannel triggersChannel = (LXChannel) lx.engine.mixer.channels.get(channelName.getIndex());
        Random random = new Random();
        int numClips = triggersChannel.clips.size();
        int randIdx = random.nextInt(0, numClips);
        LXClip clip = triggersChannel.clips.get(randIdx);
        return clip;
    }
}
