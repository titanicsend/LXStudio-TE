package titanicsend.app.autopilot.utils;

import heronarts.lx.LX;
import heronarts.lx.clip.LXClip;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXGroup;
import heronarts.lx.pattern.LXPattern;
import titanicsend.app.autopilot.TEChannelName;
import titanicsend.app.autopilot.TEPhrase;
import titanicsend.pattern.yoffa.config.ShaderPanelsPatternConfig;
import titanicsend.util.TE;

import java.util.*;

/**
 * Contains useful methods for manipulating and controlling
 * the LX mixer that are specific to TE.
 */
public class TEMixerUtils {
    public static final double FADER_LEVEL_OFF_THRESH = 0.01;

//    public static void turnDownAllChannels(LX lx, boolean onlyAffectPhraseChannels) {
//        for (TEChannelName name : TEChannelName.values()) {
//            if (onlyAffectPhraseChannels && (
//                    (name == TEChannelName.STROBES)
//                            || name == TEChannelName.TRIGGERS
//                            || name == TEChannelName.FX))
//                continue;
//            setFaderTo(lx, name, 0.0);
//        }
//    }

//    public static void setFaderTo(LX lx, TEChannelName name, double faderLevel) {
//        try {
//        	// Don't crash
//        	if (lx.engine.mixer.channels.size() <= name.getIndex()) {
//                TE.err("Error setting fader, mixer does't see any channels (!)");
//                return;
//            }
//
//            LXChannel channel = (LXChannel) lx.engine.mixer.channels.get(name.getIndex());
//            channel.fader.setValue(faderLevel);
//
//            // save CPU cycles by disabling OFF channels
//            if (faderLevel < FADER_LEVEL_OFF_THRESH) {
//                channel.enabled.setValue(false);
//            } else if (faderLevel >= FADER_LEVEL_OFF_THRESH) {
//                channel.enabled.setValue(true);
//            }
//        } catch (NullPointerException np) {
//            TE.err("setFaderTo(lx, %s, %f) failed!", name, faderLevel);
//        }
//    }

//    public static double readFaderValue(LX lx, TEChannelName name) {
//        return lx.engine.mixer.channels.get(name.getIndex()).fader.getValue();
//    }

    public static TEChannelName getChannelNameFromPhraseType(TEPhrase phraseType) {
        if (phraseType == TEPhrase.CHORUS)
            return TEChannelName.CHORUS;
        else if (phraseType == TEPhrase.UP)
            return TEChannelName.UP;
        else if (phraseType == TEPhrase.DOWN)
            return TEChannelName.DOWN;
        else if (phraseType == TEPhrase.TRO)
            // for now, maybe we'll have a special channel in future
            return TEChannelName.DOWN;
        return null;
    }

    public static LXChannel getChannelByName(LX lx, TEChannelName name) {
        if (name == null)
            return null;

        // this backoff/retry loop is needed in case there's a delay in intializing
        // the LX mixer / channels. this often happens in LX projects where there are
        // a large number of shaders that need to be loaded
        int numTries = 0;
        while (numTries < 8) {
            try {
            	// JKB mod, now that autopilot loads after everything else,
            	//  if channel index isn't there then the .lxp isn't AutoVJ and it's not going to show up...
            	//if (lx.engine.mixer.channels.contains(name))
            	if (lx.engine.mixer.channels.size() > name.getIndex()) {
            		return (LXChannel) lx.engine.mixer.channels.get(name.getIndex());
            	} else {
                    TE.err("Error in getChannelByName(%s), mixer does't see enough channels (!)", name);
            		return null;
            	}
            } catch (IndexOutOfBoundsException e) {
                numTries++;
                double waitMs = Math.pow(2.0, (double)numTries) * 1000;
                TE.log("LX mixer wasn't ready yet, waiting %f seconds...", waitMs / 1000.);
                try {
                    Thread.sleep((long)waitMs);
                } catch (InterruptedException ie) {
                    TE.err("Could not sleep waiting for mixer, %s: %s", ie, ie.getMessage());
                }
            }
        }
        return null;
    }
}
