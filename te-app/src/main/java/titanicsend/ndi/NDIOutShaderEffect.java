package titanicsend.ndi;

import heronarts.glx.ui.UI2dContainer;
import heronarts.lx.GpuDevice;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.structure.LXFixture;
import heronarts.lx.structure.LXStructure;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import titanicsend.effect.TEEffect;

@LXCategory("Titanics End")
@LXComponentName("NDI Out Shader")
public class NDIOutShaderEffect extends TEEffect
    implements GpuDevice, UIDeviceControls<NDIOutShaderEffect>, LXStructure.Listener {

  private boolean modelChanged = true;

  private final List<Output> outputs = new ArrayList<>();
  private final Map<NDIOutFixture, Output> fixtureToOutput = new HashMap<>();

  public NDIOutShaderEffect(LX lx) {
    super(lx);

    for (LXFixture fixture : this.lx.structure.fixtures) {
      addFixture(fixture);
    }
    this.lx.structure.addListener(this);
  }

  // LXStructure.Listener

  @Override
  public void fixtureAdded(LXFixture fixture) {
    addFixture(fixture);
  }

  @Override
  public void fixtureRemoved(LXFixture fixture) {
    removeFixture(fixture);
  }

  @Override
  public void fixtureMoved(LXFixture fixture, int index) {}

  // Tracking of Fixtures

  private void addFixture(LXFixture fixture) {
    if (fixture instanceof NDIOutFixture ndiFixture) {
      Output output = new Output(ndiFixture);
      this.outputs.add(output);
      this.fixtureToOutput.put(ndiFixture, output);
    }
  }

  private void removeFixture(LXFixture fixture) {
    if (fixture instanceof NDIOutFixture ndiFixture) {
      Output output = this.fixtureToOutput.remove(ndiFixture);
      if (output != null) {
        this.outputs.remove(output);
        output.dispose();
      }
    }
  }

  @Override
  protected void onModelChanged(LXModel model) {
    super.onModelChanged(model);
    this.modelChanged = true;
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    // Update the model coords texture only when changed (and the first run)
    if (this.modelChanged) {
      this.modelChanged = false;
      LXModel m = getModel();
      for (Output output : this.outputs) {
        output.modelChanged(m);
      }
    }

    for (Output o : this.outputs) {
      o.shader.run();
    }
  }

  // Incoming texture handle to the effect
  private int iDst = 0;

  public void setDst(int iDst) {
    for (Output output : this.outputs) {
      output.shader.setDst(iDst);
    }
    this.iDst = iDst;
  }

  public int getRenderTexture() {
    // The shader output texture is not for the LX signal path. It is for sending.
    return this.iDst;
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    for (Output output : this.outputs) {
      output.shader.onActive();
    }
  }

  @Override
  protected void onDisable() {
    for (Output output : this.outputs) {
      output.shader.onInactive();
    }
    super.onDisable();
  }

  @Override
  public void dispose() {
    this.lx.structure.removeListener(this);
    for (Output output : this.outputs) {
      output.shader.dispose();
    }
    super.dispose();
  }

  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, NDIOutShaderEffect device) {
    uiDevice.setLayout(UI2dContainer.Layout.VERTICAL, 2);

    // TODO: display list of the NDI Out Fixtures that were detected
  }

  /** Wraps a NDIOutFixture from the runtime Fixtures list and creates a shader to match. */
  private class Output {

    private final NDIOutFixture fixture;

    private final NDIOutShader shader;

    private int width;
    private int height;

    private final LXParameterListener labelListener =
        (p) -> {
          refreshLabel();
        };

    private Output(NDIOutFixture fixture) {
      this.fixture = fixture;
      this.width = fixture.widthPixels.getValuei();
      this.height = fixture.heightPixels.getValuei();

      // Create a new NDI Out Shader to go with this NDI Out Fixture
      this.shader = new NDIOutShader(lx, this.width, this.height);
      refreshOffset();
      refreshLabel();

      this.fixture.stream.addListener(this.labelListener);

      // If LXEffect was already running, activate shader
      if (isEnabled()) {
        shader.onActive();
      }
    }

    void refreshOffset() {
      int offset = this.fixture.points.getFirst().index;
      this.shader.setNdiOffsetPixels(offset);
    }

    void refreshLabel() {
      String label = this.fixture.stream.getString();
      if (!LXUtils.isEmpty(label)) {
        this.shader.setNdiStreamLabel(label);
      }
    }

    private void modelChanged(LXModel model) {
      this.shader.setModelCoordinates(model);

      // If our fixture size changed, set the NDI stream resolution to match
      int width = fixture.widthPixels.getValuei();
      int height = fixture.heightPixels.getValuei();
      if (this.width != width || this.height != height) {
        this.width = width;
        this.height = height;
        this.shader.setNdiResolution(width, height);
      }

      // Double-check buffer offset for this fixture's points
      refreshOffset();
    }

    private void dispose() {
      this.shader.dispose();
      this.fixture.stream.removeListener(this.labelListener);
    }
  }
}
