package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import titanicsend.effect.TEEffect;

/**
 * Wrapper class for OpenGL shaders. Simplifies handling of
 * context and native memory management, and provides a
 * convenient interface for adding shaders to a pattern.
 */
public class GLShaderEffect extends TEEffect {
  public interface GLShaderFrameSetup {
    default void OnFrame(GLShader shader) {}
  }

  protected class ShaderInfo {
    protected GLShader shader;
    protected GLShaderFrameSetup setup;

    public ShaderInfo(GLShader shader, GLShaderFrameSetup setup) {
      this.shader = shader;
      this.setup = setup;
    }
  }

  protected GLEffectControl controlData;

  protected ByteBuffer imageBuffer;

  // convenience, to simplify user setup of shader OnFrame() functions
  protected double deltaMs;

  // list of shaders to run, with associated setup functions
  protected final ArrayList<ShaderInfo> shaderInfo = new ArrayList<>();

  // function to paint the final shader output to the car
  private ShaderPaintFn painter;

  public GLShaderEffect(LX lx) {
    super(lx);
    setPainter(new ShaderPaintFn() {});
    controlData = new GLEffectControl(this);
    imageBuffer = GLShader.allocateBackBuffer();
  }

  public void setPainter(ShaderPaintFn painter) {
    this.painter = painter;
  }

  // Add shader with OnFrame() function, which allows the pattern to do
  // whatever additional computation and setting of uniforms and so forth
  // before the shader runs.
  public void addShader(GLShader shader, GLShaderFrameSetup setup) {
    // add the shader and its frame-time setup function to our
    //
    shaderInfo.add(new ShaderInfo(shader, setup));
  }

  // add shader with default OnFrame() function
  public void addShader(GLShader shader) {
    addShader(shader, new GLShaderFrameSetup() {});
  }

  // Add a shader by fragment shader filename, using the default OnFrame() function.
  // The simple option for shaders that use only the default TEPerformancePattern
  // uniforms and don't require any additional computation in Java.
  public void addShader(String shaderName,      String... textureFilenames) {
    addShader(new GLShader(lx, shaderName, controlData, textureFilenames));
  }

  // Add a shader by fragment shader filename, with an OnFrame() function.
  public void addShader(String shaderName, GLShaderFrameSetup setup) {
    addShader(new GLShader(lx, shaderName, controlData), setup);
  }

  protected ByteBuffer getImageBuffer() {
    return imageBuffer;
  }

  protected void run(double deltaMs, double enabledAmount) {
    ShaderInfo s = null;
    ByteBuffer image = null;
    this.deltaMs = deltaMs;

    painter.mapToBuffer(modelTE.getPoints(), imageBuffer, colors);

    int n = shaderInfo.size();

    // run the chain of shaders, except for the last one,
    // copying the output of each to the next shader's input texture
    for (int i = 0; i < n; i++) {
      s = shaderInfo.get(i);
      s.shader.useProgram();
      s.setup.OnFrame(s.shader);
      s.shader.run();
    }

    // paint the final shader output to the car.
    painter.mapToPoints(getModel().getPoints(), s.shader.getImageBuffer(),getColors());
  }

  @Override
   protected void onEnable() {
    super.onEnable();
    for (ShaderInfo s : shaderInfo) {
      s.shader.onActive();
    }
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    for (ShaderInfo s : shaderInfo) {
      s.shader.onInactive();
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    for (ShaderInfo s : shaderInfo) {
      s.shader.dispose();
    }
  }
}

