/**
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
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
 *
 *
 * JKB: This file is part of the LX Studio library.  Including
 * this code in the TE repo is a temporary measure to add
 * experimental features to the surface.
 */

package titanicsend.lx;

import java.util.*;

import heronarts.lx.LX;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.clip.LXClip;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.LXShortMessage;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.surface.APC40Mk2Colors;
import heronarts.lx.midi.surface.FocusedDevice;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXMixerEngine;
import heronarts.lx.parameter.AggregateParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXListenableParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.utils.LXUtils;
import titanicsend.model.justin.ViewCentral;
import titanicsend.model.justin.ViewCentral.ViewPerChannel;

public class APC40Mk2 extends LXMidiSurface implements LXMidiSurface.Bidirectional {

  public static final String DEVICE_NAME = "APC40 mkII";

  public static final byte GENERIC_MODE = 0x40;
  public static final byte ABLETON_MODE = 0x41;
  public static final byte ABLETON_ALTERNATE_MODE = 0x42;

  protected static final int LED_STYLE_OFF = 0;
  protected static final int LED_STYLE_SINGLE = 1;
  protected static final int LED_STYLE_UNIPOLAR = 2;
  protected static final int LED_STYLE_BIPOLAR = 3;

  public static final int NUM_CHANNELS = 8;
  public static final int CLIP_LAUNCH_ROWS = 5;
  public static final int CLIP_LAUNCH_COLUMNS = NUM_CHANNELS;
  public static final int PALETTE_SWATCH_ROWS = 5;
  public static final int PALETTE_SWATCH_COLUMNS = NUM_CHANNELS;
  public static final int MASTER_SWATCH = -1;
  public static final int RAINBOW_GRID_COLUMNS = 72;  // Should be a factor of MAX_HUE
  public static final int RAINBOW_GRID_ROWS = 5;

  // How much hue should increase from column to column
  public static final int RAINBOW_HUE_STEP = LXColor.MAX_HUE / RAINBOW_GRID_COLUMNS;

  // Saturation and brightness values to use for each row of the grid when in Rainbow Mode.
  // Each should have RAINBOW_HUE_ROWS elements.
  private static final int[] RAINBOW_GRID_SAT = { 100,  70,  50, 100, 100 };
  private static final int[] RAINBOW_GRID_BRI = { 100, 100, 100,  50,  25 };

  // CCs
  public static final int CHANNEL_FADER = 7;
  public static final int TEMPO = 13;
  public static final int MASTER_FADER = 14;
  public static final int CROSSFADER = 15;
  public static final int CUE_LEVEL = 47;

  public static final int DEVICE_KNOB = 16;
  public static final int DEVICE_KNOB_NUM = 8;
  public static final int DEVICE_KNOB_MAX = DEVICE_KNOB + DEVICE_KNOB_NUM;

  public static final int DEVICE_KNOB_STYLE = 24;
  public static final int DEVICE_KNOB_STYLE_MAX = DEVICE_KNOB_STYLE + DEVICE_KNOB_NUM;

  public static final int CHANNEL_KNOB = 48;
  public static final int CHANNEL_KNOB_NUM = 8;
  public static final int CHANNEL_KNOB_MAX = CHANNEL_KNOB + CHANNEL_KNOB_NUM - 1;

  public static final int CHANNEL_KNOB_STYLE = 56;
  public static final int CHANNEL_KNOB_STYLE_MAX = CHANNEL_KNOB_STYLE + CHANNEL_KNOB_NUM;

  // Notes
  public static final int CLIP_LAUNCH = 0;
  public static final int CLIP_LAUNCH_NUM = 40;
  public static final int CLIP_LAUNCH_MAX = CLIP_LAUNCH + CLIP_LAUNCH_NUM - 1;

  public static final int CHANNEL_ARM = 48;
  public static final int CHANNEL_SOLO = 49;
  public static final int CHANNEL_ACTIVE = 50;
  public static final int CHANNEL_FOCUS = 51;
  public static final int CLIP_STOP = 52;

  public static final int DEVICE_LEFT = 58;
  public static final int DEVICE_RIGHT = 59;
  public static final int BANK_LEFT = 60;
  public static final int BANK_RIGHT = 61;

  public static final int DEVICE_ON_OFF = 62;
  public static final int DEVICE_LOCK = 63;
  public static final int CLIP_DEVICE_VIEW = 64;
  public static final int DETAIL_VIEW = 65;

  public static final int CHANNEL_CROSSFADE_GROUP = 66;
  public static final int MASTER_FOCUS = 80;

  public static final int STOP_ALL_CLIPS = 81;
  public static final int SCENE_LAUNCH = 82;
  public static final int SCENE_LAUNCH_NUM = 5;
  public static final int SCENE_LAUNCH_MAX = SCENE_LAUNCH + SCENE_LAUNCH_NUM - 1;

  public static final int PAN = 87;
  public static final int SENDS = 88;
  public static final int USER = 89;

  public static final int PLAY = 91;
  public static final int RECORD = 93;
  public static final int SESSION = 102;

  public static final int BANK_SELECT_UP = 94;
  public static final int BANK_SELECT_DOWN = 95;
  public static final int BANK_SELECT_RIGHT = 96;
  public static final int BANK_SELECT_LEFT = 97;

  public static final int SHIFT = 98;

  public static final int METRONOME = 90;
  public static final int TAP_TEMPO = 99;
  public static final int NUDGE_MINUS = 100;
  public static final int NUDGE_PLUS = 101;

  public static final int BANK = 103;

  // LED color + mode definitions
  public static final int LED_OFF = 0;
  public static final int LED_ON = 1;
  public static final int LED_GRAY = 2;
  public static final int LED_CYAN = 114;
  public static final int LED_GRAY_DIM = 117;
  public static final int LED_RED = 120;
  public static final int LED_RED_HALF = 121;
  public static final int LED_ORANGE_RED = 60;
  public static final int LED_GREEN = 122;
  public static final int LED_GREEN_HALF = 123;
  public static final int LED_YELLOW = 124;
  public static final int LED_AMBER = 126;
  public static final int LED_AMBER_HALF = 9;
  public static final int LED_AMBER_DIM = 10;

  public static final int LED_MODE_PRIMARY = 0;
  public static final int LED_MODE_PULSE = 10;
  public static final int LED_MODE_BLINK = 15;

  // We use three modifier keys:
  // SHIFT: Momentary, can't be lit. Used for all sorts of purposes.
  private boolean shiftOn = false;
  // BANK: Toggle, lit when on. Makes the grid control patterns instead of clips.
  private boolean bankOn = true;
  // DEV. LOCK: Toggle, lit when on. Repurposes much of the hardware for color control.
  private boolean deviceLockOn = false;

  // "Copies" a color for pasting into the main swatch
  private Integer colorClipboard = null;
  // The entry in the main swatch that CUE LEVEL adjusts
  private LXDynamicColor focusColor = null;
  // Display full spectrum of colors for use with copy/paste
  private boolean rainbowMode = false;
  // Scroll offset for Rainbow Mode
  private int rainbowColumnOffset = 0;

  private boolean isAux = false;

  private final APC40Mk2Colors apc40Mk2Colors = new APC40Mk2Colors();

  private final Map<LXAbstractChannel, ChannelListener> channelListeners = new HashMap<LXAbstractChannel, ChannelListener>();

  private final DeviceListener deviceListener;

  private enum GridMode {
    PATTERN,
    CLIP,
    PALETTE;
  }

  private GridMode gridMode = getGridMode();

  private GridMode getGridMode() {
    if (this.deviceLockOn) {
      return GridMode.PALETTE;
    } else if (this.bankOn) {
      return GridMode.PATTERN;
    } else {
      return GridMode.CLIP;
    }
  }

  private void updateGridMode() {
    this.gridMode = getGridMode();
    sendChannelGrid();
    sendChannelFocus();
  }

  // When turning a knob that sometimes controls Hue, sometimes Brightness,
  // sometimes Saturation, look up the current mode of the surface and return
  // the appropriate subparameter that this knob currently controls.
  private LXListenableNormalizedParameter getActiveSubparameter(AggregateParameter agg) {
    if (agg instanceof LinkedColorParameter) {
      LinkedColorParameter lcp = (LinkedColorParameter) agg;
      if (lcp.mode.getEnum() == LinkedColorParameter.Mode.PALETTE) {
        // If it's palette-linked, the knob always controls the palette index.
        return lcp.index;
      }
      // Fall through to handle other color parameter types
    }
    if (agg instanceof ColorParameter) {
      // So far, ColorParameters are the only kind of AggregateParameter. If
      // we add more later, we'll have to add custom code here to handle them.
      ColorParameter colorParameter = (ColorParameter) agg;
      if (this.shiftOn) {
        return colorParameter.saturation;
      } else {
        return colorParameter.hue;
      }
    }

    LX.error("APC40Mk2 found AggregateParameter type with no subparameter: " + agg.getClass().getName());
    return null;
  }

  private void sendPerformanceLights() {
    boolean performanceMode = this.lx.engine.performanceMode.isOn();
    sendNoteOn(0, PLAY, performanceMode && !this.isAux ? LED_ON : LED_OFF);
    sendNoteOn(0, RECORD, performanceMode && this.isAux ? LED_ON : LED_OFF);
    sendNoteOn(0, SESSION, performanceMode ? LED_ON : LED_OFF);
  }

  private void sendCueLights() {
    if (isAuxActive()) {
      sendNoteOn(0, CLIP_DEVICE_VIEW, this.lx.engine.mixer.auxA.isOn() ? 1 : 0);
      sendNoteOn(0, DETAIL_VIEW, this.lx.engine.mixer.auxB.isOn() ? 1 : 0);
    } else {
      sendNoteOn(0, CLIP_DEVICE_VIEW, this.lx.engine.mixer.cueA.isOn() ? 1 : 0);
      sendNoteOn(0, DETAIL_VIEW, this.lx.engine.mixer.cueB.isOn() ? 1 : 0);
    }
  }

  private void setAux(boolean isAux) {
    this.isAux = isAux;
    this.deviceListener.focusedDevice.setAux(isAux);
    this.lx.engine.performanceMode.setValue(true);
    sendPerformanceLights();
    sendCueLights();
    sendChannelFocus();
    sendChannelCues();
  }

  private class DeviceListener implements FocusedDevice.Listener, LXParameterListener {

    private final FocusedDevice focusedDevice;
    private LXDeviceComponent device = null;
    private int bankNumber = 0;

    private final LXListenableParameter[] knobs =
      new LXListenableParameter[DEVICE_KNOB_NUM];

    private DeviceListener(LX lx) {
      Arrays.fill(this.knobs, null);
      this.focusedDevice = new FocusedDevice(lx, APC40Mk2.this, this);
    }

    @Override
    public void onDeviceFocused(LXDeviceComponent device) {
      registerDevice(device);
    }

    private void resend() {
      for (int i = 0; i < this.knobs.length; ++i) {
        // If the knob is a LinkedColorParameter, we need to look up which subparameter
        // to pull the value from to send to the surface to display.
        LXListenableNormalizedParameter parameter = parameterForKnob(this.knobs[i]);
        if (parameter != null) {
          sendControlChange(0, DEVICE_KNOB_STYLE + i, parameter.getPolarity() == LXParameter.Polarity.BIPOLAR ? LED_STYLE_BIPOLAR : LED_STYLE_UNIPOLAR);
          double normalized = (parameter instanceof CompoundParameter) ?
            ((CompoundParameter) parameter).getBaseNormalized() :
            parameter.getNormalized();
          sendControlChange(0, DEVICE_KNOB + i, (int) (normalized * 127));
        } else {
          sendControlChange(0, DEVICE_KNOB_STYLE + i, LED_STYLE_OFF);
        }
      }
      sendDeviceOnOff();
    }

    private void sendDeviceOnOff() {
      boolean isEnabled = false;
      if (this.device instanceof LXEffect) {
        LXEffect effect = (LXEffect) this.device;
        isEnabled = effect.enabled.isOn();
      } else if (this.device instanceof LXPattern) {
        LXPattern pattern = (LXPattern) this.device;
        isEnabled = isPatternEnabled(pattern);
      }
      sendNoteOn(0, DEVICE_ON_OFF, isEnabled ? LED_ON : LED_OFF);
    }

    private void clearKnobsAfter(int i) {
      while (i < this.knobs.length) {
        sendControlChange(0, DEVICE_KNOB_STYLE + i, LED_STYLE_OFF);
        ++i;
      }
    }

    private void registerDevice(LXDeviceComponent device) {
      if (this.device == device) {
        return;
      }
      unregisterDevice();
      this.device = device;
      this.bankNumber = 0;

      boolean isEnabled = false;
      if (this.device instanceof LXEffect) {
        LXEffect effect = (LXEffect) this.device;
        effect.enabled.addListener(this);
        isEnabled = effect.isEnabled();
      } else if (this.device instanceof LXPattern) {
        LXPattern pattern = (LXPattern) this.device;
        pattern.enabled.addListener(this);
        isEnabled = isPatternEnabled(pattern);
      }
      if (this.device != null) {
        this.device.remoteControlsChanged.addListener(this);
      }

      sendNoteOn(0, DEVICE_ON_OFF, isEnabled ? LED_ON : LED_OFF);
      if (this.device == null) {
        clearKnobsAfter(0);
        return;
      }

      registerDeviceKnobs();
    }

    private boolean isPatternEnabled(LXPattern pattern) {
      switch (pattern.getChannel().compositeMode.getEnum()) {
      case BLEND:
        return pattern.enabled.isOn();
      default:
      case PLAYLIST:
        return pattern == pattern.getChannel().getTargetPattern();
      }
    }

    private void incrementBank(int amt) {
      if (this.device != null) {
        int test = this.bankNumber + amt;
        if ((test >= 0) && (test * DEVICE_KNOB_NUM < this.device.getRemoteControls().length)) {
          this.bankNumber = test;
          unregisterDeviceKnobs();
          registerDeviceKnobs();
          this.focusedDevice.updateRemoteControlFocus();
        }
      }
    }

    private void registerDeviceKnobs() {
      int i = 0;
      int skip = this.bankNumber * DEVICE_KNOB_NUM;
      int s = 0;
      final List<LXParameter> uniqueParameters = new ArrayList<LXParameter>();
      // Avoid registering enabled twice
      if (this.device instanceof LXEffect) {
        uniqueParameters.add(((LXEffect)this.device).enabled);
      } else if (this.device instanceof LXPattern) {
        uniqueParameters.add(((LXPattern)this.device).enabled);
      }
      for (LXListenableNormalizedParameter parameter : this.device.getRemoteControls()) {
        if (s++ < skip) {
          continue;
        }
        if (i >= this.knobs.length) {
          break;
        }
        if (parameter == null) {
          this.knobs[i] = null;
          sendControlChange(0, DEVICE_KNOB_STYLE + i, LED_STYLE_OFF);
          ++i;
          continue;
        }

        AggregateParameter parent = parameter.getParentParameter();
        if (parent != null) {
          // When an Aggregate's first sub is encountered, put the agg on a knob,
          // add listeners for all of its children, and remember that we've seen
          // the parent so we can skip this process when we encounter its other subs.
          this.knobs[i] = parent;
          if (!uniqueParameters.contains(parent)) {
            uniqueParameters.add(parent);
            for (LXListenableParameter subParam : parent.subparameters.values()) {
              subParam.addListener(this);
            }
          }
        } else {
          this.knobs[i] = parameter;
          if (!uniqueParameters.contains(parameter)) {
            uniqueParameters.add(parameter);
            parameter.addListener(this);
          }
        }

        LXListenableNormalizedParameter knobParam = parameterForKnob(this.knobs[i]);
        if (knobParam == null) {
          // Weird situation, AggregateParameter has no control surface subs...
          sendControlChange(0, DEVICE_KNOB_STYLE + i, LED_STYLE_OFF);
        } else {
          int ledStyle = (parameter.getPolarity() == LXParameter.Polarity.BIPOLAR) ? LED_STYLE_BIPOLAR : LED_STYLE_UNIPOLAR;
          sendControlChange(0, DEVICE_KNOB_STYLE + i, ledStyle);
          sendKnobValue(knobParam, i);
        }

        ++i;
      }
      clearKnobsAfter(i);
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
      LXEffect effect = (this.device instanceof LXEffect) ? (LXEffect) this.device : null;
      LXPattern pattern = (this.device instanceof LXPattern) ? (LXPattern) this.device : null;
      if (parameter == this.device.remoteControlsChanged) {
        unregisterDeviceKnobs();
        registerDeviceKnobs();
      } else {
        if ((effect != null) && (parameter == effect.enabled)) {
          sendNoteOn(0, DEVICE_ON_OFF, effect.enabled.isOn() ? LED_ON : LED_OFF);
        } else if ((pattern != null) && (parameter == pattern.enabled)) {
          sendNoteOn(0, DEVICE_ON_OFF, isPatternEnabled(pattern) ? LED_ON : LED_OFF);
          sendChannelPatterns(pattern.getChannel().getIndex(), pattern.getChannel());
        }
        // enabled could be a remote parameter
        for (int i = 0; i < this.knobs.length; ++i) {
          LXListenableNormalizedParameter knobParam = parameterForKnob(this.knobs[i]);
          if (parameter == knobParam) {
            sendKnobValue(knobParam, i);
          }
        }
      }
    }

    private void sendKnobValue(LXListenableNormalizedParameter knobParam, int i) {
      double normalized = (knobParam instanceof CompoundParameter) ?
        ((CompoundParameter) knobParam).getBaseNormalized() :
        knobParam.getNormalized();

      // Wrappable discrete parameters need to inset the values a bit to avoid fiddly jumping at 0/1
      if ((knobParam instanceof DiscreteParameter) && knobParam.isWrappable()) {
        DiscreteParameter discrete = (DiscreteParameter) knobParam;
        normalized = (discrete.getValuei() - discrete.getMinValue() + 0.5f) / discrete.getRange();
      }
      sendControlChange(0, DEVICE_KNOB + i, (int) (normalized * 127));
    }

    // Returns what parameter should be changed when a physical knob is turned. This
    // is trivial except for LinkedColorParameters, whose function changes depending
    // on the surface's mode.
    private LXListenableNormalizedParameter parameterForKnob(LXListenableParameter knob) {
      if (knob == null || knob instanceof LXListenableNormalizedParameter) {
        // If the knob is unused or linked to a vanilla parameter, we're done.
        return (LXListenableNormalizedParameter) knob;
      }

      if (knob instanceof AggregateParameter) {
        // Find an appropriate subparam for an AggregateParameter.
        return getActiveSubparameter((AggregateParameter) knob);
      }

      return null;
    }

    private void onDeviceOnOff() {
      if (this.device instanceof LXPattern) {
        final LXPattern pattern = (LXPattern) this.device;
        final LXChannel channel = pattern.getChannel();
        if (channel.compositeMode.getEnum() == LXChannel.CompositeMode.BLEND) {
          pattern.enabled.toggle();
        } else {
          pattern.getChannel().goPatternIndex(pattern.getIndex());
        }
        sendNoteOn(0, DEVICE_ON_OFF, isPatternEnabled(pattern) ? LED_ON : LED_OFF);
      } else if (this.device instanceof LXEffect) {
        LXEffect effect = (LXEffect) this.device;
        effect.enabled.toggle();
      }
    }

    private void onKnob(int index, double normalized) {
      LXListenableParameter knob = this.knobs[index];

      if (knob == null) {
        return;
      }

      // If the knob's an LCP, touching it should link it to the focusColor,
      // if one is set and it belongs to the main palette and not a side
      // swatch, or set it static and initialize it to the colorClipboard,
      // if there is one. Otherwise, have it control the value as normal.
      if (knob instanceof LinkedColorParameter) {
        LinkedColorParameter lcp = (LinkedColorParameter) knob;

        if (focusColor != null) {
          int palIndex = lx.engine.palette.swatch.colors.indexOf(focusColor);
          if (palIndex >= 0) {
            lcp.mode.setValue(LinkedColorParameter.Mode.PALETTE);
            lcp.index.setValue(palIndex + 1);
            return;
          }
        }
        if (colorClipboard != null) {
          lcp.mode.setValue(LinkedColorParameter.Mode.STATIC);
          // Fallthrough; next section will set the color.
        }
      }

      // If the knob's an LCP, or even just an unlinked ColorParameter,
      // and there's a colorClipboard, set the value to it rather than
      // adjusting it like normal.
      if (knob instanceof ColorParameter) {
        ColorParameter cp = (ColorParameter) knob;

        if (focusColor != null) {
          cp.setColor(focusColor.getColor());
          return;
        }
        if (colorClipboard != null) {
          cp.setColor(colorClipboard);
          return;
        }
      }

      LXListenableNormalizedParameter knobParam = parameterForKnob(knob);

      // If it's a wrappable parameter, let it wrap
      if (knobParam.isWrappable()) {
        if (normalized == 0.0) {
          normalized = 1.0;
        } else if (normalized == 1.0) {
          normalized = 0.0;
        }
      }
      knobParam.setNormalized(normalized);
    }

    private void unregisterDevice() {
      if (this.device != null) {
        if (this.device instanceof LXEffect) {
          LXEffect effect = (LXEffect) this.device;
          effect.enabled.removeListener(this);
        } else if (this.device instanceof LXPattern) {
          LXPattern pattern = (LXPattern) this.device;
          pattern.enabled.removeListener(this);
        }
        this.device.remoteControlsChanged.removeListener(this);
        unregisterDeviceKnobs();
        this.device = null;
      }
    }

    private void unregisterDeviceKnobs() {
      if (this.device != null) {
        final List<LXParameter> uniqueParameters = new ArrayList<LXParameter>();
        // Avoid unregistering enabled twice
        if (this.device instanceof LXEffect) {
          uniqueParameters.add(((LXEffect)this.device).enabled);
        } else if (this.device instanceof LXPattern) {
          uniqueParameters.add(((LXPattern)this.device).enabled);
        }
        for (int i = 0; i < this.knobs.length; ++i) {
          if (this.knobs[i] == null) {
            continue;
          } else if (this.knobs[i] instanceof AggregateParameter) {
            AggregateParameter ap = (AggregateParameter) this.knobs[i];
            if (!uniqueParameters.contains(ap)) {
              uniqueParameters.add(ap);
              for (LXListenableParameter sub : ap.subparameters.values()) {
                sub.removeListener(this);
              }
            }
          } else {
            if (!uniqueParameters.contains(this.knobs[i])) {
              uniqueParameters.add(this.knobs[i]);
              this.knobs[i].removeListener(this);
            }
          }
          this.knobs[i] = null;
          sendControlChange(0, DEVICE_KNOB + i, 0);
          sendControlChange(0, DEVICE_KNOB_STYLE + i, LED_STYLE_OFF);
        }
      }
    }

    private void dispose() {
      unregisterDevice();
    }

  }

  private class ChannelListener implements LXChannel.Listener, LXBus.ClipListener, LXParameterListener {

    private final LXAbstractChannel channel;
    private final LXParameterListener onCompositeModeChanged = this::onCompositeModeChanged;
    ViewPerChannel viewPerChannel = null;

    ChannelListener(LXAbstractChannel channel) {
      this.channel = channel;
      if (channel instanceof LXChannel) {
        ((LXChannel) channel).addListener(this);
        ((LXChannel) channel).compositeMode.addListener(this.onCompositeModeChanged);
      } else {
        channel.addListener(this);
      }
      channel.addClipListener(this);
      channel.cueActive.addListener(this);
      channel.auxActive.addListener(this);
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
          clip.loop.addListener(this);
        }
      }

      // Listen to custom view parameter for channel
      if (ViewCentral.ENABLED) {
        this.viewPerChannel = ViewCentral.get().get(channel);
        if (this.viewPerChannel != null) {
          this.viewPerChannel.view.addListener(this);
          if (channelKnobIsView.isOn()) {
            sendChannelKnob((int) (this.viewPerChannel.view.getNormalized() * 127));
          }
        }
      }
    }

    public void dispose() {
      if (this.channel instanceof LXChannel) {
        ((LXChannel) this.channel).removeListener(this);
        ((LXChannel) this.channel).compositeMode.removeListener(this.onCompositeModeChanged);
      } else {
        this.channel.removeListener(this);
      }
      this.channel.removeClipListener(this);
      this.channel.cueActive.removeListener(this);
      this.channel.auxActive.removeListener(this);
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
          clip.loop.removeListener(this);
        }
      }
      if (this.viewPerChannel != null) {
        this.viewPerChannel.view.removeListener(this);
        this.viewPerChannel = null;
      }
    }

    private void onCompositeModeChanged(LXParameter p) {
      final int index = this.channel.getIndex();
      if (index >= CLIP_LAUNCH_COLUMNS) {
        return;
      }
      sendChannelPatterns(index, this.channel);
      sendDeviceOnOff();
    }

    public void onParameterChanged(LXParameter p) {
      final int index = this.channel.getIndex();
      if (index >= CLIP_LAUNCH_COLUMNS) {
        return;
      }

      if (p == this.channel.cueActive) {
        sendNoteOn(index, CHANNEL_SOLO, this.channel.cueActive.isOn() ? LED_ON : LED_OFF);
      } else if (p == this.channel.auxActive) {
        if (lx.engine.performanceMode.isOn()) {
          sendNoteOn(index, CHANNEL_ARM, this.channel.auxActive.isOn() ? LED_ON : LED_OFF);
        }
      } else if (p == this.channel.enabled) {
        sendNoteOn(index, CHANNEL_ACTIVE, this.channel.enabled.isOn() ? LED_ON : LED_OFF);
      } else if (p == this.channel.crossfadeGroup) {
        sendNoteOn(index, CHANNEL_CROSSFADE_GROUP, this.channel.crossfadeGroup.getValuei());
      } else if (p == this.channel.arm) {
        if (!lx.engine.performanceMode.isOn()) {
          sendNoteOn(index, CHANNEL_ARM, this.channel.arm.isOn() ? LED_ON : LED_OFF);
        }
        sendChannelClips(this.channel.getIndex(), this.channel);
      } else if (this.viewPerChannel != null && p == this.viewPerChannel.view) {
        if (channelKnobIsView.isOn()) {
          sendChannelKnob((int) (this.viewPerChannel.view.getNormalized() * 127));
        }
        return;
      } else if (p.getParent() instanceof LXClip) {
        LXClip clip = (LXClip) p.getParent();
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
    public void patternAdded(LXChannel channel, LXPattern pattern) {
      sendChannelPatterns(channel.getIndex(), channel);
    }

    @Override
    public void patternRemoved(LXChannel channel, LXPattern pattern) {
      sendChannelPatterns(channel.getIndex(), channel);
    }

    @Override
    public void patternMoved(LXChannel channel, LXPattern pattern) {
      sendChannelPatterns(channel.getIndex(), channel);
    }

    @Override
    public void patternWillChange(LXChannel channel, LXPattern pattern, LXPattern nextPattern) {
      sendChannelPatterns(channel.getIndex(), channel);
    }

    @Override
    public void patternDidChange(LXChannel channel, LXPattern pattern) {
      sendChannelPatterns(channel.getIndex(), channel);
    }

    @Override
    public void clipAdded(LXBus bus, LXClip clip) {
      clip.running.addListener(this);
      clip.loop.addListener(this);
      sendClip(this.channel.getIndex(), this.channel, clip.getIndex(), clip);
    }

    @Override
    public void clipRemoved(LXBus bus, LXClip clip) {
      clip.running.removeListener(this);
      clip.loop.removeListener(this);
      sendChannelClips(this.channel.getIndex(), this.channel);
    }

    private void sendChannelKnob(int value) {
      if (this.channel.getIndex() < CHANNEL_KNOB_NUM) {
        sendControlChange(0, CHANNEL_KNOB + this.channel.getIndex(), value);
      }
    }
  }

  public final BooleanParameter masterFaderEnabled =
    new BooleanParameter("Master Fader", true)
    .setDescription("Whether the master fader for output brightness is enabled");

  public final BooleanParameter crossfaderEnabled =
    new BooleanParameter("Crossfader", true)
    .setDescription("Whether the A/B crossfader is enabled");

  public final BooleanParameter channelKnobIsView =
    new BooleanParameter("CH Knob is View", true)
    .setDescription("Whether to control Views with the channel knobs at the top of the APC40Mk2");

  public APC40Mk2(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
    this.deviceListener = new DeviceListener(lx);
    addSetting("masterFaderEnabled", this.masterFaderEnabled);
    addSetting("crossfaderEnabled", this.crossfaderEnabled);
    addSetting("channelKnobIsView", this.channelKnobIsView);
  }

  @Override
  protected void onEnable(boolean on) {
    if (on) {
      setApcMode(ABLETON_ALTERNATE_MODE);
      initialize(false);
      register();
    } else {
      this.deviceListener.registerDevice(null);
      for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
        if (channel instanceof LXChannel) {
          ((LXChannel)channel).controlSurfaceFocusLength.setValue(0);
        }
      }
      if (this.isRegistered) {
        unregister();
      }
      setApcMode(GENERIC_MODE);
    }
  }

  @Override
  protected void onReconnect() {
    if (this.enabled.isOn()) {
      setApcMode(ABLETON_ALTERNATE_MODE);
      initialize(true);
      this.deviceListener.resend();
    }
  }

  private void setApcMode(byte mode) {
    this.output.sendSysex(new byte[] {
      (byte) 0xf0, // sysex start
      0x47, // manufacturers id
      0x00, // device id
      0x29, // product model id
      0x60, // message
      0x00, // bytes MSB
      0x04, // bytes LSB
      mode,
      0x09, // version maj
      0x03, // version min
      0x01, // version bugfix
      (byte) 0xf7, // sysex end
    });
  }

  private void initialize(boolean reconnect) {
    this.output.sendNoteOn(0, BANK, this.bankOn ? LED_ON : LED_OFF);
    this.output.sendNoteOn(0, DEVICE_LOCK, this.deviceLockOn ? LED_ON : LED_OFF);

    if (!reconnect) {
      resetPaletteVars();
    }

    sendPerformanceLights();

    for (int i = 0; i < DEVICE_KNOB_NUM; ++i) {
      sendControlChange(0, DEVICE_KNOB_STYLE+i, LED_STYLE_OFF);
    }
    for (int i = 0; i < CHANNEL_KNOB_NUM; ++i) {
      // Initialize channel knobs for generic control, but don't
      // reset their values if we're in a reconnect situation
      sendControlChange(0, CHANNEL_KNOB_STYLE+i, LED_STYLE_SINGLE);
      if (!reconnect) {
        sendControlChange(0, CHANNEL_KNOB+i, 64);
      }
    }
    sendChannels();
    this.cueState.reset();
    this.auxState.reset();
  }

  private void resetPaletteVars() {
    this.colorClipboard = null;
    this.focusColor = null;
    this.rainbowMode = false;

    clearSceneLaunch();
  }

  private void sendChannels() {
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendChannel(i, getChannel(i));
    }
    sendChannelFocus();
  }

  private void sendChannelCues() {
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      LXAbstractChannel channel = getChannel(i);
      if (channel != null) {
        sendNoteOn(i, CHANNEL_SOLO, channel.cueActive.isOn() ? LED_ON : LED_OFF);
        sendNoteOn(i, CHANNEL_ARM, (this.lx.engine.performanceMode.isOn() ? channel.auxActive.isOn() : channel.arm.isOn()) ? LED_ON : LED_OFF);
      } else {
        sendNoteOn(i, CHANNEL_SOLO, LED_OFF);
        sendNoteOn(i, CHANNEL_ARM, LED_OFF);
      }
    }
  }

  private void sendChannelGrid() {
    if (!this.rainbowMode) {
      for (int i = 0; i < NUM_CHANNELS; ++i) {
        LXAbstractChannel channel = getChannel(i);
        sendChannelPatterns(i, channel);
        sendChannelClips(i, channel);
        sendSwatch(i);
      }
    }

    sendSwatch(MASTER_SWATCH);
  }

  private void clearChannelGrid() {
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendChannel(i, null);
    }
  }

  private void sendChannel(int index, LXAbstractChannel channel) {
    if (channel != null) {
      sendNoteOn(index, CHANNEL_ACTIVE, channel.enabled.isOn() ? LED_ON : LED_OFF);
      sendNoteOn(index, CHANNEL_CROSSFADE_GROUP, channel.crossfadeGroup.getValuei());
      sendNoteOn(index, CHANNEL_SOLO, channel.cueActive.isOn() ? LED_ON : LED_OFF);
      sendNoteOn(index, CHANNEL_ARM, (this.lx.engine.performanceMode.isOn() ? channel.auxActive.isOn() : channel.arm.isOn()) ? LED_ON : LED_OFF);
    } else {
      sendNoteOn(index, CHANNEL_ACTIVE, LED_OFF);
      sendNoteOn(index, CHANNEL_CROSSFADE_GROUP, LED_OFF);
      sendNoteOn(index, CHANNEL_SOLO, LED_OFF);
      sendNoteOn(index, CHANNEL_ARM, LED_OFF);
    }
    sendChannelPatterns(index, channel);
    sendChannelClips(index, channel);
  }

  private void sendChannelPatterns(int index, LXAbstractChannel channelBus) {
    if (index >= CLIP_LAUNCH_COLUMNS || this.gridMode != GridMode.PATTERN) {
      return;
    }

    if (channelBus instanceof LXChannel) {
      final LXChannel channel = (LXChannel) channelBus;
      final boolean blendMode = channel.compositeMode.getEnum() == LXChannel.CompositeMode.BLEND;
      final int baseIndex = channel.controlSurfaceFocusIndex.getValuei();
      final int endIndex = channel.patterns.size() - baseIndex;
      final int activeIndex = channel.getActivePatternIndex() - baseIndex;
      final int nextIndex = channel.getNextPatternIndex() - baseIndex;
      final int focusedIndex = (channel.patterns.size() == 0) ? -1 : channel.focusedPattern.getValuei() - baseIndex;

      for (int y = 0; y < CLIP_LAUNCH_ROWS; ++y) {
        final int note = CLIP_LAUNCH + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - y) + index;
        int midiChannel = LED_MODE_PRIMARY;
        int color = LED_OFF;

        if (blendMode) {
          if (y < endIndex) {
            if (channel.patterns.get(baseIndex + y).enabled.isOn()) {
              // Pattern is enabled!
              color = y == focusedIndex ? LED_ORANGE_RED : LED_AMBER_HALF;
            } else {
              // Pattern is present but off
              color = y == focusedIndex ? LED_GRAY : LED_GRAY_DIM;
            }
          }
        } else {
          if (y == activeIndex) {
            // This pattern is active (may also be focused)
            color = LED_ORANGE_RED;
          } else if (y == nextIndex) {
            // This pattern is being transitioned to
            sendNoteOn(LED_MODE_PRIMARY, note, LED_ORANGE_RED);
            midiChannel = LED_MODE_PULSE;
            color = LED_AMBER_HALF;
          } else if (y == focusedIndex) {
            // This pattern is not active, but it is focused
            color = LED_AMBER_DIM;
          } else if (y < endIndex) {
            // There is a pattern present
            color = LED_GRAY_DIM;
          }
        }

        sendNoteOn(midiChannel, note, color);
      }
    } else {
      for (int y = 0; y < CLIP_LAUNCH_ROWS; ++y) {
        sendNoteOn(
          LED_MODE_PRIMARY,
          CLIP_LAUNCH + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - y) + index,
          LED_OFF
        );
      }
    }
  }

  private void sendChannelClips(int index, LXAbstractChannel channel) {
    int clipOffset = lx.engine.clips.clipViewGridOffset.getValuei();
    for (int i = 0; i < CLIP_LAUNCH_ROWS; ++i) {
      LXClip clip = null;
      int clipIndex = clipOffset + i;
      if (channel != null) {
        clip = channel.getClip(clipIndex);
      }
      sendClip(index, channel, clipIndex, clip);
    }
  }

  private void sendClip(int channelIndex, LXAbstractChannel channel, int clipIndex, LXClip clip) {
    int slotIndex = clipIndex - lx.engine.clips.clipViewGridOffset.getValuei();
    if (this.gridMode != GridMode.CLIP ||
            channelIndex >= CLIP_LAUNCH_COLUMNS || slotIndex >= CLIP_LAUNCH_ROWS) {
      return;
    }
    int color = LED_OFF;
    int mode = LED_MODE_PRIMARY;
    int pitch = CLIP_LAUNCH + channelIndex + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - slotIndex);
    if (channel != null && clip != null) {
      color = channel.arm.isOn() ? LED_RED_HALF :
              clip.loop.isOn() ? LED_CYAN : LED_GRAY;
      if (clip.isRunning()) {
        color = channel.arm.isOn() ? LED_RED : LED_GREEN;
        sendNoteOn(LED_MODE_PRIMARY, pitch, color);
        mode = LED_MODE_PULSE;
        color = channel.arm.isOn() ? LED_RED_HALF :
                clip.loop.isOn() ? LED_CYAN : LED_GREEN_HALF;
      }
    }
    sendNoteOn(mode, pitch, color);
  }

  private void clearSceneLaunch() {
    for (int i = 0; i < SCENE_LAUNCH_NUM; i++) {
      sendNoteOn(LED_MODE_PRIMARY, SCENE_LAUNCH + i, LED_OFF);
    }
  }

  private void sendDeviceOnOff() {
    this.deviceListener.sendDeviceOnOff();
  }

  private void sendSwatches() {
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendSwatch(i);
    }
    sendSwatch(MASTER_SWATCH);
  }

  // Sends the specified swatch index to the channel grid, or
  // if MASTER_SWATCH, sends the main palette swatch to the SCENE LAUNCH column
  private void sendSwatch(int index) {
    if (this.gridMode != GridMode.PALETTE || (index >= PALETTE_SWATCH_COLUMNS)) {
      return;
    }

    LXSwatch swatch = getSwatch(index);

    for (int i = 0; i < PALETTE_SWATCH_ROWS; ++i) {
      int color = LED_OFF;
      int mode = LED_MODE_PRIMARY;

      int pitch;
      if (index == MASTER_SWATCH) {
        pitch = SCENE_LAUNCH + i;
      } else {
        pitch = CLIP_LAUNCH + index + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - i);
      }
      if (swatch != null && i < swatch.colors.size()) {
        int palColor = swatch.colors.get(i).getColor();
        color = this.apc40Mk2Colors.nearest(palColor);
      }
      sendNoteOn(mode, pitch, color);
    }
  }

  private static int[] makeRainbowGrid() {
    int[] rainbowGrid = new int[RAINBOW_GRID_COLUMNS * RAINBOW_GRID_ROWS];
    for (int col = 0; col < RAINBOW_GRID_COLUMNS; ++col) {
      int hue = col * RAINBOW_HUE_STEP;
      for (int row = 0; row < RAINBOW_GRID_ROWS; ++row) {
        int i = col * RAINBOW_GRID_ROWS + row;
        rainbowGrid[i++] = LXColor.hsb(hue, RAINBOW_GRID_SAT[row], RAINBOW_GRID_BRI[row]);
      }
    }
    return rainbowGrid;
  }

  // Grouped by column
  private static final int[] RAINBOW_GRID = makeRainbowGrid();

  private int rainbowGridColor(int relCol, int row) {
    int absCol = (RAINBOW_GRID_COLUMNS + relCol + this.rainbowColumnOffset) % RAINBOW_GRID_COLUMNS;
    return RAINBOW_GRID[absCol * RAINBOW_GRID_ROWS + row];
  }

  // Cover the grid with a rainbox of APC colors
  private void sendRainbowPickerGrid() {
    for (int col = 0; col < NUM_CHANNELS; ++col) {
      for (int row = 0; row < PALETTE_SWATCH_ROWS; ++row) {
        int pitch = CLIP_LAUNCH + col + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - row);
        int color = rainbowGridColor(col, row);
        int colorId = this.apc40Mk2Colors.nearest(color);
        sendNoteOn(LED_MODE_PRIMARY, pitch, colorId);
      }
    }
  }

  // For each knob controlling a ColorParameter, update its lighting to match
  // the value it would change if turned right now. If we ever have an
  // AggregateParameter knob where some subparameters are unipolar and others
  // bipolar, we'll need to do a sendControlChange() as well here. For now,
  // though, that never happens.
  private void updateColorKnobs() {
    for (int i = 0; i < this.deviceListener.knobs.length; i++) {
      LXListenableParameter knob = this.deviceListener.knobs[i];
      if (knob instanceof ColorParameter) {
        ColorParameter cp = (ColorParameter) knob;
        LXListenableNormalizedParameter subparam = getActiveSubparameter(cp);
        double normalized = subparam.getNormalized();
        sendControlChange(0, DEVICE_KNOB + i, (int) (normalized * 127));
      };
    }
  }

  private void sendChannelFocus() {

    final int focusedChannel = this.lx.engine.mixer.focusedChannel.getValuei();
    final int focusedChannelAux = this.lx.engine.mixer.focusedChannelAux.getValuei();

    final boolean masterFocused = (focusedChannel == this.lx.engine.mixer.channels.size());
    final boolean masterFocusedAux = (focusedChannelAux == this.lx.engine.mixer.channels.size());

    final boolean isAuxActive = isAuxActive();

    final int focusedChannelMain = isAuxActive ? focusedChannelAux : focusedChannel;
    final boolean masterFocusedMain = isAuxActive ? masterFocusedAux : masterFocused;

    final int focusedChannelAlt = isAuxActive ? focusedChannel : focusedChannelAux;
    final boolean masterFocusedAlt = isAuxActive ? masterFocused : masterFocusedAux;

    for (int i = 0; i < NUM_CHANNELS; ++i) {
      boolean clipStopOn = false, focusOn = false;
      if (this.gridMode == GridMode.PALETTE) {
        if (!this.rainbowMode) {
          clipStopOn = i < lx.engine.palette.swatches.size();
        }
      } else if (this.gridMode == GridMode.CLIP) {
        // clip stop is for clip stop in this mode, it's off unless pressed
      } else if (this.gridMode == GridMode.PATTERN) {
        if (this.lx.engine.performanceMode.isOn()) {
          clipStopOn = !masterFocusedAlt && (i == focusedChannelAlt);
        }
      }
      focusOn = !masterFocusedMain && (i == focusedChannelMain);

      sendNoteOn(i, CLIP_STOP, clipStopOn ? LED_ON : LED_OFF);
      sendNoteOn(i, CHANNEL_FOCUS, focusOn ? LED_ON : LED_OFF);
    }
    sendNoteOn(0, MASTER_FOCUS, masterFocusedMain ? LED_ON : LED_OFF);
  }

  private final LXMixerEngine.Listener mixerEngineListener = new LXMixerEngine.Listener() {
    @Override
    public void channelRemoved(LXMixerEngine mixer, LXAbstractChannel channel) {
      unregisterChannel(channel);
      sendChannels();
    }

    @Override
    public void channelMoved(LXMixerEngine mixer, LXAbstractChannel channel) {
      sendChannels();
    }

    @Override
    public void channelAdded(LXMixerEngine mixer, LXAbstractChannel channel) {
      sendChannels();
      registerChannel(channel);
    }
  };

  private boolean isAuxActive() {
    return this.lx.engine.performanceMode.isOn() && this.isAux;
  }

  private final LXParameterListener cueAListener = (p) -> {
    if (!isAuxActive()) {
      sendNoteOn(0, CLIP_DEVICE_VIEW, this.lx.engine.mixer.cueA.isOn() ? 1 : 0);
    }
  };

  private final LXParameterListener cueBListener = (p) -> {
    if (!isAuxActive()) {
      sendNoteOn(0, DETAIL_VIEW, this.lx.engine.mixer.cueB.isOn() ? 1 : 0);
    }
  };

  private final LXParameterListener auxAListener = (p) -> {
    if (isAuxActive()) {
      sendNoteOn(0, CLIP_DEVICE_VIEW, this.lx.engine.mixer.auxA.isOn() ? 1 : 0);
    }
  };

  private final LXParameterListener auxBListener = (p) -> {
    if (isAuxActive()) {
      sendNoteOn(0, DETAIL_VIEW, this.lx.engine.mixer.auxB.isOn() ? 1 : 0);
    }
  };

  private final LXParameterListener tempoListener = (p) -> {
    sendNoteOn(0, METRONOME, this.lx.engine.tempo.enabled.isOn() ? LED_ON : LED_OFF);
  };

  private final LXParameterListener performanceModeListener = (p) -> {
    sendPerformanceLights();
    sendCueLights();
    sendChannelFocus();
    sendChannelCues();
  };

  private final LXParameterListener focusedChannelListener = (p) -> {
    sendChannelFocus();
  };

  private final LXParameterListener clipGridListener = (p) -> {
    sendChannelGrid();
  };

  private boolean isRegistered = false;

  private void register() {
    this.isRegistered = true;

    this.deviceListener.focusedDevice.register();

    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      registerChannel(channel);
    }

    this.lx.engine.performanceMode.addListener(this.performanceModeListener, true);

    this.lx.engine.clips.clipViewGridOffset.addListener(this.clipGridListener);
    this.lx.engine.clips.numScenes.addListener(this.clipGridListener);
    this.lx.engine.mixer.addListener(this.mixerEngineListener);
    this.lx.engine.mixer.focusedChannel.addListener(this.focusedChannelListener);
    this.lx.engine.mixer.focusedChannelAux.addListener(this.focusedChannelListener);
    this.lx.engine.mixer.cueA.addListener(this.cueAListener, true);
    this.lx.engine.mixer.cueB.addListener(this.cueBListener, true);
    this.lx.engine.mixer.auxA.addListener(this.auxAListener, true);
    this.lx.engine.mixer.auxB.addListener(this.auxBListener, true);
    this.lx.engine.tempo.enabled.addListener(this.tempoListener, true);
  }

  private void unregister() {
    this.isRegistered = false;

    this.deviceListener.focusedDevice.unregister();

    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      unregisterChannel(channel);
    }

    this.lx.engine.performanceMode.removeListener(this.performanceModeListener);
    this.lx.engine.clips.clipViewGridOffset.removeListener(this.clipGridListener);
    this.lx.engine.clips.numScenes.removeListener(this.clipGridListener);
    this.lx.engine.mixer.removeListener(this.mixerEngineListener);
    this.lx.engine.mixer.focusedChannel.removeListener(this.focusedChannelListener);
    this.lx.engine.mixer.focusedChannelAux.removeListener(this.focusedChannelListener);
    this.lx.engine.mixer.cueA.removeListener(this.cueAListener);
    this.lx.engine.mixer.cueB.removeListener(this.cueBListener);
    this.lx.engine.mixer.auxA.removeListener(this.auxAListener);
    this.lx.engine.mixer.auxB.removeListener(this.auxBListener);
    this.lx.engine.tempo.enabled.removeListener(this.tempoListener);

    clearChannelGrid();
  }

  private void registerChannel(LXAbstractChannel channel) {
    this.channelListeners.put(channel, new ChannelListener(channel));
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

  private LXAbstractChannel getChannel(LXShortMessage message) {
    return getChannel(message.getChannel());
  }

  private LXSwatch getSwatch(int index) {
    if (index < 0) {
      return this.lx.engine.palette.swatch;
    }
    if (index < this.lx.engine.palette.swatches.size()) {
      return this.lx.engine.palette.swatches.get(index);
    }
    return null;
  }

  private LXBus getFocusedChannel() {
    return isAuxActive() ? this.lx.engine.mixer.getFocusedChannelAux() : this.lx.engine.mixer.getFocusedChannel();
  }

  private DiscreteParameter getFocusedChannelTarget() {
    return isAuxActive() ? this.lx.engine.mixer.focusedChannelAux : this.lx.engine.mixer.focusedChannel;
  }

  private DiscreteParameter getFocusedChannelAltTarget() {
    return isAuxActive() ? this.lx.engine.mixer.focusedChannel : this.lx.engine.mixer.focusedChannelAux;
  }

  private class CueState {
    private int cueDown = 0;
    private boolean singleCueStartedOn = false;

    private void reset() {
      this.cueDown = 0;
      this.singleCueStartedOn = false;
    }
  }

  private final CueState cueState = new CueState();
  private final CueState auxState = new CueState();

  private void noteReceived(MidiNote note, boolean on) {
    int pitch = note.getPitch();

    // Global toggle messages
    switch (pitch) {
    case SHIFT:
      this.shiftOn = on;
      updateColorKnobs();
      return;
    case BANK:
      if (on) {
        if (this.shiftOn) {
          this.lx.engine.clips.clipViewExpanded.toggle();
        } else if (this.deviceLockOn) {
          this.deviceLockOn = false;
          sendNoteOn(note.getChannel(), DEVICE_LOCK, LED_OFF);
          resetPaletteVars();
        } else {
          this.bankOn = !this.bankOn;
          sendNoteOn(note.getChannel(), pitch, this.bankOn ? LED_ON : LED_OFF);
        }
        updateGridMode();
      }
      return;
    case DEVICE_LOCK:
      if (on) {
        this.deviceLockOn = !this.deviceLockOn;
        sendNoteOn(note.getChannel(), pitch, this.deviceLockOn ? LED_ON : LED_OFF);
        if (!this.deviceLockOn) {
          resetPaletteVars();
        }
        updateGridMode();
      }
      return;
    case METRONOME:
      if (on) {
        lx.engine.tempo.enabled.toggle();
      }
      return;
    case TAP_TEMPO:
      if (this.rainbowMode) {
        this.rainbowMode = false;
        this.focusColor = null;
        this.colorClipboard = null;
        sendChannelFocus();
        sendChannelGrid();
      } else {
        lx.engine.tempo.tap.setValue(on);
      }
      return;
    case NUDGE_MINUS:
      lx.engine.tempo.nudgeDown.setValue(on);
      return;
    case NUDGE_PLUS:
      lx.engine.tempo.nudgeUp.setValue(on);
      return;
    }

    // Global momentary light-up buttons
    switch (pitch) {
    case CLIP_STOP:
      if (this.gridMode == GridMode.CLIP) {
        sendNoteOn(note.getChannel(), pitch, on ? LED_ON : LED_OFF);
      }
      break;
    case BANK_LEFT:
    case BANK_RIGHT:
    case DEVICE_LEFT:
    case DEVICE_RIGHT:
      sendNoteOn(note.getChannel(), pitch, on ? LED_ON : LED_OFF);
      break;
    }
    if (pitch >= SCENE_LAUNCH && pitch <= SCENE_LAUNCH_MAX && this.gridMode != GridMode.PALETTE) {
      sendNoteOn(note.getChannel(), pitch, on ? LED_GREEN : LED_OFF);
    }

    // Global momentary
    if (on) {
      LXBus bus;
      switch (pitch) {
      case PLAY:
        setAux(false);
        return;
      case RECORD:
        setAux(true);
        return;
      case SESSION:
        this.lx.engine.performanceMode.toggle();
        return;
      case MASTER_FOCUS:
        getFocusedChannelTarget().setValue(lx.engine.mixer.channels.size());
        if (!isAuxActive()) {
          lx.engine.mixer.selectChannel(lx.engine.mixer.masterBus);
        }
        return;
      case BANK_SELECT_LEFT:
        this.deviceListener.focusedDevice.previousChannel();
        if (!isAuxActive()) {
          lx.engine.mixer.selectChannel(lx.engine.mixer.getFocusedChannel());
        }
        return;
      case BANK_SELECT_RIGHT:
        this.deviceListener.focusedDevice.nextChannel();
        if (!isAuxActive()) {
          lx.engine.mixer.selectChannel(lx.engine.mixer.getFocusedChannel());
        }
        return;
      case BANK_SELECT_UP:
        bus = getFocusedChannel();
        if (this.shiftOn) {
          lx.engine.clips.clipViewGridOffset.decrement();
        } else if (bus instanceof LXChannel) {
          ((LXChannel) bus).focusedPattern.decrement(this.shiftOn ? CLIP_LAUNCH_ROWS : 1, false);
        }
        return;
      case BANK_SELECT_DOWN:
        bus = getFocusedChannel();
        if (this.shiftOn) {
          lx.engine.clips.clipViewGridOffset.increment();
        } else  if (bus instanceof LXChannel) {
          ((LXChannel) bus).focusedPattern.increment(this.shiftOn ? CLIP_LAUNCH_ROWS : 1, false);
        }
        return;
      case CLIP_DEVICE_VIEW:
        if (isAuxActive()) {
          this.lx.engine.mixer.auxA.toggle();
        } else {
          this.lx.engine.mixer.cueA.toggle();
        }
        return;
      case DETAIL_VIEW:
        if (isAuxActive()) {
          this.lx.engine.mixer.auxB.toggle();
        } else {
          this.lx.engine.mixer.cueB.toggle();
        }
        return;
      case STOP_ALL_CLIPS:
        if (this.gridMode == GridMode.PALETTE) {
          this.colorClipboard = null;
          this.focusColor = null;
        } else if (this.gridMode == GridMode.CLIP) {
          this.lx.engine.clips.stopClips();
        } else if (this.gridMode == GridMode.PATTERN) {
          if (this.lx.engine.performanceMode.isOn()) {
            getFocusedChannelAltTarget().setValue(lx.engine.mixer.channels.size());
            if (isAuxActive()) {
              this.lx.engine.mixer.selectChannel(lx.engine.mixer.masterBus);
            }
          }
        }
        return;

      }

      if (pitch >= SCENE_LAUNCH && pitch <= SCENE_LAUNCH_MAX) {
        int index = pitch - SCENE_LAUNCH;
        if (this.gridMode == GridMode.PALETTE) {
          // A button corresponding to an entry in the main palette swatch has been tapped.
          // Make it the focusColor (i.e., turning the CUE LEVEL knob will tweak it), and
          // if there's a color on the clipboard, paste it here.
          boolean colorChanged = false;
          LXSwatch swatch = getSwatch(MASTER_SWATCH);
          if (index > swatch.colors.size()-1 && index < LXSwatch.MAX_COLORS) {
            swatch.addColor();
            colorChanged = true;
          }
          this.focusColor = swatch.getColor(index);
          if (this.colorClipboard != null) {
            this.focusColor.primary.setColor(this.colorClipboard);
            colorChanged = true;
          }
          if (colorChanged) {
            sendSwatch(MASTER_SWATCH);
          }
        } else {
          this.lx.engine.clips.launchScene(index + lx.engine.clips.clipViewGridOffset.getValuei());
        }
        return;
      }

      if (pitch >= CLIP_LAUNCH && pitch <= CLIP_LAUNCH_MAX) {
        int channelIndex = (pitch - CLIP_LAUNCH) % CLIP_LAUNCH_COLUMNS;
        int index = CLIP_LAUNCH_ROWS - 1 - ((pitch - CLIP_LAUNCH) / CLIP_LAUNCH_COLUMNS);

        if (this.rainbowMode) {
          this.colorClipboard = rainbowGridColor(channelIndex, index);
          return;
        }

        if (this.gridMode == GridMode.PALETTE) {
          LXSwatch swatch = getSwatch(channelIndex);
          if (swatch != null) {
            if (index < swatch.colors.size()) {
              this.focusColor = swatch.colors.get(index);
              this.colorClipboard = this.focusColor.primary.getColor();
            } else if (index < LXSwatch.MAX_COLORS) {
              LXDynamicColor color = swatch.addColor();
              if (this.colorClipboard != null) {
                color.primary.setColor(this.colorClipboard);
              } else {
                this.colorClipboard = color.primary.getColor();
              }
              sendSwatch(channelIndex);
            } else {
              this.colorClipboard = null;
            }
          }
          return;
        }
        LXAbstractChannel channel = getChannel(channelIndex);
        if (channel != null) {
          if (this.gridMode == GridMode.PATTERN) {
            if (channel instanceof LXChannel) {
              LXChannel c = (LXChannel) channel;
              index += c.controlSurfaceFocusIndex.getValuei();
              if (index < c.getPatterns().size()) {
                c.focusedPattern.setValue(index);
                if (!this.shiftOn) {
                  if (c.compositeMode.getEnum() == LXChannel.CompositeMode.BLEND) {
                    c.patterns.get(index).enabled.toggle();
                  } else {
                    c.goPatternIndex(index);
                  }
                }
              }
            }
          } else {
            int clipIndex = index + lx.engine.clips.clipViewGridOffset.getValuei();
            LXClip clip = channel.getClip(clipIndex);
            if (clip == null) {
              clip = channel.addClip(clipIndex);
              clip.loop.setValue(this.shiftOn);
            } else if (this.shiftOn) {
              clip.loop.toggle();
            } else if (clip.isRunning()) {
              clip.stop();
            } else {
              clip.trigger();
              this.lx.engine.clips.setFocusedClip(clip);
            }
          }
        }
        return;
      }
    }


    // Channel messages
    if (this.gridMode == GridMode.PALETTE) {
      if (this.rainbowMode || !on) {
        return;
      }
      switch (note.getPitch()) {
      case CLIP_STOP:
        int swatchNum = note.getChannel();
        if (swatchNum < lx.engine.palette.swatches.size()) {
          lx.engine.palette.setSwatch(lx.engine.palette.swatches.get(swatchNum));
          sendSwatch(MASTER_SWATCH);
        }
        break;
      default:
        LXMidiEngine.error("APC40mk2 in DEV_LOCK received unmapped note: " + note);
      }
      return;
    }

    // The rest of the buttons are channel specific
    LXAbstractChannel channel = getChannel(note);
    if (channel == null) {
      // No channel, bail out
      return;
    }

    if (note.getPitch() == CHANNEL_SOLO) {
      handleMultiCue(on, this.cueState, channel, false);
      return;
    }

    if ((note.getPitch() == CHANNEL_ARM) && this.lx.engine.performanceMode.isOn()) {
      handleMultiCue(on, this.auxState, channel, true);
      return;
    }

    // From here on, we only handle button *presses* - not releases
    if (!on) {
      return;
    }

    switch (note.getPitch()) {
    case CHANNEL_ARM:
      if (!this.lx.engine.performanceMode.isOn()) {
        channel.arm.toggle();
      }
      break;
    case CHANNEL_ACTIVE:
      channel.enabled.toggle();
      break;
    case CHANNEL_CROSSFADE_GROUP:
      if (this.shiftOn) {
        channel.blendMode.increment();
      } else {
        channel.crossfadeGroup.increment();
      }
      break;
    case CLIP_STOP:
      if (this.gridMode == GridMode.CLIP) {
        channel.stopClips();
      } else if (this.gridMode == GridMode.PATTERN) {
        if (this.lx.engine.performanceMode.isOn()) {
          getFocusedChannelAltTarget().setValue(channel.getIndex());
        }
      }
      break;
    case CHANNEL_FOCUS:
      if (this.shiftOn) {
        if (channel instanceof LXChannel) {
          ((LXChannel) channel).autoCycleEnabled.toggle();
        }
      } else {
        getFocusedChannelTarget().setValue(channel.getIndex());
        if (!isAuxActive()) {
          lx.engine.mixer.selectChannel(lx.engine.mixer.getFocusedChannel());
        }
      }
      break;
    case DEVICE_ON_OFF:
      this.deviceListener.onDeviceOnOff();
      break;
    case DEVICE_LEFT:
      this.deviceListener.focusedDevice.previousDevice();
      break;
    case DEVICE_RIGHT:
      this.deviceListener.focusedDevice.nextDevice();
      break;
    case BANK_LEFT:
    case BANK_RIGHT:
      this.deviceListener.incrementBank((pitch == BANK_LEFT) ? -1 : 1);
      break;

    default:
      LXMidiEngine.error("APC40mk2 received unmapped note: " + note);
    }

  }

  private void handleMultiCue(boolean on, CueState state, LXAbstractChannel channel, boolean aux) {
    BooleanParameter active = aux ? channel.auxActive : channel.cueActive;
    if (on) {
      boolean alreadyOn = active.isOn();

      // First cue pressed, if active, could be un-cue or start of multi-select
      state.singleCueStartedOn = (state.cueDown == 0) && alreadyOn;
      if (alreadyOn) {
        if (state.cueDown == 0) {
          // Turn off all other cues on the first fresh press, leave this one on
          if (aux) {
            this.lx.engine.mixer.enableChannelAux(channel, true);
          } else {
            this.lx.engine.mixer.enableChannelCue(channel, true);
          }
        } else {
          active.setValue(false);
        }
      } else {
        if (aux) {
          this.lx.engine.mixer.enableChannelAux(channel, state.cueDown == 0);
        } else {
          this.lx.engine.mixer.enableChannelCue(channel, state.cueDown == 0);
        }
      }
      ++state.cueDown;
    } else {
      // Play defense here, just in case a button was down *before* control surface mode
      // was activated and gets released after
      state.cueDown = LXUtils.max(0, state.cueDown - 1);

      if (state.singleCueStartedOn) {
        // Turn this one off.  Already got the others on the cue down
        active.setValue(false);
        state.singleCueStartedOn = false;
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
    case TEMPO:
      if (this.gridMode == GridMode.PALETTE) {
        if (this.rainbowMode) {
          this.rainbowColumnOffset = (this.rainbowColumnOffset + cc.getRelative())
                  % RAINBOW_GRID_COLUMNS;
          this.focusColor = null;
          this.colorClipboard = null;
        } else {
          this.rainbowMode = true;
          sendChannelFocus();
        }
        sendRainbowPickerGrid();
      } else if (this.shiftOn) {
        this.lx.engine.tempo.adjustBpm(.1 * cc.getRelative());
      } else {
        this.lx.engine.tempo.adjustBpm(cc.getRelative());
      }
      return;
    case CUE_LEVEL:
      if (this.focusColor == null) {
        this.focusColor = this.lx.engine.palette.color;
      }
      LXListenableNormalizedParameter subparam = getActiveSubparameter(this.focusColor.primary);
      CompoundParameter cp = (CompoundParameter) subparam;
      cp.incrementValue(cc.getRelative());
      this.colorClipboard = this.focusColor.primary.getColor();
      if (this.gridMode == GridMode.PALETTE) {
        if (this.rainbowMode) {
          sendSwatch(MASTER_SWATCH);
        } else {
          sendSwatches();
        }
      }
      return;
    case CHANNEL_FADER:
      int channel = cc.getChannel();
      if (channel < this.lx.engine.mixer.channels.size()) {
        this.lx.engine.mixer.channels.get(channel).fader.setNormalized(cc.getNormalized());
      }
      return;
    case MASTER_FADER:
      if (this.masterFaderEnabled.isOn()) {
        this.lx.engine.output.brightness.setNormalized(cc.getNormalized());
      }
      return;
    case CROSSFADER:
      if (this.crossfaderEnabled.isOn()) {
        this.lx.engine.mixer.crossfader.setNormalized(cc.getNormalized());
      }
      return;
    }

    if (number >= DEVICE_KNOB && number <= DEVICE_KNOB_MAX) {
      this.deviceListener.onKnob(number - DEVICE_KNOB, cc.getNormalized());
      return;
    }

    if (number >= CHANNEL_KNOB && number <= CHANNEL_KNOB_MAX) {
      int channel = number - CHANNEL_KNOB;
      if (channel < this.lx.engine.mixer.channels.size() && this.channelKnobIsView.isOn() && ViewCentral.ENABLED) {
        ViewCentral.get().get(this.lx.engine.mixer.channels.get(channel)).view.setNormalized(cc.getNormalized());
      } else {
        sendControlChange(cc.getChannel(), cc.getCC(), cc.getValue());
      }
      return;
    }

    // LXMidiEngine.error("APC40mk2 UNMAPPED: " + cc);
  }

  @Override
  public int getRemoteControlStart() {
    return DEVICE_KNOB_NUM * this.deviceListener.bankNumber;
  }

  @Override
  public int getRemoteControlLength() {
    return DEVICE_KNOB_NUM;
  }

  @Override
  public boolean isRemoteControlAux() {
    return isAuxActive();
  }

  @Override
  public void dispose() {
    if (this.isRegistered) {
      unregister();
    }
    if (this.enabled.isOn()) {
      setApcMode(GENERIC_MODE);
    }
    this.deviceListener.dispose();
    super.dispose();
  }

}
