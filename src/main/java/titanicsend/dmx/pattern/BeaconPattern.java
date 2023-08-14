/**
 * Copyright 2023- Justin Belcher, Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.studio.TEApp;
import titanicsend.dmx.model.BeaconModel;
import titanicsend.dmx.parameter.DmxCompoundParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameter;
import titanicsend.dmx.parameter.DmxDiscreteParameterOption;
import titanicsend.model.TEWholeModel;

abstract public class BeaconPattern extends DmxPattern {

  protected final TEWholeModel modelTE;

  // Don't mind this code duplication with the model class.
  // It's just a coincidence.

  DmxCompoundParameter pan = new DmxCompoundParameter("Pan")
    .setNumBytes(2);
  DmxCompoundParameter tilt = new DmxCompoundParameter("Tilt", BeaconModel.TILT_MIN, BeaconModel.TILT_MIN, BeaconModel.TILT_MAX)
    .setNumBytes(2);
  DmxCompoundParameter cyan = new DmxCompoundParameter("Cyan", 0, 0, 100);
  DmxCompoundParameter magenta = new DmxCompoundParameter("Magenta", 0, 0, 100);
  DmxCompoundParameter yellow = new DmxCompoundParameter("Yellow", 0, 0, 100);
  DmxDiscreteParameter colorWheel = new DmxDiscreteParameter("ClrWheel",
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
        new DmxDiscreteParameterOption("Medium Blue", 121),
        new DmxDiscreteParameterOption("Scroll CW fast-slow", 128, 180),
        new DmxDiscreteParameterOption("Scroll idle", 181),
        new DmxDiscreteParameterOption("Scroll CCW slow-fast", 182, 234),
        new DmxDiscreteParameterOption("Random Fast", 235),
        new DmxDiscreteParameterOption("Random Medium", 240),
        new DmxDiscreteParameterOption("Random Slow", 245),
        new DmxDiscreteParameterOption("Open", 250)
      });
  // "Rotating Gobo"
  DmxDiscreteParameter gobo1 = new DmxDiscreteParameter("Gobo1",
    new DmxDiscreteParameterOption[] 
      { new DmxDiscreteParameterOption("Open", 0),
        new DmxDiscreteParameterOption("Spot Open", 11),
        new DmxDiscreteParameterOption("Gobo 1", 22),
        new DmxDiscreteParameterOption("Gobo 2", 32),
        new DmxDiscreteParameterOption("Gobo 3", 42),
        new DmxDiscreteParameterOption("Gobo 4", 52),
        new DmxDiscreteParameterOption("Gobo 5", 62),
        new DmxDiscreteParameterOption("Gobo 6", 72),
        new DmxDiscreteParameterOption("Gobo 7", 82),
        new DmxDiscreteParameterOption("Gobo 8", 92),
        new DmxDiscreteParameterOption("Gobo 1 Shake", 102),
        new DmxDiscreteParameterOption("Gobo 2 Shake", 113),
        new DmxDiscreteParameterOption("Gobo 3 Shake", 124),
        new DmxDiscreteParameterOption("Gobo 4 Shake", 135),
        new DmxDiscreteParameterOption("Gobo 5 Shake", 146),
        new DmxDiscreteParameterOption("Gobo 6 Shake", 157),
        new DmxDiscreteParameterOption("Gobo 7 Shake", 168),
        new DmxDiscreteParameterOption("Gobo 8 Shake", 179),
        new DmxDiscreteParameterOption("Scroll CW fast-slow", 190, 221),
        new DmxDiscreteParameterOption("Idle", 222),
        new DmxDiscreteParameterOption("Scroll CCW slow-fast", 224, 255)
      });
  // Could use parameter options if they worked with Compound type
  DmxCompoundParameter gobo1rotation = new DmxCompoundParameter("G1 Rotation", 0, 0, 255)
      .setNumBytes(2);
  // "Fixed Gobo"
  DmxDiscreteParameter gobo2 = new DmxDiscreteParameter("Gobo2",
    new DmxDiscreteParameterOption[] 
      { new DmxDiscreteParameterOption("Open", 0),
        new DmxDiscreteParameterOption("Gobo 1", 5),
        new DmxDiscreteParameterOption("Gobo 2", 10),
        new DmxDiscreteParameterOption("Gobo 3", 15),
        new DmxDiscreteParameterOption("Gobo 4", 20),
        new DmxDiscreteParameterOption("Gobo 5", 25),
        new DmxDiscreteParameterOption("Gobo 6", 30),
        new DmxDiscreteParameterOption("Gobo 7", 35),
        new DmxDiscreteParameterOption("Gobo 8", 40),
        new DmxDiscreteParameterOption("Gobo 9", 45),
        new DmxDiscreteParameterOption("Gobo 10", 50),
        new DmxDiscreteParameterOption("Gobo 11", 55),
        new DmxDiscreteParameterOption("Gobo 12", 60),
        new DmxDiscreteParameterOption("Gobo 13", 65),
        new DmxDiscreteParameterOption("Gobo 14", 70),
        new DmxDiscreteParameterOption("Gobo 15", 75),
        new DmxDiscreteParameterOption("Gobo 16", 80),
        new DmxDiscreteParameterOption("Gobo 17", 85),
        new DmxDiscreteParameterOption("Gobo 1 shake slow-fast", 88),
        new DmxDiscreteParameterOption("Gobo 2 shake slow-fast", 94),
        new DmxDiscreteParameterOption("Gobo 3 shake slow-fast", 100),
        new DmxDiscreteParameterOption("Gobo 4 shake slow-fast", 106),
        new DmxDiscreteParameterOption("Gobo 5 shake slow-fast", 112),
        new DmxDiscreteParameterOption("Gobo 6 shake slow-fast", 118),
        new DmxDiscreteParameterOption("Gobo 7 shake slow-fast", 124),
        new DmxDiscreteParameterOption("Gobo 8 shake slow-fast", 130),
        new DmxDiscreteParameterOption("Gobo 9 shake slow-fast", 136),
        new DmxDiscreteParameterOption("Gobo 10 shake slow-fast", 142),
        new DmxDiscreteParameterOption("Gobo 11 shake slow-fast", 148),
        new DmxDiscreteParameterOption("Gobo 12 shake slow-fast", 154),
        new DmxDiscreteParameterOption("Gobo 13 shake slow-fast", 160),
        new DmxDiscreteParameterOption("Gobo 14 shake slow-fast", 166),
        new DmxDiscreteParameterOption("Gobo 15 shake slow-fast", 172),
        new DmxDiscreteParameterOption("Gobo 16 shake slow-fast", 178),
        new DmxDiscreteParameterOption("Gobo 17 shake slow-fast", 184),
        new DmxDiscreteParameterOption("Scroll CW fast-slow", 190, 221),
        new DmxDiscreteParameterOption("Idle", 222),
        new DmxDiscreteParameterOption("Scroll CCW slow-fast", 224, 255)
      });
  DmxDiscreteParameter prism1 = new DmxDiscreteParameter("Prism1",
    new DmxDiscreteParameterOption[] 
      { new DmxDiscreteParameterOption("Open", 0),
        new DmxDiscreteParameterOption("Beam Expander", 51),
        new DmxDiscreteParameterOption("8 Facet", 101),
        new DmxDiscreteParameterOption("4 Facet Linear", 151),
        new DmxDiscreteParameterOption("8 + 4", 201)
      });
  DmxCompoundParameter prism1rotation = new DmxCompoundParameter("P1 Rotation",
    new DmxDiscreteParameterOption[]     
      { new DmxDiscreteParameterOption("Indexing", 0, 127),
        new DmxDiscreteParameterOption("CW rotate fast-slow", 128, 189),
        new DmxDiscreteParameterOption("No rotate", 190, 193),
        new DmxDiscreteParameterOption("CCW rotate slow-fast", 194, 255),
      })
    .setNumBytes(2);
  DmxCompoundParameter prism2rotation = new DmxCompoundParameter("P2 Rotation",
    new DmxDiscreteParameterOption[]     
      { new DmxDiscreteParameterOption("Indexing", 0, 127),
        new DmxDiscreteParameterOption("CW rotate fast-slow", 128, 189),
        new DmxDiscreteParameterOption("No rotate", 190, 193),
        new DmxDiscreteParameterOption("CCW rotate slow-fast", 194, 255),
      })
    .setNumBytes(2);
  DmxCompoundParameter focus = new DmxCompoundParameter("Focus", 0, 0, 255)
    .setNumBytes(2);
  DmxDiscreteParameter shutter = new DmxDiscreteParameter("Shutter",
    new DmxDiscreteParameterOption[] 
      { new DmxDiscreteParameterOption("Closed", 0),
        new DmxDiscreteParameterOption("Open", 32),
        new DmxDiscreteParameterOption("Strobe slow-fast", 64, 95),
        new DmxDiscreteParameterOption("Open", 96, 127),
        new DmxDiscreteParameterOption("Pulse in sequences", 128, 159),  // is range?
        new DmxDiscreteParameterOption("Open", 160),
        new DmxDiscreteParameterOption("Random slow-fast", 192),
        new DmxDiscreteParameterOption("Open", 224)
      });
  // It would be fine for a pattern to set dimmer to max and let the faders do the rest
  DmxCompoundParameter dimmer = new DmxCompoundParameter("Dimmer", 0, 0, 100)
      .setNumBytes(2)
      .setScaleToAlpha(true);   // Faders applied to this parameter
  DmxCompoundParameter frost1 = new DmxCompoundParameter("Frost1", 0, 0, 100);
  DmxCompoundParameter frost2 = new DmxCompoundParameter("Frost2", 0, 0, 100);
  DmxDiscreteParameter ptSpeed = (DmxDiscreteParameter)
    new DmxDiscreteParameter("ptSpd",
    new DmxDiscreteParameterOption[]     
        { new DmxDiscreteParameterOption("Fast-Slow", 0, 225),
          new DmxDiscreteParameterOption("Blackout by movement", 226),
          new DmxDiscreteParameterOption("Blackout by all wheel change", 236)
          })
      .setDescription("Pan/Tilt Speed");
  DmxDiscreteParameter control = new DmxDiscreteParameter("Control",
    new DmxDiscreteParameterOption[] 
      { new DmxDiscreteParameterOption("Normal", 0),
        new DmxDiscreteParameterOption("Idle", 20),
        new DmxDiscreteParameterOption("Lamp on", 40),
        new DmxDiscreteParameterOption("Lamp off", 50),
        new DmxDiscreteParameterOption("Lamp power 370W", 60),
        new DmxDiscreteParameterOption("Lamp power 430W", 67),
        new DmxDiscreteParameterOption("Lamp power 560W", 74),
        new DmxDiscreteParameterOption("All motor reset", 80),
        new DmxDiscreteParameterOption("Pan Tilt motor reset", 85),
        new DmxDiscreteParameterOption("Color motor reset", 88),
        new DmxDiscreteParameterOption("Gobo motor reset", 91),
        new DmxDiscreteParameterOption("Shutter & Dimmer motor reset", 94),
        new DmxDiscreteParameterOption("Other motor reset", 97),
        new DmxDiscreteParameterOption("Idle", 100),
        new DmxDiscreteParameterOption("CMY Normal", 165),
        new DmxDiscreteParameterOption("CMY Fast (default)", 167),
        new DmxDiscreteParameterOption("Vent Cleaning ON", 169),
        new DmxDiscreteParameterOption("Vent Cleaning OFF", 171),
        new DmxDiscreteParameterOption("173 Hibernation OFF", 173),
        new DmxDiscreteParameterOption("175 Hibernation", 175),
        new DmxDiscreteParameterOption("177 Sun Protect ON", 177),
        new DmxDiscreteParameterOption("178 Sun Protect OFF", 178),
        new DmxDiscreteParameterOption("Idle", 179),
        new DmxDiscreteParameterOption("Pan Tilt Smooth (default)", 181),
        new DmxDiscreteParameterOption("Pan Tilt Fast", 191),
        new DmxDiscreteParameterOption("Dimmer Curve Linear (default)", 201),
        new DmxDiscreteParameterOption("Dimmer Curve Square", 211),
        new DmxDiscreteParameterOption("Dimmer Curve Inverse Square", 221),
        new DmxDiscreteParameterOption("Dimmer Curve S-Curve", 231),
        new DmxDiscreteParameterOption("Internal Program 1", 241),
        new DmxDiscreteParameterOption("Internal Program 2", 242),
        new DmxDiscreteParameterOption("Internal Program 3", 243),
        new DmxDiscreteParameterOption("Internal Program 4", 244),
        new DmxDiscreteParameterOption("Internal Program 5", 245),
        new DmxDiscreteParameterOption("Internal Program 6", 246),
        new DmxDiscreteParameterOption("Internal Program 7", 247),
        new DmxDiscreteParameterOption("Idle", 248),
        new DmxDiscreteParameterOption("Display OFF", 250),
        new DmxDiscreteParameterOption("Display ON", 252),
        new DmxDiscreteParameterOption("Idle", 254)
      });

  public BeaconPattern(LX lx) {
    super(lx);
    this.modelTE = TEApp.wholeModel;
  }

}
