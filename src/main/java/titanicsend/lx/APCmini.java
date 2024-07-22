/**
 * Copyright 2022- Justin Belcher, Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package titanicsend.lx;

import heronarts.lx.midi.surface.LXMidiSurface;
import java.util.HashMap;
import java.util.Map;

import heronarts.lx.LX;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.clip.LXClip;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXGroup;
import heronarts.lx.mixer.LXMixerEngine;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;

public class APCmini extends LXMidiSurface implements LXMidiSurface.Bidirectional {

  public static final String DEVICE_NAME = "APC MINI";

  public static final int NUM_CHANNELS = 8;
  public static final double PARAMETER_INCREMENT_AMOUNT = 0.1;


  // CCs
  public static final int CHANNEL_FADER = 48;
  public static final int CHANNEL_FADER_MAX = CHANNEL_FADER + NUM_CHANNELS - 1;
  public static final int MASTER_FADER = 56;


  // Notes
  public static final int MIDI_CHANNEL = 0;

  public static final int CLIP_LAUNCH = 0;
  public static final int CLIP_LAUNCH_ROWS = 8;
  public static final int CLIP_LAUNCH_COLUMNS = NUM_CHANNELS;
  public static final int CLIP_LAUNCH_NUM = CLIP_LAUNCH_ROWS * CLIP_LAUNCH_COLUMNS;
  public static final int CLIP_LAUNCH_MAX = CLIP_LAUNCH + CLIP_LAUNCH_NUM - 1;

  public static final int CHANNEL_BUTTON = 64;
  public static final int CHANNEL_BUTTON_MAX = CHANNEL_BUTTON + NUM_CHANNELS - 1;

  public static final int SCENE_LAUNCH = 82;
  public static final int SCENE_LAUNCH_NUM = 6;
  public static final int SCENE_LAUNCH_MAX = SCENE_LAUNCH + SCENE_LAUNCH_NUM - 1;

  public static final int TOGGLE_CLIPS = 88;
  public static final int TOGGLE_PARAMETERS = 89;

  public static final int PARAMETER_COLUMNS = 8;
  public static final int PARAMETER_COLUMN_STRIDE = 1;
  public static final int PARAMETER_ROWS = 2;
  public static final int PARAMETER_ROW_STRIDE = -4;
  public static final int PARAMETER_NUM = PARAMETER_COLUMNS * PARAMETER_ROWS;
  public static final int PARAMETER_START = (CLIP_LAUNCH_ROWS - 1) * CLIP_LAUNCH_COLUMNS + CLIP_LAUNCH;


  // Notes in combination with Shift
  public static final int SHIFT = 98;

  public static final int SELECT_UP = 64;
  public static final int SELECT_DOWN = 65;
  public static final int SELECT_LEFT = 66;
  public static final int SELECT_RIGHT = 67;

  public static final int FADER_CTRL_VOLUME = 68;
  public static final int FADER_CTRL_PAN = 69;
  public static final int FADER_CTRL_SEND = 70;
  public static final int FADER_CTRL_DEVICE = 71;

  public static final int CHANNEL_BUTTON_FOCUS = FADER_CTRL_VOLUME;
  public static final int CHANNEL_BUTTON_ENABLED = FADER_CTRL_PAN;
  public static final int CHANNEL_BUTTON_CUE = FADER_CTRL_SEND;
  public static final int CHANNEL_BUTTON_ARM = FADER_CTRL_DEVICE;
  public static final int CHANNEL_BUTTON_CROSSFADEGROUP = 0;

  public static final int CLIP_STOP = 82;
  public static final int SOLO = 83;
  public static final int REC_ARM = 84;
  public static final int MUTE = 85;
  public static final int SELECT = 86;
  public static final int EXTRA1 = 87;
  public static final int EXTRA2 = 88;
  public static final int STOP_ALL_CLIPS = 89;


  // LED color definitions

  // Single and multi-color buttons
  public static final int LED_OFF = 0;

  // Single color buttons
  public static final int LED_ON = 1;
  public static final int LED_BLINK = 2;

  // Multi color buttons
  public static final int LED_GREEN = 1;
  public static final int LED_GREEN_BLINK = 2;
  public static final int LED_RED = 3;
  public static final int LED_RED_BLINK = 4;
  public static final int LED_YELLOW = 5;
  public static final int LED_YELLOW_BLINK = 6;

  // Configurable color options
  public static final int LED_PATTERN_ACTIVE = LED_RED;
  public static final int LED_PATTERN_TRANSITION = LED_RED_BLINK;
  public static final int LED_PATTERN_FOCUSED = LED_YELLOW_BLINK;
  public static final int LED_PATTERN_INACTIVE = LED_YELLOW;

  public static final int LED_CLIP_INACTIVE = LED_YELLOW;
  public static final int LED_CLIP_PLAY = LED_GREEN;
  public static final int LED_CLIP_ARM = LED_RED;
  public static final int LED_CLIP_RECORD = LED_RED_BLINK;

  public static final int LED_PARAMETER_INCREMENT = LED_GREEN;
  public static final int LED_PARAMETER_DECREMENT = LED_YELLOW;
  public static final int LED_PARAMETER_RESET = LED_RED;
  public static final int LED_PARAMETER_ISDEFAULT = LED_OFF;

  public enum ChannelButtonMode {
    ARM,
    CROSSFADEGROUP,
    CUE,
    ENABLED,
    FOCUS
  };

  private ChannelButtonMode channelButtonMode = ChannelButtonMode.FOCUS;

  public enum GridMode {
    PATTERNS,
    PARAMETERS,
    CLIPS
  };

  private GridMode gridMode = GridMode.PATTERNS;

  private boolean shiftOn = false;

  private final Map<LXAbstractChannel, ChannelListener> channelListeners = new HashMap<LXAbstractChannel, ChannelListener>();

  private final DeviceListener deviceListener = new DeviceListener();

  private class DeviceListener implements LXParameterListener {

    private LXDeviceComponent device = null;
    private LXEffect effect = null;
    private LXPattern pattern = null;
    private LXBus channel = null;

    private final LXListenableNormalizedParameter[] knobs =
        new LXListenableNormalizedParameter[PARAMETER_NUM];

    DeviceListener() {
      for (int i = 0; i < this.knobs.length; ++i) {
        this.knobs[i] = null;
      }
    }

    void resend() {
      if (gridMode == GridMode.PARAMETERS) {
        for (int i = 0; i < this.knobs.length; ++i) {
          LXListenableNormalizedParameter parameter = this.knobs[i];
          int patternButton = getPatternButton(i);
          if (parameter != null) {
            sendNoteOn(MIDI_CHANNEL, patternButton, LED_PARAMETER_INCREMENT);
            sendNoteOn(MIDI_CHANNEL, patternButton - CLIP_LAUNCH_COLUMNS, LED_PARAMETER_DECREMENT);
            sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 2), parameter.isDefault() ? LED_PARAMETER_ISDEFAULT : LED_PARAMETER_RESET);
          } else {
            sendNoteOn(MIDI_CHANNEL, patternButton, LED_OFF);
            sendNoteOn(MIDI_CHANNEL, patternButton - CLIP_LAUNCH_COLUMNS, LED_OFF);
            sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_OFF);
          }
          sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
        }
      }
    }

    int getPatternButton(int index) {
      int row = index / PARAMETER_COLUMNS;
      int column = index % PARAMETER_COLUMNS;
      return PARAMETER_START + (row * CLIP_LAUNCH_COLUMNS * PARAMETER_ROW_STRIDE) + (column * PARAMETER_COLUMN_STRIDE);
    }

    void registerChannel(LXBus channel) {
      unregisterChannel();
      this.channel = channel;
      if (channel instanceof LXChannel) {
        ((LXChannel) channel).focusedPattern.addListener(this);
        register(((LXChannel) channel).getFocusedPattern());
      } else if (channel.effects.size() > 0) {
        register(channel.getEffect(0));
      } else {
        register(null);
      }
    }

    void registerPrevious() {
      if (this.effect != null) {
        int effectIndex = this.effect.getIndex();
        if (effectIndex > 0) {
          register(this.effect.getBus().getEffect(effectIndex - 1));
        } else if (this.channel instanceof LXChannel) {
          register(((LXChannel) this.channel).getFocusedPattern());
        }
      }
    }

    void registerNext() {
      if (this.effect != null) {
        int effectIndex = this.effect.getIndex();
        if (effectIndex < this.effect.getBus().effects.size() - 1) {
          register(this.effect.getBus().getEffect(effectIndex + 1));
        }
      } else if (this.pattern != null) {
        if (channel.effects.size() > 0) {
          register(channel.getEffect(0));
        }
      }
    }

    void register(LXDeviceComponent device) {
      if (this.device != device) {
        unregister(false);
        this.device = device;
        if (this.device instanceof LXPattern) {
          this.pattern = (LXPattern) this.device;
        }

        int i = 0;
        if (this.device != null) {
          for (LXListenableNormalizedParameter parameter : this.device.getRemoteControls()) {
            if (i >= this.knobs.length) {
              break;
            }
            this.knobs[i] = parameter;
            int patternButton = getPatternButton(i);
            if (parameter != null) {
              parameter.addListener(this);
              if (gridMode == GridMode.PARAMETERS) {
                sendNoteOn(MIDI_CHANNEL, patternButton, LED_PARAMETER_INCREMENT);
                sendNoteOn(MIDI_CHANNEL, patternButton - CLIP_LAUNCH_COLUMNS, LED_PARAMETER_DECREMENT);
                sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 2), parameter.isDefault() ? LED_PARAMETER_ISDEFAULT : LED_PARAMETER_RESET);
                sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
              }
            } else {
              sendNoteOn(MIDI_CHANNEL, patternButton, LED_OFF);
              sendNoteOn(MIDI_CHANNEL, patternButton - CLIP_LAUNCH_COLUMNS, LED_OFF);
              sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_OFF);
              sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
            }
            ++i;
          }
          this.device.controlSurfaceSemaphore.increment();
        }
        if (gridMode == GridMode.PARAMETERS) {
          while (i < this.knobs.length) {
            int patternButton = getPatternButton(i);
            sendNoteOn(MIDI_CHANNEL, patternButton, LED_OFF);
            sendNoteOn(MIDI_CHANNEL, patternButton - CLIP_LAUNCH_COLUMNS, LED_OFF);
            sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_OFF);
            sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
            ++i;
          }
        }
      }
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
      if ((this.channel != null) &&
          (this.channel instanceof LXChannel) &&
          (parameter == ((LXChannel)this.channel).focusedPattern)) {
        if ((this.device == null) || (this.device instanceof LXPattern)) {
          register(((LXChannel) this.channel).getFocusedPattern());
        }
      } else {
        if (gridMode == GridMode.PARAMETERS) {
          for (int i = 0; i < this.knobs.length; ++i) {
            if (parameter == this.knobs[i]) {
              int patternButton = getPatternButton(i);
              sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 2), ((LXListenableNormalizedParameter)parameter).isDefault() ? LED_PARAMETER_ISDEFAULT : LED_PARAMETER_RESET);
              break;
            }
          }
        }
      }
    }

    void onParameterButton(int columnIndex, int rowIndex) {
      int paramIndex = 0;
      int button = rowIndex;
      while (button > 3) {
        paramIndex += PARAMETER_COLUMNS;
        button -= 4;
      }
      paramIndex += columnIndex;

      LXListenableNormalizedParameter param = this.knobs[paramIndex];
      if (param != null) {
        switch (button) {
          case 0:
            if (param instanceof BooleanParameter) {
              ((BooleanParameter)param).setValue(true);
            } else if (param instanceof DiscreteParameter) {
              ((DiscreteParameter)param).increment();
            } else {
              param.setNormalized(param.getNormalized() + PARAMETER_INCREMENT_AMOUNT);
            }
            break;
          case 1:
            if (param instanceof BooleanParameter) {
              ((BooleanParameter)param).setValue(false);
            } else if (param instanceof DiscreteParameter) {
              ((DiscreteParameter)param).decrement();
            } else {
              param.setNormalized(param.getNormalized() - PARAMETER_INCREMENT_AMOUNT);
            }
            break;
          case 2:
            param.reset();
            break;
        }
      }
    }

    private void unregister(boolean clearParams) {
      if (this.device != null) {
        for (int i = 0; i < this.knobs.length; ++i) {
          if (this.knobs[i] != null) {
            this.knobs[i].removeListener(this);
            this.knobs[i] = null;
            if (gridMode == GridMode.PARAMETERS && clearParams) {
              int patternButton = getPatternButton(i);
              sendNoteOn(MIDI_CHANNEL, patternButton, LED_OFF);
              sendNoteOn(MIDI_CHANNEL, patternButton - CLIP_LAUNCH_COLUMNS, LED_OFF);
              sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_OFF);
              sendNoteOn(MIDI_CHANNEL, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
            }
          }
        }
        this.device.controlSurfaceSemaphore.decrement();
      }
      this.pattern = null;
      this.effect = null;
      this.device = null;
    }

    private void unregisterChannel() {
      if (this.channel != null) {
        if (this.channel instanceof LXChannel) {
          ((LXChannel) this.channel).focusedPattern.removeListener(this);
        }
      }
      this.channel = null;
    }

    private void dispose() {
      unregister(true);
      unregisterChannel();
    }

  }

  private class ChannelListener implements LXChannel.Listener, LXBus.ClipListener, LXParameterListener {

    private final LXAbstractChannel channel;

    ChannelListener(LXAbstractChannel channel) {
      this.channel = channel;
      if (channel instanceof LXChannel) {
        ((LXChannel) channel).addListener(this);
      } else {
        channel.addListener(this);
      }
      channel.addClipListener(this);
      channel.cueActive.addListener(this);
      channel.enabled.addListener(this);
      channel.crossfadeGroup.addListener(this);
      channel.arm.addListener(this);
      if (channel instanceof LXChannel) {
        LXChannel c = (LXChannel) channel;
        c.focusedPattern.addListener(this);
        c.controlSurfaceFocusLength.setValue(CLIP_LAUNCH_ROWS);
        int focusedPatternIndex = c.getFocusedPatternIndex();
        c.controlSurfaceFocusIndex.setValue(focusedPatternIndex < CLIP_LAUNCH_ROWS ? 0 : (focusedPatternIndex - CLIP_LAUNCH_ROWS + 1));
      }
      for (LXClip clip : this.channel.clips) {
        if (clip != null) {
          clip.running.addListener(this);
        }
      }
    }

    public void dispose() {
      if (this.channel instanceof LXChannel) {
        ((LXChannel) this.channel).removeListener(this);
      } else {
        this.channel.removeListener(this);
      }
      this.channel.removeClipListener(this);
      this.channel.cueActive.removeListener(this);
      this.channel.enabled.removeListener(this);
      this.channel.crossfadeGroup.removeListener(this);
      this.channel.arm.removeListener(this);
      if (this.channel instanceof LXChannel) {
        LXChannel c = (LXChannel) this.channel;
        c.focusedPattern.removeListener(this);
        c.controlSurfaceFocusLength.setValue(0);
        c.controlSurfaceFocusIndex.setValue(0);
      }
      for (LXClip clip : this.channel.clips) {
        if (clip != null) {
          clip.running.removeListener(this);
        }
      }
    }

    public void onParameterChanged(LXParameter p) {
      int index = this.channel.getIndex();
      if (index >= CLIP_LAUNCH_COLUMNS) {
        return;
      }

      if (p == this.channel.cueActive) {
        if (channelButtonMode == ChannelButtonMode.CUE) {
          sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, this.channel.cueActive.isOn() ? LED_ON : LED_OFF);
        }
      } else if (p == this.channel.enabled) {
        if (channelButtonMode == ChannelButtonMode.ENABLED) {
          sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, this.channel.enabled.isOn() ? LED_ON : LED_OFF);
        }
      } else if (p == this.channel.crossfadeGroup) {
        // Button press toggles through the 3 modes. Button does not stay lit.
        sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, LED_OFF);
      } else if (p == this.channel.arm) {
        sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, channel.arm.isOn() ? LED_ON : LED_OFF);
        sendChannelClips(this.channel.getIndex(), this.channel);
      } else if (p.getParent() instanceof LXClip) {
        LXClip clip = (LXClip)p.getParent();
        sendClip(index, this.channel, clip.getIndex(), clip);
      }
      if (this.channel instanceof LXChannel) {
        LXChannel c = (LXChannel) this.channel;
        if (p == c.focusedPattern) {
          int focusedPatternIndex = c.getFocusedPatternIndex();
          int channelSurfaceIndex = c.controlSurfaceFocusIndex.getValuei();
          if (focusedPatternIndex < channelSurfaceIndex) {
            c.controlSurfaceFocusIndex.setValue(focusedPatternIndex);
          } else if (focusedPatternIndex >= channelSurfaceIndex + CLIP_LAUNCH_ROWS) {
            c.controlSurfaceFocusIndex.setValue(focusedPatternIndex - CLIP_LAUNCH_ROWS + 1);
          }
          sendChannelPatterns(index, c);
        }
      }
    }

    @Override
    public void effectAdded(LXBus channel, LXEffect effect) {
    }

    @Override
    public void effectRemoved(LXBus channel, LXEffect effect) {
    }

    @Override
    public void effectMoved(LXBus channel, LXEffect effect) {
      // TODO(mcslee): update device focus??  *JKB: Note retained from APC40mkII
    }

    @Override
    public void indexChanged(LXAbstractChannel channel) {
      // Handled by the engine channelMoved listener.
    }

    @Override
    public void groupChanged(LXChannel channel, LXGroup group) {

    }

    @Override
    public void patternAdded(LXChannel channel, LXPattern pattern) {
      if (gridMode == GridMode.PATTERNS) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void patternRemoved(LXChannel channel, LXPattern pattern) {
      if (gridMode == GridMode.PATTERNS) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void patternMoved(LXChannel channel, LXPattern pattern) {
      if (gridMode == GridMode.PATTERNS) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void patternWillChange(LXChannel channel, LXPattern pattern, LXPattern nextPattern) {
      if (gridMode == GridMode.PATTERNS) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void patternDidChange(LXChannel channel, LXPattern pattern) {
      if (gridMode == GridMode.PATTERNS) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void clipAdded(LXBus bus, LXClip clip) {
      clip.running.addListener(this);
      sendClip(this.channel.getIndex(), this.channel, clip.getIndex(), clip);
    }

    @Override
    public void clipRemoved(LXBus bus, LXClip clip) {
      clip.running.removeListener(this);
      sendChannelClips(this.channel.getIndex(), this.channel);
    }

  }

  public APCmini(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
  }

  @Override
  protected void onEnable(boolean on) {
    if (on) {
      initialize(false);
      register();
    } else {
      this.deviceListener.register(null);
      for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
        if (channel instanceof LXChannel) {
          ((LXChannel)channel).controlSurfaceFocusLength.setValue(0);
        }
      }
      if (this.isRegistered) {
        unregister();
      }
    }
  }

  @Override
  protected void onReconnect() {
    if (this.enabled.isOn()) {
      initialize(true);
      this.deviceListener.resend();
    }
  }

  private void initialize(boolean reconnect) {
    sendGrid();
  }

  private void sendGrid() {
    sendNoteOn(MIDI_CHANNEL, TOGGLE_CLIPS, this.gridMode == GridMode.CLIPS ? LED_ON : LED_OFF);
    sendNoteOn(MIDI_CHANNEL, TOGGLE_PARAMETERS, this.gridMode == GridMode.PARAMETERS ? LED_ON : LED_OFF);
    sendChannelButtonRow();
    if (this.gridMode == GridMode.PARAMETERS) {
      this.deviceListener.resend();
    } else {
      for (int i = 0; i < NUM_CHANNELS; ++i) {
        LXAbstractChannel channel = getChannel(i);
        switch (this.gridMode) {
          case PATTERNS:
            sendChannelPatterns(i, channel);
            break;
          case CLIPS:
            sendChannelClips(i, channel);
            break;
          case PARAMETERS:
            break;
        }
      }
    }
  }

  private void clearGrid() {
    sendNoteOn(MIDI_CHANNEL, TOGGLE_CLIPS, LED_OFF);
    sendNoteOn(MIDI_CHANNEL, TOGGLE_PARAMETERS, LED_OFF);
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendChannelPatterns(i, null);
    }
  }

  private void sendChannelPatterns(int index, LXAbstractChannel channelBus) {
    if (index >= CLIP_LAUNCH_COLUMNS) {
      return;
    }
    if (channelBus instanceof LXChannel) {
      LXChannel channel = (LXChannel) channelBus;
      int baseIndex = channel.controlSurfaceFocusIndex.getValuei();
      int endIndex = channel.patterns.size() - baseIndex;
      int activeIndex = channel.getActivePatternIndex() - baseIndex;
      int nextIndex = channel.getNextPatternIndex() - baseIndex;
      int focusedIndex = channel.focusedPattern.getValuei() - baseIndex;
      if (channel.patterns.size() == 0) {
        focusedIndex = -1;
      }
      for (int y = 0; y < CLIP_LAUNCH_ROWS; ++y) {
        int note = CLIP_LAUNCH + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - y) + index;
        int color = LED_OFF;
        if (y == activeIndex) {
          // This pattern is active (may also be focused)
          color = LED_PATTERN_ACTIVE;
        } else if (y == nextIndex) {
          // This pattern is being transitioned to
          color = LED_PATTERN_TRANSITION;
        } else if (y == focusedIndex) {
          // This pattern is not active, but it is focused
          color = LED_PATTERN_FOCUSED;
        } else if (y < endIndex) {
          // There is a pattern present
          color = LED_PATTERN_INACTIVE;
        }

        sendNoteOn(MIDI_CHANNEL, note, color);
      }
    } else {
      for (int y = 0; y < CLIP_LAUNCH_ROWS; ++y) {
        sendNoteOn(
            MIDI_CHANNEL,
            CLIP_LAUNCH + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - y) + index,
            LED_OFF
        );
      }
    }
  }

  private void sendChannelClips(int index, LXAbstractChannel channel) {
    for (int i = 0; i < CLIP_LAUNCH_ROWS; ++i) {
      LXClip clip = null;
      if (channel != null) {
        clip = channel.getClip(i);
      }
      sendClip(index, channel, i, clip);
    }
  }

  private void sendClip(int channelIndex, LXAbstractChannel channel, int clipIndex, LXClip clip) {
    if (this.gridMode != GridMode.CLIPS || channelIndex >= CLIP_LAUNCH_COLUMNS || clipIndex >= CLIP_LAUNCH_ROWS) {
      return;
    }
    int color = LED_OFF;
    int pitch = CLIP_LAUNCH + channelIndex + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - clipIndex);
    if (channel != null && clip != null) {
      if (channel.arm.isOn()) {
        color = clip.isRunning() ? LED_CLIP_RECORD : LED_CLIP_ARM;
      } else {
        color = clip.isRunning() ? LED_CLIP_PLAY : LED_CLIP_INACTIVE;
      }
    }
    sendNoteOn(MIDI_CHANNEL, pitch, color);
  }

  private void sendChannelFocus() {
    if (this.channelButtonMode == ChannelButtonMode.FOCUS && !this.shiftOn) {
      sendChannelButtonRow();
    }
  }

  private void setChannelButtonMode(ChannelButtonMode mode) {
    this.channelButtonMode = mode;
    sendChannelButtonRow();
  }

  private void sendChannelButtonRow() {
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendChannelButton(i, getChannel(i));
    }
  }

  private void clearChannelButtonRow() {
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + i, LED_OFF);
    }
  }

  private void sendChannelButton(int index, LXAbstractChannel channel) {
    if (this.shiftOn) {
      // Shift
      int shiftCode = index + CHANNEL_BUTTON;
      int color = LED_OFF;
      switch (shiftCode) {
        case CHANNEL_BUTTON_FOCUS:
          color = this.channelButtonMode == ChannelButtonMode.FOCUS ? LED_ON : LED_OFF;
          break;
        case CHANNEL_BUTTON_ENABLED:
          color = this.channelButtonMode == ChannelButtonMode.ENABLED ? LED_ON : LED_OFF;
          break;
        case CHANNEL_BUTTON_CUE:
          color = this.channelButtonMode == ChannelButtonMode.CUE ? LED_ON : LED_OFF;
          break;
        case CHANNEL_BUTTON_ARM:
          color = this.channelButtonMode == ChannelButtonMode.ARM ? LED_ON : LED_OFF;
          break;
      }
      sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, color);
    } else {
      // Not shift
      if (channel != null) {
        switch (this.channelButtonMode) {
          case FOCUS:
            sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, index == this.lx.engine.mixer.focusedChannel.getValuei() ? LED_ON : LED_OFF);
            break;
          case ENABLED:
            sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, channel.enabled.isOn() ? LED_ON : LED_OFF);
            break;
          case CUE:
            sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, channel.cueActive.isOn() ? LED_ON : LED_OFF);
            break;
          case ARM:
            sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, channel.arm.isOn() ? LED_ON : LED_OFF);
            break;
          case CROSSFADEGROUP:
            // Button press toggles through the 3 modes. Button does not stay lit.
            sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, LED_OFF);
            break;
        }
      } else {
        sendNoteOn(MIDI_CHANNEL, CHANNEL_BUTTON + index, LED_OFF);
      }
    }
  }

  private final LXMixerEngine.Listener mixerEngineListener = new LXMixerEngine.Listener() {
    @Override
    public void channelRemoved(LXMixerEngine mixer, LXAbstractChannel channel) {
      unregisterChannel(channel);
      if (gridMode != GridMode.PARAMETERS) {
        sendGrid();
      }
    }

    @Override
    public void channelMoved(LXMixerEngine mixer, LXAbstractChannel channel) {
      if (gridMode != GridMode.PARAMETERS) {
        sendGrid();
      } else {
        sendChannelButtonRow();
      }
    }

    @Override
    public void channelAdded(LXMixerEngine mixer, LXAbstractChannel channel) {
      if (gridMode != GridMode.PARAMETERS) {
        sendGrid();
      }
      registerChannel(channel);
    }
  };

  private final LXParameterListener focusedChannelListener = (p) -> {
    sendChannelFocus();
    this.deviceListener.registerChannel(this.lx.engine.mixer.getFocusedChannel());
  };

  private boolean isRegistered = false;

  private void register() {
    isRegistered = true;

    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      registerChannel(channel);
    }

    this.lx.engine.mixer.addListener(this.mixerEngineListener);
    this.lx.engine.mixer.focusedChannel.addListener(this.focusedChannelListener);

    this.deviceListener.registerChannel(this.lx.engine.mixer.getFocusedChannel());
  }

  private void unregister() {
    isRegistered = false;

    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      unregisterChannel(channel);
    }

    this.lx.engine.mixer.removeListener(this.mixerEngineListener);
    this.lx.engine.mixer.focusedChannel.removeListener(this.focusedChannelListener);

    clearGrid();
    clearChannelButtonRow();
  }

  private void registerChannel(LXAbstractChannel channel) {
    ChannelListener channelListener = new ChannelListener(channel);
    this.channelListeners.put(channel, channelListener);
  }

  private void unregisterChannel(LXAbstractChannel channel) {
    ChannelListener channelListener = this.channelListeners.remove(channel);
    if (channelListener != null) {
      channelListener.dispose();
    }
  }

  private LXAbstractChannel getChannel(int index) {
    if (index < this.lx.engine.mixer.channels.size()) {
      return this.lx.engine.mixer.channels.get(index);
    }
    return null;
  }

  private void noteReceived(MidiNote note, boolean on) {
    int pitch = note.getPitch();

    // Global momentary
    if (pitch == SHIFT) {
      this.shiftOn = on;
      sendChannelButtonRow();
      return;
    }

    if (this.shiftOn) {
      // Shift

      // Light-up momentary buttons
      switch (pitch) {
        case CLIP_STOP:
        case SOLO:
        case REC_ARM:
        case MUTE:
        case SELECT:
        case STOP_ALL_CLIPS:
        case SELECT_UP:
        case SELECT_DOWN:
        case SELECT_LEFT:
        case SELECT_RIGHT:
          sendNoteOn(note.getChannel(), pitch, on ? LED_ON : LED_OFF);
          break;
      }

      // Button actions with Shift
      if (on) {
        LXBus bus;
        switch (pitch) {
          case SELECT_LEFT:
            if (this.gridMode == GridMode.PARAMETERS) {
              this.deviceListener.registerPrevious();
            } else {
              this.lx.engine.mixer.focusedChannel.decrement(false);
              lx.engine.mixer.selectChannel(lx.engine.mixer.getFocusedChannel());
            }
            return;
          case SELECT_RIGHT:
            if (this.gridMode == GridMode.PARAMETERS) {
              this.deviceListener.registerNext();
            } else {
              this.lx.engine.mixer.focusedChannel.increment(false);
              lx.engine.mixer.selectChannel(lx.engine.mixer.getFocusedChannel());
            }
            return;
          case SELECT_UP:
            bus = this.lx.engine.mixer.getFocusedChannel();
            if (bus instanceof LXChannel) {
              ((LXChannel) bus).focusedPattern.decrement(1 , false);
            }
            return;
          case SELECT_DOWN:
            bus = this.lx.engine.mixer.getFocusedChannel();
            if (bus instanceof LXChannel) {
              ((LXChannel) bus).focusedPattern.increment(1 , false);
            }
            return;
          case CHANNEL_BUTTON_FOCUS:
            setChannelButtonMode(ChannelButtonMode.FOCUS);
            return;
          case CHANNEL_BUTTON_ENABLED:
            setChannelButtonMode(ChannelButtonMode.ENABLED);
            return;
          case CHANNEL_BUTTON_CUE:
            setChannelButtonMode(ChannelButtonMode.CUE);
            return;
          case CHANNEL_BUTTON_ARM:
            setChannelButtonMode(ChannelButtonMode.ARM);
            return;
          case CHANNEL_BUTTON_CROSSFADEGROUP:
            // Not an available mode currently due to 4 button limitation
            return;
          case CLIP_STOP:
          case SOLO:
          case REC_ARM:
          case MUTE:
          case SELECT:
          case EXTRA1:
          case EXTRA2:
            // Not implemented
            return;
          case STOP_ALL_CLIPS:
            this.lx.engine.clips.stopClips();
            return;
        }
      }
    } else {
      // Not Shift

      // Light-up momentary buttons
      if (pitch >= SCENE_LAUNCH && pitch <= SCENE_LAUNCH_MAX) {
        sendNoteOn(note.getChannel(), pitch, on ? LED_ON : LED_OFF);
      }

      // Button actions without Shift
      if (on) {
        switch (pitch) {
          case TOGGLE_CLIPS:
            this.gridMode = this.gridMode == GridMode.CLIPS ? GridMode.PATTERNS : GridMode.CLIPS;
            sendGrid();
            return;
          case TOGGLE_PARAMETERS:
            this.gridMode = this.gridMode == GridMode.PARAMETERS ? GridMode.PATTERNS : GridMode.PARAMETERS;
            sendGrid();
            return;
        }

        if (pitch >= SCENE_LAUNCH && pitch <= SCENE_LAUNCH_MAX) {
          this.lx.engine.clips.launchScene(pitch - SCENE_LAUNCH);
          return;
        }

        // Grid button
        if (pitch >= CLIP_LAUNCH && pitch <= CLIP_LAUNCH_MAX) {
          int channelIndex = (pitch - CLIP_LAUNCH) % CLIP_LAUNCH_COLUMNS;
          int index = CLIP_LAUNCH_ROWS - 1 - ((pitch - CLIP_LAUNCH) / CLIP_LAUNCH_COLUMNS);
          if (this.gridMode == GridMode.PARAMETERS) {
            // Grid button: Parameter
            this.deviceListener.onParameterButton(channelIndex, index);
            return;
          } else {
            LXAbstractChannel channel = getChannel(channelIndex);
            if (channel != null) {
              if (this.gridMode == GridMode.CLIPS) {
                // Grid button: Clip
                LXClip clip = channel.getClip(index);
                if (clip == null) {
                  clip = channel.addClip(index);
                } else {
                  if (clip.isRunning()) {
                    clip.stop();
                  } else {
                    clip.trigger();
                    this.lx.engine.clips.setFocusedClip(clip);
                  }
                }
              } else {
                // Grid button: Pattern
                if (channel instanceof LXChannel) {
                  LXChannel c = (LXChannel) channel;
                  index += c.controlSurfaceFocusIndex.getValuei();
                  if (index < c.getPatterns().size()) {
                    c.focusedPattern.setValue(index);
                    if (!this.shiftOn) {
                      c.goPatternIndex(index);
                    }
                  }
                }
              }
            }
          }
          return;
        }

        if (pitch >= CHANNEL_BUTTON && pitch <= CHANNEL_BUTTON_MAX) {
          LXAbstractChannel channel = getChannel(pitch - CHANNEL_BUTTON);
          if (channel != null) {
            switch (this.channelButtonMode) {
              case FOCUS:
                this.lx.engine.mixer.focusedChannel.setValue(channel.getIndex());
                lx.engine.mixer.selectChannel(lx.engine.mixer.getFocusedChannel());
                break;
              case ENABLED:
                channel.enabled.toggle();
                break;
              case CUE:
                channel.cueActive.toggle();
                break;
              case ARM:
                channel.arm.toggle();
                break;
              case CROSSFADEGROUP:
                channel.crossfadeGroup.increment();
                break;
            }
          }
        }
      }
    }
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    noteReceived(note, true);
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    noteReceived(note, false);
  }

  @Override
  public void controlChangeReceived(MidiControlChange cc) {
    int number = cc.getCC();
    switch (number) {
      case MASTER_FADER:
        this.lx.engine.mixer.masterBus.fader.setNormalized(cc.getNormalized());
        return;
    }

    if (number >= CHANNEL_FADER && number <= CHANNEL_FADER_MAX) {
      int channel = number - CHANNEL_FADER;
      if (channel < this.lx.engine.mixer.channels.size()) {
        this.lx.engine.mixer.channels.get(channel).fader.setNormalized(cc.getNormalized());
      }
      return;
    }

    LXMidiEngine.error("APC MINI unmapped control change: " + cc);
  }

  @Override
  public void dispose() {
    if (this.isRegistered) {
      unregister();
    }
    this.deviceListener.dispose();
    super.dispose();
  }

}