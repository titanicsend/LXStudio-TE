package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.TEApp;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.color.TEColorType;
import titanicsend.dmx.model.BeaconModel;
import titanicsend.dmx.model.ChauvetBeamQ60Model;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.parameter.DmxCompoundParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameter;
import titanicsend.gamepad.GamepadEngine;
import titanicsend.ui.UIUtils;

@LXCategory("DMX")
public class BeaconGamePattern extends BeaconPattern
        implements UIDeviceControls<BeaconGamePattern>, GamepadEngine.Gamepad.GamepadListener {

    public static final float TRIGGER_THRESHOLD = 0.1f;

    public final DiscreteParameter input = new DiscreteParameter("Gamepad", 16)
            .setDescription("Gamepad input number 1-16");

    private final GamepadEngine.Gamepad gamepad;

    public BeaconGamePattern(LX lx) {
        super(lx);

        this.pan.setNormalized(0.5);
        this.tilt.setNormalized(0.5);

        // TODO: remove visual controls from game pattern
        addParameter("Pan", this.pan);
        addParameter("Tilt", this.tilt);
        addParameter("Cyan", this.cyan);
        addParameter("Magenta", this.magenta);
        addParameter("Yellow", this.yellow);
        addParameter("ClrWheel", this.colorWheel);
        addParameter("Gobo1", this.gobo1);
        addParameter("Gobo1Rotate", this.gobo1rotation);
        addParameter("Gobo2", this.gobo2);
        addParameter("Prism1", this.prism1);
        addParameter("Prism1Rotate", this.prism1rotation);
        addParameter("Prism2Rotate", this.prism2rotation);
        addParameter("Focus", this.focus);
        addParameter("Shutter", this.shutter);
        addParameter("Dimmer", this.dimmer);
        addParameter("Frost1", this.frost1);
        addParameter("Frost2", this.frost2);
        addParameter("ptSpd", this.ptSpeed);
        addParameter("Control", this.control); // Use caution!

        addParameter("Gamepad", this.input);

        this.gamepad = TEApp.gamepadEngine.createGamepad();
        this.gamepad.addListener(this);
    }

    @Override
    public void onParameterChanged(LXParameter p) {
      if (p == this.input) {
        this.gamepad.input.setValue(this.input.getValue());
      }
    }

    private static final int ptSpeedTick = 5;

  @Override
  public void onGamepadParameterChanged(LXParameter p) {
      // Axis parameters
      if (p == this.gamepad.axisLeftX) {
        gamepadAxisToBeaconParameter(this.pan, this.gamepad.axisLeftX);
      } else if (p == this.gamepad.axisRightY) {
        gamepadAxisToBeaconParameter(this.tilt, this.gamepad.axisRightY);

      // Button pairs for Up/Down discrete parameter options.
      // These cycle through a hardcoded short list.

      // DPad up/down
      } else if (p == this.gamepad.dpUp && this.gamepad.dpUp.isOn()) {
        incrementBeaconParameter(this.gobo1, this.gobo1CycleValues);
      } else if (p == this.gamepad.dpDown && this.gamepad.dpDown.isOn()) {
        decrementBeaconParameter(this.gobo1, this.gobo1CycleValues);

      // DPad right/left
      } else if (p == this.gamepad.dpRight && this.gamepad.dpRight.isOn()) {
        incrementBeaconParameter(this.gobo2, this.gobo2CycleValues);
      } else if (p == this.gamepad.dpLeft && this.gamepad.dpLeft.isOn()) {
        decrementBeaconParameter(this.gobo2, this.gobo2CycleValues);

      // Shoulder right/left
      } else if (p == this.gamepad.rightShoulder && this.gamepad.rightShoulder.isOn()) {
          incrementBeaconParameter(this.colorWheel, this.clrWheelCycleValues);
      } else if (p == this.gamepad.leftShoulder && this.gamepad.leftShoulder.isOn()) {
        decrementBeaconParameter(this.colorWheel, this.clrWheelCycleValues);

      // A/B
      } else if (p == this.gamepad.a && this.gamepad.a.isOn()) {
        incrementBeaconParameter(this.prism1, this.prism1.getDmxValuesInt());
      } else if (p == this.gamepad.b && this.gamepad.b.isOn()) {
        decrementBeaconParameter(this.prism1, this.prism1.getDmxValuesInt());

      // X/Y
      } else if (p == this.gamepad.x && this.gamepad.x.isOn()) {
        if (this.gobo1rotation.getDmxValueLimited() < 255) {
          this.gobo1rotation.incrementNormalized(3 / 255.);
        }
      } else if (p == this.gamepad.y && this.gamepad.y.isOn()) {
        if (this.gobo1rotation.getDmxValueLimited() > 128) {
          this.gobo1rotation.incrementNormalized(-(3 / 255.));
        }
      }

      // LX.log("Gamepad parameter changed: " + p.getLabel() + " -> " + p.getValue());
  }

    private void incrementBeaconParameter(DmxDiscreteParameter parameter, int[] options) {
      int value = parameter.getDmxValue();
      int iNext = 0;
      for (int i = 0; i < options.length; i++) {
        if (options[i] == value) {
          iNext = (i + 1) % options.length;
          break;
        }
      }
      parameter.setDmxValue(options[iNext]);
    }

    private void decrementBeaconParameter(DmxDiscreteParameter parameter, int[] options) {
        int value = parameter.getDmxValue();
        int iPrev = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == value) {
                iPrev = i - 1;
                if (iPrev < 0) {
                    iPrev = options.length - 1;
                }
                break;
            }
        }
        parameter.setDmxValue(options[iPrev]);
    }

    private void gamepadAxisToBeaconParameter(DmxCompoundParameter target, LXNormalizedParameter source) {
      target.setValue(target.range.normalizedToValue(source.getNormalized(), 3, BoundedParameter.NormalizationCurve.BIAS_CENTER));
    }

    // Values of trigger axis
    private int shutterValue = BeaconModel.SHUTTER_OPEN;
    private int focusValue = BeaconModel.DEFAULT_FOCUS;

    private static final int STROBE_MIN = 75;   // A little faster than the minimum
    private static final int STROBE_MAX = BeaconModel.STROBE_MAX;

    @Override
    public void run(double deltaMs) {
        float rightTrigger = this.gamepad.axisRightTrigger.getNormalizedf();
        if (rightTrigger < TRIGGER_THRESHOLD) {
            shutterValue = BeaconModel.SHUTTER_OPEN;
        } else {
            shutterValue = (int) ((rightTrigger - TRIGGER_THRESHOLD) * (STROBE_MAX-STROBE_MIN) / (1 - TRIGGER_THRESHOLD) + STROBE_MIN);
        }
        this.shutter.setDmxValue(shutterValue);

        float leftTrigger = this.gamepad.axisLeftTrigger.getNormalizedf();
        if (leftTrigger < TRIGGER_THRESHOLD) {
            focusValue = BeaconModel.DEFAULT_FOCUS;
        } else {
            focusValue = (int) ((leftTrigger - TRIGGER_THRESHOLD) / (1 - TRIGGER_THRESHOLD) * 255);
        }
        this.focus.setDmxValue(focusValue);

        // Reminder: Don't use Normalized for DmxDiscreteParameters,
        // they likely do not scale linearly to 0-255.
        double cyan = this.cyan.getNormalized();
        double magenta = this.magenta.getNormalized();
        double yellow = this.yellow.getNormalized();
        int colorWheel = this.colorWheel.getDmxValue();
        double dimmer = this.dimmer.getNormalized();
        int control = this.control.getDmxValue();

        // For tactile control
        double pan = this.pan.getNormalized();
        double tilt = this.tilt.getNormalized();
        double focus = this.focus.getNormalized();
        int shutter = this.shutter.getDmxValue();

        int gobo1 = this.gobo1.getDmxValue();
        double gobo1rotate = this.gobo1rotation.getNormalized();
        int gobo2 = this.gobo2.getDmxValue();
        int prism1 = this.prism1.getDmxValue();
        double prism1rotate = this.prism1rotation.getNormalized();
        double prism2rotate = this.prism2rotation.getNormalized();
        double frost1 = this.frost1.getNormalized();
        double frost2 = this.frost2.getNormalized();
        int ptSpd = this.ptSpeed.getDmxValue();

        boolean invertPan = false;

        for (DmxModel d : this.modelTE.getBeacons()) {
            if (d instanceof BeaconModel) {
                setDmxNormalized(d, BeaconModel.INDEX_PAN, invertPan ? pan : (1 - pan));
                setDmxNormalized(d, BeaconModel.INDEX_TILT, tilt);
                setDmxNormalized(d, BeaconModel.INDEX_CYAN, cyan);
                setDmxNormalized(d, BeaconModel.INDEX_MAGENTA, magenta);
                setDmxNormalized(d, BeaconModel.INDEX_YELLOW, yellow);
                setDmxValue(d, BeaconModel.INDEX_COLOR_WHEEL, colorWheel);
                setDmxValue(d, BeaconModel.INDEX_GOBO1, gobo1);
                setDmxNormalized(d, BeaconModel.INDEX_GOBO1_ROTATION, gobo1rotate);
                setDmxValue(d, BeaconModel.INDEX_GOBO2, gobo2);
                setDmxValue(d, BeaconModel.INDEX_PRISM1, prism1);
                setDmxNormalized(d, BeaconModel.INDEX_PRISM1_ROTATION, prism1rotate);
                setDmxNormalized(d, BeaconModel.INDEX_PRISM2_ROTATION, prism2rotate);
                setDmxNormalized(d, BeaconModel.INDEX_FOCUS, focus);
                setDmxValue(d, BeaconModel.INDEX_SHUTTER, shutter);
                setDmxNormalized(d, BeaconModel.INDEX_DIMMER, dimmer);
                setDmxNormalized(d, BeaconModel.INDEX_FROST1, frost1);
                setDmxNormalized(d, BeaconModel.INDEX_FROST2, frost2);
                setDmxValue(d, BeaconModel.INDEX_PT_SPEED, ptSpd);
                // CONTROL_NORMAL is a default value so isn't required to be set by pattern
                setDmxValue(d, BeaconModel.INDEX_CONTROL, control);

            } else if (d instanceof ChauvetBeamQ60Model) {
                setDmxNormalized(d, BeaconModel.INDEX_PAN, invertPan ? pan : (1 - pan));
                setDmxNormalized(d, ChauvetBeamQ60Model.INDEX_TILT, tilt);
                setDmxValue(d, ChauvetBeamQ60Model.INDEX_PT_SPEED, ptSpd);
                setDmxValue(d, ChauvetBeamQ60Model.INDEX_SHUTTER, shutterValue);
                setDmxNormalized(d, ChauvetBeamQ60Model.INDEX_DIMMER, dimmer);
                int color =  getColor(TEColorType.PRIMARY);
                setDmxNormalized(d, ChauvetBeamQ60Model.INDEX_RED, redNormalized(color));
                setDmxNormalized(d, ChauvetBeamQ60Model.INDEX_GREEN, greenNormalized(color));
                setDmxNormalized(d, ChauvetBeamQ60Model.INDEX_BLUE, blueNormalized(color));
            }
            invertPan = !invertPan;
        }
    }

    private int getColor(TEColorType colorType) {
      return this.lx.engine.palette.getSwatchColor(colorType.swatchIndex()).getColor();
    }

    private float redNormalized(int color) {
      return ((float)Byte.toUnsignedInt(LXColor.red(color))) / 255f;
    }

    private float greenNormalized(int color) {
      return ((float)Byte.toUnsignedInt(LXColor.green(color))) / 255f;
    }

    private float blueNormalized(int color) {
      return ((float)Byte.toUnsignedInt(LXColor.blue(color))) / 255f;
    }

    @Override
    public void dispose() {
        this.gamepad.removeListener(this);
        this.gamepad.dispose();
        super.dispose();
    }

    @Override
    public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, BeaconGamePattern device) {
        UIUtils.buildMftStyleDeviceControls(ui, uiDevice, device);
    }
}
