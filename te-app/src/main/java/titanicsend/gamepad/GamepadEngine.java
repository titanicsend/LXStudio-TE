package titanicsend.gamepad;

import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_LAST;
import static org.lwjgl.glfw.GLFW.glfwUpdateGamepadMappings;

import heronarts.glx.event.GamepadEvent;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.utils.LXUtils;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import org.lwjgl.system.MemoryUtil;

/**
 * x A relay between Chromatik system gamepad events and consumers such as patterns. Two mechanisms
 * are available for classes wishing to use gamepad data: 1. Listen to GamepadEngine to be notified
 * of raw gamepad events. 2. Create a new Gamepad object which has axis+button parameters and an
 * adjustable Input parameter to choose from one of the 16 system gamepad slots.
 */
public class GamepadEngine extends LXComponent {

  public static final String MAPPINGS_FILE = "resources/gamecontrollers/gamecontrollerdb.txt";
  // Fixed number of gamepad slots as defined by GLFW
  public static final int NUM_GAMEPADS = GLFW_JOYSTICK_LAST + 1;

  public static interface Listener {
    public default void onGamepadButtonPressed(GamepadEvent gamepadEvent, int button) {}

    public default void onGamepadButtonReleased(GamepadEvent gamepadEvent, int button) {}

    public default void onGamepadAxisChanged(GamepadEvent gamepadEvent, int axis, float value) {}
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  private final SystemGamepad[] systemGamePads = new SystemGamepad[NUM_GAMEPADS];

  public GamepadEngine(LX lx) {
    super(lx, "GamepadEngine");

    for (int i = 0; i < NUM_GAMEPADS; i++) {
      this.systemGamePads[i] = new SystemGamepad(lx, i);
    }
  }

  /** Update gamepad mappings for this runtime. */
  public void updateGamepadMappings() {
    // To get the latest mappings, download a new copy of
    // https://github.com/mdqinc/SDL_GameControllerDB/blob/master/gamecontrollerdb.txt

    // Load gamepad mappings from a file
    String mappings = loadGamepadMappingsFromFile();

    if (!LXUtils.isEmpty(mappings)) {
      ByteBuffer mappingBuffer = MemoryUtil.memUTF8(mappings);

      if (glfwUpdateGamepadMappings(mappingBuffer)) {
        LX.log("Gamepad mappings updated successfully.");
      } else {
        LX.error("Failed to update gamepad mappings.");
      }
    }
  }

  /**
   * Load gamepad mappings from a text file. Only needed for mappings that did not exist in the
   * lwjgl db, but it's fine to load the full latest db too.
   */
  private String loadGamepadMappingsFromFile() {
    StringBuilder mappings = new StringBuilder();
    try (Scanner s = new Scanner(new File(MAPPINGS_FILE))) {
      while (s.hasNextLine()) {
        String line = s.nextLine();
        mappings.append(line).append("\n");
      }
    } catch (Throwable e) {
      LX.error(e, "Error reading gamepad mappings file: " + e.getMessage());
      return null;
    }

    return mappings.toString().trim();
  }

  /* Listeners */

  public GamepadEngine addListener(Listener listener) {
    Objects.requireNonNull(listener);
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException(("Cannot add duplicate GamepadEngine.Listener: " + listener));
    }
    this.listeners.add(listener);
    return this;
  }

  public GamepadEngine removeListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException(
          "May not remove non-registered GamepadEngine.Listener: " + listener);
    }
    this.listeners.remove(listener);
    return this;
  }

  /* Input: system event notifications from TEApp */

  public void lxGamepadButtonPressed(GamepadEvent gamepadEvent, int button) {
    for (Listener listener : this.listeners) {
      listener.onGamepadButtonPressed(gamepadEvent, button);
    }
    lxGamepadButton(gamepadEvent, button, true);
  }

  public void lxGamepadButtonReleased(GamepadEvent gamepadEvent, int button) {
    for (Listener listener : this.listeners) {
      listener.onGamepadButtonReleased(gamepadEvent, button);
    }
    lxGamepadButton(gamepadEvent, button, false);
  }

  private void lxGamepadButton(GamepadEvent gamepadEvent, int button, boolean on) {
    SystemGamepad gamepad = this.systemGamePads[gamepadEvent.gamepadId];
    switch (button) {
      case GamepadEvent.BUTTON_A:
        gamepad.a.setValue(on);
        break;
      case GamepadEvent.BUTTON_B:
        gamepad.b.setValue(on);
        break;
      case GamepadEvent.BUTTON_X:
        gamepad.x.setValue(on);
        break;
      case GamepadEvent.BUTTON_Y:
        gamepad.y.setValue(on);
        break;
      case GamepadEvent.BUTTON_DPAD_UP:
        gamepad.dpUp.setValue(on);
        break;
      case GamepadEvent.BUTTON_DPAD_DOWN:
        gamepad.dpDown.setValue(on);
        break;
      case GamepadEvent.BUTTON_DPAD_LEFT:
        gamepad.dpLeft.setValue(on);
        break;
      case GamepadEvent.BUTTON_DPAD_RIGHT:
        gamepad.dpRight.setValue(on);
        break;
      case GamepadEvent.BUTTON_LEFT_BUMPER:
        gamepad.leftShoulder.setValue(on);
        break;
      case GamepadEvent.BUTTON_RIGHT_BUMPER:
        gamepad.rightShoulder.setValue(on);
        break;
      case GamepadEvent.BUTTON_LEFT_THUMB:
        gamepad.leftStick.setValue(on);
        break;
      case GamepadEvent.BUTTON_RIGHT_THUMB:
        gamepad.rightStick.setValue(on);
        break;
    }
  }

  public void lxGamepadAxisChanged(GamepadEvent gamepadEvent, int axis, float value) {
    for (Listener listener : this.listeners) {
      listener.onGamepadAxisChanged(gamepadEvent, axis, value);
    }
    SystemGamepad gamepad = this.systemGamePads[gamepadEvent.gamepadId];
    switch (axis) {
      case 0:
        gamepad.axisLeftX.setValue(value);
        break;
      case 1:
        gamepad.axisLeftY.setValue(value);
        break;
      case 2:
        gamepad.axisRightX.setValue(value);
        break;
      case 3:
        gamepad.axisRightY.setValue(value);
        break;
      case 4:
        gamepad.axisLeftTrigger.setValue(value);
        break;
      case 5:
        gamepad.axisRightTrigger.setValue(value);
        break;
    }
  }

  public Gamepad createGamepad() {
    return new Gamepad(this.lx);
  }

  public Gamepad createGamepad(int input) {
    return new Gamepad(this.lx, input);
  }

  public void dispose() {
    this.listeners.clear();
  }

  /**
   * Common base class for system and user gamepads. Represents a physical gamepad with parameters
   * for button and axis states.
   */
  public abstract class GamepadBase extends LXComponent {
    public interface GamepadListener {
      public void onGamepadParameterChanged(LXParameter parameter);
    }

    public final BoundedParameter axisLeftX = new BoundedParameter("Axis L-X", 0, -1, 1);
    public final BoundedParameter axisLeftY = new BoundedParameter("Axis L-Y", 0, -1, 1);
    public final BoundedParameter axisRightX = new BoundedParameter("Axis R-X", 0, -1, 1);
    public final BoundedParameter axisRightY = new BoundedParameter("Axis R-Y", 0, -1, 1);

    public final BoundedParameter axisLeftTrigger = new BoundedParameter("Axis LTrig-Y", 0, -1, 1);
    public final BoundedParameter axisRightTrigger = new BoundedParameter("Axis RTrig-Y", 0, -1, 1);

    public final BooleanParameter a = new BooleanParameter("A");
    public final BooleanParameter b = new BooleanParameter("B");
    public final BooleanParameter x = new BooleanParameter("X");
    public final BooleanParameter y = new BooleanParameter("Y");
    public final BooleanParameter back = new BooleanParameter("Back");
    public final BooleanParameter start = new BooleanParameter("Start");
    public final BooleanParameter guide = new BooleanParameter("Guide");
    public final BooleanParameter dpUp = new BooleanParameter("DP up");
    public final BooleanParameter dpRight = new BooleanParameter("DP right");
    public final BooleanParameter dpDown = new BooleanParameter("DP down");
    public final BooleanParameter dpLeft = new BooleanParameter("DP left");
    public final BooleanParameter leftShoulder = new BooleanParameter("L Shoulder");
    public final BooleanParameter rightShoulder = new BooleanParameter("R Shoulder");
    public final BooleanParameter leftStick = new BooleanParameter("L Stick");
    public final BooleanParameter rightStick = new BooleanParameter("R Stick");

    public GamepadBase(LX lx) {
      super(lx);
      addParameter("axisLeftX", this.axisLeftX);
      addParameter("axisLeftY", this.axisLeftY);
      addParameter("axisRightX", this.axisRightX);
      addParameter("axisRightY", this.axisRightY);
      addParameter("axisLeftTrigger", this.axisLeftTrigger);
      addParameter("axisRightTrigger", this.axisRightTrigger);
      addParameter("a", this.a);
      addParameter("b", this.b);
      addParameter("x", this.x);
      addParameter("y", this.y);
      addParameter("back", this.back);
      addParameter("start", this.start);
      addParameter("guide", this.guide);
      addParameter("dpUp", this.dpUp);
      addParameter("dpRight", this.dpRight);
      addParameter("dpDown", this.dpDown);
      addParameter("dpLeft", this.dpLeft);
      addParameter("leftShoulder", this.leftShoulder);
      addParameter("rightShoulder", this.rightShoulder);
      addParameter("leftStick", this.leftStick);
      addParameter("rightStick", this.rightStick);
    }

    private List<GamepadListener> listeners = new ArrayList<GamepadListener>();

    public GamepadBase addListener(GamepadListener listener) {
      Objects.requireNonNull(listener);
      if (this.listeners.contains(listener)) {
        throw new IllegalStateException(("Cannot add duplicate GamepadListener: " + listener));
      }
      this.listeners.add(listener);
      return this;
    }

    public GamepadBase removeListener(GamepadListener listener) {
      if (!this.listeners.contains(listener)) {
        throw new IllegalStateException(
            "May not remove non-registered GamepadListener: " + listener);
      }
      this.listeners.remove(listener);
      return this;
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
      for (GamepadListener listener : this.listeners) {
        listener.onGamepadParameterChanged(parameter);
      }
    }
  }

  /** Gamepad representing a fixed input number 1-16 */
  public class SystemGamepad extends GamepadBase {
    public final int input;

    public SystemGamepad(LX lx, int input) {
      super(lx);
      this.input = input;
    }
  }

  /** Gamepad with an adjustable input number allowing selection from the system gamepad slots. */
  public class Gamepad extends GamepadBase {
    public final DiscreteParameter input =
        new DiscreteParameter("Input", NUM_GAMEPADS).setDescription("Gamepad input number 1-16");

    // Event source
    private SystemGamepad source;

    private GamepadListener sourceListener =
        (p) -> {
          // Follow source parameters
          getParameter(p.getPath()).setValue(p.getValue());
        };

    private Gamepad(LX lx) {
      this(lx, 0);
    }

    private Gamepad(LX lx, int input) {
      super(lx);
      addParameter("input", this.input);

      // Set input number, which will immediately sync this to the corresponding system gamepad
      this.input.setValue(input);
      if (input == 0) {
        onInputChanged(input);
      }
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
      super.onParameterChanged(parameter);
      if (parameter == this.input) {
        onInputChanged(this.input.getValuei());
      }
    }

    private void onInputChanged(int input) {
      if (this.source != null) {
        unregister();
      }
      this.source = systemGamePads[input];
      register();
      refreshFromSource();
    }

    private void register() {
      this.source.addListener(this.sourceListener);
    }

    private void unregister() {
      this.source.removeListener(this.sourceListener);
    }

    /** Copy parameter values from source gamepad */
    private void refreshFromSource() {
      this.axisLeftX.setValue(this.source.axisLeftX.getValue());
      this.axisLeftY.setValue(this.source.axisLeftY.getValue());
      this.axisRightX.setValue(this.source.axisRightX.getValue());
      this.axisRightY.setValue(this.source.axisRightY.getValue());
      this.axisLeftTrigger.setValue(this.source.axisLeftTrigger.getValue());
      this.axisRightTrigger.setValue(this.source.axisRightTrigger.getValue());
      this.a.setValue(this.source.a.getValue());
      this.b.setValue(this.source.b.getValue());
      this.x.setValue(this.source.x.getValue());
      this.y.setValue(this.source.y.getValue());
      this.back.setValue(this.source.back.getValue());
      this.start.setValue(this.source.start.getValue());
      this.guide.setValue(this.source.guide.getValue());
      this.dpUp.setValue(this.source.dpUp.getValue());
      this.dpRight.setValue(this.source.dpRight.getValue());
      this.dpDown.setValue(this.source.dpDown.getValue());
      this.dpLeft.setValue(this.source.dpLeft.getValue());
      this.leftShoulder.setValue(this.source.leftShoulder.getValue());
      this.rightShoulder.setValue(this.source.rightShoulder.getValue());
      this.leftStick.setValue(this.source.leftStick.getValue());
      this.rightStick.setValue(this.source.rightStick.getValue());
    }

    @Override
    public void dispose() {
      if (this.source != null) {
        unregister();
      }
      super.dispose();
    }
  }
}
