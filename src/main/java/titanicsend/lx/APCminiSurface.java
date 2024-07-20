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

import heronarts.lx.color.LXColor;
import heronarts.lx.midi.surface.FocusedDevice;
import heronarts.lx.midi.surface.LXMidiParameterControl;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.midi.surface.MixerSurface;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import heronarts.lx.LX;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.clip.LXClip;
import heronarts.lx.clip.LXClipEngine;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXGroup;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.utils.LXUtils;
import titanicsend.color.ColorPaletteManager;
import titanicsend.util.TE;

public abstract class APCminiSurface extends LXMidiSurface implements LXMidiSurface.Bidirectional {

  public static final int NUM_CHANNELS = 8;
  public static final double PARAMETER_INCREMENT_AMOUNT = 0.1;

  // CCs
  public static final int CHANNEL_FADER = 48;
  public static final int CHANNEL_FADER_MAX = CHANNEL_FADER + NUM_CHANNELS - 1;
  public static final int MASTER_FADER = 56;

  interface NoteDefinitions {
    public int getShift();
    public int getClipStop();
    public int getSolo();
    public int getMute();
    public int getRecArm();
    public int getSelect();
    public int getDrumMode();
    public int getNoteMode();
    public int getStopAllClips();

    public int getFaderCtrlVolume();
    public int getFaderCtrlPan();
    public int getFaderCtrlSend();
    public int getFaderCtrlDevice();
    public int getSelectUp();
    public int getSelectDown();
    public int getSelectLeft();
    public int getSelectRight();

    public int getChannelButton();
  }

  protected abstract NoteDefinitions getNoteDefinitions();

  private class Note {

    public final int SHIFT;

    public final int CLIP_STOP;
    public final int SOLO;
    public final int MUTE;
    public final int REC_ARM;
    public final int SELECT;
    public final int DRUM_MODE;
    public final int NOTE_MODE;
    public final int STOP_ALL_CLIPS;

    public final int FADER_CTRL_VOLUME;
    public final int FADER_CTRL_PAN;
    public final int FADER_CTRL_SEND;
    public final int FADER_CTRL_DEVICE;
    public final int SELECT_UP;
    public final int SELECT_DOWN;
    public final int SELECT_LEFT;
    public final int SELECT_RIGHT;

    public final int CHANNEL_BUTTON;
    public final int CHANNEL_BUTTON_MAX;

    // The FADER CTRL buttons are used with shift to set the grid mode
    public final int GRID_MODE_PATTERNS;
    public final int GRID_MODE_CLIPS;
    public final int GRID_MODE_PARAMETERS;

    public static final int SCENE_LAUNCH_NUM = 8;
    public final int SCENE_LAUNCH;
    public final int SCENE_LAUNCH_MAX;

    // The SOFT KEYS buttons are used with shift to set the channel mode
    public final int CHANNEL_BUTTON_MODE_FOCUS;
    public final int CHANNEL_BUTTON_MODE_ENABLED;
    public final int CHANNEL_BUTTON_MODE_CUE;
    public final int CHANNEL_BUTTON_MODE_ARM;
    public final int CHANNEL_BUTTON_MODE_CLIP_STOP;

    private Note() {
      final NoteDefinitions def = getNoteDefinitions();

      this.SHIFT = def.getShift();

      this.CLIP_STOP = def.getClipStop();
      this.SOLO = def.getSolo();
      this.MUTE = def.getMute();
      this.REC_ARM = def.getRecArm();
      this.SELECT = def.getSelect();
      this.DRUM_MODE = def.getDrumMode();
      this.NOTE_MODE = def.getNoteMode();
      this.STOP_ALL_CLIPS = def.getStopAllClips();

      this.CHANNEL_BUTTON_MODE_FOCUS = SELECT;
      this.CHANNEL_BUTTON_MODE_ENABLED = MUTE;
      this.CHANNEL_BUTTON_MODE_CUE = SOLO;
      this.CHANNEL_BUTTON_MODE_ARM = REC_ARM;
      this.CHANNEL_BUTTON_MODE_CLIP_STOP = CLIP_STOP;

      this.SCENE_LAUNCH = CLIP_STOP;
      this.SCENE_LAUNCH_MAX = SCENE_LAUNCH + SCENE_LAUNCH_NUM - 1;

      this.FADER_CTRL_VOLUME = def.getFaderCtrlVolume();
      this.FADER_CTRL_PAN = def.getFaderCtrlPan();
      this.FADER_CTRL_SEND = def.getFaderCtrlSend();
      this.FADER_CTRL_DEVICE = def.getFaderCtrlDevice();
      this.SELECT_UP = def.getSelectUp();
      this.SELECT_DOWN = def.getSelectDown();
      this.SELECT_LEFT = def.getSelectLeft();
      this.SELECT_RIGHT = def.getSelectRight();

      this.GRID_MODE_PATTERNS = FADER_CTRL_VOLUME;
      this.GRID_MODE_CLIPS = FADER_CTRL_PAN;
      this.GRID_MODE_PARAMETERS = FADER_CTRL_DEVICE;

      this.CHANNEL_BUTTON = def.getChannelButton();
      this.CHANNEL_BUTTON_MAX = CHANNEL_BUTTON + NUM_CHANNELS - 1;

    }

    public boolean isSelect(int pitch) {
      return
          (pitch == SELECT_UP) ||
              (pitch == SELECT_DOWN) ||
              (pitch == SELECT_LEFT) ||
              (pitch == SELECT_RIGHT);
    }
  }

  private final Note NOTE = new Note();

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

  // LEDs
  // Single color (perimeter buttons)
  public static final int MIDI_CHANNEL_SINGLE = 0;

  // Single AND multi color buttons
  public static final int LED_OFF = 0;

  // Single color buttons
  public static final int LED_ON = 1;
  public static final int LED_BLINK = 2;

  private static int LED_ON(boolean condition) {
    return condition ? LED_ON : LED_OFF;
  }

  interface LedDefinitions {
    public int getDefaultMultiBehavior();
    public int getParameterIncrementBehavior();
    public int getParameterIncrementColor();
    public int getParameterDecrementBehavior();
    public int getParameterDecrementColor();
    public int getParameterIsDefaultBehavior();
    public int getParameterIsDefaultColor();
    public int getParameterResetBehavior();
    public int getParameterResetColor();

    public int getPatternActiveBehavior();
    public int getPatternActiveColor();
    public int getPatternEnabledBehavior();
    public int getPatternEnabledColor();
    public int getPatternDisabledBehavior();
    public int getPatternDisabledColor();
    public int getPatternDisabledFocusedBehavior();
    public int getPatternDisabledFocusedColor();
    public int getPatternFocusedBehavior();
    public int getPatternFocusedColor();
    public int getPatternInactiveBehavior();
    public int getPatternInactiveColor();
    public int getPatternTransitionBehavior();
    public int getPatternTransitionColor();

    public int getClipRecordBehavior();
    public int getClipRecordColor();
    public int getClipArmBehavior();
    public int getClipArmColor();
    public int getClipInactiveBehavior();
    public int getClipInactiveColor();
    public int getClipPlayBehavior();
    public int getClipPlayColor();

  }

  private final class Led {
    public final int DEFAULT_MULTI_BEHAVIOR;

    public final int PARAMETER_INCREMENT_BEHAVIOR;
    public final int PARAMETER_INCREMENT_COLOR;
    public final int PARAMETER_DECREMENT_BEHAVIOR;
    public final int PARAMETER_DECREMENT_COLOR;
    public final int PARAMETER_ISDEFAULT_BEHAVIOR;
    public final int PARAMETER_ISDEFAULT_COLOR;
    public final int PARAMETER_RESET_BEHAVIOR;
    public final int PARAMETER_RESET_COLOR;

    public final int PATTERN_ACTIVE_BEHAVIOR;
    public final int PATTERN_ACTIVE_COLOR;
    public final int PATTERN_ENABLED_BEHAVIOR;
    public final int PATTERN_ENABLED_COLOR;
    public final int PATTERN_DISABLED_BEHAVIOR;
    public final int PATTERN_DISABLED_COLOR;
    public final int PATTERN_DISABLED_FOCUSED_BEHAVIOR;
    public final int PATTERN_DISABLED_FOCUSED_COLOR;
    public final int PATTERN_FOCUSED_BEHAVIOR;
    public final int PATTERN_FOCUSED_COLOR;
    public final int PATTERN_INACTIVE_BEHAVIOR;
    public final int PATTERN_INACTIVE_COLOR;
    public final int PATTERN_TRANSITION_BEHAVIOR;
    public final int PATTERN_TRANSITION_COLOR;

    public final int CLIP_RECORD_BEHAVIOR;
    public final int CLIP_RECORD_COLOR;
    public final int CLIP_ARM_BEHAVIOR;
    public final int CLIP_ARM_COLOR;
    public final int CLIP_INACTIVE_BEHAVIOR;
    public final int CLIP_INACTIVE_COLOR;
    public final int CLIP_PLAY_BEHAVIOR;
    public final int CLIP_PLAY_COLOR;

    private Led() {
      final LedDefinitions def = getLedDefinitions();

      this.DEFAULT_MULTI_BEHAVIOR = def.getDefaultMultiBehavior();

      this.PARAMETER_INCREMENT_BEHAVIOR = def.getParameterIncrementBehavior();
      this.PARAMETER_INCREMENT_COLOR = def.getParameterIncrementColor();
      this.PARAMETER_DECREMENT_BEHAVIOR = def.getParameterDecrementBehavior();
      this.PARAMETER_DECREMENT_COLOR = def.getParameterDecrementColor();
      this.PARAMETER_ISDEFAULT_BEHAVIOR = def.getParameterIsDefaultBehavior();
      this.PARAMETER_ISDEFAULT_COLOR = def.getParameterIsDefaultColor();
      this.PARAMETER_RESET_BEHAVIOR = def.getParameterResetBehavior();
      this.PARAMETER_RESET_COLOR = def.getParameterResetColor();

      this.PATTERN_ACTIVE_BEHAVIOR = def.getPatternActiveBehavior();
      this.PATTERN_ACTIVE_COLOR = def.getPatternActiveColor();
      this.PATTERN_ENABLED_BEHAVIOR = def.getPatternEnabledBehavior();
      this.PATTERN_ENABLED_COLOR = def.getPatternEnabledColor();
      this.PATTERN_DISABLED_BEHAVIOR = def.getPatternDisabledBehavior();
      this.PATTERN_DISABLED_COLOR = def.getPatternDisabledColor();
      this.PATTERN_DISABLED_FOCUSED_BEHAVIOR = def.getPatternDisabledFocusedBehavior();
      this.PATTERN_DISABLED_FOCUSED_COLOR = def.getPatternDisabledFocusedColor();
      this.PATTERN_FOCUSED_BEHAVIOR = def.getPatternFocusedBehavior();
      this.PATTERN_FOCUSED_COLOR = def.getPatternFocusedColor();
      this.PATTERN_INACTIVE_BEHAVIOR = def.getPatternInactiveBehavior();
      this.PATTERN_INACTIVE_COLOR = def.getPatternInactiveColor();
      this.PATTERN_TRANSITION_BEHAVIOR = def.getPatternTransitionBehavior();
      this.PATTERN_TRANSITION_COLOR = def.getPatternTransitionColor();

      this.CLIP_RECORD_BEHAVIOR = def.getClipRecordBehavior();
      this.CLIP_RECORD_COLOR = def.getClipRecordColor();
      this.CLIP_ARM_BEHAVIOR = def.getClipArmBehavior();
      this.CLIP_ARM_COLOR = def.getClipArmColor();
      this.CLIP_INACTIVE_BEHAVIOR = def.getClipInactiveBehavior();
      this.CLIP_INACTIVE_COLOR = def.getClipInactiveColor();
      this.CLIP_PLAY_BEHAVIOR = def.getClipPlayBehavior();
      this.CLIP_PLAY_COLOR = def.getClipPlayColor();
    }

  }

  private final Led LED = new Led();

  protected abstract LedDefinitions getLedDefinitions();

  private boolean shiftOn = false;

  private boolean isRegistered = false;

  private ColorPaletteManager paletteManager = null;

  /**
   * Populated by generateColorGrid() - used as a lookup in noteReceived()
   */
  private int[] noteToColor = new int[64];

  public APCminiSurface(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
  }

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
    try {
      paletteManager = (ColorPaletteManager) this.lx.engine.getChild("paletteManagerA");
    } catch (Exception e) {
      TE.log("Palette manager not found");
    }
    this.isRegistered = true;
  }

  private void unregister() {
    this.isRegistered = false;
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
    super.dispose();
  }

  public static int hsvToHex(float hue, float saturation, float value) {
    int rgb = Color.HSBtoRGB(hue, saturation, value);
    return 0xFFFFFF & rgb;
  }

  private void noteReceived(MidiNote note, boolean on) {
    final int pitch = note.getPitch();

    // Global momentary
    if (pitch == NOTE.SHIFT) {
      // Shift doesn't have an LED, odd.
      this.shiftOn = on;
      return;
    }

    // Clip grid buttons
    if (LXUtilsWithRange.inRange(pitch, CLIP_LAUNCH, CLIP_LAUNCH_MAX)) {
      if (pitch < 0 || pitch >= 64) {
        LXMidiEngine.error("APCminiMk2 received unmapped note: " + note);
        return;
      }
      int color = noteToColor[pitch];
      System.out.printf("0x%06X (on=%s)\n", color, on);
      float h = LXColor.h(color);
      float s = LXColor.s(color);
      float b = LXColor.b(color);
      System.out.printf("h: %f, s: %f, b: %f\n", h, s, b);
      if (this.paletteManager != null) {
        this.paletteManager.hue.setValue(h);
        this.paletteManager.saturation.setValue(s);
        this.paletteManager.brightness.setValue(b);
      }
      return;
    }

    // Scene launch buttons
    if (LXUtilsWithRange.inRange(pitch, NOTE.SCENE_LAUNCH, NOTE.SCENE_LAUNCH_MAX)) {
      return;
    }

    if (LXUtilsWithRange.inRange(pitch, NOTE.CHANNEL_BUTTON, NOTE.CHANNEL_BUTTON_MAX)) {
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

  private void sendGrid() {
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        int idx = (row * 8) + col;
        sendSysEx(idx, idx, new int[]{0x000000});
        try {
          // Need to sleep between each SysEx message (to avoid MIDI overflow?)
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    int[][] grid = generateColorGrid();
    for (int row = 0; row < grid.length; row++) {
      for (int col = 0; col < grid[0].length; col++) {
        int idx = (row * 8) + col;
        int color = grid[row][col];

        noteToColor[idx] = color;

//        System.out.printf("row: %d, col: %d, idx: %d, color: %06X\n", row, col, idx, color);
        sendSysEx(idx, idx, new int[]{color});
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static int[][] generateColorGrid() {
    int[][] grid = new int[8][8];

    // Generate first 1/4 of the hue wheel (0 to 90 degrees) at full and half saturation
    for (int i = 0; i < 8; i++) {
      float hue = i * 11.25f / 360; // 0 to 90 degrees in 8 steps, normalized to [0,1]
      grid[0][i] = hsvToHex(hue, 1.0f, 1.0f); // Full saturation
      grid[1][i] = hsvToHex(hue, 0.5f, 1.0f); // Half saturation
    }

    for (int i = 0; i < 8; i++) {
      float hue = (90 + i * 11.25f) / 360; // 180 to 270 degrees in 8 steps, normalized to [0,1]
      grid[2][i] = hsvToHex(hue, 1.0f, 1.0f); // Full saturation
      grid[3][i] = hsvToHex(hue, 0.5f, 1.0f); // Half saturation
    }

    for (int i = 0; i < 8; i++) {
      float hue = (180 + i * 11.25f) / 360; // 180 to 270 degrees in 8 steps, normalized to [0,1]
      grid[4][i] = hsvToHex(hue, 1.0f, 1.0f); // Full saturation
      grid[5][i] = hsvToHex(hue, 0.5f, 1.0f); // Half saturation
    }

    for (int i = 0; i < 8; i++) {
      float hue = (270 + i * 11.25f) / 360; // 180 to 270 degrees in 8 steps, normalized to [0,1]
      grid[6][i] = hsvToHex(hue, 1.0f, 1.0f); // Full saturation
      grid[7][i] = hsvToHex(hue, 0.5f, 1.0f); // Half saturation
    }

    return grid;
  }

  private void sendSysEx(int startPad, int endPad, int[] colors) {
    int length = (endPad - startPad) + 1;
    if (colors.length != length) {
      throw new IllegalArgumentException("Invalid number of colors for pads: (endPad - startPad + 1) = " + length + " != " + colors.length);
    }
    int numBytesToFollow = 2 + (length * 6);

    byte numBytesLSB = (byte) (numBytesToFollow % 256);
    byte numBytesMSB = (byte) (numBytesToFollow / 256);
//    System.out.printf("numBytesToFollow: %d, numBytesMSB: %d, numBytesLSB: %d\n", numBytesToFollow, numBytesMSB & 0xFF, numBytesLSB & 0xFF);
//    System.out.printf("numBytesMSB: 0x%02X\n", numBytesMSB);
//    System.out.printf("numBytesMSB: 0x%02X\n", numBytesLSB);

    byte startPadByte = (byte) startPad;
    byte endPadByte = (byte) endPad;
//    System.out.printf("startPadByte: 0x%02X\n", startPadByte);
//    System.out.printf("endPadByte: 0x%02X\n", endPadByte);

    int b = 0xF7;
    int msb = (b >> 7) & 0x7F;
    int lsb = b & 0x7F;
//    System.out.printf("b: 0x%02X\n", b);
//    System.out.printf("b: 0x%02X\n", msb);
//    System.out.printf("b: 0x%02X\n", lsb);

    int messageLength = numBytesToFollow + 8;
    byte[] data = new byte[messageLength];
    data[0] = (byte) 0xF0; // MIDI system exclusive message start
    data[1] = (byte) 0x47; // manufacturers ID byte
    data[2] = (byte) 0x7F; // system exclusive device ID
    data[3] = (byte) 0x4F; // product model ID
    data[4] = (byte) 0x24; // message type identifier
    data[5] = numBytesMSB; // number of bytes to follow (most significant)
    data[6] = numBytesLSB; // number of bytes to follow (least significant)
    data[7] = startPadByte; // start pad (index of starting pad ID)
    data[8] = endPadByte; // end pad (index of ending pad ID)
    for (int i = 0; i < colors.length; i++) {
      int color = colors[i];
      if (color < 0x000000 || color > 0xFFFFFF) {
        throw new IllegalArgumentException("Invalid color");
      }
//        System.out.printf("color[%d]: 0x%06X\n", i, color);

      int c0 = (color >> 16) & 0xFF;
      int c1 = (color >> 8) & 0xFF;
      int c2 = color & 0xFF;

//        System.out.printf("\tc0: 0x%02X (%d)\n", c0, c0);
//        System.out.printf("\tc1: 0x%02X (%d)\n", c1, c1);
//        System.out.printf("\tc2: 0x%02X (%d)\n", c2, c2);

      int c0msb = (c0 >> 7) & 0x7F;
      int c0lsb = c0 & 0x7F;

//        System.out.printf("\tc0[msb]: 0x%02X\n", c0msb);
//        System.out.printf("\tc0[lsb]: 0x%02X\n", c0lsb);

      int c1msb = (c1 >> 7) & 0x7F;
      int c1lsb = c1 & 0x7F;

//        System.out.printf("\tc1[msb]: 0x%02X\n", c1msb);
//        System.out.printf("\tc1[lsb]: 0x%02X\n", c1lsb);

      int c2msb = (c2 >> 7) & 0x7F;
      int c2lsb = c2 & 0x7F;

//        System.out.printf("\tc2[msb]: 0x%02X\n", c2msb);
//        System.out.printf("\tc2[lsb]: 0x%02X\n", c2lsb);

      data[9 + (i * 6)] = (byte) c0msb; // red MSB
      data[10 + (i * 6)] = (byte) c0lsb; // red LSB
      data[11 + (i * 6)] = (byte) c1msb; // green MSB
      data[12 + (i * 6)] = (byte) c1lsb; // green LSB
      data[13 + (i * 6)] = (byte) c2msb; // blue MSB
      data[14 + (i * 6)] = (byte) c2lsb; // blue LSB
    }
    data[messageLength - 1] = (byte) 0xF7; // MIDI system exclusive message end
//    System.out.println(prettyPrintByteArray(data));
    this.output.sendSysex(data);
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
    sendNoteOn(MIDI_CHANNEL_SINGLE, NOTE.FADER_CTRL_VOLUME, LED_OFF);
    sendNoteOn(MIDI_CHANNEL_SINGLE, NOTE.FADER_CTRL_PAN, LED_OFF);
    sendNoteOn(MIDI_CHANNEL_SINGLE, NOTE.FADER_CTRL_SEND, LED_OFF);
    sendNoteOn(MIDI_CHANNEL_SINGLE, NOTE.FADER_CTRL_DEVICE, LED_OFF);
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
          LED.DEFAULT_MULTI_BEHAVIOR,
          CLIP_LAUNCH + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - y) + index,
          LED_OFF
      );
    }
  }
}