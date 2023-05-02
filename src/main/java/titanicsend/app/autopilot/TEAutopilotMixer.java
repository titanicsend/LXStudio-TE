package titanicsend.app.autopilot;

import com.google.gson.*;
import heronarts.lx.LX;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXGroup;
import heronarts.lx.pattern.LXPattern;
import titanicsend.pattern.yoffa.config.ShaderPanelsPatternConfig;
import titanicsend.util.TE;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 * Our wrapper around the LX engine mixer, specially for AutoVJ functionality.
 */
public class TEAutopilotMixer {
    // this is what the label of the LX group will be
    public static final String AUTO_VJ_GROUP_NAME = "AUTO_VJ";

    // number of patterns on a channel in the AutoVJ channel group to be considered valid
    public static final int MIN_NUM_PATTERNS_ON_AUTO_VJ_CHANNEL = 1; // TODO: increase this

    // the index at which we'll insert our AutoVJ mixer group on the LX mixer
    // we are aiming for this to be the last channel fader on the APC40
    public static final int AUTO_VJ_GROUP_MIXER_IDX = 7; // last channel on APC40

    // if we try to set the fader below this, just disable the channel to save CPU
    public static final double FADER_LEVEL_OFF_THRESH = 0.01;

    // mapping from TEChannelName -> LX mixer index
    private HashMap<TEChannelName, Integer> channel2idx;

    private LX lx;
    private TEPatternLibrary library;

    // our LX mixer group
    private LXGroup autoVjGroup = null;

    public TEAutopilotMixer(LX lx, TEPatternLibrary library) {
        this.lx = lx;
        this.library = library;

        // this will be our mapping from channel name to mixer index
        channel2idx = new HashMap<>();
    }

    public void setFaderTo(TEChannelName name, double faderLevel) {
        try {
            // Don't crash
            if (lx.engine.mixer.channels.size() <= name.getIndex()) {
                TE.err("Error setting fader, mixer does't see any channels (!)");
                return;
            }

            int channelIdx = channel2idx.get(name);
            LXChannel channel = (LXChannel) lx.engine.mixer.channels.get(channelIdx);
            channel.fader.setValue(faderLevel);

            // save CPU cycles by disabling OFF channels
            if (faderLevel < FADER_LEVEL_OFF_THRESH) {
                channel.enabled.setValue(false);
            } else if (faderLevel >= FADER_LEVEL_OFF_THRESH) {
                channel.enabled.setValue(true);
            }
        } catch (NullPointerException np) {
            TE.err("setFaderTo(lx, %s, %f) failed!", name, faderLevel);
        }
    }

    public LXChannel getChannelByName(TEChannelName name) {
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
                int channelIdx = channel2idx.get(name);
            	if (lx.engine.mixer.channels.size() > channelIdx) {
            		return (LXChannel) lx.engine.mixer.channels.get(channelIdx);
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

    public void turnDownAllChannels(boolean onlyAffectPhraseChannels) {
        for (TEChannelName name : TEChannelName.values()) {
            if (onlyAffectPhraseChannels && (
                    (name == TEChannelName.STROBES)
                            || name == TEChannelName.TRIGGERS
                            || name == TEChannelName.FX))
                continue;
            this.setFaderTo(name, 0.0);
        }
    }

    class AutoVJScanResult {
        private boolean found;
        private int groupIdx;

        public AutoVJScanResult(boolean f, int gidx) {
            this.found = f;
            this.groupIdx = gidx;
        }
    }

    /**
     * Function to determine if AutoVJ is set up correctly. Today we run this every
     * Autopilot loop, but can optimize do every few loops, only upon channels being removed, etc.
     *
     * Scans our channels to find a AutoVJ compatible group and record its index
     * in the LX mixer if so.
     *
     * Also return a boolean "found" if the AutoVJ compatible group that was also
     * valid, and doesn't need to be removed, cleaned up, and replaced.
     *
     * @return AutoVJScanResult with both found and groupIdx set
     */
    public AutoVJScanResult findAutoVJGroupIndex(boolean verbose) {
        boolean found = false;  // if we find group named properly AND has proper channels
        int groupIdx = -1;  // if a group is named "AUTO_VJ", record index here. otherwise -1
        if (verbose) TE.log("In findAutoVJGroupIndex()");

        // iterate through each channel, looking for ones that are groups named properly
        for (ListIterator<LXAbstractChannel> it = this.lx.engine.mixer.channels.listIterator(); it.hasNext(); ) {
            LXAbstractChannel ch = it.next();

            if (ch.isGroup() && ch.label.getString().equals(AUTO_VJ_GROUP_NAME)) {
                if (verbose) TE.log("Found group named correctly");
                LXGroup group = (LXGroup) ch;
                if (group == null) continue;
                groupIdx = ch.getIndex();
                if (verbose)
                    TE.log("Found group named correctly at: %d, num channels: %d, required: %d"
                        , groupIdx, group.channels.size(), TEChannelName.values().length);

                if (group.channels.size() < TEChannelName.values().length)
                    // if this group doesn't have enough channels, it's not a valid AutoVJ group
                    continue;

                if (verbose) TE.log("Right number of channels");

                // validate that each of our required channels are there
                // (yes, double for loop is sloppy, but we're talking about <10 channels here)
                int vjChannelsFound = 0;
                for (ListIterator<LXChannel> iter = group.channels.listIterator(); iter.hasNext(); ) {
                    LXChannel vjCh = iter.next();
                    for (TEChannelName name : TEChannelName.values()) {
                        if (name.toString().equals(vjCh.label.getString()) && vjCh.patterns.size() >= MIN_NUM_PATTERNS_ON_AUTO_VJ_CHANNEL) {
                            vjChannelsFound++;
                            if (verbose) TE.log("Found channel: %s", name.toString());
                            break;
                        }
                    }
                }

                // if we found every AutoVJ channel, then we're good to go
                if (vjChannelsFound == TEChannelName.values().length) {
                    if (verbose) TE.log("We found all the channels we needed to!");
                    found = true;
                }
            }
        }

        return new AutoVJScanResult(found, groupIdx);
    }

    /**
     * Finds an LXGroup channel group that is set up for AutoVJ to use.
     *
     * If a compatible group is not found, create one and populate it with
     * the AutoVJ library of patterns.
     *
     * Also populates:
     * - channel2idx (mapping from channel name -> index on LX mixer)
     * - autoVjGroupIndex (index of the AutoVJ group on the LX mixer)
     */
    public boolean ensureSetup() {
        boolean setupWasNeeded = false; // if we had to populate the mixer with new channels

        synchronized (this.lx.engine.mixer) {
            AutoVJScanResult firstScan = this.findAutoVJGroupIndex(false);

            // if we didn't find the setup we need for AutoVJ to run correctly,
            // we're going to read from our template .lxp file for AutoVJ
            // and populate the mixer accordingly with a new group that should satisfy
            // our requirements!
            LXGroup group = null;
            if (!firstScan.found) {
                setupWasNeeded = true;

                // remove group at groupIdx if != -1
                // this would mean we found a group with name "Auto_VJ", but it wasn't
                // set up properly, or was missing required channels
                if (firstScan.groupIdx != -1) {
                    TE.log("A corrupted version of the AutoVJ mixer group was found, removing...");
                    this.lx.engine.mixer.removeChannel(lx.engine.mixer.channels.get(firstScan.groupIdx));
                }

                // we're now ready to populate the LX mixer with the channels and
                // group that AutoVJ needs to operate with

                // create group
                // TODO: maybe remove this if JSON file loading works? otherwise might end up with
                //       two different groups on the mixer with the name "AUTO_VJ"
                TE.log("Creating and populating AutoVJ channel set...");
                int groupIdx = AUTO_VJ_GROUP_MIXER_IDX;
                group = lx.engine.mixer.addGroup(groupIdx);
                group.label.setValue(AUTO_VJ_GROUP_NAME);
                //group.enabled.setValue(false); // turn off initially

                // read the AutoVJ JSON file
//                String autoVJpath = "Projects/AutoVJ_grouped.lxp";
//                try {
//                    // pull out the sub-JSON objects we need
//                    JsonParser parser = new JsonParser();
//                    JsonElement rootElt = parser.parse(new FileReader(autoVJpath));
//                    JsonObject root = rootElt.getAsJsonObject();
//                    JsonObject engineObj = root.getAsJsonObject("engine");
//                    JsonObject childrenObj = engineObj.getAsJsonObject("children");
//                    JsonObject mixerObj = childrenObj.getAsJsonObject("mixer");
//                    JsonArray channelsArray = mixerObj.getAsJsonArray("channels");
//
//                    int autoVjGroupId = -1; // JSON id assigned to node for "AUTO_VJ" LXGroup
//                    int channelIdxOffset = 1; // index with which to insert new channel for group
//                    for (JsonElement chan : channelsArray) {
//                        // again, pull out sub-JSON objects we need
//                        JsonObject chanObj = chan.getAsJsonObject();
//                        String classname = chanObj.get("class").getAsString();
//                        JsonObject params = chanObj.getAsJsonObject("parameters");
//                        String label = params.get("label").getAsString();
//
//                        // every channel will have a channel ID, but only internal channels
//                        // to a group will have a group JSON ID
//                        int channelId = Integer.parseInt(chanObj.get("id").getAsString());
//                        int groupId = -2; // need a different default than autoVjGroupId :)
//                        try {
//                            groupId = Integer.parseInt(chanObj.get("group").getAsString());
//                        } catch (Exception e) {
//                            // do nothing here, just didn't have a parent group
//                        }
//
//                        // if we haven't found the AUTO_VJ group yet, keep going
//                        if (autoVjGroupId == -1) {
//                            if (label.equals(AUTO_VJ_GROUP_NAME) && classname.equals("heronarts.lx.mixer.LXGroup")) {
//                                autoVjGroupId = channelId;
//                                TE.log("AutoVJ group id: %d", autoVjGroupId);

//                                // attempting to add the entire group object to the mixer
//                                lx.engine.mixer.load(lx, chanObj);
//
//                                // now that we added it, find and validate our group
//                                AutoVJScanResult postLoadScan = this.findAutoVJGroupIndex(false);
//                                if (postLoadScan.found) {
//                                    group = (LXGroup) lx.engine.mixer.channels.get(postLoadScan.groupIdx);
//                                    TE.log("Found compatible group post load at: %d", postLoadScan.groupIdx);
//                                    break;
//                                }
//                            }
//                        }
//                        else if (groupId == autoVjGroupId) {
//                            // this is a channel within the AutoVJ template group, let's pull the classnames from here
//                            // and populate a new channel in our running app
//                            TE.log("Found an AutoVJ channel: %s", label);
//                            TEChannelName channelName = TEChannelName.valueOf(label);
//
//                            // create a new channel
//                            LXChannel c = lx.engine.mixer.addChannel(groupIdx + channelIdxOffset);
//                            c.label.setValue(channelName.toString());
//                            channelIdxOffset++;
//
//                            // add channel to group
//                            group.addChannel(c);
//
//                            TE.log("Chan obj: %s, groupID: %d", chanObj, group.getId());
//
//                            // load the channel into LX
//                            c.load(lx, chanObj);
//                        }
//                    }
//
//                } catch (FileNotFoundException e) {
//                    TE.err("TEAutopilotMixer, FileNotFoundException: %s", e);
//                } catch (IOException ioe) {
//                    TE.err("TEAutopilotMixer, IOException: %s", ioe);
//                }

                /**
                 * TODO: replace the logic below with the commented out logic above
                 *
                 * This logic is able to populate the AutoVJ channels, but not with the parameterizations
                 * from the .lxp, and not for non-phrase channels like STROBES or TRIGGERS
                 */
                // create channels, add to group
                int added = 1;
                for (TEChannelName name : TEChannelName.values()) {
                    LXChannel c = lx.engine.mixer.addChannel(groupIdx + added);
                    c.label.setValue(name.toString());

                    for (Map.Entry<TEPatternLibrary.PhrasePatternCompositeKey, TEPatternLibrary.TEPatternRecord> e : this.library.getPatternMapping().entrySet()) {
                        TEPatternLibrary.PhrasePatternCompositeKey key = e.getKey();
                        TEChannelName chNameFromPhrase = TEChannelName.getChannelNameFromPhraseType(key.phrase);
                        if (name == chNameFromPhrase) {
                            TEPatternLibrary.TEPatternRecord record = e.getValue();
                            try {
                                Class<? extends LXPattern> patternClass = record.patternClass;
                                Constructor<? extends LXPattern> constructor = patternClass.getConstructor(LX.class);
                                LXPattern myPattern = constructor.newInstance(lx);
                                c.addPattern(myPattern);
                            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException err) {
                                TE.err("Error in populating TEAutopilotMixer: %s", err);
                            }
                        }
                    }
                    group.addChannel(c);
                    added++;
                }
                /**
                 * End logic that needs to be replaced.
                 *
                 * The stuff below is vital, and should be kept when we switch over to JSON loading logic.
                 */

                // finally, record correct indices per channel since they are in the group
                // this will allow proper look up (ie: get channel idx for phrase of type CHORUS)
                this.channel2idx.clear();
                for (LXChannel c : group.channels) {
                    //TE.log("Channel: %s, key: %s, value: %d", c.toString(), c.label.getString(), c.getIndex());
                    this.channel2idx.put(TEChannelName.valueOf(c.label.getString()), c.getIndex());
                }

                // keep a reference to our LXGroup instance
                this.autoVjGroup = group;
            }
        }

        // so we know if we need to re-index with the TELibrarian
        return setupWasNeeded;
    }
}
