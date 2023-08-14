package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.studio.TEApp;
import titanicsend.dmx.model.AdjStealthModel;
import titanicsend.dmx.parameter.DmxCompoundParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameterOption;
import titanicsend.model.TEWholeModel;

abstract public class DjLightsPattern extends DmxPattern {

  protected final TEWholeModel modelTE;

  // Don't mind this code duplication with the model class.
  // It's just a coincidence.

  DmxCompoundParameter pan = new DmxCompoundParameter("Pan")
    .setNumBytes(2);
  DmxCompoundParameter tilt = new DmxCompoundParameter("Tilt", 0, -180, 180)
    .setNumBytes(2);
  DmxCompoundParameter red = new DmxCompoundParameter("Red", 0, 0, 100);
  DmxCompoundParameter green = new DmxCompoundParameter("Green", 0, 0, 100);
  DmxCompoundParameter blue = new DmxCompoundParameter("Blue", 0, 0, 100);
  DmxCompoundParameter white = new DmxCompoundParameter("White", 0, 0, 100);
  DmxCompoundParameter dimmer = new DmxCompoundParameter("Dimmer", 0, 0, 100)
    .setScaleToAlpha(true);   // Faders applied to this parameter
  DmxDiscreteParameter shutter = new DmxDiscreteParameter("Shutter",
    new DmxDiscreteParameterOption[] 
      { new DmxDiscreteParameterOption("Closed", AdjStealthModel.SHUTTER_CLOSED),
        new DmxDiscreteParameterOption("Strobe slow-fast", 10, 245),
        new DmxDiscreteParameterOption("Open", AdjStealthModel.SHUTTER_OPEN)
      });
  DmxCompoundParameter focus = new DmxCompoundParameter("Focus");
  DmxCompoundParameter colorTemp = new DmxCompoundParameter("ClrTemperature");
  DmxDiscreteParameter colorEffect = new DmxDiscreteParameter("ClrEffect",
    new DmxDiscreteParameterOption[] 
      { new DmxDiscreteParameterOption("Nothing", 0),
        new DmxDiscreteParameterOption("Red", 11),
        new DmxDiscreteParameterOption("Green", 21),
        new DmxDiscreteParameterOption("Blue", 31),
        new DmxDiscreteParameterOption("White", 41),
        new DmxDiscreteParameterOption("Red+Green", 51),
        new DmxDiscreteParameterOption("Green+Blue", 61),
        new DmxDiscreteParameterOption("Blue+White", 71),
        new DmxDiscreteParameterOption("Red+Blue", 81),
        new DmxDiscreteParameterOption("Green+White", 91),
        new DmxDiscreteParameterOption("Red+White", 101),
        new DmxDiscreteParameterOption("Red+Green+Blue", 111),
        new DmxDiscreteParameterOption("Red+Green+White", 121),
        new DmxDiscreteParameterOption("Green+Blue+White", 131),
        new DmxDiscreteParameterOption("RGBW", 141),
        new DmxDiscreteParameterOption("Change slow-fast", 151, 255)
      });
  DmxDiscreteParameter colorFade = new DmxDiscreteParameter("ClrFade",
    new DmxDiscreteParameterOption[] 
      { new DmxDiscreteParameterOption("Nothing", 0),
        new DmxDiscreteParameterOption("Fade slow-fast", 11, 194)
      });
  DmxCompoundParameter ptSpeed = new DmxCompoundParameter("ptSpd");
  DmxDiscreteParameter programs = new DmxDiscreteParameter("Programs",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("Nothing", 0),
          new DmxDiscreteParameterOption("Program 1", 21),
          new DmxDiscreteParameterOption("Program 2", 41),
          new DmxDiscreteParameterOption("Program 3", 61),
          new DmxDiscreteParameterOption("Program 4", 81),
          new DmxDiscreteParameterOption("Program 5", 101),
          new DmxDiscreteParameterOption("Program 6", 121),
          new DmxDiscreteParameterOption("Program 7", 141),
          new DmxDiscreteParameterOption("Program 8", 161),
          new DmxDiscreteParameterOption("Nothing", 181),
          new DmxDiscreteParameterOption("Motor Reset", 201),
          new DmxDiscreteParameterOption("Nothing", 226)
        });

  public DjLightsPattern(LX lx) {
    super(lx);
    this.modelTE = TEApp.wholeModel;
  }

}
