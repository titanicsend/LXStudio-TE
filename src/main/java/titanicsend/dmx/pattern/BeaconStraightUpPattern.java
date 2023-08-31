package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.dmx.model.BeaconModel;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.parameter.DmxDiscreteParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameterOption;
import titanicsend.ui.UIUtils;

@LXCategory("DMX")
public class BeaconStraightUpPattern extends BeaconPattern implements UIDeviceControls<BeaconStraightUpPattern>{

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

  public BeaconStraightUpPattern(LX lx) {
    super(lx);

    addParameter("clrWheelFixed", this.colorWheelFixed);
    addParameter("focus", this.focus);
  }

  @Override
  protected void run(double deltaMs) {

    int colorWheel = this.colorWheelFixed.getDmxValue();
    double focus = this.focus.getNormalized();

    for (DmxModel d : this.modelTE.beacons) {      
      setDmxNormalized(d, BeaconModel.INDEX_TILT, 0.5);
      setDmxValue(d, BeaconModel.INDEX_COLOR_WHEEL, colorWheel);
      setDmxNormalized(d, BeaconModel.INDEX_FOCUS, focus);
      setDmxValue(d, BeaconModel.INDEX_SHUTTER, BeaconModel.SHUTTER_OPEN);
      setDmxNormalized(d, BeaconModel.INDEX_DIMMER, BeaconModel.DIMMER_NORMALIZED_100);
    }
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, BeaconStraightUpPattern device) {
    UIUtils.buildMftStyleDeviceControls(ui, uiDevice, device);
  }
}
