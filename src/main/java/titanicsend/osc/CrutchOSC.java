/**
 * Licensing Notes (JKB)
 *
 * The expected permanent home for this concept and its derivatives is the LX Studio
 * software library or a LX Studio / Chromatik extension distributed by JKB.
 *
 * Due to time constraints, doing a first release of this code in either
 * of the above code bases would add too much delay to be usable
 * for the immediate TE events.
 *
 * It is acknowledged that by releasing the code here, the TE code base may
 * continue to use this original version in perpetuity.
 * It is also the stated intent that the long-term license for this code
 * and its derivatives will be the LX Studio Software License and
 * Distribution Agreement (http://lx.studio/license), or another license
 * as determined by the author. 
 *
 * @author Justin Belcher <jkbelcher@gmail.com>
 */

package titanicsend.osc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.color.LXPalette;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXMixerEngine;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.osc.LXOscEngine;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameter.Polarity;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;

public class CrutchOSC extends LXComponent implements LXOscComponent, LXMixerEngine.Listener, LXPalette.Listener {

  public static final String OSC_PATH = "focus";
  public static final String PATH_PRIMARY = "/lx/" + OSC_PATH + "/channel/pattern/focused/parameter/";
  public static final String PATH_AUX = "/lx/" + OSC_PATH + "/channelAux/pattern/focused/parameter/";
  public static final int PATH_PRIMARY_LENGTH = PATH_PRIMARY.length();
  public static final int PATH_AUX_LENGTH = PATH_AUX.length();

  private static CrutchOSC current;
  public static CrutchOSC get() {
    return current;
  }

  public final BooleanParameter transmitActive =
    new BooleanParameter("OSC to iPads", true)
    .setDescription("CrutchOSC output");

  public CrutchOSC(LX lx) {
    super(lx);
    current = this;

    // Listen and fire immediately
    lx.engine.mixer.focusedChannel.addListener(this, true);
    lx.engine.mixer.focusedChannelAux.addListener(this, true);

    // Collection counts
    lx.engine.mixer.addListener(this);
    lx.engine.palette.addListener(this);
  }

  protected String getCrutchOSCaddress(int position, boolean isAux) {
    if (isAux) {
      return PATH_AUX + (position+1);
    } else {
      return PATH_PRIMARY + (position+1);
    }
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (parameter == lx.engine.mixer.focusedChannel) {
      onFocusedChannel();
    } else if (parameter == lx.engine.mixer.focusedChannelAux) {
      onFocusedChannelAux();
    }
  }

  private LXParameterListener focusedPatternListener = (p) -> {
    if (this.channel != null && p == this.channel.focusedPattern) {
      onFocusedPattern(this.channel.getFocusedPattern(), false);
    }
  };

  private LXParameterListener focusedPatternAuxListener = (p) -> {
    if (this.channelAux != null && p == this.channelAux.focusedPattern) {
      onFocusedPattern(this.channelAux.getFocusedPattern(), true);
    }
  };

  private LXChannel channel;
  private LXChannel channelAux;
  private LXPattern pattern;
  private LXPattern patternAux;

  private void onFocusedChannel() {
    if (this.channel != null) {
      unregisterChannel(this.channel, false);
    }
    this.channel = null;
    LXBus focusedBus = this.lx.engine.mixer.getFocusedChannel();
    if (focusedBus instanceof LXChannel) {
      this.channel = (LXChannel)focusedBus;
      registerChannel(this.channel, false);
    }
  }


  private void onFocusedChannelAux() {
    if (this.channelAux != null) {
      unregisterChannel(this.channelAux, true);
    }
    this.channelAux = null;
    LXBus focusedBus = this.lx.engine.mixer.getFocusedChannelAux();
    if (focusedBus instanceof LXChannel) {
      this.channelAux = (LXChannel)focusedBus;
      registerChannel(this.channelAux, true);
    }
  }

  private void onFocusedPattern(LXPattern pattern, boolean isAux) {
    // We don't actually have to call patternListener.unregisterPattern,
    // it will be done automatically.
    if (isAux) {
      this.patternAux = pattern;
    } else {
      this.pattern = pattern;
    }
    registerPattern(this.pattern, isAux);
  }

  private void registerChannel(LXChannel channel, boolean isAux) {
    LXPattern pattern = channel.getFocusedPattern();
    if (isAux) {
      channel.focusedPattern.addListener(focusedPatternAuxListener);
      this.patternAux = pattern;
      registerPattern(this.patternAux, isAux);  // ok to pass null
    } else {
      channel.focusedPattern.addListener(focusedPatternListener);
      this.pattern = pattern;
      registerPattern(this.pattern, isAux);  // ok to pass null
    }
  }

  private void unregisterChannel(LXChannel channel, boolean isAux) {
    if (isAux) {
      channel.focusedPattern.removeListener(focusedPatternAuxListener);
      if (this.pattern != null) {
        unRegisterPattern(isAux);
      }
    } else {
      channel.focusedPattern.removeListener(focusedPatternListener);
      if (this.patternAux != null) {
        unRegisterPattern(isAux);
      }
    }
  }

  private boolean canSend() {
    // Copied from LXComponent: These checks are necessary for bootstrapping, before the OSC engine is spun up
    return this.transmitActive.isOn() && (this.lx != null) && (this.lx.engine != null) && (this.lx.engine.osc != null) && (this.lx.engine.osc.transmitActive.isOn());
  }

  private void parameterInstanceChanged(LXListenableNormalizedParameter parameter, int position, boolean isAux) {
    if (canSend()) {
      String address = getCrutchOSCaddress(position, isAux);
      lx.engine.osc.sendMessage(address + "/label", getLabel(parameter));
      lx.engine.osc.sendMessage(address + "/type", getType(parameter));
      lx.engine.osc.sendMessage(address + "/polarity", getPolarity(parameter));
      // Value
      sendParameterValue(parameter, address);
    }
  }

  private void parameterValueChanged(LXListenableNormalizedParameter parameter, int position, boolean isAux) {
    if (canSend()) {
      sendParameterValue(parameter, getCrutchOSCaddress(position, isAux));
    }
  }

  private void sendParameterValue(LXListenableNormalizedParameter parameter, String address) {
    lx.engine.osc.sendMessage(address + "/normalized", getValueNormalized(parameter));
    lx.engine.osc.sendMessage(address + "/displayValue", getValueString(parameter));
  }

  protected String getLabel(LXListenableNormalizedParameter parameter) {
    if (parameter != null) {
      return parameter.getLabel();
    }
    return "";
  }

  protected float getType(LXListenableNormalizedParameter parameter) {
    // Type: 0=null, 1=boolean, 2=compound or discrete
    if (parameter != null) {
      return parameter instanceof BooleanParameter ? 1f : 2f;
    }
    return 0;
  }

  protected float getPolarity(LXListenableNormalizedParameter parameter) {
    // Polarity: 0=Unipolar, 1=Bipolar
    if (parameter != null) {
      return parameter.getPolarity() == Polarity.BIPOLAR ? 1 : 0;
    }
    return 0;
  }

  protected float getValueNormalized(LXListenableNormalizedParameter parameter) {
    if (parameter != null) {
      return parameter.getNormalizedf();
    }
    return 0;
  }

  protected void sendSizeMixer() {
    if (canSend()) {
      sendSize(this.lx.engine.mixer.getOscAddress(), this.lx.engine.mixer.channels.size());
    }
  }

  protected void sendSizePalette() {
    if (canSend()) {
      sendSize(this.lx.engine.palette.getOscAddress() + "/swatches", this.lx.engine.palette.swatches.size());
    }
  }

  protected void sendSize(String address, int size) {
    lx.engine.osc.sendMessage(address + "/count", size);
  }

  // This method copied from GLX's UIParameterControl:
  // https://github.com/mcslee/GLX/blob/dev/src/main/java/heronarts/glx/ui/component/UIParameterControl.java#L227
  protected String getValueString(LXListenableNormalizedParameter parameter) {
    if (parameter != null) {
      if (parameter instanceof DiscreteParameter) {
        return ((DiscreteParameter) parameter).getOption();
      } else if (parameter instanceof BooleanParameter) {
        return ((BooleanParameter) parameter).isOn() ? "ON" : "OFF";
      } else if (parameter instanceof CompoundParameter) {
        return parameter.getFormatter().format(((CompoundParameter) parameter).getBaseValue());
      } else {
        return parameter.getFormatter().format(parameter.getValue());
      }
    }
    return "-";
  }

  private class PatternParameterListener implements LXParameterListener {

    private final boolean isAux;

    private LXPattern pattern;

    private final List<LXListenableNormalizedParameter> params = new ArrayList<LXListenableNormalizedParameter>();

    public PatternParameterListener(boolean isAux) {
      this.isAux = isAux;
    }

    private LXParameterListener remoteControlsChangedListener = (p) -> {
      unregisterParameters();
      registerParameters();
    };

    public void registerPattern(LXPattern pattern) {
      if (this.pattern != null) {
        unregisterPattern();
      }
      this.pattern = pattern;

      if (this.pattern != null) {
        this.pattern.remoteControlsChanged.addListener(remoteControlsChangedListener);
        registerParameters();
      }
    }

    private void unregisterPattern() {
      if (pattern != null) {
        unregisterParameters();
        this.pattern.remoteControlsChanged.removeListener(remoteControlsChangedListener);
      }
    }

    private void registerParameters() {
      // params is clear from unregisterPattern;
      this.params.addAll(Arrays.asList(this.pattern.getRemoteControls()));
      int p;
      for (p = 0; p < this.params.size(); p++) {
        LXListenableNormalizedParameter param = this.params.get(p);
        registerParameter(param, p);
      }
      // Quick solution, hide unused knobs in MFT heads up display
      while (p < 32) {
        parameterInstanceChanged(null, p, this.isAux);
        p++;
      }
    }

    private void registerParameter(LXListenableNormalizedParameter parameter, int position) {
      // Ehh.. should keep a map of indices for faster reverse lookups?
      if (parameter != null) {
        try {
          parameter.addListener(this);
        } catch (IllegalStateException ex) {
          // Parameter was in remote controls twice? Ok...
        }
      }
      parameterInstanceChanged(parameter, position, this.isAux);
    }

    private void unregisterParameters() {
      for (LXListenableNormalizedParameter parameter : this.params) {
        unregisterParameter(parameter);
      }
      this.params.clear();
    }

    private void unregisterParameter(LXListenableNormalizedParameter parameter) {
      if (parameter != null) {
        try {
          parameter.removeListener(this);
        } catch (Exception ex) {
          // Parameter was in remote controls twice? Look the other way...
          // Don't mind the red text in the log.
        }
      }
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
      int i = params.indexOf(parameter);
      parameterValueChanged((LXListenableNormalizedParameter)parameter, i, this.isAux);
    }

    public void setValue(int index, float normalized) {
      if (index < this.params.size() && this.params.get(index) != null) {
        this.params.get(index).setNormalized(normalized);
      }
    }

    public void setValue(String path, float value) {
      if (this.pattern != null) {
        LXParameter param = this.pattern.getParameter(path);
        if (param != null) {
          param.setValue(value);
        }
      }
    }

    public void sendAll() {
      int p;
      for (p = 0; p < this.params.size(); p++) {
        LXListenableNormalizedParameter parameter = this.params.get(p);
        parameterInstanceChanged(parameter, p, this.isAux);
      }
      // Quick solution, hide unused knobs in MFT heads up display
      while (p < 32) {
        parameterInstanceChanged(null, p, this.isAux);
        p++;
      }
    }

    public void dispose() {
      if (this.pattern != null) {
        unregisterPattern();
        pattern = null;
      }
    }
  }

  private final PatternParameterListener patternListener = new PatternParameterListener(false);
  private final PatternParameterListener patternListenerAux = new PatternParameterListener(true);

  private void registerPattern(LXPattern pattern, boolean isAux) {
    if (isAux) {
      this.patternListenerAux.registerPattern(pattern);
    } else {
      this.patternListener.registerPattern(pattern);
    }
  }

  private void unRegisterPattern(boolean isAux) {
    if (isAux) {
      this.patternListenerAux.registerPattern(null);
    } else {
      this.patternListener.registerPattern(null);
    }
  }

  @Override
  public String getOscPath() {
    return OSC_PATH;
  }

  @Override
  public boolean handleOscMessage(OscMessage message, String[] parts, int index) {
    String path = parts[index];

    if (path.equals("osc-query")) {
      crutchOscQuery();
      return true;
    }

    String address = message.getAddressPattern().toString();
    String piString = parts[parts.length-2];
    try {
      boolean isInt = piString.matches("\\d+");
      int pi = -1;
      if (isInt) {
        pi = Integer.parseInt(piString) - 1;
      }

      if (address.startsWith(PATH_PRIMARY)) {
        LXBus fc = this.lx.engine.mixer.getFocusedChannel();
        if (fc != null && fc instanceof LXChannel) {
          LXPattern fp = ((LXChannel)fc).getFocusedPattern();
          if (fp != null) {
            if (isInt) {
              this.patternListener.setValue(pi, message.getFloat());
            } else {
              this.patternListener.setValue(piString, message.getFloat());
            }
            return true;
          }
        }
        return true;
      } else if (address.startsWith(PATH_AUX)) {
        LXBus fc = this.lx.engine.mixer.getFocusedChannelAux();
        if (fc != null && fc instanceof LXChannel) {
          LXPattern fp = ((LXChannel)fc).getFocusedPattern();
          if (fp != null) {
            if (isInt) {
              this.patternListenerAux.setValue(pi, message.getFloat());
            } else {
              this.patternListenerAux.setValue(piString, message.getFloat());
            }
          }
        }
      }
      return true;
    } catch (Exception ex) {
      LXOscEngine.error(ex, "Invalid OSC message for CrutchOSC");
    }

    return super.handleOscMessage(message, parts, index);
  }

  private void crutchOscQuery() {
    sendSizeMixer();
    sendSizePalette();

    this.patternListener.sendAll();
    this.patternListenerAux.sendAll();
  }

  // LXMixerEngine.Listener methods

  @Override
  public void channelAdded(LXMixerEngine mixer, LXAbstractChannel channel) {
    sendSizeMixer();
  }

  @Override
  public void channelRemoved(LXMixerEngine mixer, LXAbstractChannel channel) {
    sendSizeMixer();
  }

  @Override
  public void channelMoved(LXMixerEngine mixer, LXAbstractChannel channel) { }

  // LXPalette.Listener methods

  @Override
  public void swatchAdded(LXPalette palette, LXSwatch swatch) {
    sendSizePalette();
  }

  @Override
  public void swatchRemoved(LXPalette palette, LXSwatch swatch) {
    sendSizePalette();
  }

  @Override
  public void swatchMoved(LXPalette palette, LXSwatch swatch) { }

  @Override
  public void dispose() {
    lx.engine.mixer.focusedChannel.removeListener(this);
    lx.engine.mixer.focusedChannelAux.removeListener(this);
    lx.engine.mixer.removeListener(this);
    lx.engine.palette.removeListener(this);

    if (this.channel != null) {
      unregisterChannel(this.channel, false);
      this.channel = null;
    }
    if (this.channelAux != null) {
      unregisterChannel(this.channelAux, true);
      this.channelAux = null;
    }
    this.patternListener.dispose();
    this.patternListenerAux.dispose();
    super.dispose();
  }

}
