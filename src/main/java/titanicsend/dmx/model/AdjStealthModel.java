package titanicsend.dmx.model;

import titanicsend.dmx.DmxBuffer;
import titanicsend.dmx.parameter.DmxCompoundParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameterOption;

/**
 * ADJ Stealth Wash Zoom
 * 
 * Device must be set to 16-channel DMX mode
 */
public class AdjStealthModel extends DmxModel {

  static public final String MODEL_TYPE = "DJLight";

  /* 0-based index of DMX parameters within DmxBuffer
   * DO NOT skip numbers for multi-byte.
   * This is NOT the same as DMX channel.
   *
   * 16-CHANNEL MODE for ADJ Stealth Wash Zoom
   */
  static public final int INDEX_PAN = 0;
  static public final int INDEX_TILT = 1;
  static public final int INDEX_RED = 2;
  static public final int INDEX_GREEN = 3;
  static public final int INDEX_BLUE = 4;
  static public final int INDEX_WHITE = 5;
  static public final int INDEX_DIMMER = 6;
  static public final int INDEX_SHUTTER = 7;
  static public final int INDEX_FOCUS = 8;
  static public final int INDEX_COLOR_TEMP = 9;
  static public final int INDEX_COLOR_EFFECT = 10;
  static public final int INDEX_COLOR_FADE = 11;
  static public final int INDEX_PT_SPEED = 12;
  static public final int INDEX_PROGRAMS = 13;

  static public final int SHUTTER_CLOSED = 0;
  static public final int SHUTTER_OPEN = 246;

  public AdjStealthModel(DmxCommonConfig config, String ... tags) {
    super(MODEL_TYPE, config, tags);

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
        { new DmxDiscreteParameterOption("Closed", SHUTTER_CLOSED),
          new DmxDiscreteParameterOption("Strobe slow-fast", 10, 245),
          new DmxDiscreteParameterOption("Open", SHUTTER_OPEN)
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

    addField(new FieldDefinition(pan));
    addField(new FieldDefinition(tilt));   
    addField(new FieldDefinition(red));
    addField(new FieldDefinition(green));
    addField(new FieldDefinition(blue));
    addField(new FieldDefinition(white));
    addField(new FieldDefinition(dimmer));
    addField(new FieldDefinition(shutter));
    addField(new FieldDefinition(focus));
    addField(new FieldDefinition(colorTemp));
    addField(new FieldDefinition(colorEffect));
    addField(new FieldDefinition(colorFade));
    addField(new FieldDefinition(ptSpeed));
    addField(new FieldDefinition(programs));
  }

  @Override
  public void validate(DmxBuffer buffer) {
    // Do any cross-field safety checking here.
  }

}
