package titanicsend.effect;

import heronarts.lx.GpuDevice;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXModel;
import titanicsend.ndi.NDIOutShader;

@LXCategory("Titanics End")
public class NDIOutShaderEffect extends TEEffect implements GpuDevice {

  final NDIOutShader shader;

  private boolean modelChanged = true;

  public NDIOutShaderEffect(LX lx) {
    super(lx);

    this.shader = new NDIOutShader(lx);
  }

  public void setDst(int iDst) {
    this.shader.setDst(iDst);
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    // Update the model coords texture only when changed (and the first run)
    if (this.modelChanged) {
      this.modelChanged = false;
      LXModel m = getModel();
      this.shader.setModelCoordinates(m);
    }

    this.shader.run();
  }

  public int getRenderTexture() {
    return this.shader.getRenderTexture();
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    this.shader.onActive();
  }

  @Override
  protected void onDisable() {
    this.shader.onInactive();
    super.onDisable();
  }

  @Override
  public void dispose() {
    this.shader.dispose();
    super.dispose();
  }
}
