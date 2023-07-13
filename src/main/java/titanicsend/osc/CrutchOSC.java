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
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameter.Polarity;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.studio.TEApp;
import titanicsend.util.TE;

public class CrutchOSC extends LXComponent implements LXOscComponent {

  static public final boolean ENABLED = TEApp.ENABLE_TOUCHOSC_IPADS;

  public static final String OSC_PATH = "focus";
  public static final String PATH_PRIMARY = "/lx/focus/channel/pattern/focused/parameter/";
  public static final String PATH_AUX = "/lx/focus/channelAux/pattern/focused/parameter/";
  public static final int PATH_PRIMARY_LENGTH = PATH_PRIMARY.length();
  public static final int PATH_AUX_LENGTH = PATH_AUX.length();

  public CrutchOSC(LX lx) {
    super(lx);

    // Listen and fire immediately
    lx.engine.mixer.focusedChannel.addListener(this, true);
    lx.engine.mixer.focusedChannelAux.addListener(this, true);
  }

  protected String getCrutchOSCaddress(int position, boolean isAux) {
    if (isAux) {
      return PATH_AUX + position;
    } else {
      return PATH_PRIMARY + position;
    }
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    try {
      if (parameter == lx.engine.mixer.focusedChannel) {
        onFocusedChannel();
      } else if (parameter == lx.engine.mixer.focusedChannelAux) {
        onFocusedChannelAux();
      }
    } catch (Exception ex) {
      TE.err(ex, "CrutchOSC ");
    }
  }

  private LXParameterListener focusedPatternListener = (p) -> {
    try {
      if (this.channel != null && p == this.channel.focusedPattern) {
        onFocusedPattern(this.channel.getFocusedPattern(), false);
      }
    } catch (Exception ex) {
      TE.err(ex, "CrutchOSC focusedPatternListener");
    }
  };

  private LXParameterListener focusedPatternAuxListener = (p) -> {
    try {
      if (this.channelAux != null && p == this.channelAux.focusedPattern) {
        onFocusedPattern(this.channelAux.getFocusedPattern(), true);
      }
    } catch (Exception ex) {
      TE.err(ex, "CrutchOSC ");
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

  private void parameterInstanceChanged(LXListenableNormalizedParameter parameter, int position, boolean isAux) {
    String address = getCrutchOSCaddress(position, isAux);
    lx.engine.osc.sendMessage(address + "/label", getLabel(parameter));
    lx.engine.osc.sendMessage(address + "/type", getType(parameter));
    lx.engine.osc.sendMessage(address + "/polarity", getPolarity(parameter));
    // Value
    sendParameterValue(parameter, address);
  }

  private void parameterValueChanged(LXListenableNormalizedParameter parameter, int position, boolean isAux) {
    sendParameterValue(parameter, getCrutchOSCaddress(position, isAux));
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
    // Type: 0=compound, 1=boolean
    if (parameter != null) {
      return parameter instanceof BooleanParameter ? 1f : 0f;
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
      try {
        unregisterParameters();
        registerParameters();
      } catch (Exception ex) {
        TE.err(ex, "CrutchOSC ");
      }
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
      for (int p = 0; p < this.params.size(); p++) {
        LXListenableNormalizedParameter param = this.params.get(p);
        registerParameter(param, p);
      }      
    }

    private void registerParameter(LXListenableNormalizedParameter parameter, int position) {
      // Ehh.. should keep a map of indices for faster reverse lookups?
      if (parameter != null) {
        parameter.addListener(this);
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
        parameter.removeListener(this);
      }
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
      try {
        int i = params.indexOf(parameter);
        parameterValueChanged((LXListenableNormalizedParameter)parameter, i, this.isAux);
      } catch (Exception ex) {
        TE.err(ex, "CrutchOSC ");
      }
    }

    public void setValue(int index, float normalized) {
      if (index < this.params.size() && this.params.get(index) != null) {
        this.params.get(index).setNormalized(normalized);
      }
    }

    public void sendAll() {
      for (int p = 0; p < this.params.size(); p++) {
        LXListenableNormalizedParameter parameter = this.params.get(p);
        parameterInstanceChanged(parameter, p, this.isAux);
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
  //public void oscMessage(OscMessage message) {
  public boolean handleOscMessage(OscMessage message, String[] parts, int index) {
    try {
      String path = parts[index];

      if (path.equals("osc-query")) {
        oscQuery();
        return true;
      }

      String address = message.getAddressPattern().toString();
      String piString = parts[parts.length-2];
      LX.log(piString);
      try {
        int pi = Integer.parseInt(piString);

        if (address.startsWith(PATH_PRIMARY)) {
          LXBus fc = this.lx.engine.mixer.getFocusedChannel();
          if (fc != null && fc instanceof LXChannel) {
            LXPattern fp = ((LXChannel)fc).getFocusedPattern();
            if (fp != null) {
              this.patternListener.setValue(pi, message.getFloat());
              return true;
            }
          }
          return true;
        } else if (address.startsWith(PATH_AUX)) {
          LXBus fc = this.lx.engine.mixer.getFocusedChannelAux();
          if (fc != null && fc instanceof LXChannel) {
            LXPattern fp = ((LXChannel)fc).getFocusedPattern();
            if (fp != null) {
              this.patternListenerAux.setValue(pi, message.getFloat());
            }
          }
        }
        return true;
      } catch (Exception ex) {
        TE.err(ex, "Invalid OSC message for CrutchOSC");
      }

    } catch (Exception ex) {
      TE.err(ex, "CrutchOSC ");
    }

    return super.handleOscMessage(message, parts, index);
  }

  // Send out the values of all our children by OSC
  private void oscQuery() {
    this.patternListener.sendAll();
  }

  @Override
  public void dispose() {
    lx.engine.mixer.focusedChannel.removeListener(this);
    lx.engine.mixer.focusedChannelAux.removeListener(this);
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
