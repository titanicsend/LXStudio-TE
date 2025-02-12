/**
 * Copyright 2024- Justin Belcher, Mark C. Slee, Heron Arts LLC
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
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package titanicsend.lx;

import static java.lang.Thread.sleep;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.surface.LXMidiParameterControl;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import titanicsend.app.director.Director;
import titanicsend.color.ColorPaletteManager;
import titanicsend.util.TE;

/**
 * Midi control surface using
 */
public class DirectorAPCminiMk2 extends LXMidiSurface implements LXMidiSurface.Bidirectional {

  // To use locally, rename system device to match LOCAL_DEVICE_NAME
  public static final String LOCAL_DEVICE_NAME = "Director";
  public static final String DEVICE_NAME = LOCAL_DEVICE_NAME + " Control";

  public static final boolean EXPORT_GRID_TO_CSV = false;

  public static final float PARTIAL_SATURATION = 0.6f;

  // LEDs
  // Single color (perimeter buttons)
  public static final int MIDI_CHANNEL_SINGLE = 0;

  // Single AND multi color buttons
  public static final int LED_OFF = 0;

  // Single color buttons
  public static final int LED_ON = 1;
  public static final int LED_BLINK = 2;

  // Notes in combination with Shift
  public static final int SHIFT = 122;

  public static final int CLIP_STOP = 112;
  public static final int SOLO = 113;
  public static final int MUTE = 114;
  public static final int REC_ARM = 115;
  public static final int SELECT = 116;
  public static final int DRUM_MODE = 117;
  public static final int NOTE_MODE = 118;
  public static final int STOP_ALL_CLIPS = 119;

  public static final int FADER_CTRL_VOLUME = 100;
  public static final int FADER_CTRL_PAN = 101;
  public static final int FADER_CTRL_SEND = 102;
  public static final int FADER_CTRL_DEVICE = 103;

  public static final int SELECT_UP = 104;
  public static final int SELECT_DOWN = 105;
  public static final int SELECT_LEFT = 106;
  public static final int SELECT_RIGHT = 107;

  public static final int SCENE_LAUNCH_NUM = 8;
  public static final int SCENE_LAUNCH = CLIP_STOP;
  public static final int SCENE_LAUNCH_MAX = SCENE_LAUNCH + SCENE_LAUNCH_NUM - 1;

  public static final int NUM_CHANNELS = 8;
  public static final int CHANNEL_BUTTON = 100;
  public static final int CHANNEL_BUTTON_MAX = CHANNEL_BUTTON + NUM_CHANNELS - 1;

  public static final int MIDI_CHANNEL_MULTI_100_PERCENT = 6;

  // CCs
  public static final int CHANNEL_FADER = 48;
  public static final int CHANNEL_FADER_MAX = CHANNEL_FADER + NUM_CHANNELS - 1;
  public static final int MASTER_FADER = 56;

  public static final int CLIP_LAUNCH = 0;
  public static final int CLIP_LAUNCH_ROWS = 8;
  public static final int CLIP_LAUNCH_COLUMNS = NUM_CHANNELS;
  public static final int CLIP_LAUNCH_NUM = CLIP_LAUNCH_ROWS * CLIP_LAUNCH_COLUMNS;
  public static final int CLIP_LAUNCH_MAX = CLIP_LAUNCH + CLIP_LAUNCH_NUM - 1;

  public static final int PARAMETER_COLUMNS = 8;
  public static final int PARAMETER_COLUMN_STRIDE = 1;
  public static final int PARAMETER_ROWS = 2;
  public static final int PARAMETER_ROW_STRIDE = -4;
  public static final int PARAMETER_NUM = PARAMETER_COLUMNS * PARAMETER_ROWS;
  public static final int PARAMETER_START = (CLIP_LAUNCH_ROWS - 1) * CLIP_LAUNCH_COLUMNS + CLIP_LAUNCH;

  public static final int COLOR_GRID_ROWS = CLIP_LAUNCH_ROWS;
  public static final int COLOR_GRID_COLUMNS = CLIP_LAUNCH_COLUMNS;
  public static final int COLOR_NUM = COLOR_GRID_ROWS * COLOR_GRID_COLUMNS;

  private boolean shiftOn = false;

  private boolean isRegistered = false;

  private ColorPaletteManager paletteManager = null;

  public final EnumParameter<LXMidiParameterControl.Mode> faderMode =
    new EnumParameter<LXMidiParameterControl.Mode>("Fader Mode",
      LXMidiParameterControl.Mode.SCALE)
    .setDescription("Parameter control mode for faders");

  private final LXMidiParameterControl masterFader;
  private final LXMidiParameterControl[] channelFaders;

  public DirectorAPCminiMk2(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
    this.masterFader = new LXMidiParameterControl(this.lx.engine.mixer.masterBus.fader);
    this.channelFaders = new LXMidiParameterControl[NUM_CHANNELS];
    for (int i = 0; i < NUM_CHANNELS; i++) {
      this.channelFaders[i] = new LXMidiParameterControl();
    }
    updateFaderMode();

    addSetting("faderMode", this.faderMode);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    super.onParameterChanged(p);
    if (p == this.faderMode) {
      updateFaderMode();
    }
  }

  private void updateFaderMode() {
    final LXMidiParameterControl.Mode mode = this.faderMode.getEnum();
    this.masterFader.setMode(mode);
    for (LXMidiParameterControl channelFader : this.channelFaders) {
      channelFader.setMode(mode);
    }
  }

  /**
   * Populated by generateColorGrid() - used as a lookup in noteReceived()
   */
  private final int[] noteToColor = new int[COLOR_NUM];

  @Override
  protected void onEnable(boolean on) {
    if (on) {
      initialize();
      register();
    } else {
      if (this.isRegistered) {
        unregister();
      }
    }
  }

  private void initialize() {
    sendGrid();
  }

  private void register() {
    this.isRegistered = true;
    try {
      this.paletteManager = (ColorPaletteManager) this.lx.engine.getChild("paletteManagerA");
    } catch (Exception e) {
      TE.err("Palette manager not found: " + e.getMessage());
    }
    try {
      Director director = Director.get();

      this.masterFader.setTarget(director.master);

      for (int i = 0; i < NUM_CHANNELS; i++) {
        if (i < director.filters.size()) {
          this.channelFaders[i].setTarget(director.filters.get(i).fader);
        } else {
          this.channelFaders[i].setTarget(null);
        }
      }
    } catch (Exception e) {
      TE.log("Director error: " + e.getMessage());
    }
  }

  private void unregister() {
    this.isRegistered = false;

    this.masterFader.setTarget(null);
    for (int i = 0; i < NUM_CHANNELS; i++) {
      this.channelFaders[i].setTarget(null);
    }

    clearGrid();
  }

  @Override
  protected void onReconnect() {
    if (this.enabled.isOn()) {
      initialize();
    }
  }

  @Override
  public void dispose() {
    if (this.isRegistered) {
      unregister();
    }
    this.masterFader.dispose();
    for (LXMidiParameterControl fader : this.channelFaders) {
      fader.dispose();
    }
    super.dispose();
  }

  public static int hsvToHex(float hue, float saturation, float value) {
    int rgb = Color.HSBtoRGB(hue, saturation, value);
    return 0xFFFFFF & rgb;
  }

  private void noteReceived(MidiNote note, boolean on) {
    final int pitch = note.getPitch();

    // Global momentary
    if (pitch == SHIFT) {
      this.shiftOn = on;
      return;
    }

    // Clip grid buttons
    if (inRange(pitch, CLIP_LAUNCH, CLIP_LAUNCH_MAX)) {
      if (pitch < 0 || pitch >= COLOR_NUM) {
        LXMidiEngine.error("Grid button not assigned to color: " + note);
        return;
      }
      if (isWhiteButton(pitch)) {
        // Ignore white in Chromatik, this is only for lasers.
        return;
      }

      int color = noteToColor[pitch];
      float h = LXColor.h(color);
      float s = LXColor.s(color);
      float b = LXColor.b(color);
      if (this.paletteManager != null) {
        this.paletteManager.hue.setValue(h);
        this.paletteManager.saturation.setValue(s);
        this.paletteManager.brightness.setValue(b);
        // Push the managed swatch to the global palette immediately,
        // so it doesn't need to be "pinned".
        this.paletteManager.pushSwatch.trigger();
      }
      return;
    } else if (inRange(pitch, SCENE_LAUNCH, SCENE_LAUNCH_MAX)) {
      // placeholder for using scene launch buttons
      return;
    } else if (inRange(pitch, CHANNEL_BUTTON, CHANNEL_BUTTON_MAX)) {
      // placeholder for using channel buttons
      return;
    }

    LXMidiEngine.error("APCminiMk2 received unmapped note: " + note);
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    noteReceived(note, true);
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    noteReceived(note, false);
  }

  @Override
  public void controlChangeReceived(MidiControlChange cc) {
    int number = cc.getCC();

    if (number == MASTER_FADER) {
      this.masterFader.setValue(cc);
      return;
    }

    if (number >= CHANNEL_FADER && number <= CHANNEL_FADER_MAX) {
      int channel = number - CHANNEL_FADER;
      this.channelFaders[channel].setValue(cc);
      return;
    }

    LXMidiEngine.error("APCmini unmapped control change: " + cc);
  }

  private int gridNote(int row, int col) {
    return (row * 8) + col;
  }

  private void sendGrid() {
    int[][] grid = generateColorGrid();
    for (int row = 0; row < grid.length; row++) {
      for (int col = 0; col < grid[0].length; col++) {
        int idx = gridNote(row, col);
        int color = grid[row][col];
        noteToColor[idx] = color;
        sendIndividualSysEx(idx, color);

        // It would be ideal to get away from sleeping here, it lags the engine by
        // 640ms when the controller connects/reconnects
        try {
          sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  static boolean exported = false;

  private int[] whiteButtons = new int[0];

  private boolean isWhiteButton(int pitch) {
    for (int p : this.whiteButtons) {
      if (p == pitch) {
        return true;
      }
    }
    return false;
  }

  public int[][] generateColorGrid() {
    int[][] grid = new int[COLOR_GRID_ROWS][COLOR_GRID_COLUMNS];
    float[][] hues = new float[COLOR_GRID_ROWS][COLOR_GRID_COLUMNS];
    float[][] sats = new float[COLOR_GRID_ROWS][COLOR_GRID_COLUMNS];

    final int steps = 31;
    float hueStep = 1f / ((float)steps);
    float hue = 0;

    // 0 to 90 degrees in 8 steps, normalized to [0,1]
    for (int i = 0; i < 8; i++) {
      grid[0][i] = hsvToHex(hue, 1.0f, 1.0f); // Full saturation
      grid[1][i] = hsvToHex(hue, PARTIAL_SATURATION, 1.0f); // Half saturation
      hues[0][i] = hue;
      hues[1][i] = hue;
      sats[0][i] = 1.0f;
      sats[1][i] = PARTIAL_SATURATION;
      hue += hueStep;
    }
    // 90 to 180 degrees in 8 steps, normalized to [0,1]
    for (int i = 0; i < 8; i++) {
      grid[2][i] = hsvToHex(hue, 1.0f, 1.0f); // Full saturation
      grid[3][i] = hsvToHex(hue, PARTIAL_SATURATION, 1.0f); // Half saturation
      hues[2][i] = hue;
      hues[3][i] = hue;
      sats[2][i] = 1.0f;
      sats[3][i] = PARTIAL_SATURATION;
      hue += hueStep;
    }
    // 180 to 270 degrees in 8 steps, normalized to [0,1]
    for (int i = 0; i < 8; i++) {
      grid[4][i] = hsvToHex(hue, 1.0f, 1.0f); // Full saturation
      grid[5][i] = hsvToHex(hue, PARTIAL_SATURATION, 1.0f); // Half saturation
      hues[4][i] = hue;
      hues[5][i] = hue;
      sats[4][i] = 1.0f;
      sats[5][i] = PARTIAL_SATURATION;
      hue += hueStep;
    }
    // 270 to 360 degrees in 8 steps, normalized to [0,1]
    for (int i = 0; i < 8 - (32 - steps); i++) {
      grid[6][i] = hsvToHex(hue, 1.0f, 1.0f); // Full saturation
      grid[7][i] = hsvToHex(hue, PARTIAL_SATURATION, 1.0f); // Half saturation
      hues[6][i] = hue;
      hues[7][i] = hue;
      sats[6][i] = 1.0f;
      sats[7][i] = PARTIAL_SATURATION;
      hue += hueStep;
    }

    // White
    grid[6][7] = hsvToHex(0f, 0f, 1f);
    grid[7][7] = hsvToHex(0f, 0f, 1f);
    hues[6][7] = 0f;
    hues[7][7] = 0f;
    sats[6][7] = 0f;
    sats[7][7] = 0f;
    this.whiteButtons = new int[2];
    this.whiteButtons[0] = gridNote(6, 7);
    this.whiteButtons[1] = gridNote(7, 7);

    if (EXPORT_GRID_TO_CSV && !exported) {
      exported = true;
      exportGrid(hues, sats);
    }

    return grid;
  }

  private static void exportGrid(float[][] hue, float[][] sat) {
    String fileName = "colors_TE_grid.csv";

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
      // Rows are currently bottom-up to match color assignment
      for (int i = hue.length - 1; i >= 0; i--) {
        for (int j = 0; j < hue[i].length; j++) {
          String hueString = String.format("%.2f", hue[i][j] * 360f);
          // Convert sat to percentage string
          String satString = String.format("%d%%", Math.round(sat[i][j] * 100));
          String midi = String.format("Midi CH=%d NOTE=%d", MIDI_CHANNEL_SINGLE, i * 8 + j);
          writer.write(hueString + "       " + satString + "     " + midi);
          // If it's not the last cell in the row, add a comma separator
          if (j < hue[i].length - 1) {
            writer.write(",");
          }
        }
        // Add a new line at the end of each row
        writer.newLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    LX.log("Exported grid colors to " + fileName);
  }

  private void sendIndividualSysEx(int padIndex, int color) {
    sendMultipleSysEx(padIndex, padIndex, new int[]{color});
  }

  /**
   * Send a custom RGB value to LEDs on pads.
   *
   * See page 9 of APC mini mk2 communications protocol:
   * - https://cdn.inmusicbrands.com/akai/attachments/APC%20mini%20mk2%20-%20Communication%20Protocol%20-%20v1.0.pdf
   *
   * @param startPad start index of pad button (button 0 is bottom-left corner on the pad)
   * @param endPad   end index of pad button (button 63 is top-right corner on the pad)
   * @param colors   array of RGB values for each pad button
   */
  private void sendMultipleSysEx(int startPad, int endPad, int[] colors) {
    int length = (endPad - startPad) + 1;
    if (colors.length != length) {
      throw new IllegalArgumentException(
        "Invalid number of colors for pads: (endPad - startPad + 1) = " + length +
        " != " + colors.length);
    }
    int numBytesToFollow = 2 + (length * 6);
    int messageLength = numBytesToFollow + 8;
    byte[] data = new byte[messageLength];
    data[0] = (byte) 0xF0;                      // MIDI system exclusive message start
    data[1] = (byte) 0x47;                      // manufacturers ID byte
    data[2] = (byte) 0x7F;                      // system exclusive device ID
    data[3] = (byte) 0x4F;                      // product model ID
    data[4] = (byte) 0x24;                      // message type identifier
    data[5] = (byte) (numBytesToFollow / 256);  // number of bytes to follow (most significant)
    data[6] = (byte) (numBytesToFollow % 256);  // number of bytes to follow (least significant)
    data[7] = (byte) startPad;                  // start pad (index of starting pad ID)
    data[8] = (byte) endPad;                    // end pad (index of ending pad ID)
    for (int i = 0; i < colors.length; i++) {
      int color = colors[i];
      if (color < 0x000000 || color > 0xFFFFFF) {
        throw new IllegalArgumentException("Invalid color");
      }
      int red = (color >> 16) & 0xFF;
      int green = (color >> 8) & 0xFF;
      int blue = color & 0xFF;
      data[9 + (i * 6)] = (byte) ((red >> 7) & 0x7F);     // red MSB
      data[10 + (i * 6)] = (byte) (red & 0x7F);           // red LSB
      data[11 + (i * 6)] = (byte) ((green >> 7) & 0x7F);  // green MSB
      data[12 + (i * 6)] = (byte) (green & 0x7F);         // green LSB
      data[13 + (i * 6)] = (byte) ((blue >> 7) & 0x7F);   // blue MSB
      data[14 + (i * 6)] = (byte) (blue & 0x7F);          // blue LSB
    }
    data[messageLength - 1] = (byte) 0xF7; // MIDI system exclusive message end
    // System.out.println(prettyPrintByteArray(data));
    sendSysex(data);
  }

  public static String prettyPrintByteArray(byte[] byteArray) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");

    for (int i = 0; i < byteArray.length; i++) {
      sb.append("\n\t");
      sb.append(String.format("0x%02X", byteArray[i]));
      sb.append(String.format("\t%d", i));
      if (i < byteArray.length - 1) {
        sb.append(", ");
      }
    }

    sb.append("\n]");
    return sb.toString();
  }

  private void clearGrid() {
    sendNoteOn(MIDI_CHANNEL_SINGLE, FADER_CTRL_VOLUME, LED_OFF);
    sendNoteOn(MIDI_CHANNEL_SINGLE, FADER_CTRL_PAN, LED_OFF);
    sendNoteOn(MIDI_CHANNEL_SINGLE, FADER_CTRL_SEND, LED_OFF);
    sendNoteOn(MIDI_CHANNEL_SINGLE, FADER_CTRL_DEVICE, LED_OFF);
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      clearChannelPatterns(i);
    }
  }

  private void clearChannelPatterns(int index) {
    if (index < 0 || index >= CLIP_LAUNCH_COLUMNS) {
      return;
    }
    for (int y = 0; y < CLIP_LAUNCH_ROWS; ++y) {
      sendNoteOn(
          MIDI_CHANNEL_MULTI_100_PERCENT,
          CLIP_LAUNCH + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - y) + index,
          LED_OFF
      );
    }
  }

  /**
   * Returns true if value is between [min, max] inclusive
   *
   * @param val Value
   * @param min Min value
   * @param max Max value
   * @return True if contained in range
   */
  public static boolean inRange(int val, int min, int max) {
    return (val >= min) && (val <= max);
  }
}