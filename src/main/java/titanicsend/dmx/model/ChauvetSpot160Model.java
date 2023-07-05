package titanicsend.dmx.model;

import titanicsend.dmx.DmxBuffer;
import titanicsend.dmx.parameter.DmxCompoundParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameterOption;
import titanicsend.dmx.parameter.DmxParameterLimiter.LimitType;

public class ChauvetSpot160Model extends DmxModel {

  static public final String TE_MODEL_TYPE = "ChauvetSpot160";

  // 0-based index of DMX parameters within DmxBuffer
  // DO NOT skip numbers for multi-byte
  static public final int INDEX_PAN = 0;
  static public final int INDEX_TILT = 1;
  static public final int INDEX_PT_SPEED = 2;
  static public final int INDEX_COLOR_WHEEL = 3;
  static public final int INDEX_GOBO = 4;
  static public final int INDEX_DIMMER = 5;
  static public final int INDEX_SHUTTER = 6;
  static public final int INDEX_CONTROL = 7;
  static public final int INDEX_MOVEMENT_MACROS = 8;

  static public final int SHUTTER_CLOSED = 0;
  static public final int SHUTTER_OPEN = 4;
  static public final int SHUTTER_STROBE_MIN = 8;
  static public final int SHUTTER_STROBE_MAX = 76;

  public ChauvetSpot160Model(DmxCommonConfig config, String ... tags) {
    super(TE_MODEL_TYPE, config, tags);

    DmxCompoundParameter pan = new DmxCompoundParameter("Pan")
      .setNumBytes(2);
    DmxCompoundParameter tilt = new DmxCompoundParameter("Tilt", -130, -130, 114)
      .setNumBytes(2);
    // Set safety limit on tilt
    tilt.getLimiter().setLimits(-90, 90).setLimitType(LimitType.ZOOM);

    DmxCompoundParameter ptSpeed = new DmxCompoundParameter("ptSpd");
    DmxDiscreteParameter colorWheel = new DmxDiscreteParameter("ClrWheel",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("Cool White", 0),
          new DmxDiscreteParameterOption("Red", 7),
          new DmxDiscreteParameterOption("Orange", 14),
          new DmxDiscreteParameterOption("Yellow", 21),
          new DmxDiscreteParameterOption("Green", 28),
          new DmxDiscreteParameterOption("Blue", 35),
          new DmxDiscreteParameterOption("CTO", 42),
          new DmxDiscreteParameterOption("Cyan", 49),
          new DmxDiscreteParameterOption("Magenta", 56),
          new DmxDiscreteParameterOption("Lime Green", 63),
          new DmxDiscreteParameterOption("Color Indexing", 65, 189),
          new DmxDiscreteParameterOption("Rainbow cycle, fast to slow", 190, 221),
          new DmxDiscreteParameterOption("Stop", 222),
          new DmxDiscreteParameterOption("Reverse Rainbow cycle, slow to fast", 224, 255)
        });
    DmxDiscreteParameter gobo = new DmxDiscreteParameter("Gobo",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("Open", 0),
          new DmxDiscreteParameterOption("Gobo 1", 6),
          new DmxDiscreteParameterOption("Gobo 2", 12),
          new DmxDiscreteParameterOption("Gobo 3", 18),
          new DmxDiscreteParameterOption("Gobo 4", 24),
          new DmxDiscreteParameterOption("Gobo 5", 30),
          new DmxDiscreteParameterOption("Gobo 6", 36),
          new DmxDiscreteParameterOption("Gobo 7", 42),
          new DmxDiscreteParameterOption("Gobo 8", 48),
          new DmxDiscreteParameterOption("Gobo 9", 54),
          new DmxDiscreteParameterOption("Gobo 9 shake, slow to fast", 64, 69),
          new DmxDiscreteParameterOption("Gobo 8 shake, slow to fast", 70, 75),
          new DmxDiscreteParameterOption("Gobo 7 shake, slow to fast", 76, 81),
          new DmxDiscreteParameterOption("Gobo 6 shake, slow to fast", 82, 87),
          new DmxDiscreteParameterOption("Gobo 5 shake, slow to fast", 88, 93),
          new DmxDiscreteParameterOption("Gobo 4 shake, slow to fast", 94, 99),
          new DmxDiscreteParameterOption("Gobo 3 shake, slow to fast", 100, 105),
          new DmxDiscreteParameterOption("Gobo 2 shake, slow to fast", 106, 111),
          new DmxDiscreteParameterOption("Gobo 1 shake, slow to fast", 112, 117),
          new DmxDiscreteParameterOption("Open", 118),
          new DmxDiscreteParameterOption("Cycle, slow to fast", 128, 189),
          new DmxDiscreteParameterOption("Stop", 190),
          new DmxDiscreteParameterOption("Reverse Cycle, slow to fast", 194, 255)
        });
    DmxCompoundParameter dimmer = new DmxCompoundParameter("Dimmer", 0, 0, 100)
      .setScaleToAlpha(true);   // Faders applied to this parameter
    DmxDiscreteParameter shutter = new DmxDiscreteParameter("Shutter",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("Closed", 0),
          new DmxDiscreteParameterOption("Open", 4),
          new DmxDiscreteParameterOption("Strobe, slow to fast", 8, 76),
          new DmxDiscreteParameterOption("Pulse strobe, slow to fast", 77, 145),
          new DmxDiscreteParameterOption("Random strobe, slow to fast", 146, 215),
          new DmxDiscreteParameterOption("Open", 216)
        });
    DmxDiscreteParameter control = new DmxDiscreteParameter("Control",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("No Function", 0),
          new DmxDiscreteParameterOption("Blackout on pan/tilt movement", 8),
          new DmxDiscreteParameterOption("Blackout on color wheel movement", 16),
          new DmxDiscreteParameterOption("Blackout on gobo wheel movement", 24),
          new DmxDiscreteParameterOption("Blackout on pan/tilt/color wheel movement", 32),
          new DmxDiscreteParameterOption("Blackout on pan/tilt/gobo wheel movement", 40),
          new DmxDiscreteParameterOption("Blackout on pan/tilt/color/gobo wheel movement", 48),
          new DmxDiscreteParameterOption("No Function", 56),
          new DmxDiscreteParameterOption("Pan reset", 96),
          new DmxDiscreteParameterOption("Tilt reset", 104),
          new DmxDiscreteParameterOption("Color wheel reset", 112),
          new DmxDiscreteParameterOption("Gobo wheel reset", 120),
          new DmxDiscreteParameterOption("No function", 128),
          new DmxDiscreteParameterOption("Reset all", 152),
          new DmxDiscreteParameterOption("No function", 160)
        });
    DmxDiscreteParameter movementMacros = new DmxDiscreteParameter("Movement",
      new DmxDiscreteParameterOption[] 
        { new DmxDiscreteParameterOption("No Function", 0),
          new DmxDiscreteParameterOption("Movement macro 1", 8),
          new DmxDiscreteParameterOption("Movement macro 2", 24),
          new DmxDiscreteParameterOption("Movement macro 3", 40),
          new DmxDiscreteParameterOption("Movement macro 4", 56),
          new DmxDiscreteParameterOption("Movement macro 5", 72),
          new DmxDiscreteParameterOption("Movement macro 6", 88),
          new DmxDiscreteParameterOption("Movement macro 7", 104),
          new DmxDiscreteParameterOption("Movement macro 8", 120),
          new DmxDiscreteParameterOption("Sound-active movement macro 1", 136),
          new DmxDiscreteParameterOption("Sound-active movement macro 2", 152),
          new DmxDiscreteParameterOption("Sound-active movement macro 3", 168),
          new DmxDiscreteParameterOption("Sound-active movement macro 4", 184),
          new DmxDiscreteParameterOption("Sound-active movement macro 5", 200),
          new DmxDiscreteParameterOption("Sound-active movement macro 6", 216),
          new DmxDiscreteParameterOption("Sound-active movement macro 7", 232),
          new DmxDiscreteParameterOption("Sound-active movement macro 8", 248)
        });

    addField(new FieldDefinition(pan));
    addField(new FieldDefinition(tilt));
    addField(new FieldDefinition(ptSpeed));
    addField(new FieldDefinition(colorWheel));
    addField(new FieldDefinition(gobo));
    addField(new FieldDefinition(dimmer));
    addField(new FieldDefinition(shutter));
    addField(new FieldDefinition(control));
    addField(new FieldDefinition(movementMacros));
  }

  @Override
  public void validate(DmxBuffer buffer) {
    // Do any cross-field safety checking here.
  }

}
