package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter.Mode;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.dmx.model.BeaconModel;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.parameter.DmxDiscreteParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameterOption;
import titanicsend.ui.UIUtils;

/**
 * A basic beacon pattern with access to all the standard controls,
 * with the extra DMX fluff removed.
 * 
 * Always sets shutter to Open.
 */
@LXCategory("DMX")
public class BeaconEverythingPattern extends BeaconPattern implements UIDeviceControls<BeaconEverythingPattern>{

  // Color Wheel (without the scroll options)
  DmxDiscreteParameter colorWheelFixed = (DmxDiscreteParameter)
      new DmxDiscreteParameter("ClrWheel",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("Open", 0),
          new DmxDiscreteParameterOption("Red", 16),
          new DmxDiscreteParameterOption("Green", 23),
          new DmxDiscreteParameterOption("Yellow", 30),
          new DmxDiscreteParameterOption("Purple", 37),
          new DmxDiscreteParameterOption("Light Green", 44),
          new DmxDiscreteParameterOption("Orange", 51),
          new DmxDiscreteParameterOption("Magenta", 58),
          new DmxDiscreteParameterOption("Cyan", 65),
          new DmxDiscreteParameterOption("Open", 72),
          new DmxDiscreteParameterOption("Deep Red", 79),
          new DmxDiscreteParameterOption("Dark Amber", 86),
          new DmxDiscreteParameterOption("Pink", 93),
          new DmxDiscreteParameterOption("UV", 100),
          new DmxDiscreteParameterOption("CTB", 107),
          new DmxDiscreteParameterOption("CTO", 114),
          new DmxDiscreteParameterOption("Medium Blue", 121)
        })
      .setWrappable(true)
      .setDescription("Color Wheel");
  // Gobo1 (without the scroll options)
  DmxDiscreteParameter gobo1Fixed = (DmxDiscreteParameter)
      new DmxDiscreteParameter("Gobo1",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("Open", 0),
          new DmxDiscreteParameterOption("Spot Open", 11),
          new DmxDiscreteParameterOption("Gobo1.1", 22),
          new DmxDiscreteParameterOption("Gobo1.2", 32),
          new DmxDiscreteParameterOption("Gobo1.3", 42),
          new DmxDiscreteParameterOption("Gobo1.4", 52),
          new DmxDiscreteParameterOption("Gobo1.5", 62),
          new DmxDiscreteParameterOption("Gobo1.6", 72),
          new DmxDiscreteParameterOption("Gobo1.7", 82),
          new DmxDiscreteParameterOption("Gobo1.8", 92)
        });
  // Gobo2 (without the scroll options)
  DmxDiscreteParameter gobo2Fixed = (DmxDiscreteParameter)
      new DmxDiscreteParameter("Gobo2",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("Open", 0),
          new DmxDiscreteParameterOption("Gobo2.1", 5),
          new DmxDiscreteParameterOption("Gobo2.2", 10),
          new DmxDiscreteParameterOption("Gobo2.3", 15),
          new DmxDiscreteParameterOption("Gobo2.4", 20),
          new DmxDiscreteParameterOption("Gobo2.5", 25),
          new DmxDiscreteParameterOption("Gobo2.6", 30),
          new DmxDiscreteParameterOption("Gobo2.7", 35),
          new DmxDiscreteParameterOption("Gobo2.8", 40),
          new DmxDiscreteParameterOption("Gobo2.9", 45),
          new DmxDiscreteParameterOption("Gobo2.10", 50),
          new DmxDiscreteParameterOption("Gobo2.11", 55),
          new DmxDiscreteParameterOption("Gobo2.12", 60),
          new DmxDiscreteParameterOption("Gobo2.13", 65),
          new DmxDiscreteParameterOption("Gobo2.14", 70),
          new DmxDiscreteParameterOption("Gobo2.15", 75),
          new DmxDiscreteParameterOption("Gobo2.16", 80),
          new DmxDiscreteParameterOption("Gobo2.17", 85)
        });

  CompoundParameter ptSpdLinear = new CompoundParameter("ptSpd", 0, 225, 0)
      .setDescription("Pan/Tilt Speed, slow->fast");

  CompoundParameter strobeSpeed = new CompoundParameter("StrbSpd", 64, 64, 95)
      .setDescription("Strobe speed, slow->fast");

  BooleanParameter isStrobe = new BooleanParameter("Strobe!", false)
      .setMode(Mode.MOMENTARY);

  public BeaconEverythingPattern(LX lx) {
    super(lx);

    addParameter("pan", this.pan);
    addParameter("tilt", this.tilt);
    addParameter("ptSpdLinear", this.ptSpdLinear);
    addParameter("clrWheelFixed", this.colorWheelFixed);

    addParameter("dimmer", this.dimmer);
    addParameter("focus", this.focus);
    addParameter("frost1", this.frost1);
    addParameter("frost2", this.frost2);

    addParameter("gobo1Fixed", this.gobo1Fixed);
    addParameter("gobo2Fixed", this.gobo2Fixed);
    addParameter("prism1", this.prism1);
    addParameter("prism2Rotate", this.prism2rotation);

    addParameter("gobo1Rotate", this.gobo1rotation);
    addParameter("prism1Rotate", this.prism1rotation);
    addParameter("strobeSpeed", this.strobeSpeed);
    addParameter("isStrobe", this.isStrobe);
  }

  @Override
  protected void run(double deltaMs) {
    // Reminder: Don't use Normalized for DmxDiscreteParameters,
    // they likely do not scale linearly to 0-255.
    double pan = this.pan.getNormalized();
    double tilt = this.tilt.getNormalized();
    int ptSpd = (int)this.ptSpdLinear.getValue();
    int colorWheel = this.colorWheelFixed.getDmxValue();
    int gobo1 = this.gobo1Fixed.getDmxValue();
    double gobo1rotate = this.gobo1rotation.getNormalized();
    int gobo2 = this.gobo2Fixed.getDmxValue();
    int prism1 = this.prism1.getDmxValue();
    double prism1rotate = this.prism1rotation.getNormalized();
    double prism2rotate = this.prism2rotation.getNormalized();
    double focus = this.focus.getNormalized();
    int shutter = this.isStrobe.isOn() ? (int)this.strobeSpeed.getValue() : BeaconModel.SHUTTER_OPEN;
    double dimmer = this.dimmer.getNormalized();
    double frost1 = this.frost1.getNormalized();
    double frost2 = this.frost2.getNormalized();

    for (DmxModel d : this.modelTE.beacons) {      
      setDmxNormalized(d, BeaconModel.INDEX_PAN, pan);
      setDmxNormalized(d, BeaconModel.INDEX_TILT, tilt);
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
      setDmxValue(d, BeaconModel.INDEX_CONTROL, BeaconModel.CONTROL_NORMAL);
    }
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, BeaconEverythingPattern device) {
    UIUtils.buildMftStyleDeviceControls(ui, uiDevice, device);
  }

}
