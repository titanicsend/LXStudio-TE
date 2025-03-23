package titanicsend.midi;

import titanicsend.lx.DirectorAPCminiMk2;

public class MidiNames {

  // We use both Bomebox Direct MIDI and Bomebox Virtual MIDI Ports

  // Prefix for Bomebox Direct MIDI Ports
  public static final String BOMEBOX = "FoH: ";

  // APC40Mk2
  public static final String APC40MK2 = heronarts.lx.midi.surface.APC40Mk2.DEVICE_NAME;
  public static final String BOMEBOX_APC40MK2 = BOMEBOX + APC40MK2;

  // APCminiMk2
  public static final String APCMINIMK2 = heronarts.lx.midi.surface.APCminiMk2.DEVICE_NAME;
  public static final String APCMINIMK2_DIRECTOR = DirectorAPCminiMk2.DEVICE_NAME;
  // APCminiMk2 midi input names get changed by Bomebox, and we want to run multiple.
  // To bypass the hassle we're mapping it to a virtual midi port in Bome Network Pro
  public static final String BOMEBOX_VIRTUAL_APCMINIMK2_DIRECTOR = "Director";
  public static final String BOMEBOX_VIRTUAL_APCMINIMK2_SUPERMOD = "SuperMod";

  // MidiFighter64
  public static final String MF64 = "Midi Fighter 64";
  public static final String BOMEBOX_MF64 = BOMEBOX + MF64;

  // MidiFighterTwister
  public static final String BOMEBOX_MIDIFIGHTERTWISTER1 = BOMEBOX + "Midi Fighter Twister";
  public static final String BOMEBOX_MIDIFIGHTERTWISTER2 = BOMEBOX + "Midi Fighter Twister (2)";
  public static final String BOMEBOX_MIDIFIGHTERTWISTER3 = BOMEBOX + "Midi Fighter Twister (3)";
  public static final String BOMEBOX_MIDIFIGHTERTWISTER4 = BOMEBOX + "Midi Fighter Twister (4)";
}
