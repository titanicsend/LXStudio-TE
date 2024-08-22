package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Wrapper class for OpenGL shaders. Simplifies handling of
 * context and native memory management, and provides a
 * convenient interface for adding shaders to a pattern.
 */
public class GLShaderPattern extends TEPerformancePattern {
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

  protected GLPatternControl controlData;

  // TODO - mappedBuffer functionality not yet supported for shader patterns
  protected ByteBuffer mappedBuffer;
  protected final int mappedBufferWidth = GLEngine.getMappedBufferWidth();
  protected final int mappedBufferHeight = GLEngine.getMappedBufferHeight();

  // convenience, to simplify user setup of shader OnFrame() functions
  protected double deltaMs;

  // list of shaders to run, with associated setup functions
  protected final ArrayList<ShaderInfo> shaderInfo = new ArrayList<>();

  public GLShaderPattern(LX lx) {
    this(lx, TEShaderView.ALL_POINTS);
  }

  public GLShaderPattern(LX lx, TEShaderView view) {
    super(lx, view);
    controlData = new GLPatternControl(this);
  }

  public GLPatternControl getControlData() {
    return controlData;
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
    addShader(new GLShader(lx, shaderName, getControlData(), textureFilenames));
  }

  // Add a shader by fragment shader filename, with an OnFrame() function.
  public void addShader(String shaderName, GLShaderFrameSetup setup) {
    addShader(new GLShader(lx, shaderName, controlData), setup);
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    ShaderInfo s = null;
    this.deltaMs = deltaMs;

    int n = shaderInfo.size();

    // run the chain of shaders, except for the last one,
    // copying the output of each to the next shader's input texture
    for (int i = 0; i < n; i++) {
      s = shaderInfo.get(i);
      s.shader.useProgram();
      s.setup.OnFrame(s.shader);
      s.shader.run();
    }

    // paint the final shader output to the car
    ShaderPainter.mapToPointsDirect(getModel().getPoints(), s.shader.getImageBuffer(),getColors());
  }

  @Override
  public void onActive() {
    // fix exception on slow startup
    if (this.colors == null) return;
    super.onActive();
    for (ShaderInfo s : shaderInfo) {
      s.shader.onActive();
    }
  }

  @Override
  public void onInactive() {
    // fix exception on slow startup
    if (this.colors == null) return;
    super.onInactive();
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
