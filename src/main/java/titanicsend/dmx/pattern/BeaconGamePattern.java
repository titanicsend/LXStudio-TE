package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.TEApp;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.dmx.model.BeaconModel;
import titanicsend.dmx.model.ChauvetBeamQ60Model;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.parameter.DmxCompoundParameter;
import titanicsend.gamepad.GamepadEngine;
import titanicsend.ui.UIUtils;

@LXCategory("DMX")
public class BeaconGamePattern extends BeaconPattern
        implements UIDeviceControls<BeaconGamePattern>, GamepadEngine.Gamepad.GamepadListener {

    public final DiscreteParameter input = new DiscreteParameter("Input", 16)
            .setDescription("Gamepad input number 1-16");

    private final GamepadEngine.Gamepad gamepad;

    public BeaconGamePattern(LX lx) {
        super(lx);

        addParameter("Gamepad", this.input);

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
      if (p == this.gamepad.axisLeftX) {
        gamepadAxisToBeaconParameter(this.pan, this.gamepad.axisLeftX);
      } else if (p == this.gamepad.axisRightX) {
        gamepadAxisToBeaconParameter(this.tilt, this.gamepad.axisRightY);
      } else if (p == this.gamepad.dpUp) {
        if (this.ptSpeed.getDmxValue() < (225-ptSpeedTick)) {
          this.ptSpeed.increment(ptSpeedTick);
        }
//        this.ptSpeed.setNormalized(this.ptSpeed.getNormalized() + .05);
      } else if (p == this.gamepad.dpDown) {
          if (this.ptSpeed.getDmxValue() > ptSpeedTick) {
              this.ptSpeed.decrement(ptSpeedTick);
          }
//        this.ptSpeed.setNormalized(this.ptSpeed.getNormalized() - .05);
      }
      // TODO: map other buttons to beacon parameters on playa!
    }

    private void gamepadAxisToBeaconParameter(DmxCompoundParameter target, LXNormalizedParameter source) {
      target.setValue(target.range.normalizedToValue(source.getNormalized(), 3, BoundedParameter.NormalizationCurve.BIAS_CENTER));
    }

    private int shutterValue = 32;

    @Override
    public void run(double deltaMs) {
        float rightTrigger = this.gamepad.axisRightTrigger.getNormalizedf();
        if (rightTrigger < .1) {
            shutterValue = 32;
        } else {
            shutterValue = (int) ((rightTrigger - .1) * (95-75) / (1 - .1) + 75);
        }

        // Reminder: Don't use Normalized for DmxDiscreteParameters,
        // they likely do not scale linearly to 0-255.
        double pan = this.pan.getNormalized();
        double tilt = this.tilt.getNormalized();
        double cyan = this.cyan.getNormalized();
        double magenta = this.magenta.getNormalized();
        double yellow = this.yellow.getNormalized();
        int colorWheel = this.colorWheel.getDmxValue();
        int gobo1 = this.gobo1.getDmxValue();
        double gobo1rotate = this.gobo1rotation.getNormalized();
        int gobo2 = this.gobo2.getDmxValue();
        int prism1 = this.prism1.getDmxValue();
        double prism1rotate = this.prism1rotation.getNormalized();
        double prism2rotate = this.prism2rotation.getNormalized();
        double focus = this.focus.getNormalized();
        int shutter = this.shutter.getDmxValue();
        double dimmer = this.dimmer.getNormalized();
        double frost1 = this.frost1.getNormalized();
        double frost2 = this.frost2.getNormalized();
        int ptSpd = this.ptSpeed.getDmxValue();
        int control = this.control.getDmxValue();

        for (DmxModel d : this.modelTE.getBeacons()) {
            if (d instanceof BeaconModel) {
                setDmxNormalized(d, BeaconModel.INDEX_PAN, pan);
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
                setDmxNormalized(d, ChauvetBeamQ60Model.INDEX_PAN, pan);
                setDmxNormalized(d, ChauvetBeamQ60Model.INDEX_TILT, tilt);
                setDmxValue(d, ChauvetBeamQ60Model.INDEX_PT_SPEED, ptSpd);
                setDmxValue(d, ChauvetBeamQ60Model.INDEX_SHUTTER, shutterValue);
                setDmxNormalized(d, ChauvetBeamQ60Model.INDEX_DIMMER, 1);
                setDmxNormalized(d, ChauvetBeamQ60Model.INDEX_RED, 100);
            }
        }
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
