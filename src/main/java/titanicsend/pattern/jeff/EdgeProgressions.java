package titanicsend.pattern.jeff;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;
import heronarts.lx.blend.MultiplyBlend;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.Click;
import heronarts.lx.parameter.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import titanicsend.color.TEColorType;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEAudioPattern;

/**
 * Loads a JSON file of scenes. A scene is a set of edges to light.
 *
 * <p>The scene list is a sequence that can repeat scenes. The pattern steps through them on a
 * tempoDivision (such as every beat or measure), or when triggered by audio, or via manual scene
 * selection. In Manual mode, the current scene can be advanced by a trigger (which can be linked to
 * a MIDI note or triggered by the beat modulator).
 *
 * <p>By default, loading an edgeSet will also include all symmetric edges found in the model, this
 * can be overridden in the JSON file with `"includeSymmetric": false`.
 *
 * <p>The resulting edges can be palette linked; when used in a channel with a Multiply blendMode,
 * it automatically produces a B&W mask.
 */
@LXCategory("Geometry Masks")
public class EdgeProgressions extends TEAudioPattern {
  public final LinkedColorParameter colorParam =
      registerColor(
          "Color",
          "color",
          TEColorType.PRIMARY,
          "Primary color for edges, when not in auto-mask mode");

  // In this pattern the "energy" is how quickly the scenes can progress,
  // IE shorter tempoDivisions
  Tempo.Division[] divisions;
  public ObjectParameter<Tempo.Division> tempoDivision;

  // Click modulator to trigger scene advance in tempo mode
  public final Click tempoDivisionClick = new Click("Tempo Division", lx.engine.tempo.period);

  // Manual mode can have the trigger parameter linked to MIDI or a beatDetect
  public enum Mode {
    TEMPO,
    BASS,
    MANUAL
  }

  public final EnumParameter<Mode> triggerMode =
      new EnumParameter<>("Mode", Mode.TEMPO)
          .setDescription("What advances the scene. Manual can also be MIDI or BeatMod.");

  public final DiscreteParameter sceneSelect =
      new DiscreteParameter("Scene", 0, 1).setDescription("Manual scene selection");

  // Can link this to beatModulator or Midi notes
  public final BooleanParameter trigger =
      new BooleanParameter("Trigger")
          .setDescription("Manual advance. Can be MIDI or beatMod mapped.")
          .setMode(BooleanParameter.Mode.MOMENTARY);

  public final BooleanParameter downbeat =
      new BooleanParameter("Beat1")
          .setDescription("Reset sequence to a downbeat (multiple of 16)")
          .setMode(BooleanParameter.Mode.MOMENTARY);

  // Collection of edges that should be on based on current state
  protected Set<TEEdgeModel> litEdges = new HashSet<>();

  /*  Holds the sequence of scenes (a scene is a named set of edges).
   *  Using a list allows us to repeat showing a particular scene.
   *  It's suggested to add scenes in aesthetic groups of 4, 8, or 16.
   *  This may be extended to a state machine later, or collections of
   *  related scenes.
   **/
  protected List<String> scenes = new ArrayList<>();
  protected static int SCENE_GROUP_SIZE = 16;

  // Map of name->Set<edges> loaded from a JSON file that contains all
  // defined scenes.
  protected HashMap<String, Set<TEEdgeModel>> edgeSets = new LinkedHashMap<>();

  public EdgeProgressions(LX lx) {
    super(lx);
    addDivisionParam();
    triggerMode.setWrappable(false);
    addParameter("triggerMode", triggerMode);

    addParameter("sceneSelect", sceneSelect);
    addParameter("trigger", trigger);
    addParameter("downbeat", downbeat);

    tempoDivisionClick.tempoSync.setValue(true);
    tempoDivisionClick.tempoDivision.setValue(tempoDivision.getValue());
    startModulator(tempoDivisionClick);
    tempoDivision.bang(); // Initialize the beatRetrigger threshold

    loadScenes();
    sceneSelect.setRange(0, scenes.size());
    setEdges();
  }

  private void addDivisionParam() {
    divisions =
        new Tempo.Division[] {
          Tempo.Division.EIGHT,
          Tempo.Division.FOUR,
          Tempo.Division.DOUBLE,
          Tempo.Division.WHOLE,
          Tempo.Division.HALF,
          Tempo.Division.QUARTER,
          Tempo.Division.EIGHTH,
          Tempo.Division.SIXTEENTH
        };

    tempoDivision =
        new ObjectParameter<>("Energy", divisions, divisions[5])
            .setDescription("Advance rate in tempo mode, retrigger threshold in audio mode.");
    tempoDivision.setWrappable(false);

    addParameter("tempoDivision", tempoDivision);
  }

  // TODO Remove when Click fix referenced below is available
  double lastClickBasis;

  public void runTEAudioPattern(double deltaMs) {
    int color = colorParam.getColor();
    if (getChannel() != null) {
      if (getChannel().blendMode.getObject().getClass().equals(MultiplyBlend.class)) {
        // Operate in Mask mode
        setEdges(LXColor.BLACK);
        color = LXColor.WHITE;
      } else {
        clearEdges();
      }
    }

    // TODO Swap when fix
    // https://github.com/heronarts/LX/commit/c10fba3a15766bd9441506a09e3e27e938f9b4e0
    // is in our snapshot version of 0.4.1.
    // if (triggerMode.getEnum().equals(Mode.TEMPO) && tempoDivisionClick.click()) nextScene();
    if (triggerMode.getEnum().equals(Mode.TEMPO) && tempoDivisionClick.getBasis() < lastClickBasis)
      nextScene();
    lastClickBasis = tempoDivisionClick.getBasis();
    if (triggerMode.getEnum().equals(Mode.BASS) && bassHit()) nextScene();

    for (TEEdgeModel edge : litEdges) {
      for (LXPoint point : edge.points) {
        colors[point.index] = color;
      }
    }
  }

  public static final class EdgeSet {
    private final String name;
    private final Boolean includeSymmetric;
    private final List<String> edgeIds;
    private static int instanceCount = 0;

    // Gson will override fields using reflection, if defined in json
    public EdgeSet() {
      name = "anonSet" + ++instanceCount;
      includeSymmetric = true;
      edgeIds = new ArrayList<>();
    }
  }

  protected void loadScenes() {
    Gson gson = new Gson();
    JsonReader reader = new JsonReader(loadFile("resources/pattern/edgeSets.json"));
    EdgeSet[] edgeSetData = gson.fromJson(reader, EdgeSet[].class);

    for (EdgeSet es : edgeSetData) {
      edgeSets.putIfAbsent(es.name, new HashSet<>());

      for (String edgeId : es.edgeIds) {
        if (this.modelTE.hasEdge(edgeId)) {
          TEEdgeModel edge = this.modelTE.getEdge(edgeId);
          if (es.includeSymmetric) {
            edgeSets.get(es.name).addAll(edge.symmetryGroup);
          } else {
            edgeSets.get(es.name).add(edge);
          }
        }
      }
    }

    // Future: enhance to add repeated scenes or nonlinear traversal paths
    scenes.addAll(edgeSets.keySet());
  }

  protected BufferedReader loadFile(String filename) {
    try {
      File f = new File(filename);
      return new BufferedReader(new FileReader(f));
    } catch (FileNotFoundException e) {
      throw new Error(filename + " not found below " + System.getProperty("user.dir"));
    }
  }

  protected void setEdges() {
    // LX.log("Setting edge scene: " + scenes.get(sceneSelect.getValuei()));
    litEdges = edgeSets.get(scenes.get(sceneSelect.getValuei()));
  }

  protected void nextScene() {
    sceneSelect.setValue((sceneSelect.getValue() + 1) % scenes.size());
    // This triggers setEdges via sceneSelect's onParameterChange()
  }

  @Override
  public void onActive() {
    super.onActive();

    if (triggerMode.getEnum().equals(Mode.TEMPO)) {
      ;
      lx.engine.tempo.trigger();
      sceneSelect.setValue(scenes.size());
    }
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    super.onParameterChanged(parameter);

    if (parameter.equals(tempoDivision)) {
      tempoDivisionClick.tempoDivision.setValue(tempoDivision.getObject());
      // Allow the beat detect to only trigger as fast as the tempo division
      bassRetriggerMs =
          15. / 16 * lx.engine.tempo.period.getValue() / tempoDivision.getObject().multiplier;
    }

    if (parameter.equals(sceneSelect)) {
      setEdges();
    }

    if (parameter.equals(trigger) && trigger.isOn()) {
      trigger.setValue(false);
      switch (triggerMode.getEnum()) {
        case TEMPO:
          // If tempo mode is enabled, but the user linked a beatDetect
          // modulator in manual mode, this would double advance and look bad,
          // so the manual trigger does nothing. Use tap to align phase.
          break;
        case BASS:
          // Here however, manual advance is important to align longer durations
          // between beatDetects (e.g. when each scene should be 8 beats long)
          resetBassGate();
          nextScene();
          break;
        case MANUAL:
          nextScene();
          break;
        default: // should be unreachable!
          throw new IllegalStateException("Mike Schiraldi would like to have a word with you");
      }
    }

    // Jump to the next multiple of 16
    if (parameter.equals(downbeat) && downbeat.isOn()) {
      int sceneGroupCount = scenes.size() / SCENE_GROUP_SIZE + 1;
      // Scene 0 is all-off "empty"; so index 1, 17, 33, etc. begin nice scene groups
      int sIdx = sceneSelect.getValuei();
      sIdx = (((sIdx - 1) / SCENE_GROUP_SIZE) + 1) % sceneGroupCount * SCENE_GROUP_SIZE + 1;
      sceneSelect.setValue(sIdx);

      setEdges();
      resetBassGate();
      lx.engine.tempo.trigger(); // Todo: Retest when click is fixed
    }
  }
}
