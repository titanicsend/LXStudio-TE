package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import titanicsend.effect.TEEffect;
import titanicsend.pattern.jon.VariableSpeedTimer;

/**
 * Wrapper class for OpenGL shaders. Simplifies handling of context and native memory management,
 * and provides a convenient interface for adding shaders to a pattern.
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
  private final VariableSpeedTimer iTime = new VariableSpeedTimer();
  protected ByteBuffer imageBuffer;
  protected ByteBuffer mappedBuffer;
  protected final int mappedBufferWidth = GLEngine.getMappedBufferWidth();
  protected final int mappedBufferHeight = GLEngine.getMappedBufferHeight();

  // convenience, to simplify user setup of shader OnFrame() functions
  protected double deltaMs;

  // list of shaders to run, with associated setup functions
  protected final ArrayList<ShaderInfo> shaderInfo = new ArrayList<>();

  public GLShaderEffect(LX lx) {
    super(lx);

    controlData = new GLEffectControl(this);
    imageBuffer = GLShader.allocateBackBuffer();
    mappedBuffer = GLShader.allocateMappedBuffer(mappedBufferWidth, mappedBufferHeight);
    // zero mappedBuffer
    mappedBuffer.rewind();
    for (int i = 0; i < mappedBuffer.capacity(); i++) {
      mappedBuffer.put(i, (byte) 0);
    }
  }

  // Add shader with OnFrame() function, which allows the pattern to do
  // whatever additional computation and setting of uniforms and so forth
  // before the shader runs.
  public void addShader(GLShader shader, GLShaderFrameSetup setup) {
    // add the shader and its frame-time setup function to our
    //
    ShaderInfo s = new ShaderInfo(shader, setup);
    s.shader.setMappedBuffer(mappedBuffer, mappedBufferWidth, mappedBufferHeight);
    shaderInfo.add(s);
  }

  // add shader with default OnFrame() function
  public void addShader(GLShader shader) {
    addShader(shader, new GLShaderFrameSetup() {});
  }

  // Add a shader by fragment shader filename, using the default OnFrame() function.
  // The simple option for shaders that use only the default TEPerformancePattern
  // uniforms and don't require any additional computation in Java.
  public void addShader(String shaderName, String... textureFilenames) {
    addShader(new GLShader(lx, shaderName, getControlData(), textureFilenames));
  }

  // Add a shader by fragment shader filename, with an OnFrame() function.
  public void addShader(String shaderName, GLShaderFrameSetup setup) {
    addShader(new GLShader(lx, shaderName, getControlData()), setup);
  }

  protected ByteBuffer getImageBuffer() {
    return imageBuffer;
  }

  public GLEffectControl getControlData() {
    return controlData;
  }

  public double getTime() {
    return iTime.getTime();
  }

  protected void run(double deltaMs, double enabledAmount) {
    LXModel m = getModel();
    ShaderInfo s;
    this.deltaMs = deltaMs;
    iTime.tick();

    // set up rectangular texture buffers for effects that need them
    ShaderPainter.mapToBufferDirect(m.points, imageBuffer, colors);
    ShaderPainter.mapFromLinearBuffer(
        m.points, mappedBufferWidth, mappedBufferHeight, mappedBuffer, colors);

    // run the chain of shaders, except for the last one,
    // copying the output of each to the next shader's input texture
    int n = shaderInfo.size();
    for (int i = 0; i < n; i++) {
      s = shaderInfo.get(i);
      s.shader.useViewCoordinates(m);
      s.shader.useProgram();
      s.setup.OnFrame(s.shader);
      s.shader.run();
    }

    // paint the final shader output to the car.
    ShaderPainter.mapToPointsDirect(m.points, imageBuffer, getColors());
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
