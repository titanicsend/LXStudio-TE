package titanicsend.lx;

import heronarts.lx.LX;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import titanicsend.pattern.TEPerformancePattern;

@LXMidiSurface.Name("Akai APC40 mkII TE")
@LXMidiSurface.DeviceName("APC40 mkII")
public class APC40Mk2 extends heronarts.lx.midi.surface.APC40Mk2 {

  public static enum UserButton {
    PAN(APC40Mk2.PAN),
    SENDS(APC40Mk2.SENDS),
    USER(APC40Mk2.USER);

    public final int note;

    private UserButton(int note) {
      this.note = note;
    }
  }

  // Bi-directional map, late night version
  private static final Map<UserButton, BooleanParameter> userButtons =
      new HashMap<UserButton, BooleanParameter>();
  private static final Map<BooleanParameter, UserButton> userButtonsRev =
      new HashMap<BooleanParameter, UserButton>();

  public static void setUserButton(UserButton button, BooleanParameter parameter) {
    BooleanParameter oldParameter = userButtons.get(button);
    if (parameter != null) {
      userButtons.put(button, parameter);
      userButtonsRev.put(parameter, button);
    } else {
      if (userButtons.containsKey(button)) {
        userButtonsRev.remove(userButtons.get(button));
        userButtons.remove(button);
      }
    }
    // Notify surfaces
    for (APC40Mk2 surface : surfaces) {
      surface.userButtonChanged(button, oldParameter, parameter);
    }
  }

  private static final List<APC40Mk2> surfaces = new ArrayList<APC40Mk2>();

  private boolean shiftOn = false;

  private int[] channelKnobValues = new int[CHANNEL_KNOB_NUM];

  public APC40Mk2(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
    surfaces.add(this);

    // Initialize channel knob values
    Arrays.fill(this.channelKnobValues, 0);
  }

  @Override
  protected void onEnable(boolean on) {
    super.onEnable(on);
    if (on) {
      registerUserButtons();
    } else {
      if (this.isRegistered) {
        unregisterUserButtons();
      }
    }
  }

  @Override
  protected void onReconnect() {
    super.onReconnect();
    if (this.enabled.isOn()) {
      sendUserButtons();
    }
  }

  private boolean isRegistered = false;

  private final LXParameterListener userButtonListener =
      (p) -> {
        UserButton button = userButtonsRev.get((BooleanParameter) p);
        sendUserButton(button, ((BooleanParameter) p).isOn());
      };

  private void registerUserButtons() {
    if (this.isRegistered) {
      throw new IllegalStateException("APC40Mk2 user buttons are already registered.");
    }
    this.isRegistered = true;

    for (Entry<UserButton, BooleanParameter> entrySet : userButtons.entrySet()) {
      registerUserButton(entrySet.getValue());
    }

    sendUserButtons();
  }

  private void unregisterUserButtons() {
    if (!this.isRegistered) {
      throw new IllegalStateException("APC40Mk2 user buttons are not registered.");
    }
    this.isRegistered = false;

    // User buttons.  Improve later:
    for (Entry<UserButton, BooleanParameter> entrySet : userButtons.entrySet()) {
      unregisterUserButton(entrySet.getValue());
    }

    sendUserButtons();
  }

  private void registerUserButton(BooleanParameter parameter) {
    parameter.addListener(userButtonListener, true);
  }

  private void unregisterUserButton(BooleanParameter parameter) {
    parameter.removeListener(userButtonListener);
  }

  /**
   * A static call registers a parameter for all instances of the surface, which notifies each
   * surface so they can listen to the parameters.
   */
  private void userButtonChanged(
      UserButton button, BooleanParameter oldParameter, BooleanParameter newParameter) {
    if (this.isRegistered) {
      if (oldParameter != null) {
        unregisterUserButton(oldParameter);
      }
      if (newParameter != null) {
        registerUserButton(newParameter);
      }
    }
  }

  /* JKB: can't remember the point of this one
  @Override
  protected void onSetAux(boolean isAux) {
    sendUserButtons();
  }*/

  private void sendUserButtons() {
    for (UserButton button : UserButton.values()) {
      BooleanParameter parameter = userButtons.get(button);
      sendUserButton(button, parameter != null && parameter.isOn());
    }
  }

  private void sendUserButton(UserButton button, boolean on) {
    sendNoteOn(0, button.note, on ? LED_ON : LED_OFF);
  }

  private boolean noteReceived(MidiNote note, boolean on) {
    int pitch = note.getPitch();

    // Track shift state (temporary until available from upstream)
    if (pitch == SHIFT) {
      this.shiftOn = on;
      if (on) {
        // TODO: Send normalized value for selected preset? So far it seems okay...
      } else {
        // When shift is release, send (restore) channel knob values for pattern browsing
        for (int i = 0; i < CHANNEL_KNOB_NUM; i++) {
          sendControlChange(0, CHANNEL_KNOB + i, this.channelKnobValues[i]);
        }
      }
      return false;
    }

    if (on) {
      for (UserButton userButton : UserButton.values()) {
        if (pitch == userButton.note) {
          // A user button was pushed.  Do we have custom mappings?
          if (userButtons.containsKey(userButton)) {
            userButtons.get(userButton).toggle();
          }
          // It was a user button with no associated parameters
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    if (noteReceived(note, true)) {
      return;
    }
    super.noteOnReceived(note);
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    if (noteReceived(note, false)) {
      return;
    }
    super.noteOffReceived(note);
  }

  @Override
  public void controlChangeReceived(MidiControlChange cc) {
    int number = cc.getCC();

    // TE: Channel knobs set focused pattern, if fader is down.
    if (number >= CHANNEL_KNOB && number <= CHANNEL_KNOB_MAX) {
      int channelIndex = number - CHANNEL_KNOB;
      if (channelIndex < this.lx.engine.mixer.channels.size()) {
        LXAbstractChannel abstractChannel = this.lx.engine.mixer.channels.get(channelIndex);
        if (abstractChannel instanceof LXChannel channel && isNotLive(abstractChannel)) {
          // Knob corresponds to channel, and channel is not live.
          if (this.shiftOn) {
            // Change user preset on selected pattern
            LXPattern pattern = channel.getActivePattern();
            if (pattern instanceof TEPerformancePattern tePattern) {
              tePattern.getControls().getPresetSelectorOffair().setNormalized(cc.getNormalized());
            }
          } else {
            // Remember knob position in case shift is pressed
            this.channelKnobValues[channelIndex] = cc.getValue();
            // Change active pattern on the channel
            int numPatterns = channel.patterns.size();
            if (numPatterns > 0) {
              double normalized = cc.getNormalized();
              // Set active pattern
              channel.goPatternIndex((int) (normalized * (numPatterns - 1)));
              // Alternative for focused pattern
              // ((LXChannel)bus).focusedPattern.setNormalized(normalized);
            }
          }
        }
      }
      return;
    }

    super.controlChangeReceived(cc);
  }

  private boolean isNotLive(LXAbstractChannel abstractChannel) {
    return abstractChannel.fader.getValue() == 0 || !abstractChannel.enabled.isOn();
  }

  @Override
  public void dispose() {
    if (this.isRegistered) {
      unregisterUserButtons();
    }
    surfaces.remove(this);
    super.dispose();
  }
}
