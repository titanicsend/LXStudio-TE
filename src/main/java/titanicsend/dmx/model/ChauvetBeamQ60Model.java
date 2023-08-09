package titanicsend.dmx.model;

import titanicsend.dmx.parameter.DmxCompoundParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameterOption;

public class ChauvetBeamQ60Model extends DmxModel {

  static public final String MODEL_TYPE = "ChauvetBeamQ60";

  /* 0-based index of DMX parameters within DmxBuffer
   * DO NOT skip numbers for multi-byte.
   * This is NOT the same as DMX channel.
   *
   * 16-CHANNEL MODE for ChauvetDJ Beam Q60
   */
  static public final int INDEX_PAN = 0;
  static public final int INDEX_TILT = 1;
  static public final int INDEX_PAN_CONTINUOUS = 2;
  static public final int INDEX_TILT_CONTINUOUS = 3;
  static public final int INDEX_PT_SPEED = 4;
  static public final int INDEX_SHUTTER = 5;
  static public final int INDEX_DIMMER = 6;
  static public final int INDEX_VIRTUAL_COLOR_WHEEL_CONTROL = 7;
  static public final int INDEX_VIRTCAL_COLOR_WHEEL = 8;
  static public final int INDEX_RED = 9;
  static public final int INDEX_GREEN = 10;
  static public final int INDEX_BLUE = 11;
  static public final int INDEX_WHITE = 12;
  static public final int INDEX_CONTROL = 13;

  public ChauvetBeamQ60Model(DmxCommonConfig config, String... tags) {
    super(MODEL_TYPE, config, tags);

    DmxCompoundParameter pan = new DmxCompoundParameter("Pan")
       .setNumBytes(2);
    DmxCompoundParameter tilt = new DmxCompoundParameter("Tilt", -180, 180, 0)
      .setNumBytes(2);
    DmxDiscreteParameter panContinuous = new DmxDiscreteParameter("PanContinuous",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("No Function", 0),
          new DmxDiscreteParameterOption("Rotate fast-slow", 4, 127),
          new DmxDiscreteParameterOption("Stop", 128),
          new DmxDiscreteParameterOption("Reverse slow-fast", 132, 255)
        });
    DmxDiscreteParameter tiltContinuous = new DmxDiscreteParameter("TiltContinuous",
        new DmxDiscreteParameterOption[] 
          { new DmxDiscreteParameterOption("No Function", 0),
            new DmxDiscreteParameterOption("Tilt fast-slow", 4, 127),
            new DmxDiscreteParameterOption("Stop", 128),
            new DmxDiscreteParameterOption("Tilt slow-fast", 132, 255)
          });
    DmxCompoundParameter ptSpd = new DmxCompoundParameter("ptSpd");
    DmxDiscreteParameter shutter = 
      new DmxDiscreteParameter("Shutter",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("Closed", 0),
          new DmxDiscreteParameterOption("Open", 32),
          new DmxDiscreteParameterOption("Strobe, slow to fast", 64, 95),
          new DmxDiscreteParameterOption("Open", 96),
          new DmxDiscreteParameterOption("Pulse strobe, slow to fast", 128, 159),
          new DmxDiscreteParameterOption("Open", 160),
          new DmxDiscreteParameterOption("Random strobe, slow to fast", 192, 223),
          new DmxDiscreteParameterOption("Open", 224)
        });
    DmxCompoundParameter dimmer = new DmxCompoundParameter("Dimmer", 0, 0, 100)
      .setScaleToAlpha(true);   // Faders applied to this parameter
    DmxDiscreteParameter virtualColorWheelControl = new DmxDiscreteParameter("VCW Control",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("None", 0),
            new DmxDiscreteParameterOption("CTC", 16),    // Color Temperature Correction
            new DmxDiscreteParameterOption("Rainbow", 32),
            new DmxDiscreteParameterOption("Reverse Rainbow", 48),
            new DmxDiscreteParameterOption("Continuous", 64),
            new DmxDiscreteParameterOption("Color Bounce", 80)
        });
    // VCW varies by preceding VCW-Control parameter
    DmxCompoundParameter virtualColorWheel = new DmxCompoundParameter("VCW");
    DmxCompoundParameter red = new DmxCompoundParameter("Red", 0, 0, 100);
    DmxCompoundParameter green = new DmxCompoundParameter("Green", 0, 0, 100);
    DmxCompoundParameter blue = new DmxCompoundParameter("Blue", 0, 0, 100);
    DmxCompoundParameter white = new DmxCompoundParameter("White", 0, 0, 100);
    DmxDiscreteParameter control = new DmxDiscreteParameter("Control",
        new DmxDiscreteParameterOption[] 
          { new DmxDiscreteParameterOption("None", 0),
              new DmxDiscreteParameterOption("Reset All", 8),
              new DmxDiscreteParameterOption("Blackout while pt ON 5-sec", 16),
              new DmxDiscreteParameterOption("Blackout while pt OFF 5-sec", 24),
              new DmxDiscreteParameterOption("Sound-active pt ON 5-sec", 32),
              new DmxDiscreteParameterOption("Sound-active pt OFF 5-sec", 40),
              new DmxDiscreteParameterOption("Sound-active color ON 5-sec", 48),
              new DmxDiscreteParameterOption("Sound-active color OFF 5-sec", 56),
              new DmxDiscreteParameterOption("Display OFF", 64),
              new DmxDiscreteParameterOption("Display ON", 72),
              new DmxDiscreteParameterOption("Fan speed Auto", 80),
              new DmxDiscreteParameterOption("Fan speed Full", 88),
              new DmxDiscreteParameterOption("Dimmer speed mode linear", 96),
              new DmxDiscreteParameterOption("Dimmer speed mode slow", 104),
              new DmxDiscreteParameterOption("Dimmer speed mode medium", 112),
              new DmxDiscreteParameterOption("Dimmer speed mode fast", 120),
              new DmxDiscreteParameterOption("Blackout on movement", 128),
              new DmxDiscreteParameterOption("Pan forward spin", 136),
              new DmxDiscreteParameterOption("Pan reverse spin", 144),
              new DmxDiscreteParameterOption("Tilt forward spin", 152),
              new DmxDiscreteParameterOption("Tilt reverse spin", 160),
              new DmxDiscreteParameterOption("Pan and Tilt forward spin", 168),
              new DmxDiscreteParameterOption("Pan and Tilt reverse spin", 176),
              new DmxDiscreteParameterOption("Pan forward spin & Tilt reverse spin", 184),
              new DmxDiscreteParameterOption("Pan reverse spin & Tilt forward spin", 192)
          });

    addField(new FieldDefinition(pan));
    addField(new FieldDefinition(tilt));
    addField(new FieldDefinition(panContinuous));
    addField(new FieldDefinition(tiltContinuous));
    addField(new FieldDefinition(ptSpd));
    addField(new FieldDefinition(shutter));
    addField(new FieldDefinition(dimmer));
    addField(new FieldDefinition(virtualColorWheelControl));
    addField(new FieldDefinition(virtualColorWheel));
    addField(new FieldDefinition(red));
    addField(new FieldDefinition(green));
    addField(new FieldDefinition(blue));
    addField(new FieldDefinition(white));
    addField(new FieldDefinition(control));
  }

}
