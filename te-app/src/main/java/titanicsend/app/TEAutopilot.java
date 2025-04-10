package titanicsend.app;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.Tempo;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BooleanParameter.Mode;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import titanicsend.app.autopilot.*;
import titanicsend.app.autopilot.events.TEPhraseEvent;
import titanicsend.app.autopilot.utils.TETimeUtils;
import titanicsend.util.TE;
import titanicsend.util.TEMath;

public class TEAutopilot extends LXComponent implements LXLoopTask, LX.ProjectListener {

  public final BooleanParameter enabled =
      new BooleanParameter("Enabled", false).setMode(Mode.TOGGLE).setDescription("AutoVJ On/Off");

  private final LXParameterListener enabledListener =
      (p) -> {
        onEnabled(this.enabled.isOn());
      };

  // number of bars to fade out on various occasions
  private static final int MISPREDICTED_FADE_OUT_BARS = 2;
  private static final int PREV_FADE_OUT_BARS = 2;

  // when we're inot getting OSC phrase messages, this is how long
  // we wait to change phrases
  private static final int SYNTHETIC_PHRASE_LEN_BARS = 32;

  // number of bars after a chorus to continue leaving
  // FX channels visible
  private static final double TRIGGERS_AT_CHORUS_LENGTH_BARS = 1.5; // 1.0;
  private static final double STROBES_AT_CHORUS_LENGTH_BARS = 1.75; // 1.25;

  // how long do we think the shortest legit phrase might be?
  private static final int MIN_NUM_BEATS_IN_PHRASE = 4;

  // length of time in beats since a transition that we think will weed
  // out erroneous phrase messages
  private static final int MIN_NUM_BEATS_SINCE_TRANSITION_FOR_NEW_PHRASE = 2;

  // various fader levels of importance
  private static final double LEVEL_FULL = 1.0,
      LEVEL_MISPREDICTED_FADE_OUT = 0.75, // fading out mistaken transition
      LEVEL_PREV_FADE_OUT = 0.75, // fading out prev channel
      LEVEL_BARELY_ON = 0.03,
      LEVEL_HALF = 0.5,
      LEVEL_OFF = 0.0,
      LEVEL_FADE_IN = 0.4; // fading in next channel

  // Probability that we launch CHORUS clips upon a repeated CHORUS phrase
  // sometimes there are like 5 CHORUS's in a row, and want to keep some variety
  private static final float PROB_CLIPS_ON_SAME_PHRASE = 1f;

  // if OSC messages are older than this, throw them out
  private static final long OSC_MSG_MAX_AGE_MS = 2 * 1000;

  // after a while (ie: 2 min) without receiving a phrase OSC msg,
  // on the next downbeat chose a phrase and enter it! this is how
  // non-rekordbox phrase mode is engaged
  private static final long OSC_PHRASE_TIMEOUT_MS = 2 * 60 * 1000;

  // after a while (ie: 2 min) without receiving an OSC msg,
  // non-OSC mode is engaged
  private static final long OSC_TIMEOUT_MS = 30 * 1000;
  private boolean oscBeatModeOnlyOn = false;

  // if we're not recieving OSC at all
  private boolean noOscModeOn = false;

  // how long in between palette changes
  // palette changes only happen on new CHORUS phrase changes
  private static final long PALETTE_DURATION_MS = 10 * 60 * 1000;

  // OSC message related fields
  private final ConcurrentLinkedQueue<TEOscMessage> unprocessedOscMessages;
  private long lastOscMessageReceivedAt;

  // our historical tracking object, keeping state about events in past
  public TEHistorian history;

  // our pattern library, used to filter for new patterns
  private TEPatternLibrary library;

  // "oldNext" is essentially the echo channel -- we use it
  // to gradually fade out a pattern that may have been abruptly cut off
  // for example, we were transitioning from UP -> CHORUS, but all of a sudden we
  // see a DOWN, we fade out the "old next" (CHORUS) channel over 1-2 bars
  private LXChannel prevChannel, curChannel, nextChannel, oldNextChannel;

  // whether or not we should be fading out various channels
  private boolean oldNextFadeOutMode = false;
  private boolean prevFadeOutMode = false;

  private TEChannelName prevChannelName, curChannelName, nextChannelName, oldNextChannelName;
  private TEPhrase prevPhrase = null, curPhrase = null, nextPhrase = null, oldNextPhrase = null;

  // use this to track the last set value of each of these faders
  private double nextChannelFaderVal = 0.0;

  // FX channels
  private LXChannel triggerChannel = null, strobesChannel = null;

  private final TEAutopilotMixer autoMixer;

  /**
   * Instantiate the autopilot with a reference to both LX and the pattern library.
   *
   * @param lx
   * @param l
   */
  public TEAutopilot(LX lx, TEPatternLibrary l, TEHistorian history) {
    super(lx);
    this.library = l;
    this.history = history;
    this.autoMixer = new TEAutopilotMixer(lx, this.library);

    addParameter("Enabled", this.enabled);
    this.enabled.addListener(enabledListener);

    // this queue needs to be accessible from OSC listener in diff thread
    unprocessedOscMessages = new ConcurrentLinkedQueue<TEOscMessage>();

    lx.addProjectListener(this);
  }

  /**
   * Reset history around autopilot, channel state, phrase state, etc.
   *
   * <p>Return False if this step fails.
   */
  public boolean resetHistory() {
    // historical logs of events for calculations
    history = new TEHistorian();

    // phrase state
    prevPhrase = null;
    curPhrase = TEPhrase.DOWN;
    nextPhrase = TEPhrase.UP;
    oldNextPhrase = null;

    prevChannelName = null;
    curChannelName = TEChannelName.getChannelNameFromPhraseType(curPhrase);
    nextChannelName = TEChannelName.getChannelNameFromPhraseType(nextPhrase);
    oldNextChannelName = null;

    // this call will also wait for the mixer to be initialized
    curChannel = autoMixer.getChannelByName(curChannelName);
    nextChannel = autoMixer.getChannelByName(nextChannelName);
    autoMixer.setFaderTo(curChannelName, LEVEL_FULL);
    triggerChannel = autoMixer.getChannelByName(TEChannelName.TRIGGERS);
    strobesChannel = autoMixer.getChannelByName(TEChannelName.STROBES);

    oldNextFadeOutMode = false;
    prevFadeOutMode = false;

    // If the channel indexes didn't exist, it might just be a project with too few channels.  Bail
    // on this.
    if (curChannel == null
        || nextChannel == null
        || triggerChannel == null
        || strobesChannel == null) {
      // Cancel autopilot
      TE.log("Cancelling autopilot, special channels not found.");
      return false;
    }

    // set palette timer
    history.startPaletteTimer();

    // remap pattern objects
    this.library.indexPatterns(this.autoMixer);

    return true;
  }

  /**
   * This is the entrypoint for OSC messages that come externally from ShowKontrol. First they are
   * processed by TEOscListener, but once they are deemed in scope for Autopilot, they are
   * dispatched here.
   *
   * @param msg
   */
  protected void onOscMessage(OscMessage msg) {
    if (!this.enabled.isOn()) {
      // if autopilot isn't enabled, don't bother tracking these
      return;
    }

    try {
      TEOscMessage oscTE = new TEOscMessage(msg);

      // TE.log("Adding OSC message to queue: %s", address);
      history.setLastOscMsgAt(oscTE.timestamp);

      // if we'd previously entered No OSC mode, let's turn that off
      if (noOscModeOn) {
        noOscModeOn = false;
        TE.log("No OSC mode OFF! We got OSC message");
      }

      // then add message to queue to be processed on next loop()
      unprocessedOscMessages.add(oscTE);
    } catch (Exception e) {
      TE.log("Exception parsing OSC message (%s): %s", msg.toString(), e.toString());
    }
  }

  /**
   * Change to a new LX palette swatch.
   *
   * @param pickRandom should we pick the next swatch randomly? or just use the next one in order?
   * @param immediate should this change happen immediately?
   * @param numBarsTransition if not immediately, how many bars should we set the transition to
   *     happen over?
   */
  public void changePaletteSwatch(boolean pickRandom, boolean immediate, int numBarsTransition) {
    // should we change the transition duration?
    try {
      if (numBarsTransition > 0) {
        double transitionDurationMs =
            TETimeUtils.calcPhraseLengthMs(lx.engine.tempo.bpm(), numBarsTransition);
        lx.engine.palette.transitionTimeSecs.setValue(transitionDurationMs);
      } else {
        lx.engine.palette.transitionTimeSecs.setValue(0);
      }
    } catch (Exception e) {
      TE.err("Error changing palette transition duration!");
      e.printStackTrace();
    }

    // pick a random swatch
    if (pickRandom) {
      Random rand = new Random();
      int numSwatches = lx.engine.palette.swatches.size();
      LXSwatch newSwatch = lx.engine.palette.swatches.get(rand.nextInt(0, numSwatches));

      // change swatch
      lx.engine.palette.setSwatch(newSwatch);

      // if you repeat the setSwatch operation it happens immediately
      if (immediate) lx.engine.palette.setSwatch(newSwatch);

    } else {
      // otherwise, just proceed to the next one
      lx.engine.palette.triggerSwatchCycle.setValue(true);
      if (immediate) lx.engine.palette.triggerSwatchCycle.setValue(true);
    }
  }

  /**
   * Called when an OSC beat event comes through. Will only be triggered if autoVJ is enabled.
   *
   * @param msg OscMessage from ShowKontrol
   * @throws Exception
   */
  public void onBeatEvent(TEOscMessage msg) throws Exception {
    long beatAt = msg.timestamp;
    int beatCount = msg.extractBeatCount(); // 0-indexed
    history.logBeat(beatAt, beatCount);

    // if this isn't a downbeat, ignore. want to wait for the start of a
    // bar to change modes or launch a new phrase if warranted
    if (beatCount != 0) return;

    // if we're seeing phrase OSC messages regularly, no need to continue either
    if (beatAt - history.getLastOscPhraseAt() < OSC_PHRASE_TIMEOUT_MS) return;

    // are we currently in Osc phrase mode and need to transition?
    if (!oscBeatModeOnlyOn) {
      // it has been so long without a phrase that we need to enter
      // oscBeatModeOnlyOn=true and trigger phrases ourselves!
      this.resetHistory();
      oscBeatModeOnlyOn = true;
      noOscModeOn = false;
      TE.log("OSC beat-only mode activated");

      // now trigger a phrase (DOWN is probably safest)
      String syntheticOscAddr = TEOscMessage.makeOscPhraseChangeAddress(TEPhrase.DOWN);
      onPhraseChange(syntheticOscAddr, beatAt);
      history.setLastSynthethicPhraseAt(beatAt);

    } else {
      // we've been in osc beat only mode for a while now, we just need to decide if it's
      // time to transition to a new phrase

      // TODO(will) based on audio signal, is this a likely CHORUS start?
      // if so, can change nextPhrase

      // get some useful stats about the current phrase
      double repeatedPhraseLengthBars = history.getRepeatedPhraseBarProgress(lx.engine.tempo.bpm());

      // otherwise: if it's been OSC_BEAT_ONLY_PHRASE_LEN_BARS bars, let's change
      if (SYNTHETIC_PHRASE_LEN_BARS - repeatedPhraseLengthBars < 1) {
        // now trigger the next phrase
        String syntheticOscAddr = TEOscMessage.makeOscPhraseChangeAddress(nextPhrase);
        TE.log("OSC beat-only mode: triggering synthetic phrase=%s", syntheticOscAddr);
        history.setLastSynthethicPhraseAt(beatAt);
        onPhraseChange(syntheticOscAddr, beatAt);
      }
    }
  }

  /**
   * Main event loop for autopilot. Mostly a no-op if autopilot is disabled.
   *
   * @param deltaMs ms since last loop ran
   */
  @Override
  public void loop(double deltaMs) {
    if (!this.enabled.isOn()) {
      // if autopilot isn't enabled, just ignore for now
      return;
    }

    long now = System.currentTimeMillis();

    try {
      // check for new OSC messages
      StringBuilder msgs = new StringBuilder();
      while (!unprocessedOscMessages.isEmpty()) {
        // grab a new message off the queue
        TEOscMessage oscTE = unprocessedOscMessages.poll();
        if (oscTE == null) {
          // this should never happen, since we test for size() of queue, but good to check
          TE.log("unprocessedOscMessages pulled off null value -- should never happen!");
          continue;

        } else if (oscTE.timestamp <= now - OSC_MSG_MAX_AGE_MS) {
          // if these messages are older than this, ignore
          continue;
        }

        // grab message & update with the most recent OscMessage received at
        String address = oscTE.message.getAddressPattern().toString();

        if (!oscTE.message.toString().contains("/tempo/beat"))
          msgs.append(
              String.format(", %s (%d ms ago)", oscTE.message.toString(), now - oscTE.timestamp));

        // handle OSC message based on type
        if (TEOscMessage.isBeat(address)) {
          onBeatEvent(oscTE);

        } else if (TEOscMessage.isPhraseChange(address)) {
          // let's make sure this is a valid phrase change!
          int msSinceLastMasterChange = history.calcMsSinceLastDeckChange();
          int msSinceLastDownbeat = history.calcMsSinceLastDownbeat();
          int msSinceLastOscPhrase = history.calcMsSinceLastOscPhraseChange();

          int msInBeat = (int) TETimeUtils.calcMsPerBeat(lx.engine.tempo.bpm());
          double howFarThroughMeasure = lx.engine.tempo.getBasis(Tempo.Division.WHOLE); // 0 to 1

          // TE.log("msSinceLastMasterChange=%d, msSinceLastOscPhrase=%d, msSinceLastDownbeat=%d,
          // howFarThroughMeasure=%f",
          //        msSinceLastMasterChange, msSinceLastOscPhrase, msSinceLastDownbeat,
          // howFarThroughMeasure);

          // conditions
          boolean isInMiddleOfMeasure = howFarThroughMeasure > 0.2 && howFarThroughMeasure < 0.8;
          boolean wasRecentMasterChange =
              msSinceLastMasterChange < msInBeat * MIN_NUM_BEATS_SINCE_TRANSITION_FOR_NEW_PHRASE;
          boolean wasRecentPhraseChange = msSinceLastOscPhrase < msInBeat * MIN_NUM_BEATS_IN_PHRASE;

          // make decision -- this is configurable. I found that a pretty zero tolerance policy was
          // most effective
          if (wasRecentMasterChange || wasRecentPhraseChange) {
            // TE.log("isInMiddleOfMeasure=%s, wasRecentMasterChange=%s, wasRecentPhraseChange=%s",
            //        isInMiddleOfMeasure, wasRecentMasterChange, wasRecentPhraseChange);
            // TE.log("Not a real phrase event -> filtering!");
            continue;
          }

          // was valid OSC mode event
          oscBeatModeOnlyOn = false;
          onPhraseChange(address, oscTE.timestamp);

        } else {
          // unrecognized OSC message!
          TE.log("Don't recognize OSC message: %s", address);
        }
      }
      // if (!msgs.equals(""))
      //    TE.log("OSC RECEIEVED: %s", msgs);

    } catch (Exception e) {
      TE.err("ERROR - unexpected exception in Autopilot.run(): %s", e.toString());
      e.printStackTrace(System.out);
    }

    // should we enter into non-OSC mode?
    try {
      // if we're not in this mode already, let's test to make sure
      if (!noOscModeOn && (now - history.getLastOscMsgAt() > OSC_TIMEOUT_MS)) {
        // it has been so long without a phrase that we need to enter
        // oscBeatModeOnlyOn=true and trigger phrases ourselves!
        this.resetHistory();
        noOscModeOn = true;
        oscBeatModeOnlyOn = false;

        // now trigger a phrase (DOWN is probably safest)
        String syntheticOscAddr = TEOscMessage.makeOscPhraseChangeAddress(TEPhrase.DOWN);
        TE.log("No OSC mode activated: triggering synthethic prhase=%s", syntheticOscAddr);
        history.setLastSynthethicPhraseAt(now);
        onPhraseChange(syntheticOscAddr, (long) now);

        // if we are in this mode already, just check if we need to trigger another phrase
      } else if (noOscModeOn) {
        double msInPhrase =
            TETimeUtils.calcPhraseLengthMs(lx.engine.tempo.bpm(), SYNTHETIC_PHRASE_LEN_BARS);
        if (now - history.getLastSynthethicPhraseAt() > msInPhrase) {
          // time to trigger a new phrase
          TEPhrase next = guessNextPhrase(curPhrase);
          String syntheticOscAddr = TEOscMessage.makeOscPhraseChangeAddress(next);
          history.setLastSynthethicPhraseAt(now);
          TE.log("No OSC mode, another phrase: triggering synthetic phrase=%s", syntheticOscAddr);
          onPhraseChange(syntheticOscAddr, (long) now);
        }
      }

    } catch (Exception oscBeatOnlyException) {
      TE.err("No OSC mode check, something went wrong: %s", oscBeatOnlyException);
      oscBeatOnlyException.printStackTrace();
    }

    // get some useful stats about the current phrase
    int repeatedPhraseCount = history.getRepeatedPhraseCount();
    double repeatedPhraseLengthBars = history.getRepeatedPhraseBarProgress(lx.engine.tempo.bpm());
    double currentPhraseLengthBars =
        repeatedPhraseLengthBars - history.getRepeatedPhraseLengthBars();

    // update ongoing transitions!
    try {
      // update fader value for NEXT channel
      if (nextChannelName != null) {
        int estPhraseLengthBars = 16;
        double estFracCompleted = currentPhraseLengthBars / estPhraseLengthBars;

        // over consecutive phrases, we want to steadily approach full fader, but never get there
        double normalizedNumPhrases = repeatedPhraseLengthBars / estPhraseLengthBars + 1.;
        double nextChannelFaderFloorLevel =
            Math.min(
                nextChannelFaderVal,
                LEVEL_FADE_IN
                    * (1.0 - Math.pow(0.5, normalizedNumPhrases - 1))); // 0 -> .5 - > .75  -> etc
        double nextChannelFaderCeilingLevel =
            LEVEL_FADE_IN * (1.0 - Math.pow(0.5, normalizedNumPhrases)); // .5 -> .75 -> .875 -> etc

        double range = nextChannelFaderCeilingLevel - nextChannelFaderFloorLevel;
        // can play around with the 1.5 exponent to make curve steeper!
        nextChannelFaderVal = range * Math.pow(estFracCompleted, 1.5) + nextChannelFaderFloorLevel;
        // TE.log("NextChannel: Setting fader=%s to %f", nextChannelName, nextChannelFaderVal);
        autoMixer.setFaderTo(nextChannelName, nextChannelFaderVal);
      }

      // fade out channels, if needed
      updateFadeOutChannels(currentPhraseLengthBars);

      // update FX channels, if needed
      updateFXChannels(currentPhraseLengthBars);

    } catch (IndexOutOfBoundsException e) {
      // no phrase events detected yet
    }
  }

  /**
   * Based on state set around phrase, update our faders with an eye towards the next expected
   * phrase.
   *
   * @param curPhraseLenBars: num contiguous bars in the current phrase type
   */
  private void updateFadeOutChannels(double curPhraseLenBars) {
    // update fader value for OLD NEXT channel
    if (oldNextChannelName != null
        && oldNextFadeOutMode
        && curPhraseLenBars < MISPREDICTED_FADE_OUT_BARS) {
      // TE.log("FADE OLD NEXT: Fading out %s", oldNextChannelName);
      double newVal =
          TEMath.ease(
              TEMath.EasingFunction.LINEAR_RAMP_DOWN,
              curPhraseLenBars,
              0.0,
              MISPREDICTED_FADE_OUT_BARS,
              LEVEL_OFF,
              LEVEL_MISPREDICTED_FADE_OUT);
      autoMixer.setFaderTo(oldNextChannelName, newVal);
    }

    // update fader for prev channel
    if (prevChannelName != null) {
      if (prevFadeOutMode && curPhraseLenBars < PREV_FADE_OUT_BARS) {
        // TE.log("FADE PREV: Fading out %s", prevChannelName);
        double newVal =
            TEMath.ease(
                TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                curPhraseLenBars,
                0.0,
                PREV_FADE_OUT_BARS,
                LEVEL_OFF,
                LEVEL_PREV_FADE_OUT);
        autoMixer.setFaderTo(prevChannelName, newVal);

      } else if (curPhraseLenBars >= PREV_FADE_OUT_BARS) {
        // TE.log("FADE PREV: Fading out %s", prevChannelName);
        autoMixer.setFaderTo(prevChannelName, LEVEL_OFF);
      }
    }
  }

  /**
   * Based on state set around phrase, update our FX faders with an eye towards the next expected
   * phrase.
   *
   * @param curPhraseLenBars
   */
  private void updateFXChannels(double curPhraseLenBars) {
    // Avoid crash
    // first, set strobes
    if (strobesChannel != null) {
      double newStrobeChannelVal = -1;
      if (strobesChannel.fader.getValue() > 0.0
          && curPhraseLenBars < STROBES_AT_CHORUS_LENGTH_BARS) {
        newStrobeChannelVal =
            TEMath.ease(
                TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                curPhraseLenBars,
                0.0,
                STROBES_AT_CHORUS_LENGTH_BARS,
                LEVEL_OFF,
                LEVEL_FULL);

      } else if (curPhraseLenBars >= PREV_FADE_OUT_BARS) {
        newStrobeChannelVal = LEVEL_OFF;
      }

      if (newStrobeChannelVal >= 0) {
        // TE.log("Strobes: Setting fader=%s to %f", TEChannelName.STROBES, newStrobeChannelVal);
        autoMixer.setFaderTo(TEChannelName.STROBES, newStrobeChannelVal);
      }
    }

    // now set triggers
    if (triggerChannel != null) {
      double newTriggerChannelVal = -1;
      if (triggerChannel.fader.getValue() > 0.0
          && curPhraseLenBars < TRIGGERS_AT_CHORUS_LENGTH_BARS) {
        newTriggerChannelVal =
            TEMath.ease(
                TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                curPhraseLenBars,
                0.0,
                TRIGGERS_AT_CHORUS_LENGTH_BARS,
                LEVEL_OFF,
                LEVEL_FULL);

      } else if (curPhraseLenBars >= TRIGGERS_AT_CHORUS_LENGTH_BARS) {
        newTriggerChannelVal = LEVEL_OFF;
      }

      if (newTriggerChannelVal >= 0) {
        // TE.log("Triggers: Setting fader=%s to %f", TEChannelName.TRIGGERS, newTriggerChannelVal);
        autoMixer.setFaderTo(TEChannelName.TRIGGERS, newTriggerChannelVal);
      }
    }
  }

  /**
   * Given a new phrase, update our phrase state around current, previous, and next phrase. This is
   * the foundation of how we fade in and out of different phrase-based channels to make transitions
   * happen.
   *
   * @param newPhrase
   */
  private void updatePhraseState(TEPhrase newPhrase) {
    oldNextFadeOutMode = false;

    // phrase state
    oldNextPhrase = nextPhrase;
    prevPhrase = curPhrase;
    curPhrase = newPhrase;
    nextPhrase = guessNextPhrase(newPhrase);
  }

  /**
   * Start an LXPattern. We want this to happen without delay.
   *
   * @param channel
   * @param pattern
   */
  private void startPattern(LXChannel channel, LXPattern pattern) {
    // disable the 100ms latency restriction LX has
    channel.transitionEnabled.setValue(false);

    // trigger the pattern
    channel.goPattern(pattern);

    // if we're in a mode where we don't know the exact phrase bounaries
    // then let's make the transition happen more slowly
    if (noOscModeOn || oscBeatModeOnlyOn) {
      channel.transitionEnabled.setValue(true);
      double secInTransition = TETimeUtils.calcPhraseLengthMs(lx.engine.tempo.bpm(), 8) / 1000.0;
      channel.transitionTimeSecs.setValue(secInTransition);
      return;
    } else {
      // turn transitions back off if we're in normal phrase OSC mode!
      channel.transitionEnabled.setValue(false);
      channel.transitionTimeSecs.setValue(0);
    }

    // if not, we want this to happen immediately
    channel.goPattern(pattern);
  }

  /**
   * Callback that happens when a new phrase is triggered.
   *
   * @param oscAddress String address that denotes what kind of phrase
   * @param timestamp when this phrase was triggered
   * @throws Exception
   */
  private void onPhraseChange(String oscAddress, long timestamp) throws Exception {
    // detect phrase type
    TEPhrase detectedPhrase = TEPhrase.resolvePhrase(oscAddress);
    if (detectedPhrase == TEPhrase.UNKNOWN)
      // skip if we don't understand the phrase
      return;

    // update state to reflect this
    this.updatePhraseState(detectedPhrase);
    boolean predictedCorrectly = (oldNextPhrase == curPhrase);
    boolean isSamePhrase = (prevPhrase == curPhrase);
    TE.log(
        "HIT: %s: [%s -> %s -> %s (?)], (old next: %s) OSC beats=%s No OSC=%s",
        curPhrase,
        prevPhrase,
        curPhrase,
        nextPhrase,
        oldNextPhrase,
        oscBeatModeOnlyOn,
        noOscModeOn);

    // record history for pattern library
    // need to do this before we a) pick new patterns, and b) logPhrase() with historian
    double numBarsInLastPhraseRun = history.getRepeatedPhraseBarProgress(lx.engine.tempo.bpm());
    if (numBarsInLastPhraseRun > 0) {
      LXPattern curPattern = curChannel.getActivePattern();
      LXPattern curNextPattern = nextChannel.getActivePattern();
      library.logPhrase(curPattern, curNextPattern, numBarsInLastPhraseRun);
    }

    // clear mixer state
    autoMixer.turnDownAllChannels(true);

    if (isSamePhrase) {
      // our current channel should just keep running!
      // our next channel should be reset to 0.0
      // past channel == current channel, so no transition down needed
      // TE.log("[AUTOVJ] Same phrase! no changes");

    } else {
      // update channel name & references based on phrase change
      prevChannelName = TEChannelName.getChannelNameFromPhraseType(prevPhrase);
      curChannelName = TEChannelName.getChannelNameFromPhraseType(curPhrase);
      nextChannelName = TEChannelName.getChannelNameFromPhraseType(nextPhrase);
      nextChannelFaderVal = 0.0;

      prevChannel = autoMixer.getChannelByName(prevChannelName);
      curChannel = autoMixer.getChannelByName(curChannelName);
      nextChannel = autoMixer.getChannelByName(nextChannelName);

      LXPattern newCurPattern = curChannel.getActivePattern();

      // set fader levels
      if (predictedCorrectly) {
        // if this was a transition away from a CHORUS or UP or DOWN into
        // diff phrase, we don't have strobes to cover, so let's echo
        prevFadeOutMode = (curPhrase == TEPhrase.DOWN || curPhrase == TEPhrase.UP);

        // we nailed it!
        // TE.log("[AUTOVJ] We predicted correctly: prevFadeOutMode=%s", prevFadeOutMode);

      } else {
        // we didn't predict the phrase change correctly, turn off
        // the channel we were trying to transition into
        oldNextChannelName = TEChannelName.getChannelNameFromPhraseType(oldNextPhrase);
        oldNextChannel = autoMixer.getChannelByName(oldNextChannelName);
        oldNextFadeOutMode = (oldNextChannel != null);
        // TE.log("[AUTOVJ] We didn't predict right, oldNextChannelName=%s, oldNextFadeOutMode=%s",
        // oldNextChannelName, oldNextFadeOutMode);

        // pick a new pattern for our current channel, since we didn't see this coming
        // try to make it compatible with the one we were fading in, since we'll fade that mistaken
        // one out
        if (prevChannel != null) {
          newCurPattern =
              this.library.pickRandomCompatibleNextPattern(
                  prevChannel.getActivePattern(), prevPhrase, curPhrase);
        } else {
          newCurPattern = this.library.pickRandomPattern(curPhrase);
        }

        // print the current active pattern, along with what we're going to change to
        // TE.log("active pattern in current channel: %s, going to change to=%s",
        // curChannel.getActivePattern(), newCurPattern);
        startPattern(curChannel, newCurPattern);
      }

      // imagine: UP --> DOWN --> ? (but really any misprediction)
      // our next predicted would be UP, thus prevPhrase == nextPhrase, and so we'd be fading
      // out of UP while also trying to fade in AND switching the pattern, giving us the worst of
      // all worlds:
      // we'd pick a new pattern (at full fader!) and not fade out of old one, and then likely
      // enter into a build where we flopped between at least two UPs. very bad look. this IF clause
      // prevents this!
      //
      // This could equivalently go in the:
      //
      //    if (predictedCorrectly) { ... }
      //
      // block, but I think it's clearer what's going on why here, and generalizes better if we
      // decide to
      // add more phrase types later!
      if (prevPhrase != nextPhrase) {
        // pick a pattern we'll start fading into on "nextChannel" during the new few bars
        LXPattern newNextPattern =
            this.library.pickRandomCompatibleNextPattern(newCurPattern, curPhrase, nextPhrase);
        startPattern(nextChannel, newNextPattern);
        // TE.log("Selected new next pattern: %s, for channel %s", newNextPattern, nextChannelName);
      }
    }

    autoMixer.setFaderTo(curChannelName, LEVEL_FULL);

    // trigger FX if needed
    this.enableFX(isSamePhrase);

    // change palette if needed, only on CHORUS starts, for now
    long msSincePaletteStart = System.currentTimeMillis() - history.getPaletteStartedAt();
    // TE.log("Palette: %s, isSamePhrase: %s, msSincePaletteStart > PALETTE_DURATION_MS: %s (now=%d,
    // msSincePaletteStart=%d, PALETTE_DURATION_MS=%d, paletteStartedAt=%d)"
    //        , curPhrase, isSamePhrase, msSincePaletteStart > PALETTE_DURATION_MS
    //        , System.currentTimeMillis(), msSincePaletteStart, PALETTE_DURATION_MS,
    // history.getPaletteStartedAt());
    if (curPhrase == TEPhrase.CHORUS
        && !isSamePhrase
        && msSincePaletteStart > PALETTE_DURATION_MS) {
      // do it immediately and proceed to the next one
      TE.log("Palette change!");
      changePaletteSwatch(false, true, 0);
      history.startPaletteTimer();
    }

    // add to historical log of events
    history.logPhrase(timestamp, curPhrase, lx.engine.tempo.bpm.getValue());
  }

  /**
   * Determines whether or not to trigger FX around important sonic events.
   *
   * @param isSamePhrase
   */
  private void enableFX(boolean isSamePhrase) {
    if (curPhrase != TEPhrase.CHORUS)
      // only hit FX on chorus starts
      return;

    Random rand = new Random();
    if (isSamePhrase && rand.nextFloat() > PROB_CLIPS_ON_SAME_PHRASE) {
      // if it's the same phrase repeated, let's only trigger clips
      // certain fraction of the time
      return;
    }

    // make new active patterns
    if (prevPhrase != curPhrase) {
      autoMixer.setFaderTo(TEChannelName.STROBES, LEVEL_FULL);
    }

    autoMixer.setFaderTo(TEChannelName.TRIGGERS, LEVEL_FULL);
  }

  private boolean startFailed = false;

  /**
   * This will get called when the state of the Enabled parameter changes.
   *
   * <p>This is the entry point to AutoVJ's running state. External controls including UI controls
   * should toggle the Enabled parameter to start it.
   *
   * @param on The new state of the Enabled parameter.
   */
  protected void onEnabled(boolean on) {
    if (on) {
      // Attempt to start AutoVJ
      TE.log("Attempting to start AutoVJ...");
      try {
        if (!startAutoVJ()) {
          // Failed to start.  Turn off the Enabled parameter.
          TE.log("...AutoVJ start failed.");
          this.startFailed = true;
          this.enabled.setValue(false);
        } else {
          // Successful start!  Now loop() can check this.enabled.isOn()
          TE.log("...AutoVJ start success.");
          // In case this listener doesn't fire twice after a failed attempt, reset the flag.
          this.startFailed = false;
        }
      } catch (Exception ex) {
        // REALLY failed to start.
        TE.err(ex, "...AutoVJ start failed in an unexpected manner:");
        this.startFailed = true;
        this.enabled.setValue(false);
      }
    } else {
      // Is this just turning off after a failed start attempt?  If so ignore.
      if (this.startFailed) {
        TE.log("...Detected AutoVJ turned off after failed start.");
        this.startFailed = false;
        return;
      } else {
        TE.log("Attempting to stop AutoVJ...");
        // AutoVJ was running, now disable it.  Enabled parameter has already been set to False.
        stopAutoVJ();
      }
    }
  }

  /**
   * Attempt to start.
   *
   * <p>Return true if successful.
   */
  private boolean startAutoVJ() {
    // make sure AutoVJ channels and group are set up correctly
    boolean setupWasNeeded = this.autoMixer.ensureSetup();
    if (setupWasNeeded) {
      if (!resetHistory()) {
        // Failed.
        return false;
      }
    }

    TE.log("AutoVJ started!");
    return true;
  }

  /** Do any Auto-VJ disable actions here. */
  private void stopAutoVJ() {
    // No need to set the enabled parameter here, it's already off.

    TE.log("AutoVJ disabled!");
  }

  /**
   * Guess the next phrase based on new current phrase and potentially older ones. This is
   * rule-based for now.
   *
   * @param newPhrase
   * @return
   */
  public TEPhrase guessNextPhrase(TEPhrase newPhrase) {
    // TODO(will) make this smarter
    boolean isSame = false;
    try {
      TEPhraseEvent prevPhraseEvt = history.phraseEvents.get(history.phraseEvents.size() - 2);
      isSame = (newPhrase == prevPhraseEvt.getPhraseType());
    } catch (IndexOutOfBoundsException e) {
      // there was no prev phrase event in history!
    } catch (NoSuchElementException nsee) {
      // same, no such prev phrase event
    }

    TEPhrase estimatedNextPhrase;

    // very dumb rule-based approach for now
    if (newPhrase == TEPhrase.TRO) estimatedNextPhrase = TEPhrase.UP;
    else if (newPhrase == TEPhrase.UP) estimatedNextPhrase = TEPhrase.CHORUS;
    else if (newPhrase == TEPhrase.DOWN) estimatedNextPhrase = TEPhrase.UP;
    else if (newPhrase == TEPhrase.CHORUS) estimatedNextPhrase = TEPhrase.DOWN;
    else estimatedNextPhrase = TEPhrase.DOWN;

    return estimatedNextPhrase;
  }

  // Keep this for possible future reworking of the autopilot startup
  private boolean wasAutopilotEnabled = false;

  @Override
  public void projectChanged(File file, Change change) {
    if (change == Change.TRY || change == Change.NEW) {
      // About to do an openFile
      this.wasAutopilotEnabled = this.enabled.isOn();

      // JKB note: Ok the Change.Open listener gets broadcast *after* the objects are loaded from
      // file,
      // so I think we'd better release the channel references here
      releaseProjectReferences();

      if (this.enabled.isOn()) {
        LX.log("Disabling Autopilot for file open...");
        this.enabled.setValue(false);
      }
    } else if (change == Change.OPEN) {
      // This could be the first file open or a later file open.
      LX.log(
          "Autopilot detected completion of openProject(), defensive positions have been taken (or is first file open)...");

      // Don't need this, it gets called by:
      //   TEUserInterface.AutopilotComponent.autopilotEnabledToggle restoring from saved file...
      //   ...which triggers TEApp.autopilotEnableListener
      //   ...which calls setEnable(savedFromFile)
      // resetHistory();

      // Don't need this either, see above note.  But in the future you might want to do this here,
      // so leaving for reference:
      /*
      if (this.wasAutopilotEnabled) {
      	LX.log("Re-enabling autopilot after file open");
      	this.wasAutopilotEnabled = false;
      	setEnabled(true);
      }
      */
    }
  }

  private void releaseProjectReferences() {
    this.prevChannel = null;
    this.curChannel = null;
    this.nextChannel = null;
    this.oldNextChannel = null;
    this.triggerChannel = null;
    this.strobesChannel = null;
    this.fxChannel = null;
  }

  @Override
  public void dispose() {
    this.enabled.removeListener(enabledListener);
    super.dispose();
  }
}
